package com.apixio.datasource.springjdbc;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;

import com.apixio.datasource.springjdbc.Binding;

/**
 * DbTable is an experimental/proof-of-concept pattern for supporting minimal-field updates
 * of database rows.  DbField instances are collected into a DbTable, and the DbTable instance
 * can tranlate from a modified POJO entity into a SQL Update statement that includes only
 * fields that were actually marked as modified on the POJO.  This does require that the
 * POJO class support the tracking of field modifications via an EnumSet.
 *
 * IMPORTANT:  this class is now dependent on MySQL/MariaDB in createSqlUpdate() as it
 * uses "on duplicate key update ...".
 */
public class DbTable<E extends Enum<E>,T>
{
    /**
     * tableName is the actual SQL table name within the schema
     */
    private String tableName;

    /**
     * Fields are uniquely identified by the EnumSet<> and the set of modified
     * fields are also given by elements of the EnumSet, so we need to map from
     * enum to DbField
     */
    private DbField<E,T>       idField;
    private List<DbField<E,T>> fields;
    private Map<E, DbField>    fieldMap = new HashMap<>();

    /**
     * Constructs a DbTable given the SQL table name and the updateable fields within
     * that table.
     */
    public DbTable(String tableName, DbField<E,T>... fields)
    {
        this.tableName = tableName;
        this.fields    = Arrays.asList(fields);

        for (DbField<E,T> field : fields)
            fieldMap.put(field.getFieldEnum(), field);
    }

    /**
     * Setting the idField allows createSqlUpdate to produce the entire SQL statement
     * for the update rather than having the caller append the "where" clause.
     */
    public DbTable setIdField(DbField<E,T> idField)
    {
        this.idField = idField;

        return this;
    }

    /**
     * Given an actual POJO of the required Java type along with the set of modified
     * fields, produce a minimal SQL UPDATE statement that includes only those fields
     * modified.  The SQL produced can be used as a prepared statement with by-name
     * binding of parameters.
     *
     * If no fields have been modified (i.e., mods is empty) then a null is returned
     * as no actual SQL update should be done.
     */
    public SqlStatement createSqlUpdate(T obj, EnumSet<E> mods)
    {
        if ((mods == null) || mods.isEmpty())
            return null;

        StringBuilder sql     = new StringBuilder();
        Binding       binding = Binding.byName();

        sql.append("update " + tableName + " set");

        makeUpdateFields(obj, mods, sql, binding);

        if (binding.getNamedParameters().size() > 0)
        {
            if (idField != null)
            {
                sql.append(" where ");
                sql.append(idField.getColumnName());
                sql.append(" = :");
                sql.append(idField.getColumnName());

                binding.addByName(idField.getColumnName(), idField.extract(obj));
            }

            return new SqlStatement(sql.toString(), binding);
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Given an actual POJO of the required Java type create a SQL INSERT to add a new
     * row to the table.
     */
    public SqlStatement createSqlInsert(T obj)
    {
        StringBuilder sql     = new StringBuilder();
        Binding       binding = Binding.byName();
        boolean       needSep;

        sql.append("insert into " + tableName + " (");

        needSep = false;
        for (DbField field : fields)
        {
            String colName = field.getColumnName();

            if (needSep)
                sql.append(", ");

            sql.append(colName);

            binding.addByName(colName, field.extract(obj));

            needSep = true;
        }

        sql.append(") values (");

        needSep = false;
        for (DbField field : fields)
        {
            if (needSep)
                sql.append(", ");

            sql.append(":" + field.getColumnName());

            needSep = true;
        }

        sql.append(")");

        return new SqlStatement(sql.toString(), binding);
    }
    
    /**
     * Given an actual POJO of the required Java type create a SQL "upsert" statement
     * to add/replace a new row to the table.  Note that a "replace into" will NOT work
     * as it does a delete followed by an insert, which can change any "id" field.
     */
    public SqlStatement createSqlUpsert(T obj, EnumSet<E> onDup)
    {
        SqlStatement  sql    = createSqlInsert(obj);
        StringBuilder update = new StringBuilder();

        // supported by mysql & mariadb
        sql.sql += " on duplicate key update";

        makeUpdateFields(obj, onDup, update, sql.binding);

        sql.sql += update.toString();

        return sql;
    }
    
    /**
     *
     */
    private void makeUpdateFields(T obj, EnumSet<E> mods, StringBuilder sql, Binding binding)
    {
        boolean   needSep = false;

        for (E mod : mods)
        {
            DbField field = fieldMap.get(mod);

            // it's (now) okay for a modified fiag to not have a field def--this is to
            // support the case where other code wants to know about modifications but
            // the actual SQL-level update is not done here
            if (field != null)
            {
                String  colName = field.getColumnName();

                sql.append((needSep) ? ", " : " ");  // " " is important here
                sql.append(colName);
                sql.append(" = :");
                sql.append(colName);

                binding.addByName(colName, field.extract(obj));

                needSep = true;
            }
        }
    }
    
}
