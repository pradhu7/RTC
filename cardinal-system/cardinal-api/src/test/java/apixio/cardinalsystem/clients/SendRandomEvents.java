package apixio.cardinalsystem.clients;

import apixio.cardinalsystem.api.model.KafkaConfiguration;
import apixio.cardinalsystem.test.TestUtils;

public class SendRandomEvents {
    // helper to use for local testing
    // @Test
    public void testSendEvents() {
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.topicName = "cardinal-test-topic";
        kafkaConfiguration.bootstrapServers = "PLAINTEXT://localhost:9093";
        kafkaConfiguration.acks = "1";
        KafkaProducerClient kafkaProducerClient = new KafkaProducerClient(kafkaConfiguration.toProperties());
        kafkaProducerClient.sendSync(TestUtils.getTestEvents(10000), kafkaConfiguration.topicName);
    }
}
