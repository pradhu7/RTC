package com.apixio.nassembly.model;

import com.apixio.model.nassembly.*;
import combinerutils.AggregatorImpl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Merger<T extends Exchange> extends Combiner<T> {


    // Order Matters!
    default TransformationMeta[] getAggregatorPreProcesses() {
        return new TransformationMeta[]{};
    }

    default Set<String> getGroupIds() {return Stream.of(Base.Cid).collect(Collectors.toSet());}

    // Order Matters!
    default TransformationMeta[] getAggregatorPostProcesses() {
        return new TransformationMeta[]{};
    }

    T merge(List<T> assemblyExchanges);

    @Override
    default Iterable<T> combine(Combiner.CombinerInput combinerInput) {
        T[] assemblyExchanges = (T[]) combinerInput.getDataTypeNameToExchanges().get(getDataTypeName());
        return Collections.singleton(merge(Arrays.asList(assemblyExchanges)));
    }

    @Override
    default Map<String, Aggregator> fromDataTypeToAggregator() {

        Aggregator aggregator = new AggregatorImpl(this.getAggregatorPreProcesses(), this.getGroupIds(), this.getAggregatorPostProcesses());
        return new HashMap<String, Aggregator>() {{
            put(getDataTypeName(), aggregator);
        }};
    }


}
