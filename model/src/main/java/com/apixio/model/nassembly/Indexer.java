package com.apixio.model.nassembly;

import com.apixio.model.indexing.IndexedField;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: Can Indexer extend Exchange Directly?
public interface Indexer<T extends Exchange> extends Base {

    //TODO: define functions needed to index a specific data type
    //this should include ngram logic, fields to index, etc
    Set<IndexedField> getIndexedFields(T exchange);

    /**
     * True if we want to trigger persist the data in a separate batch job
     * False if we want to persist to S3 during in-line computation
     */
    default boolean deferPersist(){return true;}
}
