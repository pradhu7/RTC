package com.apixio.datasource.kafka;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.concurrent.Future;
import java.util.TimeZone;

import org.junit.Ignore;
import org.junit.Test;

import com.apixio.datasource.utility.KafkaUtility;

import com.apixio.model.event.EventType;
import com.apixio.model.event.transformer.EventTypeJSONParser;

/**
 * @author slydon
 */
@Ignore("Integration")
public class KafkaStreamTest
{
    private Long testTime = System.currentTimeMillis();
    private String stringTopic = "test_string_" + testTime.toString();
    private String eventTopic = "test_event_" + testTime.toString();
    private String errorTopic = "error_" + testTime.toString();
    private KafkaUtility util = new KafkaUtility();

    @Test
    public void testEverythingBecauseCantOrderTests() throws Exception
    {
        System.out.println(testTime);
        // test the producer
        Properties producerConfig = new Properties();
        producerConfig.put("bootstrap.servers", "localhost:9092");
        producerConfig.put("acks", "all");
        producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put("ack.distance", "1");
        StreamProducer<String,String> producer = new StreamProducer(producerConfig);
        // async send
        Future<StreamRecordMetadata> f1 = producer.send(stringTopic, "test string 1");
        // sync send
        StreamRecordMetadata pr2 = producer.syncSend(stringTopic, "test string 2", 5000L);
        StreamRecordMetadata pr1 = f1.get();
        assertNotNull(pr1);
        assertEquals(pr1.offset().longValue(), 0L);
        assertNotNull(pr2);
        assertEquals(pr2.offset().longValue(), 1L);
        StreamRecordMetadata pr3 = producer.syncSend(stringTopic, "test string 3", 5000L);
        assertNotNull(pr3);
        assertEquals(pr3.offset().longValue(), 2L);
        producer.close();

        // test the consumer
        Properties consumerConfig = new Properties();
        consumerConfig.put("bootstrap.servers", "localhost:9092");
        consumerConfig.put("group.id", "test_consumer_"+testTime.toString());
        consumerConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfig.put("auto.offset.reset", "earliest");
        consumerConfig.put("ack.distance", "2");
        StreamConsumer<String,String> consumer = new StreamConsumer(consumerConfig).setErrorTopic(errorTopic);
        consumer.subscribe(stringTopic, 0);
        StreamRecord<String,String> cr1 = consumer.get(10000L);
        StreamRecord<String,String> cr2 = consumer.get(10000L);
        assertEquals(util.toJSON(cr1), "{\"metadata\":{\"topic\":\"" + stringTopic +
            "\",\"partition\":0,\"offset\":0},\"value\":\"test string 1\"}");
        assertEquals(util.toJSON(cr2), "{\"metadata\":{\"topic\":\"" + stringTopic +
            "\",\"partition\":0,\"offset\":1},\"value\":\"test string 2\"}");

        // get when no slots
        try {
            consumer.get(10000L);
            fail();
        } catch (UnackedBufferFullException e) { }

        // out of order acking
        assertEquals(consumer.remainingSlots(), 0);
        assertFalse(consumer.ack(cr2, true));
        assertEquals(consumer.remainingSlots(), 0);
        assertTrue(consumer.ack(cr1, true));
        assertEquals(consumer.remainingSlots(), 2);

        // error() and switching topics
        assertTrue(consumer.error(consumer.get(10000L)));
        assertEquals(consumer.committed(stringTopic, 0), 2L);
        assertEquals(consumer.position(stringTopic, 0), 3L);
        consumer.unsubscribe(stringTopic, 0);
        consumer.subscribe(errorTopic, 0);
        assertEquals(util.toJSON(consumer.get(10000L)), "{\"metadata\":{\"topic\":\"" + errorTopic +
            "\",\"partition\":0,\"offset\":0},\"value\":\"{\\\"metadata\\\":{\\\"topic\\\":\\\"" + stringTopic +
            "\\\",\\\"partition\\\":0,\\\"offset\\\":2},\\\"value\\\":\\\"test string 3\\\"}\"}");
        consumer.close();

        // test that consumer can start where it left off. (but recreate to force flush+cleanup)
        producer = new StreamProducer(producerConfig);
        producer.syncSend(stringTopic, "test string 4", 5000L);
        consumer = new StreamConsumer(consumerConfig);
        consumer.subscribe(stringTopic, 0);
        StreamRecord<String,String> cr3 = consumer.get(10000L);
        assertEquals(util.toJSON(cr3), "{\"metadata\":{\"topic\":\"" + stringTopic +
            "\",\"partition\":0,\"offset\":3},\"value\":\"test string 4\"}");
        consumer.close();
    }

    @Test
    public void testEvents() throws Exception
    {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        EventTypeJSONParser parser = new EventTypeJSONParser();
        String rawEvent = "{\"subject\":{\"uri\":\"8daac322-9f7d-4016-9c06-ef58ea2ef75e\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"10\",\"codeSystem\":\"HCC2013PYFinal\",\"displayName\":\"10\"},\"time\":{\"startTime\":\"2013-01-01T08:00:00+0000\",\"endTime\":\"2013-01-01T08:00:00+0000\"},\"values\":{\"problemName\":\"N/A\",\"originalId\":\"null:A_H2949_20140510_201406_532204564A  _325edf84-\",\"encounterId\":\"ApixioEncounterId:UNKNOWN\",\"sourceId\":\"ApixioSourceId:e0d14c98-2f56-4a63-9bc0-0b3b2ea573a7\",\"sourceSystem\":\"MOR\"}},\"source\":{\"uri\":\"ebddafa5-c4e2-4d4a-b40e-805274053a30\",\"type\":\"problem\"},\"evidence\":{\"inferred\":false},\"attributes\":{\"bucketName\":\"com.apixio.advdev.StructuredConditionExtractor\",\"bucketType\":\"StructuredConditions\",\"encHash\":\"8m0lVsLEdn1curxVfV1Izw==\",\"sourceType\":\"CMS_KNOWN\",\"editType\":\"ACTIVE\",\"editTimestamp\":\"2014-07-17T23:08:45.515Z\",\"$workId\":\"219354\",\"$jobId\":\"219368\",\"$orgId\":\"339\",\"$batchId\":\"339_hcpnv\",\"$propertyVersion\":\"1404869030522\",\"$documentId\":\"doc_id9d7c96ec-cfba-426b-a9c1-75504166b36a\",\"$documentUUID\":\"1e228b18-3e4e-411e-9814-021af396822b\",\"$patientUUID\":\"8daac322-9f7d-4016-9c06-ef58ea2ef75e\",\"$eventBatchId\":\"0ddb78ba-a8ee-432f-b902-bafad16fbed5\"}}";
        EventType event = parser.parseEventTypeData(rawEvent);
        Properties producerConfig = new Properties();
        producerConfig.put("bootstrap.servers", "localhost:9092");
        producerConfig.put("acks", "all");
        producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put("value.serializer", "com.apixio.datasource.kafka.EventSerializer");
        producerConfig.put("ack.distance", "1");
        StreamProducer<String,EventType> producer = new StreamProducer<String,EventType>(producerConfig);
        StreamRecordMetadata pr = producer.syncSend(eventTopic, event, 5000L);
        assertNotNull(pr);
        assertEquals(pr.offset().longValue(), 0L);
        producer.close();

        Properties consumerConfig = new Properties();
        consumerConfig.put("bootstrap.servers", "localhost:9092");
        consumerConfig.put("group.id", "test_event_consumer"+testTime.toString());
        consumerConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfig.put("value.deserializer", "com.apixio.datasource.kafka.EventDeserializer");
        consumerConfig.put("auto.offset.reset", "earliest");
        StreamConsumer<String,EventType> consumer = new StreamConsumer<String,EventType>(consumerConfig);
        consumer.subscribe(eventTopic, 0);
        StreamRecord<String,EventType> r = consumer.get(10000L);
        assertNotNull(r);
        assertNotNull(r.value());
        assertEquals(parser.toJSON(r.value()), rawEvent);
        consumer.close();
    }
}
