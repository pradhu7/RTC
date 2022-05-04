package apixio.cardinalsystem.resources.v1;

import apixio.cardinalsystem.CardinalRestApplication;
import apixio.cardinalsystem.CardinalRestConfiguration;
import apixio.cardinalsystem.TestHelper;
import apixio.cardinalsystem.api.model.EventRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class EventRSTest {
    private static KafkaContainer kafka;
    private static String topicName;
    public static DropwizardAppRule<CardinalRestConfiguration> RULE;

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
        topicName = "cardinalRestKafkaProducerTest";
        createTopic(topicName);
        Properties producerConfig = new Properties();
        producerConfig.setProperty("bootstrap.servers", kafka.getBootstrapServers());
        producerConfig.setProperty("client.id", "testclient");
        producerConfig.setProperty("max.block.ms", "1000");
        CardinalRestConfiguration config = TestHelper.getDropWizardConfig();
        config.getKafkaProducerConfig().bootstrapServers = kafka.getBootstrapServers();
        config.getKafkaProducerConfig().topicName = topicName;
        RULE = new DropwizardAppRule<>(CardinalRestApplication.class, config);
        RULE.getTestSupport().before();

    }

    @AfterClass
    public static void afterAll() throws Exception {
        RULE.getTestSupport().after();
        kafka.stop();
    }

    @Test
    public void testPostEvent() throws JsonProcessingException {
        EventRequest eventRequest = EventRequest.EventRequestBuilder.anEventRequest().withItems(TestHelper.getTestEvents(100)).build();
        Client client = RULE.client();
        Response syncResponse = client.target(URI.create(String.format("http://localhost:%d/api/v1/cardinal/event?sync=true", RULE.getLocalPort())))
                .request()
                .post(Entity.json(eventRequest));
        assertTrue(syncResponse.getStatus() == 200);
        Response response = client.target(URI.create(String.format("http://localhost:%d/api/v1/cardinal/event", RULE.getLocalPort())))
                .request()
                .post(Entity.json(eventRequest));
        assertTrue(response.getStatus() == 200);
    }
}