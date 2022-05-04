package com.apixio.model.indexing;

import scala.Serializable;

public class IndexedField implements Indexable, Serializable {

    private final String value;
    private final IndexableType type;


    public IndexedField(String value, IndexableType type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public IndexableType getType() {
        return type;
    }
}
