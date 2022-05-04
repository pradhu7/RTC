package com.apixio.nassembly.model;

import com.apixio.model.blob.BlobType;
import com.apixio.model.nassembly.Accessor;
import com.apixio.model.nassembly.Combiner;

public interface S3Accessor extends Accessor<Byte[]> {

    // Same input as Combiner
    BlobType getBlobType(Combiner.CombinerInput combinerInput);
}
