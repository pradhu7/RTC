package com.apixio.model.nassembly;

import com.apixio.model.blob.BlobType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: Can we extend Exchange?
public interface BlobPersistable extends Base {

    default BlobType getBlobType(Exchange exchange){throw new UnsupportedOperationException("Not yet Implemented");}

    default byte[] getSerializedValue(Exchange exchange){throw new UnsupportedOperationException("Not yet Implemented");}

    /**
     * True if we want to trigger persist the data in a separate batch job
     * False if we want to persist to S3 during in-line computation
     */
    default boolean deferPersist(){return true;}
}
