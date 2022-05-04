package com.apixio.nassembly.valid;

import com.apixio.model.nassembly.*;
import com.apixio.nassembly.assemblygraph.AssemblyService;
import com.apixio.nassembly.assemblygraph.Graph;
import com.apixio.nassembly.assemblygraph.GraphNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidationServicesTest
{
    @Before
    public void setUp()
    {
    }

    @Test
    public void validGraph()
    {
        List<CPersistable> cPersistables = new ArrayList<>();
        List<BlobPersistable> blobPersistables = new ArrayList<>();

        List<Exchange> exchanges = new ArrayList<>(Arrays.asList(new ExchangeTest1(), new ExchangeTest2()));
        List<GarbageCollector> merges = new ArrayList<>(Arrays.asList(new MergeTest()));
        List<Combiner> combines = new ArrayList<>(Arrays.asList(new CombineTest()));
        List<Indexer> indexers =  new ArrayList<>(Arrays.asList(new IndexerTest()));

        System.out.println(exchanges.size());
        System.out.println(merges.size());
        System.out.println(combines.size());
        System.out.println(indexers.size());
        System.out.println();

        AssemblyService assemblyServices = new AssemblyService(cPersistables, blobPersistables, exchanges, merges, combines, indexers);
        Graph gs = new Graph(assemblyServices);

        gs.printGraphServices();

        Assert.assertNotNull(gs.getGraphNode(exchanges.get(0).getDataTypeName()));
        Assert.assertNotNull(gs.getGraphNode(exchanges.get(1).getDataTypeName()));
        Assert.assertNotNull(gs.getGraphNode(merges.get(0).getDataTypeName()));
        Assert.assertNotNull(gs.getGraphNode(combines.get(0).getDataTypeName()));
        Assert.assertNotNull(gs.getGraphNode(indexers.get(0).getDataTypeName()));

        GraphNode gn = gs.getGraphNode(exchanges.get(0).getDataTypeName());
        Assert.assertNull(gn.getCombiner());
        Assert.assertNotNull(gn.getGarbageCollector());
        Assert.assertNull(gn.getCPersistable());
        Assert.assertTrue(gn.hasExchange());

        GraphNode gn1 = gs.getGraphNode(exchanges.get(1).getDataTypeName());
        Assert.assertNotNull(gn1.getCombiner());
        Assert.assertNull(gn1.getGarbageCollector());
        Assert.assertNull(gn1.getCPersistable());
        Assert.assertTrue(gn1.hasExchange());

        GraphNode gn2 = gs.getGraphNode(merges.get(0).getDataTypeName());
        Assert.assertNull(gn2.getCombiner());
        Assert.assertNotNull(gn2.getGarbageCollector());
        Assert.assertNull(gn2.getCPersistable());
        Assert.assertTrue(gn2.hasExchange());

        GraphNode gn3 = gs.getGraphNode(combines.get(0).getDataTypeName());
        Assert.assertNotNull(gn3.getCombiner());
        Assert.assertNull(gn3.getGarbageCollector());
        Assert.assertNull(gn3.getCPersistable());
        Assert.assertTrue(gn3.hasExchange());

        GraphNode gn4 = gs.getGraphNode(indexers.get(0).getDataTypeName());
        Assert.assertNotNull(gn4.getIndexer());
    }
}
