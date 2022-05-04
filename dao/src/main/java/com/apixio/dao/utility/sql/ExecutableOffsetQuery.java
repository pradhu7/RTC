package com.apixio.dao.utility.sql;

import com.apixio.datasource.springjdbc.EntitiesDS;
import com.apixio.datasource.springjdbc.Binding;


import java.util.List;

public class ExecutableOffsetQuery<T> {
    String query;
    EntitiesDS<T> ds;
    Binding binding;

    //Executes a query against sql.
    //Uses offset and pagination to inject the LIMIT clause
    public List<T> executeQuery(Integer offset, Integer pagination) {
        String offsetQuery = String.format(this.query + " LIMIT %d OFFSET %d", pagination, offset * pagination);
        return ds.loadEntities(offsetQuery, this.binding);
    }
    public ExecutableOffsetQuery(String query, EntitiesDS<T> ds, Binding binding){
        this.query = query;
        this.ds = ds;
        this.binding = binding;
    }
}
