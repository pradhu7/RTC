package apixio.cardinalsystem.processing.flink;

import apixio.cardinalsystem.test.TestUtils;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.Contents.DataCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

@TestInstance(Lifecycle.PER_CLASS)
public class PipeLineSetupTest {

    /**
     * public MiniClusterWithClientResource flinkCluster;
     * private KafkaContainer kafka;
     * private KafkaProducerClient client;
     * private String topicName;
     * private KafkaConsumer<String, TrackingEventRecord> consumer;
     * private CassandraContainer cassandra;
     * <p>
     * <p>
     * needs to be re-worked in separate class so i can get used in a full integration test
     * public void createTopic(String topicName) {
     * Properties properties = new Properties();
     * properties.setProperty("bootstrap.servers", kafka.getBootstrapServers());
     * properties.setProperty("group.id", "test");
     * properties.setProperty("enable.auto.commit", "true");
     * properties.setProperty("auto.commit.interval.ms", "1000");
     * properties.setProperty("key.deserialize", "org.apache.kafka.common.serialization.StringDeserializer");
     * properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
     * <p>
     * AdminClient adminClient = AdminClient.create(properties);
     * NewTopic newTopic = new NewTopic(topicName, 1, (short)1); //new NewTopic(topicName, numPartitions, replicationFactor)
     * <p>
     * List<NewTopic> newTopics = new ArrayList<>();
     * newTopics.add(newTopic);
     * <p>
     * adminClient.createTopics(newTopics);
     * adminClient.close();
     * }
     *
     * @BeforeAll public void beforeAll() throws Exception {
     * Logger.getGlobal().setLevel(Level.INFO);
     * kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));
     * kafka.withStartupTimeout(Duration.ofSeconds(120));
     * kafka.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
     * kafka.withReuse(true);
     * kafka.start();
     * topicName = "cardinalKafkaProducerTest";
     * createTopic(topicName);
     * Properties producerConfig = new Properties();
     * producerConfig.setProperty("bootstrap.servers", kafka.getBootstrapServers());
     * producerConfig.setProperty("client.id", "testclient");
     * producerConfig.setProperty("max.block.ms", "1000");
     * <p>
     * Properties consumerConfig = new Properties();
     * consumerConfig.putAll(producerConfig);
     * consumerConfig.put("group.id", "cardinalTest");
     * <p>
     * client = new KafkaProducerClient(producerConfig);
     * consumer = new KafkaConsumer<>(consumerConfig, new StringDeserializer(), new TrackingEventDeserilaizer());
     * <p>
     * cassandra = new CassandraContainer("cassandra:2");
     * cassandra.start();
     * Cluster cluster = cassandra.getCluster();
     * try(Session session = cluster.connect()) {
     * session.execute("CREATE KEYSPACE IF NOT EXISTS cardinal WITH replication = \n" +
     * "{'class':'SimpleStrategy','replication_factor':'1'};");
     * FlinkHelpers.createCassandraTables(session);
     * }
     * <p>
     * flinkCluster = new MiniClusterWithClientResource(
     * new MiniClusterResourceConfiguration.Builder()
     * .setNumberSlotsPerTaskManager(10)
     * .setNumberTaskManagers(1)
     * .build());
     * <p>
     * }
     * @AfterAll public void afterAll() throws Exception {
     * kafka.stop();
     * cassandra.stop();
     * }
     * @AfterEach public void killJobs() {
     * flinkCluster.cancelAllJobs();
     * }
     */

    @Test
    public void setupProcessingTest() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        PipeLineSetup pipeLineSetup = new PipeLineSetup();
        env.setParallelism(2);

        CountingSink.count.set(0);
        KeyedStream<TrackingEventRecord, Integer> keyedStream = pipeLineSetup.setupKeyBy(
                env.fromCollection(
                        TestUtils.getTestEvents(100, EventCase.PROCESS_EVENT, null, null)
                )
        );
        pipeLineSetup.setupProcessing(keyedStream);
        pipeLineSetup.processEventsById.addSink(new CountingSink<>());
        pipeLineSetup.processEventByOrg.addSink(new CountingSink<>());
        pipeLineSetup.processEventFromXuuid.addSink(new CountingSink<>());
        pipeLineSetup.processEventToXuuid.addSink(new CountingSink<>());
        env.execute();

        // verify sinks get the appropriate number of records for the data types given
        assertEquals(400, CountingSink.count.get());

        CountingSink.count.set(0);
        keyedStream = pipeLineSetup.setupKeyBy(
                env.fromCollection(
                        TestUtils.getTestEvents(100, EventCase.TRANSFER_EVENT, null, null)
                )
        );
        pipeLineSetup.setupProcessing(keyedStream);
        pipeLineSetup.transferEventsById.addSink(new CountingSink<>());
        pipeLineSetup.transferEventByOrg.addSink(new CountingSink<>());
        pipeLineSetup.transferEventFromXuuid.addSink(new CountingSink<>());
        pipeLineSetup.transferEventToXuuid.addSink(new CountingSink<>());
        env.execute();

        // verify sinks get the appropriate number of records for the data types given
        assertEquals(400, CountingSink.count.get());

        CountingSink.count.set(0);
        keyedStream = pipeLineSetup.setupKeyBy(
                env.fromCollection(
                        TestUtils.getTestEvents(100, EventCase.PROCESS_EVENT, DataCase.FILE_INFO, null)
                )
        );
        pipeLineSetup.setupProcessing(keyedStream);
        pipeLineSetup.fileInfoByChecksum.addSink(new CountingSink<>());
        pipeLineSetup.fileInfoById.addSink(new CountingSink<>());
        pipeLineSetup.fileInfoByOrg.addSink(new CountingSink<>());
        env.execute();

        // verify sinks get the appropriate number of records for the data types given
        assertEquals(300, CountingSink.count.get());

        CountingSink.count.set(0);
        keyedStream = pipeLineSetup.setupKeyBy(
                env.fromCollection(
                        TestUtils.getTestEvents(100, EventCase.PROCESS_EVENT, DataCase.DOC_INFO, null)
                )
        );
        pipeLineSetup.setupProcessing(keyedStream);
        pipeLineSetup.docInfoByChecksum.addSink(new CountingSink<>());
        pipeLineSetup.docInfoById.addSink(new CountingSink<>());
        pipeLineSetup.docInfoByOrg.addSink(new CountingSink<>());
        pipeLineSetup.docInfoByOrgPatientId.addSink(new CountingSink<>());
        pipeLineSetup.docInfoByOrgPds.addSink(new CountingSink<>());
        env.execute();

        // verify sinks get the appropriate number of records for the data types given
        assertEquals(500, CountingSink.count.get());

    }

    /**
     * need to re-work this to use an external flink cluster
     *
     * @Test public void testProcessingWithKafkaSource() throws Exception {
     * StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
     * <p>
     * KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
     * kafkaConfiguration.bootstrapServers = kafka.getBootstrapServers();
     * kafkaConfiguration.topicName = topicName;
     * CassandraConfig cassandraConfig = new CassandraConfig();
     * cassandraConfig.setHost(cassandra.getHost());
     * cassandraConfig.setPort(cassandra.getMappedPort(9042));
     * System.out.println(cassandraConfig);
     * System.out.println(kafkaConfiguration);
     * <p>
     * PipeLineSetup pipeLineSetup = new PipeLineSetup(kafkaConfiguration, cassandraConfig);
     * env.setParallelism(1);
     * KeyedStream<TrackingEventRecord, Integer> keyedStream;// = pipeLineSetup.setupKafkaStream(env);
     * keyedStream = pipeLineSetup.setupKeyBy(
     * env.fromCollection(
     * TestUtils.getTestEvents(100)
     * )
     * );
     * //pipeLineSetup.setupProcessing(keyedStream);
     * pipeLineSetup.setupProcessing(keyedStream);
     * pipeLineSetup.setupCassandraSink();
     * KafkaProducerClient kafkaProducerClient = new KafkaProducerClient(kafkaConfiguration.toProperties());
     * <p>
     * System.out.println(env.getExecutionPlan());
     * JobClient jobClient = env.executeAsync();
     * System.out.println(jobClient.getJobID());
     * //System.out.println(env.getExecutionPlan());
     * <p>
     * //kafkaProducerClient.sendSync(TestUtils.getTestEvents(100), kafkaConfiguration.topicName);
     * Thread.sleep(10000);
     * <p>
     * ResultSet resultSet = cassandra.getCluster().newSession().execute("select * from cardinal.processevent_by_id;");
     * System.out.println(resultSet.all());
     * System.out.println(resultSet.all().size());
     * <p>
     * jobClient.cancel().get();
     * <p>
     * }
     */

    // create a testing sink
    private static class CountingSink<OUT> implements SinkFunction<OUT> {

        // must be static
        public static AtomicInteger count = new AtomicInteger(0);

        @Override
        public void invoke(OUT value, SinkFunction.Context context) {
            count.getAndAdd(1);
        }
    }
}