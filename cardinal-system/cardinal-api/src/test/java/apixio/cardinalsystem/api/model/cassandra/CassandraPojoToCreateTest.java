package apixio.cardinalsystem.api.model.cassandra;

import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrg;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.schemabuilder.Create;
import com.datastax.driver.core.schemabuilder.Create.Options;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.core.schemabuilder.SchemaBuilder.Caching;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CassandraPojoToCreateTest extends TestCase {
    private Map<String, String> testMapType;
    private Map<String, Map<Long, Integer>> testMapInMap;
    private String testString;
    private Long testLong;
    private int testPrimitiveInt;
    private Integer testInteger;
    private List<String> testList;

    public void testGetCsqlDataType() throws NoSuchFieldException {
        CassandraPojoToCreate test = new CassandraPojoToCreate(CassandraPojoToCreateTest.class);
        DataType result;
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testMapInMap").getGenericType());
        assertEquals(result, DataType.map(DataType.text(), DataType.map(DataType.bigint(), DataType.cint())));
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testMapType").getGenericType());
        assertEquals(result, DataType.map(DataType.text(), DataType.text()));
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testString").getGenericType());
        assertEquals(result, DataType.text());
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testLong").getGenericType());
        assertEquals(result, DataType.bigint());
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testPrimitiveInt").getGenericType());
        assertEquals(result, DataType.cint());
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testInteger").getGenericType());
        assertEquals(result, DataType.cint());
        result = test.getCsqlDataType(this.getClass().getDeclaredField("testList").getGenericType());
        assertEquals(result, DataType.list(DataType.text()));
    }

    public void testCreateTableBuilder() {
        CassandraPojoToCreate test = new CassandraPojoToCreate(DocInfoByOrg.class);
        test.parseFields();
        Create create = test.createTableBuilder();
        assertEquals(create.getConsistencyLevel(), ConsistencyLevel.ALL);
        String createTableStatement = create.getQueryString();
        List<String> statementList = Arrays.asList("CREATE TABLE IF NOT EXISTS cardinal.docinfo_by_org(",
                "org_xuuid text,",
                "partition_id int,",
                "last_modified_timestamp bigint,",
                "docinfo_xuuid text,",
                "PRIMARY KEY((org_xuuid, partition_id), docinfo_xuuid))");
        for (String statementLine : statementList) {
            assertTrue(createTableStatement.contains(statementLine));
        }
    }

    public void testParseFields() {
        CassandraPojoToCreate test = new CassandraPojoToCreate(DocInfoByOrg.class);
        test.parseFields();
        //assertTrue(test.getRejectedColumns().isEmpty());
        assertEquals(test.getTableAnnotation().name(), "docinfo_by_org");
        assertFalse(test.getClusteringKeys().isEmpty());
        assertTrue(test.getClusteringKeys().contains("docinfo_xuuid"));
        assertEquals(test.getClusteringKeys().size(), 1);
        assertFalse(test.getPartitionKeys().isEmpty());
        assertTrue(test.getPartitionKeys().contains("org_xuuid"));
        assertEquals(test.getPartitionKeys().size(), 2);
        assertEquals(test.getFields().keySet().size(), 4);
    }

    public void testCreateTableWithOptions() {
        CassandraPojoToCreate test = new CassandraPojoToCreate(DocInfoByOrg.class);
        test.parseFields();
        Options createTableWithOptions = test.createTableWithOptions();
        String createTableStatement = createTableWithOptions.getQueryString();
        List<String> statementList = Arrays.asList("CREATE TABLE IF NOT EXISTS cardinal.docinfo_by_org(",
                "org_xuuid text,",
                "partition_id int,",
                "last_modified_timestamp bigint,",
                "docinfo_xuuid text,",
                "PRIMARY KEY((org_xuuid, partition_id), docinfo_xuuid))",
                "WITH caching = 'keys_only' AND bloom_filter_fp_chance = 0.1 AND comment = 'automatically generated from apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrg' AND compression = {'sstable_compression' : 'LZ4Compressor', 'chunk_length_kb' : 4} AND compaction = {'class' : 'LeveledCompactionStrategy', 'tombstone_compaction_interval' : 10, 'tombstone_threshold' : 0.2, 'sstable_size_in_mb' : 160} AND dclocal_read_repair_chance = 0.1 AND gc_grace_seconds = 864000 AND read_repair_chance = 0.0"
        );
        for (String statementLine : statementList) {
            assertTrue(createTableStatement.contains(statementLine));
        }

        test.setCaching(Caching.ALL);
        test.setCompactionOptions(SchemaBuilder.sizedTieredStategy());
        test.setCompressionOptions(SchemaBuilder.snappy());
        test.setBloomFilterFPChance(0.99);
        test.setDcLocalReadRepairChance(0.99);
        test.setReadRepairChance(0.99);
        test.setDescription("this is an override");
        test.setGcGraceSeconds(99);

        createTableWithOptions = test.createTableWithOptions();
        createTableStatement = createTableWithOptions.getQueryString();
        statementList = Arrays.asList("CREATE TABLE IF NOT EXISTS cardinal.docinfo_by_org(",
                "org_xuuid text,",
                "partition_id int,",
                "last_modified_timestamp bigint,",
                "docinfo_xuuid text,",
                "PRIMARY KEY((org_xuuid, partition_id), docinfo_xuuid))",
                "WITH caching = 'all' AND bloom_filter_fp_chance = 0.99 AND comment = 'this is an override' AND compression = {'sstable_compression' : 'SnappyCompressor'} AND compaction = {'class' : 'SizeTieredCompactionStrategy'} AND dclocal_read_repair_chance = 0.99 AND gc_grace_seconds = 99 AND read_repair_chance = 0.99"
        );

        for (String statementLine : statementList) {
            assertTrue(createTableStatement.contains(statementLine));
        }
    }
}