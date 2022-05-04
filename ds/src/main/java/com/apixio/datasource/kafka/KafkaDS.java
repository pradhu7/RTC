package com.apixio.datasource.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import static com.apixio.datasource.utility.ConfigUtility.getRequiredConfig;
import static com.apixio.datasource.utility.ConfigUtility.getOptionalConfig;

/**
 * KafkaDS provides kafka configuration services along with the creation of KafkaConsumer
 * and KafkaProducer based on the configuration contained within.
 *
 * A single KafkaDS instance is needed for every different Kafka broker as there can only be a
 * single server(s) contained within the configuration.  There is separate configuration for
 * consumers and producers.  There's also support for different "subconfigurations" for
 * consumers and producers that can be used as dictated by clients when somewhat different
 * configuration is required for different instances of KafkaConsumers (and KafkaProducers)
 *
 * The configuration is given via a Map<String,Object> (which is taken from a .yaml file most
 * likely); the keys of the Map<> are the dotted-path keynames of the yaml keys.  Using yaml to
 * describe the configuration capabilities here, the general key structure is:
 *
 *    kafka_someid:
 *      servers: localhost:9092
 *      consumer:
 *        config-default:
 *          group.id:           SignalManager
 *          key.deserializer:   "org.apache.kafka.common.serialization.StringDeserializer"
 *          value.deserializer: "org.apache.kafka.common.serialization.StringDeserializer"
 *        config-more:
 *          group.id:           SomethingElse
 *      producer:
 *  	  config-default:
 *          key.serializer:     "org.apache.kafka.common.serialization.StringSerializer"
 *          value.serializer:   "org.apache.kafka.common.serialization.StringSerializer"
 *
 * The keys under "config-default" are used when createConsumer() (or createProducer()) is
 * called.  The keys under "config-more" (as an example) are used when
 * createConsumerWithConfig("more") is called, with the caveat that these extra keys override
 * (are added to) those in config-default.  The "more" is the "id" of the config section and
 * as a rule, the section id CANNOT have a "." in it (if there were a "." then there would be
 * no way to determine the keys "under" it in the original yaml as the true hierarchy is lost
 * when the dotted paths are created).
 *
 * The names of the keys under config-* MUST match the standard ones in ConsumerConfig.* and
 * ProducerConfig.*.
 *
 * Additionally, for backwards compatibility with earlier yaml key structure, a small set of
 * predefined/known keys that are peers of "config-default" are automatically converted to
 * their Kafka-native equivalants; these are: groupId, valueDeserializer, keyDeserializer,
 * keySerializer, valueSerializer.
 */
public class KafkaDS
{
    /**
     * Strings used within the yaml-based config
     */
    private static final String SERVERS           = "servers";    // legacy top-level name
    private static final String CONSUMER_PREFIX   = "consumer.";  // prefix for all keys re consumer
    private static final String PRODUCER_PREFIX   = "producer.";  // prefix for all keys re producer
    private static final String CONFIG_PREFIX     = "config-";    // prefix for config "sections"
    private static final String TOPICS            = "topics.";    // prefix for list of topic name=value

    private static final String DEFAULT_SECTION   = CONFIG_PREFIX + "default";
    private static final int    CONFIG_PREFIX_LEN = CONFIG_PREFIX.length();

    /**
     * Maps to convert legacy field names to kafka-standard names
     */
    private static Map<String, String> cConsumerLegacyFields = new HashMap<>();
    private static Map<String, String> cProducerLegacyFields = new HashMap<>();

    /**
     * Holds 2 strings for convenience
     */
    private static class SplitKey
    {
        String key;
        String rest;

        private SplitKey(String key, String rest)
        {
            this.key  = key;
            this.rest = rest;
        }

        /**
         * Given "config-more.ignore.this" and "config-", return a pair of strings:
         *  "more" and "ignore.this".  Return null if string doesn't start with prefix.
         * Return null for "rest" value if there's no "." after the key
         */
        static SplitKey splitDot(String prefix, String key)
        {
            int dot;

            if (!key.startsWith(prefix))
                return null;

            key = key.substring(prefix.length());
            dot = key.indexOf('.');

            if (dot == -1)
                return new SplitKey(key, null);
            else
                return new SplitKey(key.substring(0, dot), key.substring(dot + 1));
        }
    }

    /**
     * Common/abstract functionality for both consumer and producer yaml config
     */
    private static class Config
    {
        Map<String, Object>              original = new HashMap<>();  // contains prefix-free copy of original config
        Map<String, Map<String, Object>> sections = new HashMap<>();  // contains all config-* children
        Map<String, String>              topics   = new HashMap<>();  // contains all topic.* children

        /**
         * Copy over only those keys that have the given prefix, stripping the prefix when copying,
         * and copy legacy config and new "config-" sections to per-section map
         */
        Config(Map<String, Object> master, String prefix, Map<String, String> fromTo, String sectionName)
        {
            int len = prefix.length();

            for (Map.Entry<String, Object> entry : master.entrySet())
            {
                String key = entry.getKey();

                if (key.startsWith(prefix))
                    original.put(key.substring(len), entry.getValue());
            }

            copyLegacy(fromTo, sectionName);
            copyConfigSections();
            copyTopics();
        }

        /**
         * Force put a name=value (where name is legacy form) into given section, changing then
         * name to the non-legacy form
         */
        void addLegacy(Map<String, String> fromTo, String from, String value, String sectionName)  // sectionName is like "config-abc"
        {
            Map<String, Object> section = getSection(sectionName);

            from = fromTo.get(from);

            if (from != null)
                section.put(from, value);
        }

        /**
         * Return inner Map<String,Object> from sections map, creating it if needed.
         */
        private Map<String, Object> getSection(String name)
        {
            Map<String, Object> section = sections.get(name);

            if (section == null)
            {
                section = new HashMap<String, Object>();
                sections.put(name, section);
            }

            return section;            
        }

        /**
         * Look for keys in original that match keys in fromTo, and copy values over to
         * sections using sectionName as the outer key and inner key is fromTo.value.
         *
         * This must be called prior to other operations as it creates an inner map
         */
        private void copyLegacy(Map<String, String> fromTo, String sectionName)
        {
            Map<String, Object> inner = new HashMap<>();

            sections.put(sectionName, inner);

            for (Map.Entry<String, String> entry : fromTo.entrySet())
            {
                Object sub = original.get(entry.getKey());

                if (sub != null)
                    inner.put(entry.getValue(), sub);
            }
        }

        /**
         * Enum keys in master looking for those that start with "config-"; copy those over to
         * the section name in the inner Map.  The "id" of the section is taken to be the rest
         * of the key (after "config-") up to the first ".".
         */
        private void copyConfigSections()
        {
            for (Map.Entry<String, Object> entry : original.entrySet())
            {
                String key = entry.getKey();

                if (key.startsWith(CONFIG_PREFIX))
                {
                    SplitKey            split      = SplitKey.splitDot(CONFIG_PREFIX, key);
                    String              sectionID  = CONFIG_PREFIX + split.key;
                    int                 idLen      = sectionID.length();
                    Map<String, Object> sectionMap = getSection(sectionID);
                    String              newkey     = split.rest;

                    if ((newkey != null) && (newkey.length() > 0))
                        sectionMap.put(newkey, entry.getValue());
                }
            }
        }

        /**
         * Search for keys that start with "topics." and copy over renaming keyname=value
         */
        private void copyTopics()
        {
            for (Map.Entry<String, Object> entry : original.entrySet())
            {
                String key = entry.getKey();

                if (key.startsWith(TOPICS))
                {
                    SplitKey split = SplitKey.splitDot(TOPICS, key);

                    if (split != null)
                        topics.put(split.key, entry.getValue().toString());
                }
            }
        }

        /**
         * Debug only
         */
        @Override
        public String toString()
        {
            return "Config(original=" + original + "; sections=" + sections + "; topics=" + topics + ")";
        }
    }


    /**
     * Fully mapped configuration
     */
    private Config consumerConfig;
    private Config producerConfig;

    /**
     * Legacy consumer config; these are top-level keys that need to be copied down to config-default
     * w/ proper ConsumerConfig strings
     */
    private final static String GROUP              = "groupId";
    private final static String VALUE_DESERIALIZER = "valueDeserializer";
    private final static String KEY_DESERIALIZER   = "keyDeserializer";

    /**
     * Legacy producer config; these are top-level keys that need to be copied down to config-default
     * w/ proper ProducerConfig strings
     */
    private final static String KEY_SERIALIZER   = "keySerializer";
    private final static String VALUE_SERIALIZER = "valueSerializer";

    /**
     * Legacy support:  copy/rename from legacy to actual-needed
     */
    static
    {
        cConsumerLegacyFields.put(SERVERS,            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        cConsumerLegacyFields.put(GROUP,              ConsumerConfig.GROUP_ID_CONFIG);
        cConsumerLegacyFields.put(VALUE_DESERIALIZER, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        cConsumerLegacyFields.put(KEY_DESERIALIZER,   ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);

        cProducerLegacyFields.put(SERVERS,            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        cProducerLegacyFields.put(VALUE_SERIALIZER,   ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        cProducerLegacyFields.put(KEY_SERIALIZER,     ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
    }

    /**
     *
     */
    public KafkaDS(Map<String, Object> config)
    {
        String servers = getRequiredConfig(config, String.class, SERVERS);  // this lives outside consumer: producer: subkeys

        consumerConfig = new Config(config, CONSUMER_PREFIX, cConsumerLegacyFields, DEFAULT_SECTION);
        producerConfig = new Config(config, PRODUCER_PREFIX, cProducerLegacyFields, DEFAULT_SECTION);

        consumerConfig.addLegacy(cConsumerLegacyFields, SERVERS, servers, DEFAULT_SECTION);
        producerConfig.addLegacy(cConsumerLegacyFields, SERVERS, servers, DEFAULT_SECTION);
    }

    /**
     * Various flavors of consumer-related operations
     */
    public KafkaConsumer createConsumer()
    {
        return newConsumer(createPropertiesFromMap(consumerConfig.sections.get(DEFAULT_SECTION)));
    }

    public KafkaConsumer createConsumer(Properties extra)
    {
        return newConsumer(combine(createPropertiesFromMap(consumerConfig.sections.get(DEFAULT_SECTION)), extra));
    }

    public KafkaConsumer createConsumerWithConfig(String configBlock)
    {
        return newConsumer(combine(createPropertiesFromMap(consumerConfig.sections.get(DEFAULT_SECTION)),
                                   createPropertiesFromMap(consumerConfig.sections.get(CONFIG_PREFIX + configBlock))));
    }

    public String getConsumerTopic(String name)
    {
        return consumerConfig.topics.get(name);
    }

    public Properties getConsumerConfig()
    {
        return createPropertiesFromMap(consumerConfig.sections.get(DEFAULT_SECTION));
    }

    public Properties getConsumerConfig(String configBlock)
    {
        return combine(createPropertiesFromMap(consumerConfig.sections.get(DEFAULT_SECTION)),
                       createPropertiesFromMap(consumerConfig.sections.get(CONFIG_PREFIX + configBlock)));
    }

    private KafkaConsumer newConsumer(Properties props)
    {
        //        System.out.println("Creating a consumer w/ properties " + props);
        return new KafkaConsumer(props);
    }

    /**
     * Various flavors of producer-related operations
     */
    public KafkaProducer createProducer()
    {
        return newProducer(createPropertiesFromMap(producerConfig.sections.get(DEFAULT_SECTION)));
    }

    public KafkaProducer createProducer(Properties extra)
    {
        return newProducer(combine(createPropertiesFromMap(producerConfig.sections.get(DEFAULT_SECTION)), extra));
    }

    public KafkaProducer createProducerWithConfig(String configBlock)
    {
        return newProducer(combine(createPropertiesFromMap(producerConfig.sections.get(DEFAULT_SECTION)),
                                   createPropertiesFromMap(producerConfig.sections.get(CONFIG_PREFIX + configBlock))));
    }

    public String getProducerTopic(String name)
    {
        return producerConfig.topics.get(name);
    }

    public Properties getProducerConfig()
    {
        return createPropertiesFromMap(producerConfig.sections.get(DEFAULT_SECTION));
    }

    public Properties getProducerConfig(String configBlock)
    {
        return combine(createPropertiesFromMap(producerConfig.sections.get(DEFAULT_SECTION)),
                       createPropertiesFromMap(producerConfig.sections.get(CONFIG_PREFIX + configBlock)));
    }

    private KafkaProducer newProducer(Properties props)
    {
        //        System.out.println("Creating a producer w/ properties " + props);
        return new KafkaProducer(props);
    }

    /**
     * Converts entries in a Map<String,Object> to stringified key=value entries in a
     * Properties object.
     */
    private Properties createPropertiesFromMap(Map<String, Object> map)
    {
        Properties  props  = new Properties();

        if (map != null)
        {
            for (Map.Entry<String, Object> entry : map.entrySet())
                props.setProperty(entry.getKey(), entry.getValue().toString());
        }

        return props;
    }

    /**
     *
     */
    private Properties combine(Properties base, Properties extra)
    {
        Properties props;

        if (extra == null)
        {
            props = base;
        }
        else
        {
            props = new Properties();

            props.putAll(base);
            props.putAll(extra);
        }

        return props;
    }

    /**
     *
     */
    public static void main(String... args)
    {
        Map<String, Object> master = new HashMap<>();
        Config              config;

        master.put("servers", "localhost:9092");
        master.put("consumer.groupId", "myapp");
        master.put("consumer.keyDeserializer", "org.apache.blah");
        master.put("consumer.config-more.group.id", "theothergroup");
        master.put("consumer.config-more.stuff", "stuffvalue");
        master.put("consumer.config-more", "failure");
        master.put("consumer.topics.errorname", "theerrorqname");

        //        config = new Config(master, "consumer.", cConsumerLegacyFields, "config-default");
        //        System.out.println(config.toString());
        KafkaDS ds = new KafkaDS(master);
        System.out.println(ds.consumerConfig);
        System.out.println(ds.createConsumer());
        System.out.println(ds.getConsumerTopic("errorname"));
    }
}
