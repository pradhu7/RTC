package com.apixio.datasource.elasticSearch;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ElasticCrudTest {
    private static ElasticCrud elasticCrud;
    private static ElasticConnector elasticConnector;

    private static String testIndexName = "test_index_name";
    private static String testIndexAlias = "test_index_name_a";
    private static Map<String, Object> testIndexMapping;
    private static Map<String, Object> testIndexMapping1;


    @BeforeClass
    public static void setUp() throws Exception
    {
        elasticConnector = new ElasticConnector();
        elasticConnector.setHosts("10.1.133.233,10.1.129.56,10.1.136.103");
        elasticConnector.setBinaryPort(9200);
        elasticConnector.init();

        elasticCrud = new ElasticCrud();
        elasticCrud.setElasticConnector(elasticConnector);


        Map<String, Object> message = new HashMap<>();
        message.put("type", "text");
        Map<String, Object> message1 = new HashMap<>();
        message1.put("type", "date");
        Map<String, Object> properties = new HashMap<>();
        properties.put("message", message);
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("message", message);
        testIndexMapping = new HashMap<>();
        testIndexMapping.put("properties", properties);
        testIndexMapping.put("dynamic", "false");
        testIndexMapping1 = new HashMap<>();
        testIndexMapping1.put("properties", properties1);
        testIndexMapping1.put("dynamic", "false");
    }

    @Test
    public void deleteIndex()
    {
        try {
            elasticCrud.deleteIndexSync(testIndexName);
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    @Test
    public void createIndex() throws Exception
    {
        deleteIndex();
        Map<String, Object> settings  = new HashMap<String, Object>() {{
            put("index.number_of_shards", 2);
            put("index.number_of_replicas", 1);
        }};

        System.out.println(testIndexMapping);
        System.out.println(settings);

        elasticCrud.createIndexSync(testIndexName, settings, testIndexMapping, testIndexAlias);
    }

    @Test
    public void getIndexMapping() throws Exception
    {
        deleteIndex();
        createIndex();
        Map<String, Object> mappingQueried = elasticCrud.getIndexMappingSync(testIndexName);
        for (Map.Entry<String, Object> set : mappingQueried.entrySet()) {
            assertEquals(set.getValue(), testIndexMapping.get(set.getKey()));
        }
    }

    @Test
    public void updateMapping() throws Exception
    {
        deleteIndex();
        createIndex();
        elasticCrud.updateMappingSync(testIndexName, testIndexMapping1);
        Map<String, Object> mappingQueried = elasticCrud.getIndexMappingSync(testIndexName);
        for (Map.Entry<String, Object> set : mappingQueried.entrySet()) {
            assertEquals(set.getValue(), testIndexMapping1.get(set.getKey()));
        }
    }
}
