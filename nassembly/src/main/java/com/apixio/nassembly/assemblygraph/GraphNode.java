package com.apixio.nassembly.assemblygraph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.apixio.model.nassembly.*;
import com.apixio.nassembly.locator.AssemblyLocator;
import com.google.protobuf.Descriptors.Descriptor;

public class GraphNode implements Serializable
{
    private String      dataTypeName;
    private String      domainName;
    private Descriptor  descriptor;

    private GarbageCollector garbageCollector;
    private Combiner    combiner;
    private CPersistable cPersistable;
    private BlobPersistable blobPersistable;
    private Indexer indexer;

    private Set<GraphNode> garbageCollectorSourceNodes;
    private Set<GraphNode> garbageCollectorSinkNodes;
    private Set<GraphNode> combineSourceNodes;
    private Set<GraphNode> combineSinkNodes;

    // for convenience
    private boolean hasExchange;
    private boolean hasGarbageCollector;
    private boolean hasCombiner;
    private boolean hasCPersistable;
    private boolean hasBlobPersistable;
    private boolean hasIndexer;

    // Also, for convenience
    private Set<String> garbageCollectorSourceDataTypes;
    private Set<String> garbageCollectorSinkDataTypes;
    private Set<String> combineSourceDataTypes;
    private Set<String> combineSinkDataTypes;


    GraphNode(String dataTypeName, boolean hasExchange, AssemblyService assemblyServices)
    {
        setGraphNode(dataTypeName, hasExchange, assemblyServices);
        initializeGarbageCollectorAndCombineNodes();
    }

    private void setGraphNode(String dataTypeName, boolean hasExchange, AssemblyService assemblyServices)
    {
        this.dataTypeName = dataTypeName;
        this.hasExchange  = hasExchange;
        this.domainName = hasExchange ? assemblyServices.getExchange(dataTypeName).getDomainName() : null;
        this.descriptor = hasExchange ? assemblyServices.getExchange(dataTypeName).getDescriptor() : null;

        garbageCollector = assemblyServices.getGarbageCollector(dataTypeName);
        combiner     = assemblyServices.getCombine(dataTypeName);
        cPersistable = assemblyServices.getCPersistable(dataTypeName);
        blobPersistable = assemblyServices.getBlobPersistable(dataTypeName);
        indexer      = assemblyServices.getIndexer(dataTypeName);

        hasGarbageCollector = garbageCollector != null;
        hasCombiner = combiner != null;
        hasCPersistable = cPersistable != null;
        hasBlobPersistable = blobPersistable != null;

        hasIndexer = indexer != null;
    }

    private void initializeGarbageCollectorAndCombineNodes()
    {
        garbageCollectorSourceNodes = new HashSet<>();
        garbageCollectorSinkNodes = new HashSet<>();
        combineSourceNodes = new HashSet<>();
        combineSinkNodes   = new HashSet<>();

        // for convenience
        garbageCollectorSourceDataTypes = new HashSet<>();
        garbageCollectorSinkDataTypes = new HashSet<>();
        combineSourceDataTypes = new HashSet<>();
        combineSinkDataTypes   = new HashSet<>();
    }

    /**
     * Getters
     * @return
     */

    public String getDataTypeName()
    {
        return dataTypeName;
    }
    public String getDomainName()
    {
        return domainName;
    }
    public Descriptor getDescriptor()
    {
        return descriptor;
    }
    public boolean hasExchange()
    {
        return hasExchange;
    }

    public Exchange getExchange()
    {
        return AssemblyLocator.getExchange(dataTypeName); // since it is pure data class
    }
    public GarbageCollector getGarbageCollector()
    {
        return garbageCollector;
    }
    public Combiner getCombiner()
    {
        return combiner;
    }
    public CPersistable getCPersistable()
    {
        return cPersistable;
    }
    public BlobPersistable getBlobPersistable() { return blobPersistable; }
    public Indexer getIndexer() {
        return indexer;
    }

    public boolean hasGarbageCollector()
    {
        return hasGarbageCollector;
    }

    public boolean hasCombiner()
    {
        return hasCombiner;
    }

    public boolean hasCPersistable()
    {
        return hasCPersistable;
    }

    public boolean hasBlobPersistable() { return hasBlobPersistable; }

    public boolean hasIndexer() {
        return hasIndexer;
    }

    public Set<GraphNode> getGarbageCollectorSourceNodes()
    {
        return garbageCollectorSourceNodes;
    }
    public Set<String> getGarbageCollectorSourceDataTypes()
    {
        return garbageCollectorSourceDataTypes;
    }
    public GraphNode getGarbageCollectorSourceNode(String dataTypeName)
    {
        return garbageCollectorSourceNodes.stream().filter(gn -> gn.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    public Set<GraphNode> getGarbageCollectorSinkNodes()
    {
        return garbageCollectorSinkNodes;
    }
    public Set<String> getGarbageCollectorSinkDataTypes()
    {
        return garbageCollectorSinkDataTypes;
    }
    public GraphNode getGarbageCollectorSinkNode(String dataTypeName)
    {
        return garbageCollectorSinkNodes.stream().filter(gn -> gn.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    public Set<GraphNode> getCombineSourceNodes()
    {
        return combineSourceNodes;
    }
    public Set<String> getCombineSourceDataTypes()
    {
        return combineSourceDataTypes;
    }
    public GraphNode getCombineSourceNode(String dataTypeName)
    {
        return combineSourceNodes.stream().filter(gn -> gn.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    public Set<GraphNode> getCombineSinkNodes()
    {
        return combineSinkNodes;
    }
    public Set<String> getCombineSinkDataTypes()
    {
        return combineSinkDataTypes;
    }
    public GraphNode getCombineSinkNode(String dataTypeName)
    {
        return combineSinkNodes.stream().filter(gn -> gn.getDataTypeName().equalsIgnoreCase(dataTypeName)).findAny().orElse(null);
    }

    void addGarbageCollectorSourceNode(GraphNode node)
    {
        this.garbageCollectorSourceNodes.add(node);
        this.garbageCollectorSourceDataTypes.add(node.dataTypeName);
    }
    void addGarbageCollectorSinkNode(GraphNode node)
    {
        this.garbageCollectorSinkNodes.add(node);
        this.garbageCollectorSinkDataTypes.add(node.dataTypeName);
    }
    void addCombineSourceNode(GraphNode node)
    {
        this.combineSourceNodes.add(node);
        this.combineSourceDataTypes.add(node.dataTypeName);
    }
    void addCombineSinkNode(GraphNode node)
    {
        this.combineSinkNodes.add(node);
        this.combineSinkDataTypes.add(node.dataTypeName);
    }

    @Override
    public String toString()
    {
        return "GraphNode{" +
                "dataTypeName=" + dataTypeName +
                ", domainName=" + ( (domainName == null) ? "emptyDomainName" : domainName ) +
                ", hasExchange=" + hasExchange +
                ", hasGarbageCollector=" + (garbageCollector != null) +
                ", hasCombiner=" + (combiner != null) +
                ", hasPersistable=" + (cPersistable != null) +
                ", hasIndexer=" + (hasIndexer) +
                ", garbageCollectorSourceNodes=" + ( (garbageCollectorSourceNodes == null) ? "emptySourceNodes" : toShortString("source", garbageCollectorSourceNodes) ) +
                ", garbageCollectorSinkNodes=" + ( (garbageCollectorSinkNodes == null) ? "emptySinkNodes" : toShortString("sink", garbageCollectorSinkNodes) ) +
                ", combineSourceNodes=" + ( (combineSourceNodes == null) ? "emptySourceNodes" : toShortString("source", combineSourceNodes) ) +
                ", combineSinkNodes=" + ( (combineSinkNodes == null) ? "emptySinkNodes" : toShortString("sink", combineSinkNodes) ) +
                "}";
    }

    private String toShortString(String sourceOrSink, Set<GraphNode> nodes)
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<").append(sourceOrSink).append("=");
        buffer.append(nodes.stream().map(n -> n.dataTypeName).collect(Collectors.joining(";")));
        buffer.append(">");

        return buffer.toString();
    }

}