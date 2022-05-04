package com.apixio.datasource.springjdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple SQL query builder utility class that can help manage the dynamic construction of
 * SQL queries, e.g., where a term in the "where" clause needs to be conditionally present
 * based on caller parameters.
 *
 * This class makes sense to use only if there's dynamic inclusion of conditions.  If a SQL
 * query will always have a given condition(s), then a hardcoded SQL query will be easier
 * to manage.
 *
 * An example use of it is:
 *
 *   Query q = new Query("select * from table");
 *
 *   q.where(q.like("name", theName + "%"));
 *
 *   entitiesDS.loadEntities(q);
 *
 * or:
 *
 *   Query q = new Query("select * from table");
 *
 *   q.where(q.equals("type", type)).and(q.equals("owner", owner)).orderBy("created_dt");
 *
 */
public class Query
{
    /**
     * The model is that a query can contain a list of conditions and this is the base
     * class of the different types of conditions.
     */
    private abstract class Condition
    {
        abstract String toSql();
    }

    /**
     * Supports the construct of "field = value"
     */
    private class FieldEqualsCondition extends Condition
    {
        private String fieldName;
        private String varName;

        FieldEqualsCondition(String field, Object value)
        {
            fieldName = field;
            varName   = "_" + field;

            binding.addByName(varName, value);
        }

        String toSql()
        {
            return fieldName + " = :" + varName;
        }
    }

    /**
     * Supports the construct of "textfield like pattern"
     */
    private class FieldLikeCondition extends Condition
    {
        private String fieldName;
        private String varName;

        FieldLikeCondition(String field, String pattern)
        {
            fieldName = field;
            varName   = "_" + field;

            binding.addByName(varName, pattern);
        }

        String toSql()
        {
            return fieldName + " like :" + varName;
        }
    }

    /**
     * Supports the construct of "intfield in (v1, v2, ...)"
     */
    private class InIntListCondition extends Condition
    {
        private String        fieldName;
        private List<Integer> values;

        InIntListCondition(String field, List<Integer> values)
        {
            this.fieldName = field;
            this.values    = values;
        }

        String toSql()
        {
            return fieldName + " in (" + values.stream().map(x -> Integer.toString(x)).collect(Collectors.joining(",")) + ")";
        }
    }

    /**
     * Supports the construct of "textfield in ('v1', 'v2', ...)"
     */
    private class InStringListCondition extends Condition
    {
        private String       fieldName;
        private List<String> values;

        InStringListCondition(String field, List<String> values)
        {
            this.fieldName = field;
            this.values    = values;
        }

        String toSql()
        {
            return fieldName + " in (" + values.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")) + ")";  // warning:  cheap protection of strings
        }
    }

    /**
     * Supports the construct of "field in (select something from ...)"
     */
    private class InSubqueryCondition extends Condition
    {
        private String   fieldName;
        private Query   subquery;

        InSubqueryCondition(String field, Query subquery)
        {
            this.fieldName = field;
            this.subquery  = subquery;
        }

        String toSql()
        {
            return fieldName + " in (" + subquery.toSql() + ")";
        }
    }

    /**
     * Fields
     */
    private String          base;
    private List<Condition> conditions;
    private String          orderBy;
    private Binding         binding;       // MUST be byName binding

    /**
     *
     */
    public Query(String baseSql)
    {
        this(baseSql, Binding.byName());
    }

    public Query(String baseSql, Binding binding)
    {
        this.base       = baseSql;
        this.conditions = new ArrayList<Condition>(5);
        this.binding    = binding;
    }

    /**
     * Returns the binding used when adding name=value via .equals(field, value) and .like()
     */
    public Binding getBinding()
    {
        return binding;
    }

    /**
     * Return the constructed SQL query
     */
    public String toSql()
    {
        StringBuilder sql = new StringBuilder(base);
        int           sz  = conditions.size();

        if (sz > 0)
        {
            sql.append(" where");

            for (int i = 0; i < sz; i++)
            {
                if (i > 0)
                    sql.append(" and");

                sql.append(" ");
                sql.append(conditions.get(i).toSql());
            }
        }

        if (orderBy != null)
            sql.append(orderBy);

        return sql.toString();
    }

    /**
     * Synonyms
     */
    public Query where(Condition cond)
    {
        conditions.add(cond);

        return this;
    }

    public Query and(Condition cond)
    {
        conditions.add(cond);

        return this;
    }

    /**
     *
     */
    public Query orderBy(String fieldPlus)
    {
        if (fieldPlus != null)
            orderBy = " order by " + fieldPlus;

        return this;
    }

    /**
     *
     */
    public Condition equals(String field, Object value)
    {
        return new FieldEqualsCondition(field, value);
    }

    public Condition like(String field, String pattern)
    {
        return new FieldLikeCondition(field, pattern);
    }

    public Condition inStringList(String field, List<String> values)
    {
        return new InStringListCondition(field, values);
    }

    public Condition inIntList(String field, List<Integer> values)
    {
        return new InIntListCondition(field, values);
    }

    public Condition inSubquery(String field, Query subquery)
    {
        return new InSubqueryCondition(field, subquery);
    }

    /**
     * tests
     */
    public static void main(String... args)
    {
        if (true)
        {
            Binding binding = Binding.byName();
            Query  q       = new Query("select test1", binding);

            q.where(q.equals("a", 100.));

            System.out.println("[" + q.toSql() + "]" + " with binding " + binding.getNamedParameters());
        }

        if (true)
        {
            Binding binding = Binding.byName();
            Query  q       = new Query("select test2", binding);

            q.where(q.equals("a", 100.)).and(q.equals("b", false));

            System.out.println("[" + q.toSql() + "]" + " with binding " + binding.getNamedParameters());
        }

        if (true)
        {
            Binding binding = Binding.byName();
            Query  q       = new Query("select test3", binding);

            q.where(q.inIntList("ll", java.util.Arrays.asList(new Integer[] {-5, -4, -3})));

            System.out.println("[" + q.toSql() + "]" + " with binding " + binding.getNamedParameters());
        }

        if (true)
        {
            Binding binding = Binding.byName();
            Query  q       = new Query("select test4", binding);
            Query  q2      = new Query("select id from x", binding);

            q.where(q.inSubquery("qq", q2.where(q2.equals("z", "abc"))));
            q.orderBy("something");

            System.out.println("[" + q.toSql() + "]" + " with binding " + binding.getNamedParameters());
        }

    }

}
