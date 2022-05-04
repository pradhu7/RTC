package com.apixio.datasource.springjdbc;

import com.apixio.datasource.springjdbc.Binding;

/**
 * Production of the SQL for a minimal SQL UPDATE requires both the produced
 * SQL string along with the bindings for the actual name=value pairs
 */
public class SqlStatement
{
    public String  sql;
    public Binding binding;

    public SqlStatement(String sql, Binding binding)
    {
        this.sql     = sql;
        this.binding = binding;
    }
}

