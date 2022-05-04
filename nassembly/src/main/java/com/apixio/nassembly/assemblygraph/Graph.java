package com.apixio.nassembly.assemblygraph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This is the main class that provides access to GraphNode
 *
 * Each graph node has complete information about that node: The list of assemblies, the data type name,
 * source and sinks if necessary, etc.
 */
public class Graph implements Serializable
{
    private Map<String, GraphNode> fromDataTypeToGraphNode;
    private AssemblyService        assemblyServices;

    public Graph()
    {
        this(new AssemblyService());
    }

    public Graph(AssemblyService assemblyServices)
    {
        this.assemblyServices = assemblyServices;
        this.assemblyServices.validateAndUpdate();

        buildGraph();
    }

    public GraphNode getGraphNode(String dataTypeName)
    {
        return fromDataTypeToGraphNode.get(dataTypeName);
    }

    public void printGraphServices()
    {
        fromDataTypeToGraphNode.forEach((String k, GraphNode v) -> System.out.println(v));
    }

    private void buildGraph()
    {
        buildGraphNodes();
        buildGraphEdges();
    }

    private void buildGraphNodes()
    {
        fromDataTypeToGraphNode = new HashMap<>();

        assemblyServices.getExchanges().forEach(exchange ->
        {
            GraphNode graphNode = new GraphNode(exchange.getDataTypeName(), true, assemblyServices);
            fromDataTypeToGraphNode.put(exchange.getDataTypeName(), graphNode);
        });

        assemblyServices.getGarbageCollectors().forEach(garbageCollector ->
        {
            if (!fromDataTypeToGraphNode.containsKey(garbageCollector.getDataTypeName()))
            {
                GraphNode graphNode = new GraphNode(garbageCollector.getDataTypeName(), false, assemblyServices);
                fromDataTypeToGraphNode.put(garbageCollector.getDataTypeName(), graphNode);
            }
        });

        assemblyServices.getCombines().forEach(combine ->
        {
            if (!fromDataTypeToGraphNode.containsKey(combine.getDataTypeName()))
            {
                GraphNode graphNode = new GraphNode(combine.getDataTypeName(), false, assemblyServices);
                fromDataTypeToGraphNode.put(combine.getDataTypeName(), graphNode);
            }
        });

        assemblyServices.getIndexers().forEach(indexer -> {
            if (!fromDataTypeToGraphNode.containsKey(indexer.getDataTypeName())) {
                GraphNode gn = new GraphNode(indexer.getDataTypeName(), false, assemblyServices);
                fromDataTypeToGraphNode.put(indexer.getDataTypeName(), gn);
            }
        });
    }

    private void buildGraphEdges()
    {
        assemblyServices.getGarbageCollectors().forEach(garbageCollector ->
        {
            GraphNode      gcNode = fromDataTypeToGraphNode.get(garbageCollector.getDataTypeName());
            Stream<String> inputs    = garbageCollector.getInputDataTypeNames().stream();
            inputs.forEach(dt ->
            {
                GraphNode gn = fromDataTypeToGraphNode.get(dt);
                if (gn != null)
                {
                    gcNode.addGarbageCollectorSourceNode(gn);
                    gn.addGarbageCollectorSinkNode(gcNode);
                }
            });
        });

        assemblyServices.getCombines().forEach(combine ->
        {
            GraphNode   combineNode = fromDataTypeToGraphNode.get(combine.getDataTypeName());
            Set<String> inputs      = combine.getInputDataTypeNames();
            inputs.forEach(dt ->
            {
                GraphNode gn = fromDataTypeToGraphNode.get(dt);
                if (gn != null)
                {
                    combineNode.addCombineSourceNode(gn);
                    gn.addCombineSinkNode(combineNode);
                }
            });
        });
    }
}