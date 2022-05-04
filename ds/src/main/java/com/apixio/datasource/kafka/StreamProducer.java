
package com.apixio.datasource.kafka;

import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * The Apixio Kafka Producer client that publishes records to Kafka.
 *
 * This producer extends the default Kafka Producer to add some specific extended functionality.  Considering this, much
 * of the documentation and functionality is used from the super class.
 *
 * The producer is <i>thread safe</i> and sharing a single producer instance across threads will generally be faster than
 * having multiple instances.
 * 
 * Here is a simple example of using the producer to send records with strings containing sequential numbers as the value
 * with an empty key.
 * <pre>
 * {@code
 * Properties props = new Properties();
 * props.put("bootstrap.servers", "localhost:9092");
 * props.put("acks", "all");
 * props.put("retries", "0");
 * props.put("ack.distance", "10");
 * props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
 * props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
 * 
 * StreamProducer<String, String> producer = new StreamProducer(props);
 * for(int i = 0; i < 100; i++)
 *     producer.send("my-topic", Integer.toString(i));
 * 
 * producer.close();
 * }</pre>
 *
 */
/**
 * @author slydon
 */
public class StreamProducer<K, V> extends KafkaProducer<K, V> {
    private LinkedBlockingQueue<Future<StreamRecordMetadata>> queue = null;
    private long timeout = 10000L;

    /**
     * A producer is instantiated by providing a set of key-value pairs as configuration. Valid configuration strings
     * are documented <a href="http://kafka.apache.org/documentation.html#producerconfigs">here</a>.  The additional config is
     * "ack.distance".  When this is 0, then no ack enforcement is done.  When this is greater than or equal to 1, then an ack
     * buffer is maintained and must have slots to send more items.  This is different than "acks" which is the level of acking
     * that needs to happen.
     * @param props   The producer configs
     */
    public StreamProducer(Properties props) {
        super(props);
        if (Integer.parseInt(props.getProperty("ack.distance", "0")) > 0)
            queue = new LinkedBlockingQueue<Future<StreamRecordMetadata>>(Integer.parseInt(props.getProperty("ack.distance", "0")));
    }

    /**
     * Set the timeout used when emptying the unacked buffer.
     * @param timeout The timeout to be used when attempting to empty the unacked buffer.
     */
    public StreamProducer setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Asynchronously send a record to a topic for a message with both key and value.
     * This method will attempt to flush unacked buffer if the buffer is full and will throw Exception after timeout if unsuccessful.
     * @param topic The topic.
     * @param key   The key (needs a correctly configured key.serializer.
     * @param message The value (needs a correctly configured value.serializer.
     * @return A future that will provide a StreamRecordMetadata when the send is complete.
     */
    public Future<StreamRecordMetadata> send(String topic, K key, V message) throws UnackedBufferFullException {
        checkAcked();
        Future<StreamRecordMetadata> f = new FutureStreamRecordMetadata(send(new ProducerRecord<K,V>(topic, key, message)));
        try { queue.put(f); } catch (Exception e) { } //this is checked so exceptions aren't thrown
        return f;
    }

    /**
     * Asynchronously send a record to a topic for a message with a value.
     * This method will attempt to flush unacked buffer if the buffer is full and will throw Exception after timeout if unsuccessful.
     * @param topic The topic.
     * @param message The value (needs a correctly configured value.serializer.
     * @return A future that will provide a StreamRecordMetadata when the send is complete.
     */
    public Future<StreamRecordMetadata> send(String topic, V message) throws UnackedBufferFullException {
        checkAcked();
        Future<StreamRecordMetadata> f = new FutureStreamRecordMetadata(send(new ProducerRecord<K,V>(topic, message)));
        try { queue.put(f); } catch (Exception e) { } //this is checked so exceptions aren't thrown
        return f;
    }

    /**
     * Synchronously send a record to a topic for a message with both key and value.
     * This method will not return the metadata until an acknowledgement comes back from broker or a timeout occurs.
     * @param topic   The topic.
     * @param key     The key (needs a correctly configured key.serializer.
     * @param message   The value (needs a correctly configured value.serializer.
     * @param timeout The timeout to wait for the acknowledgement from the broker.
     * @return The metadata associated with the committed message.
     */
    public StreamRecordMetadata syncSend(String topic, K key, V message, Long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        Future<StreamRecordMetadata> f = new FutureStreamRecordMetadata(send(new ProducerRecord<K,V>(topic, key, message)));
        return f.get(timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Synchronously send a record to a topic for a message with a value.
     * This method will not return the metadata until an acknowledgement comes back from broker or a timeout occurs.
     * @param topic   The topic.
     * @param message   The value (needs a correctly configured value.serializer.
     * @param timeout The timeout to wait for the acknowledgement from the broker.
     * @return The metadata associated with the committed message.
     */
    public StreamRecordMetadata syncSend(String topic, V message, Long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        Future<StreamRecordMetadata> f = new FutureStreamRecordMetadata(send(new ProducerRecord<K,V>(topic, message)));
        return f.get(timeout, TimeUnit.MILLISECONDS);
    }

    /* Check unacked buffer.  This will not really be flushed until it is full to provide better performance. */
    private void checkAcked() throws UnackedBufferFullException {
        long start = System.currentTimeMillis();
        while (queue != null && queue.size() > 0 && (queue.peek().isDone() || queue.remainingCapacity() == 0)) {
            if (queue.peek().isDone())
                queue.remove();
            else if ((System.currentTimeMillis() - timeout) > start)
                throw new UnackedBufferFullException("Acknowledgement buffer full at size: " + queue.size());
        }
    }
}
