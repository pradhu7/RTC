package apixio.cardinalsystem.core;

import apixio.cardinalsystem.CardinalRestConfiguration;
import apixio.cardinalsystem.api.model.EventRequest;
import apixio.cardinalsystem.api.model.cassandra.CassandraClient;
import apixio.cardinalsystem.clients.KafkaProducerClient;
import com.apixio.SysServices;
import com.apixio.restbase.DaoBase;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class CardinalSysServices extends SysServices {
    private Executor kafkaExecutor;
    private KafkaProducerClient producerClient;
    private Logger logger = Logger.getLogger(EventRequest.class.getName());
    private String kafkaTopic;
    private CassandraClient cassandraClient;

    public CardinalSysServices(DaoBase seed, CardinalRestConfiguration config) {
        super(seed, config);
        kafkaExecutor = config.getExecutorConfigurations().get("kafka").getThreadPoolExecutor("cardinal-kafka");
        producerClient = new KafkaProducerClient(config.getKafkaProducerConfig().toProperties());
        kafkaTopic = config.getKafkaProducerConfig().topicName;
        logger.info(String.format(
                        "Configured kafka connection %s to topic %s",
                        config.getKafkaProducerConfig().bootstrapServers,
                        config.getKafkaProducerConfig().topicName
                )
        );
        cassandraClient = new CassandraClient(config.getCassandraConfig());
    }

    public Executor getKafkaExecutor() {
        return kafkaExecutor;
    }

    public KafkaProducerClient getProducerClient() {
        return producerClient;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public CassandraClient getCassandraClient() {
        return cassandraClient;
    }
}
