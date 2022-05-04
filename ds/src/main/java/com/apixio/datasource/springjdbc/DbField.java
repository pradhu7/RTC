package com.apixio.datasource.springjdbc;

import java.util.function.Function;

/**
 * DbField is an experimental/proof-of-concept pattern for supporting minimal-field updates
 * of database rows.  DbField instances are collected into a DbTable, and the DbTable instance
 * can tranlate from a modified POJO entity into a SQL Update statement that includes only
 * fields that were actually marked as modified on the POJO.  This does require that the
 * POJO class support the tracking of field modifications via an EnumSet.
 *
 * Basically a DbField associates three bits of information into a single entity:
 *
 *  1.  the actual column name in the database table for the field
 *  2.  the Enum-based identifier of the POJO field (defined by the POJO class)
 *  3.  the mechanism to extract/get the (modified) field value from the POJO
 *
 * Note that in order to support the possible transformation of the value returned from the
 * POJO, a more general mechanism (via the Extractor interface) is needed.  The less-general
 * but easier to define mechanism is via Java8+ method reference (Class::methodname) but requires
 * that the value retrieved from the Function.apply() method be what is pushed into the SQL
 * update statement.
 *
 * The class methods below reflect these two value extraction mechanisms.
 */
public class DbField<E,T>
{
    /**
     * Required values
     */
    private String colName;           // column name in db
    private E      fieldEnum;         // 'enum' that identifies field in POJO

    /**
     * Only 1 is needed.
     */
    private Function<T,Object> getter;
    private Extractor<T>       extractor;

    /**
     * The generalized way of getting a value from the POJO; this mechanism allows
     * transformation of the value retrieved from the POJO.
     */
    public interface Extractor<T>
    {
        public Object get(T pojo);
    }

    public static <E,T> DbField viaMethodReference(String colName, E fieldEnum, Function<T,Object> getter)
    {
        DbField field = new DbField();

        field.colName   = colName;
        field.fieldEnum = fieldEnum;
        field.getter    = getter;

        return field;
    }

    public static <E,T> DbField viaExtractor(String colName, E fieldEnum, Extractor<T> extractor)
    {
        DbField field = new DbField();

        field.colName   = colName;
        field.fieldEnum = fieldEnum;
        field.extractor = extractor;

        return field;
    }

    /**
     * Note these are package-level access.
     */
    String getColumnName()
    {
        return colName;
    }

    E getFieldEnum()
    {
        return fieldEnum;
    }

    Object extract(T pojo)
    {
        if (extractor != null)
            return extractor.get(pojo);
        else
            return getter.apply(pojo);
    }

}
