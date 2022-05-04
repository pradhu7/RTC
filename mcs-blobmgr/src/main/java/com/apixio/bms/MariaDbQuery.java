package com.apixio.bms;

import com.apixio.datasource.springjdbc.Binding;
import com.apixio.datasource.springjdbc.DbUtil;
import com.apixio.datasource.springjdbc.SqlStatement;
import static com.apixio.bms.MariaDbStorage.*;       // gross but possibly preferable to doing tons of static imports

/**
 * An implementation of Query that works with MariaDB (and its form of JSON support).
 */
public class MariaDbQuery extends Query
{
    /**
     * SqlGeneratorContext deals with and supports the following:
     *
     *  * creation of proper SQL for testing "regular" metadata fields and JSON fields
     *
     *  * flattening of SQL named parameters as there could be duplication of field names within the
     *    query tree; these are made unique by appending _nnn (monotonically increasing integer); this
     *    screws up PreparedStatement caching but there is no other way to support arbitrary query trees
     *
     *  * proper construction of SQL needed for "and" and "or" nodes in the query tree
     *
     * The last point needs some explanation.  Using the following conceptual query as a representative
     * "complex" and arbitrary query:
     *
     *   ((a=1 or extra.b=2) and extra.c=3) or (d=4)
     *
     * the question is how to express it correctly in (MariaDB) SQL.  The rough translation is
     *    
     *    select * from blobmgr_blobs where blob_id in
     *     ( select blob_id from blobmgr_meta where key_name='d' and int_val=4 )   -- d=4
     *     or blob_id in
     *     (
     *       select blob_id from blobmgr_blobs where blob_id in
     *         (select blob_id from blobmgr_blobs where json_extract(b.extra_meta, '$.c') = 3)  -- extra.c=3
     *    	 and blob_id in
     *    	 (select blob_id from blob
     *    <etc....>
     *
     * and it should be clear that each leaf-level test constructs one of the following:
     *
     *    (select blob_id from blobmgr_meta where key_name = :keyname and str_val = :str_val)  -- or int_val, ...
     *   --or--
     *    (select blob_id from blobmgr_blobs where json_value(extra_meta, '$.keyname') = :val)
     *
     * and that the "or" and "and" inner nodes do:
     *
     *    select blob_id from blobmgr_blobs where blob_id in (<term>)      -- "from blobmgr_blobs" required by syntax
     *      and blob_id in (<term>)                                        -- "and" or "or" operator
     *
     * and then all that is wrapped by the final selector to get actual blob data.  So the general algorithm is
     * for the leaf nodes to do the actual selection of blob_ids that match, and then the "and"/"or" inner nodes
     * just do set union and intersection as defined by the query.
     */
    class SqlGeneratorContext
    {
        Binding       binding    = Binding.byName();
        int           bindingNum = 1;
        StringBuilder clause     = new StringBuilder();

        /**
         * Adds to the named parameter binding by appending _nnn to the name and adding
         * the pair (name_nnn, val).  The new field name is returned as it needs to
         * possibly be used in SQL construction
         */
        private String addBinding(String name, Object val)
        {
            name = name + "_" + Integer.toString(bindingNum++);

            if (val instanceof Boolean)
                val = DbUtil.convertFromBoolean((Boolean) val, true);

            binding.addByName(name, val);

            return name;
        }

        /**
         * Appends a JSON-based select query to the clause.  Correct construction of
         * it, including properly quoted values, is critical because MariaDB doesn't
         * fail if things aren't just right, so debugging why no rows were returned can
         * be difficult.
         */
        private void addJsonTest(String path, Operator op, Object val, String fieldName)
        {
            String npVal = addBinding(safeString(path), val);
            String sql;

            // Operator.CONTAINS is special case as it doesn't use JSON_VALUE but JSON_CONTAINS on a list
            if (op == Operator.CONTAINS)
            {
                String ele;

                // syntax:  JSON_CONTAINS(column, value, $.pathToList)
                // note that the actual produced SQL for the value part must be
                // single-quoted and within the single-quotes the correct type
                // must be supplied--for a string this means that the actual string
                // value must have double-quotes, like this:
                //
                //   json_contains(extra_meta, '"v23"', '$.tags')
                //

                // note that until REST-level code can interpret query param values
                // as something other than a String, we'll always be getting strings
                // here...
                if (val instanceof String)
                    ele = "\"" + val + "\"";
                else
                    ele = val.toString();     // will work for int, bool, double

                sql = (String.format(
                           "select %s from %s where json_contains(%s, '%s', '$.%s')",
                           FIELD_BLOB_ID, TABLE_BLOBS,                           // select blob_id from blobmgr_meta where
                           fieldName, ele, path));                         //  json_contains(extra_meta, 'v23', '$.tags')
            }
            else
            {
                // syntax:  JSON_VALUE(column, $.pathToField)
                sql = (String.format(
                           "select %s from %s where json_value(%s, '$.%s') %s :%s",
                           FIELD_BLOB_ID, TABLE_BLOBS,                           // select blob_id from blobmgr_meta where
                           fieldName, path, op.toSql(), npVal));           //  json_value(extra_meta, '$.version') != :version_1
            }

            clause.append(sql);
        }

        /**
         * Adds a select query to the clause that uses the metadata table to select
         * matching blob IDs.
         */
        private void addMetaTest(String field, Operator op, Object val)
        {
            String npKey;    // named param for FIELD_KEY_NAME
            String npFld;    // named param for field
            String sqlField;
            String sql;

            if (val instanceof String)
                sqlField = FIELD_STR_VAL;
            else if (val instanceof Integer)
                sqlField = FIELD_INT_VAL;
            else if (val instanceof Boolean)
                sqlField = FIELD_BOOL_VAL;
            else if (val instanceof Double)
                sqlField = FIELD_DBL_VAL;
            else
                throw new IllegalStateException("Unsupported value type for " + val + "' (type " + val.getClass().getName() + ")");

            npKey = addBinding(FIELD_KEY_NAME, field);
            npFld = addBinding(sqlField, val);

            // select blob_id from blobmgr_meta where key_name = :key_name_1 and str_val = :str_val_2
            sql = (String.format(
                       "select %s from %s where %s = :%s and %s %s :%s",
                       FIELD_BLOB_ID, TABLE_META,                    // "select blob_id from blobmgr_meta where "
                       FIELD_KEY_NAME, npKey,                          // "key_name = :key_name_1 and
                       sqlField, op.toSql(), npFld));                    // " str_val != :str_val_2"

            clause.append(sql);
        }

        /**
         * Adds a select query to the clause that uses the core blobmgr table to select
         * matching blob IDs.
         */
        private void addCoreTest(Query.CoreField field, Operator op, Object val)
        {
            String sqlField = MariaDbStorage.cCoreMap.get(field);
            String npFld;    // named param for field
            String sql;

            npFld = addBinding(sqlField, val);

            // select blob_id from blobmgr_meta where key_name = :key_name_1 and str_val = :str_val_2
            sql = (String.format(
                       "select %s from %s where %s %s :%s",
                       FIELD_BLOB_ID, TABLE_BLOBS,                   // "select blob_id from blobmgr_blobs where "
                       sqlField, op.toSql(), npFld));                    // " str_val != :str_val_2"

            clause.append(sql);
        }

        /**
         * Top-level method to add a field in all its glory to the SQL construction.  This
         * should be called only when converting a leaf node of the query tree to SQL.
         */
        void addFieldTest(String field, Operator op, Object val)
        {
            Query.CoreField core;

            if ((extra1Prefix != null) && field.startsWith(extra1Prefix))
                addJsonTest(field.substring(extra1Prefix.length()), op, val, FIELD_EXTRA1_META);
            else if ((extra2Prefix != null) && field.startsWith(extra2Prefix))
                addJsonTest(field.substring(extra2Prefix.length()), op, val, FIELD_EXTRA2_META);
            else if ((core = Query.isCore(field)) != null)
                addCoreTest(core, op, val);
            else
                addMetaTest(field, op, val);
        }
    }

    /**
     * Make a field name safe for referencing in a prepared statement via :<name>.
     * Note that we're already appending _nnn to it so we don't need to worry about
     * uniqueness.  This is only relevant to JSON_VALUE queries as it's possible to
     * query on a JSON field name that isn't a valid PreparedStatement named parameter
     */
    private static String safeString(String s)
    {
        for (int i = 0, m = s.length(); i < m; i++)
        {
            if (!Character.isJavaIdentifierStart(s.charAt(i)))
                return s.substring(0, i);
        }

        return s;
    }

    /**
     *
     */
    private MariaDbQuery(Term root, OrderBy order, Limit limit, String extra1, String extra2)
    {
        super(root, order, limit, extra1, extra2);
    }

    /**
     *
     */
    static Query build(Term root, OrderBy order, Limit limit, String extra1, String extra2)
    {
        return new MariaDbQuery(root, order, limit, extra1, extra2);
    }

    /**
     * Generates a SqlStatement for the query by delegating to the root Term
     * and wrapping it in a final "select * from ..." to grab all fields.  Notes
     * that the final SQL query is very likely to look ugly and unnecessarily
     * complex with many nested "in" subqueries; this is expected and mostly
     * unavoidable for the general query case
     */
    @Override
    public SqlStatement generateSql()
    {
        if (rootTerm instanceof All)
        {
            // this must NOT retrieve part-level blobs

            return new SqlStatement(
                String.format(
                    "select * from %s where %s is not null",
                    TABLE_BLOBS, FIELD_BLOB_UUID),
                null);
        }
        else
        {
            SqlGeneratorContext ctx = new SqlGeneratorContext();

            if (rootTerm instanceof Term)
                rootTerm = QueryBuilder.and(rootTerm);  // easy way to wrap to a full SQL query

            convertTermToSql(rootTerm, ctx);

            // this must NOT retrieve part-level blobs

            return new SqlStatement(
                String.format(
                    "select * from %s where %s in (%s) and %s is not null",
                    TABLE_BLOBS, FIELD_BLOB_ID, ctx.clause.toString(), FIELD_BLOB_UUID
                    ),
                ctx.binding);
        }
    }

    /**
     * Recursive method to append to SQL for current term
     */
    private void convertTermToSql(Term term, SqlGeneratorContext ctx)
    {
        if (term instanceof Expression)
        {
            Expression exp = (Expression) term;
            boolean    sep = false;

            ctx.clause.append(
                String.format(
                    "select %s from %s where %s in (",
                    FIELD_BLOB_ID, TABLE_META, FIELD_BLOB_ID));

            for (Term subterm : exp.terms)
            {
                if (sep)
                    ctx.clause.append(
                        String.format(
                            ") %s %s in (", exp.op.toSql(), FIELD_BLOB_ID));    // note the ")" at the beginning

                convertTermToSql(subterm, ctx);

                sep = true;
            }

            ctx.clause.append(")");
        }
        else
        {
            ctx.addFieldTest(term.field, term.op, term.val);
        }
    }

}
