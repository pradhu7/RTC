package apixio.cardinalsystem.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class KafkaConfiguration {
    public Map<String, String> configOverride = new LinkedHashMap<>();
    public String bootstrapServers;
    public String clientId = "cardinal-rest";
    public String acks = "all";
    public String compressionType = "lz4";
    public String topicName;
    public String groupId = "cardinal-system";
    public Boolean autocommit = false;

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("client.id", clientId);
        properties.put("acks", acks);
        properties.put("compression.type", compressionType);
        if (autocommit) {
            properties.put("enable.auto.commit", "true");
            properties.put("auto.commit.interval.ms", "5000");
        }
        properties.putAll(configOverride);
        return properties;
    }
}
