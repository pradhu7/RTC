package com.apixio.datasource.springjdbc;

import java.util.Map;

/**
 * Logical inverse of Spring JDBC's RowMapper and is used to pull apart a Java POJO
 * into separate fields that can be used when inserting/updating data in an RDBMS
 */
public interface RowUnmapper<T>
{
    public Map<String, Object> unmapRow(T rowObj);

}

