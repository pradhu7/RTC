package com.apixio.nassembly.assemblygraph;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.apixio.model.nassembly.*;
import com.apixio.nassembly.locator.AssemblyLocator;

/**
 * The goal of the assembly services is to perform various validations of the implementations of assembly
 * interface and to only return valid implementation. Here are series of validations:
 *
 * 01. Check for unique assemblers. Remove duplicate DataTypes.
 * 02. Check whether classes have corresponding exchange classes.
 * 03. Check for unique exchanges. Remove duplicate exchange DataTypes.
 * 04. Check whether exchange classes have corresponding classes.
 * 05. Check for unique garbage collectors. Remove duplicate garbage collector DataTypes.
 * 06. Check whether garbage collector classes have corresponding classes.
 * 07. Check whether GarbageCollector classes have corresponding part DataTypes (GarbageCollectors are made of parts).
 * 08. Check for unique combiners. Remove duplicate meta DataTypes.
 * 09. Check whether combiners classes have corresponding classes.
 * 10. Check whether combiners classes have valid source DataTypes.
 *
 * Note:
 * - Name of DataTypes are not case sensitive
 * - In the future, we will add more validations
 */

public class AssemblyService implements Serializable
{
    private List<CPersistable> cPersistables;
    private List<BlobPersistable> blobPersistables;
    private List<Exchange>      exchanges;
    private List<GarbageCollector> garbageCollectors;
    private List<Combiner>      combines;
    private List<Indexer>       indexers;


    // This is for debugging purposes!!!
    public AssemblyService(List<CPersistable> cPersistables, List<BlobPersistable> blobPersistables, List<Exchange> exchanges, List<GarbageCollector> garbageCollectors, List<Combiner> combines, List<Indexer> indexers)
    {
        this.cPersistables = cPersistables;
        this.blobPersistables = blobPersistables;
        this.exchanges     = exchanges;
        this.garbageCollectors = garbageCollectors;
        this.combines      = combines;
        this.indexers      = indexers;
    }

    AssemblyService()
    {
        this(AssemblyLocator.allCPersistables(), AssemblyLocator.allBlobPersistables(), AssemblyLocator.allExchanges(), AssemblyLocator.allGarbageCollectors(), AssemblyLocator.allCombines(), AssemblyLocator.allIndexers());
    }

    /**
     * Getters
     *
     * @return
     */
    List<CPersistable> getCPersistables()
    {
        return cPersistables;
    }
    List<BlobPersistable> getBlobPersistables() { return blobPersistables; }
    List<Exchange> getExchanges()
    {
        return exchanges;
    }
    List<GarbageCollector> getGarbageCollectors()
    {
        return garbageCollectors;
    }
    List<Combiner> getCombines()
    {
        return combines;
    }
    List<Indexer> getIndexers() {
        return indexers;
    }

    /**
     * Return an assembler given a dataTypeName
     *
     * @return
     */
    CPersistable getCPersistable(String dataTypeName)
    {
        return cPersistables.stream().filter(a -> a.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    BlobPersistable getBlobPersistable(String dataTypeName)
    {
        return blobPersistables.stream().filter(a -> a.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    Exchange getExchange(String dataTypeName)
    {
        return exchanges.stream().filter(a -> a.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }
    GarbageCollector getGarbageCollector(String dataTypeName)
    {
        return garbageCollectors.stream().filter(a -> a.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }
    Combiner getCombine(String dataTypeName)
    {
        return combines.stream().filter(a -> a.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    //TODO: should we make these more efficient with a map of (dataTypeName -> Assembler) ??
    Indexer getIndexer(String dataTypeName) {
        return indexers.stream().filter(a -> a.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    void validateAndUpdate()
    {
        buildValidCassandraPersistables();
        buildValidBlobPersistables();
        buildValidExchanges();
        buildValidGarbageCollectors();
        buildValidCombines();
        buildValidIndexers();
    }

    private void buildValidCassandraPersistables()
    {
        DuplicateUtils.removeDuplicateCassandraPersistables(cPersistables);
    }

    private void buildValidBlobPersistables()
    {
        DuplicateUtils.removeDuplicateBlobPersistables(blobPersistables);
    }

    private void buildValidExchanges()
    {
        DuplicateUtils.removeDuplicateExchanges(exchanges);
        removeIfNoExchanges();
    }

    private void buildValidGarbageCollectors()
    {
        DuplicateUtils.removeDuplicateGarbageCollectors(garbageCollectors);
        removeGarbageCollectorsWithoutExchanges();
    }

    private void buildValidCombines()
    {
        DuplicateUtils.removeDuplicateCombines(combines);
        removeCombinesWithoutExchanges();
        removeCombinesWithoutSourceExchanges();
    }

    private void buildValidIndexers() {
        DuplicateUtils.removeDuplicateIndexers(indexers);
        removeIndexersWithoutExchanges();
    }

    private void removeIfNoExchanges()
    {
        cPersistables.removeIf(cPersistable -> !validExchange(cPersistable.getDataTypeName()));
        blobPersistables.removeIf(blobPersistable -> !validExchange(blobPersistable.getDataTypeName()));
    }

    private void removeGarbageCollectorsWithoutExchanges()
    {
        garbageCollectors.removeIf(garbageCollector -> getExchange(garbageCollector.getDataTypeName()) == null);
    }

    private void removeCombinesWithoutExchanges()
    {
        combines.removeIf(combine -> getExchange(combine.getDataTypeName()) == null);
    }

    private void removeIndexersWithoutExchanges() {
        indexers.removeIf(indexer -> getExchange(indexer.getDataTypeName()) == null);
    }

    private void removeCombinesWithoutSourceExchanges()
    {
        combines.removeIf(combine ->
        {
            return !isCombineSourceExchangesValid(combine.getInputDataTypeNames());
        });
    }

    private boolean isCombineSourceExchangesValid(Set<String> dataTypeNames)
    {
        for (String dataTypeName : dataTypeNames)
        {
            if (getExchange(dataTypeName) == null)
                return false;
        }

        return true;
    }

    private boolean metaHasCombine(String dataTypeName)
    {
        return combines.stream().anyMatch(combiner -> combiner.getDataTypeName().equalsIgnoreCase(dataTypeName));
    }

    private boolean validExchange(String dataTypeName)
    {
        return getExchange(dataTypeName) != null ? true : false;
    }
    private boolean validGarbageCollector(String dataTypeName)
    {
        return getGarbageCollector(dataTypeName) != null ? true : false;
    }
}