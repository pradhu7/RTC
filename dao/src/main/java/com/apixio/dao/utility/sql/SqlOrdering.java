package com.apixio.dao.utility.sql;

public class SqlOrdering {

    final private String fieldName;
    final private Boolean useAsc;

    public String getFieldName() {
        return fieldName;
    }

    public Boolean getUseAsc() {
        return useAsc;
    }

    public SqlOrdering(String fieldName, Boolean useAsc){
        this.fieldName = fieldName;
        this.useAsc = useAsc;
    }
}
