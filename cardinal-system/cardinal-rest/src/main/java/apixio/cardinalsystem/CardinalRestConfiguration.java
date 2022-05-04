package apixio.cardinalsystem;

import apixio.cardinalsystem.api.model.KafkaConfiguration;
import apixio.cardinalsystem.api.model.cassandra.CassandraConfig;
import apixio.cardinalsystem.config.ExecutorConfig;
import com.apixio.restbase.config.MicroserviceConfig;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class CardinalRestConfiguration extends MicroserviceConfig {
    private KafkaConfiguration kafkaProducerConfig;
    private Map<String, ExecutorConfig> executorConfigurations;
    private CassandraConfig cassandraConfig;

    public Map<String, ExecutorConfig> getExecutorConfigurations() {
        return executorConfigurations;
    }

    public void setExecutorConfigurations(Map<String, ExecutorConfig> executorConfigurations) {
        this.executorConfigurations = executorConfigurations;
    }

    public KafkaConfiguration getKafkaProducerConfig() {
        return kafkaProducerConfig;
    }

    public void setKafkaProducerConfig(KafkaConfiguration kafkaProducerConfig) {
        this.kafkaProducerConfig = kafkaProducerConfig;
    }

    public CassandraConfig getCassandraConfig() {
        return cassandraConfig;
    }

    public void setCassandraConfig(CassandraConfig cassandraConfig) {
        this.cassandraConfig = cassandraConfig;
    }
}
