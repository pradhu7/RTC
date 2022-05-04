package com.apixio.nassembly.assemblygraph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.apixio.model.nassembly.*;

/**
 * Static utility methods to remove duplicate assemblers
 */
class DuplicateUtils implements Serializable
{
    static void removeDuplicateCassandraPersistables(List<CPersistable> cPersistables)
    {
        removeDuplicates(cPersistables, (CPersistable cPersistable) -> cPersistable.getDataTypeName().toLowerCase());
    }

    static void removeDuplicateBlobPersistables(List<BlobPersistable> blobPersistables)
    {
        removeDuplicates(blobPersistables, (BlobPersistable blobPersistable) -> blobPersistable.getDataTypeName().toLowerCase());
    }

    static void removeDuplicateExchanges(List<Exchange> exchanges)
    {
        removeDuplicates(exchanges, (Exchange exchange) -> exchange.getDataTypeName().toLowerCase());
    }

    static void removeDuplicateGarbageCollectors(List<GarbageCollector> garbageCollectors)
    {
        removeDuplicates(garbageCollectors, (GarbageCollector garbageCollector) -> garbageCollector.getDataTypeName().toLowerCase());
    }

    static void removeDuplicateCombines(List<Combiner> combines)
    {
        removeDuplicates(combines, (Combiner combiner) -> combiner.getDataTypeName().toLowerCase());
    }

    static void removeDuplicateIndexers(List<Indexer> indexers) {
        removeDuplicates(indexers, (Indexer indexer) -> indexer.getDataTypeName().toLowerCase());
    }

    private static <T extends List, E> void removeDuplicates(T members, Function<E, String> lowerDataTypeNameFunction)
    {
        Map<String, Boolean> duplicateDataTypes = new HashMap<>();

        members.forEach(member ->
        {
            String lowerCaseDataTypeName = lowerDataTypeNameFunction.apply((E) member);
            if (duplicateDataTypes.containsKey(lowerCaseDataTypeName))
                duplicateDataTypes.put(lowerCaseDataTypeName, true);
            else
                duplicateDataTypes.put(lowerCaseDataTypeName, false);
        });

        members.removeIf(member -> duplicateDataTypes.get(lowerDataTypeNameFunction.apply((E) member)));
    }
}