package apixio.cardinalsystem.clients;

import apixio.cardinalsystem.test.TestUtils;
import apixio.cardinalsystem.utils.kafka.TrackingEventDeserilaizer;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class KafkaProducerClientTest {

    private static KafkaContainer kafka;
    private static KafkaProducerClient client;
    private static String topicName;
    private static KafkaConsumer<String, TrackingEventRecord> consumer;

    public static void createTopic(String topicName) {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", kafka.getBootstrapServers());
        properties.setProperty("group.id", "test");
        properties.setProperty("enable.auto.commit", "true");
        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("key.deserialize", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        AdminClient adminClient = AdminClient.create(properties);
        NewTopic newTopic = new NewTopic(topicName, 1, (short) 1); //new NewTopic(topicName, numPartitions, replicationFactor)

        List<NewTopic> newTopics = new ArrayList<>();
        newTopics.add(newTopic);

        adminClient.createTopics(newTopics);
        adminClient.close();
    }

    @BeforeClass
    public static void beforeAll() throws Exception {
        Logger.getGlobal().setLevel(Level.INFO);
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));
        kafka.withStartupTimeout(Duration.ofSeconds(120));
        kafka.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        //kafka.withStartupAttempts(5);
        //kafka.withStartupCheckStrategy(
        //        new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(10))
        //);
        kafka.withReuse(true);
        kafka.start();
        topicName = "cardinalKafkaProducerTest";
        createTopic(topicName);
        Properties producerConfig = new Properties();
        producerConfig.setProperty("bootstrap.servers", kafka.getBootstrapServers());
        producerConfig.setProperty("client.id", "testclient");
        producerConfig.setProperty("max.block.ms", "1000");

        Properties consumerConfig = new Properties();
        consumerConfig.putAll(producerConfig);
        consumerConfig.put("group.id", "cardinalTest");

        client = new KafkaProducerClient(producerConfig);
        consumer = new KafkaConsumer<>(consumerConfig, new StringDeserializer(), new TrackingEventDeserilaizer());
    }

    @AfterClass
    public static void afterAll() throws Exception {
        kafka.stop();
    }

    public List<TrackingEventRecord> getTestEvents(Integer num) {
        List<TrackingEventRecord> events = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            events.add(TestUtils.generateRandomTrackingEvent());
        }
        return events;
    }

    @Test
    public void testSendSync() {
        assertTrue(client.sendSync(getTestEvents(1), topicName));
        assertTrue(client.sendSync(getTestEvents(100), topicName));
        assertFalse(client.sendSync(getTestEvents(1), "thistopicisnotreal"));
    }

    @Test
    public void testSend() throws InterruptedException, ExecutionException {
        Future<RecordMetadata> recordMetadataFuture = client.send(TestUtils.generateRandomTrackingEvent(), topicName);
        assertTrue(recordMetadataFuture.get().hasOffset());
        assertFalse(recordMetadataFuture.isCancelled());
        assertTrue(recordMetadataFuture.isDone());
        final Future<RecordMetadata> failureRecordMetadataFuture = client.send(TestUtils.generateRandomTrackingEvent(), "thecakeisalie");
        // ensure that this would have thrown an exception unless otherwise ignore by being async
        assertThrows(ExecutionException.class, () -> {
            failureRecordMetadataFuture.get();
        });
        consumer.subscribe(Arrays.asList(topicName));
        ConsumerRecords<String, TrackingEventRecord> records = consumer.poll(Duration.ofSeconds(1));
        for (ConsumerRecord<String, TrackingEventRecord> record : records) {
            assertFalse(record.key().isEmpty());
            assertTrue(record.value().hasOrgXuuid());
            assertTrue(record.value().getOrgXuuid().getType().equals("O"));
            assertFalse(record.value().getOrgXuuid().getUuid().isEmpty());
        }
    }

    @Test
    public void testTestSend() {
        List<Future<RecordMetadata>> recordMetadataFuture = client.send(getTestEvents(10), topicName);
        assertTrue(recordMetadataFuture.size() == 10);
    }
}