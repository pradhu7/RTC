package com.apixio.nassembly.valid;

import com.apixio.model.indexing.IndexedField;
import com.apixio.model.nassembly.Exchange;
import com.apixio.model.nassembly.Indexer;

import java.util.Set;

public class IndexerTest implements Indexer {

    @Override
    public String getDataTypeName() {
        return "impl1";
    }

    @Override
    public Set<IndexedField> getIndexedFields(Exchange exchange) {
        return null;
    }
}
