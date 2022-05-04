package com.apixio.dao.patientsignal;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class PatientSignalConfig
{
    private String servers;
    private String topic;
    private String topicPrefix;
    private String paritionerClassName = null;

    public KafkaProducer<String, String> newProducer()
    {
        Properties producer_config = new Properties();
        producer_config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        producer_config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer_config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        if(paritionerClassName != null) {
            producer_config.put("partitioner.class", paritionerClassName);
        }

        return new KafkaProducer<>(producer_config);
    }

    public void setPartitionerClass(final String className) {
        paritionerClassName = className;
    }

    public void setServers(String servers)
    {
        this.servers = servers;
    }

    public String getTopicPrefix()
    {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix)
    {
        this.topicPrefix = topicPrefix;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }
}
