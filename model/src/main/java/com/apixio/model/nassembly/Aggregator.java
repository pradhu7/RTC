package com.apixio.model.nassembly;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Basic class for aggregating data together
public interface Aggregator {
    // Order Matters!
    default TransformationMeta[] getPreProcesses() {
        return new TransformationMeta[]{};
    }

    default Set<String> getGroupIds() {
        return Stream.of(Base.Cid).collect(Collectors.toSet());
    }

    // Order Matters!
    default TransformationMeta[] getPostProcesses() {
        return new TransformationMeta[]{};
    }
}