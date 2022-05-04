
package com.apixio.datasource.kafka;

import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;

import com.apixio.datasource.utility.KafkaUtility;
import com.apixio.datasource.utility.KafkaConsumerUtility$;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * A Kafka client that consumes records from a Kafka cluster that is specific to Apixio.
 *
 * This consumer will operate transactionally.  The commits are explicit, and conducted via
 * ack() and error().  This can be either enforced or permissived depending on the value you
 * give to &quot;ack.distance&quot;.
 * <h4>Simple Processing</h4>
 * This example demonstrates the simplest usage of Kafka's consumer api.
 *
 * <pre>
 *     Properties props = new Properties();
 *     props.put(&quot;bootstrap.servers&quot;, &quot;localhost:9092&quot;);
 *     props.put(&quot;group.id&quot;, &quot;test&quot;);
 *     props.put(&quot;session.timeout.ms&quot;, &quot;30000&quot;);
 *     props.put(&quot;key.serializer&quot;, &quot;org.apache.kafka.common.serializers.StringSerializer&quot;);
 *     props.put(&quot;value.serializer&quot;, &quot;org.apache.kafka.common.serializers.StringSerializer&quot;);
 *     props.put(&quot;ack.distance&quot;, &quot;0&quot;);
 *     KafkaConsumer&lt;String, String&gt; consumer = new KafkaConsumer&lt;String, String&gt;(props);
 *     consumer.subscribe(&quot;test_topic&quot;);
 *     while (true) {
 *         ConsumerRecord&lt;String, String&gt; record = consumer.get(100);
 *         System.out.printf(&quot;offset = %d, key = %s, value = %s&quot;, record.offset(), record.key(), record.value());
 *         consumer.ack(record);
 *     }
 */
/**
 * @author slydon
 */
public class StreamConsumer<K, V> extends KafkaConsumer<K, V> {
    private LinkedBlockingQueue<StreamRecord> queue = null;
    private ConcurrentLinkedQueue<StreamRecord> acked = null;

    private Properties producerProps = null;
    private StreamProducer<String,String> errorProducer = null;
    private String errorTopic = null;
    private String brokers = null;
    private String groupId = null;

    private Iterator<ConsumerRecord<K, V>> current = null;

    private KafkaUtility util = new KafkaUtility();

    static private Properties clientProps(Properties props) {
        props.put("enable.auto.commit", "false");
        return props;
    }

    /**
     * A consumer is instantiated by providing a {@link java.util.Properties} object as configuration. Valid
     * configuration strings are documented at {@link org.apache.kafka.clients.consumer.ConsumerConfig} A consumer is instantiated by providing a
     * {@link java.util.Properties} object as configuration. Valid configuration strings are documented at
     * {@link org.apache.kafka.clients.consumer.ConsumerConfig}.  The additional config is &quot;ack.distance&quot;.  When this is 0, then no ack
     * enforcement is done.  When this is >= 1, then an ack buffer is maintained and must have slots to get more
     * items.
     * @param props The consumer config
     */
    public StreamConsumer(Properties props) {
        super(StreamConsumer.clientProps(props));
        if (Integer.parseInt(props.getProperty("ack.distance", "0")) > 0) {
            queue = new LinkedBlockingQueue<StreamRecord>(Integer.parseInt(props.getProperty("ack.distance", "0")));
            acked = new ConcurrentLinkedQueue<StreamRecord>();
        }
        groupId = props.getProperty("group.id");
        brokers = props.getProperty("bootstrap.servers");
        producerProps = new Properties();
        producerProps.put("bootstrap.servers", props.getProperty("bootstrap.servers"));
        producerProps.put("acks", "all");
        producerProps.put("retries", "10");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("ack.distance", "1");
    }

    /**
     * To enable error() to send serialized messages to an error Topic, the error topic name must be provided.
     * This method will set that error Topic name, and instantiate a synchronous producer.
     */
    public StreamConsumer setErrorTopic(String topic) {
        errorTopic = topic;
        errorProducer = new StreamProducer<String,String>(producerProps);
        return this;
    }

    /**
     * Used to acknowledge a StreamRecord.
     * This method does not need to be invoked if error() is invoked.  It allows out of order acknowledgements
     * when ack.distance is greater than 1.  This method will commit the offset to the broker.
     * @return true if a commit actually happens and ack buffer has removed some entries.
     */
    public synchronized boolean ack(StreamRecord rec, boolean sync) {
        if (queue == null) return false;
        if (isSmallestUnacked(rec)) {
            // this is the smallest unacked message, collapse acked and pop from queue
            StreamRecord largest = rec;
            for (StreamRecord s : collapseAcked(rec)) {
                if (s.greaterThan(largest))
                    largest = s;
                queue.remove(s);
            }
            commit(largest.metadata().topic(), largest.metadata().partition(), largest.metadata().offset(), sync);
            return true;
        } else if (queue.contains(rec))
            // this is not the smallest unacked message, add to acked
            acked.add(rec);
        return false;
    }

    /**
     * This method will commit the offset to the broker.
     * @return
     */
    public synchronized void commit(StreamRecord rec, boolean sync) {
        HashMap<TopicPartition, Long> cmap = new HashMap<TopicPartition, Long>();
        cmap.put(new TopicPartition(rec.metadata().topic(), rec.metadata().partition()), rec.metadata().offset());
        commit(rec.metadata().topic(), rec.metadata().partition(), rec.metadata().offset(), sync);
    }

    /**
     * This method will commit the offset to the broker.
     * @return
     */
    public synchronized void commit(String topic, int partition, long offset, boolean sync) {
        HashMap<TopicPartition, OffsetAndMetadata> cmap = new HashMap<TopicPartition, OffsetAndMetadata>();
        cmap.put(new TopicPartition(topic, partition), new OffsetAndMetadata(offset));
        if (sync) {
            super.commitSync(cmap);
        } else {
            throw new NotImplementedException();
           // super.commitAsync(cmap,);
        }
    }

    /**
     * Used to acknowledge and report an error with a StreamRecord.
     * This method will acknowledge the record and then send to the error topic (assuming one is established).
     * @return true if message was successfully written to error topic.
     */
    public synchronized boolean error(StreamRecord rec) throws Exception {
        ack(rec, true);
        return errorProducer != null && errorProducer.syncSend(errorTopic, util.toJSON(rec), 10000L).offset() >= 0L;
    }

    /**
     * Fetch a message.
     * Provide a timeout to poll() for a message.
     * @return the message if successful, or null if not.
     */
    public synchronized StreamRecord<K, V> get(long timeout) throws UnackedBufferFullException {
        long start = System.currentTimeMillis();

        checkTransactionality();

        if (current == null || !current.hasNext()) {
            do {
                current = super.poll(timeout).iterator();
            } while ((current == null || !current.hasNext()) && (System.currentTimeMillis() - timeout) <= start);
        }

        return leaseRecord(current.hasNext() ? current.next() : null);
    }

    /**
     * Close the consumer.
     */
    @Override
    public synchronized void close() {
        if (errorProducer != null) {
            errorProducer.close();
            errorProducer = null;
        }
        super.close();
    }

    /**
     * Subscribe to a given topic:partition, and attempt to start where consumer left off.
     */
    public synchronized void subscribe(String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        ArrayList<TopicPartition> assignedPartitions = new ArrayList<TopicPartition>();
        assignedPartitions.add(topicPartition);
        assign(assignedPartitions);
        //best effort attempt to seek to last committed offset, defaults to start of topic
        try {
          long offset = (Long)KafkaConsumerUtility$.MODULE$.lastCommitted(brokers, groupId, topic, partition).get() + 1;
          seek(topic, partition, offset);
        } catch (java.util.NoSuchElementException e) { }
    }

    /**
     * Unsubscribe to a given topic:partition.
     * Not applicable on 2.x branch. throw exception if used so consumers can update
     * @deprecated
     */
    public synchronized void unsubscribe(String topic, int partition) {
        throw new NotImplementedException();
    }

    /**
     * Seek particular offset of the given topic:partition.
     */
    public synchronized void seek(String topic, int partition, long offset) {
        seek(new TopicPartition(topic, partition), offset);
    }

    /**
     * Seek first offset of the given topic:partition.
     */
    public synchronized void seekToBeginning(String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        ArrayList<TopicPartition> assignedPartitions = new ArrayList<TopicPartition>();
        assignedPartitions.add(topicPartition);
        seekToBeginning(assignedPartitions);
    }

    /**
     * Seek last offset of the given topic:partition.
     */
    public synchronized void seekToEnd(String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        ArrayList<TopicPartition> assignedPartitions = new ArrayList<TopicPartition>();
        assignedPartitions.add(topicPartition);
        seekToEnd(assignedPartitions);
    }

    /**
     * Returns the offset of the <i>next record</i> that will be fetched (if a record with that offset exists).
     */
    public synchronized long position(String topic, int partition) {
        return position(new TopicPartition(topic, partition));
    }

    /**
     * Fetches the last committed offset for the given partition (whether the commit happened by this process or another).
     */
    public synchronized long committed(String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        Set<TopicPartition> assignedPartitions = new HashSet<TopicPartition>();
        assignedPartitions.add(topicPartition);
        return super.committed(assignedPartitions).get(topicPartition).offset();
    }

    public int remainingSlots() {
        return queue == null ? 0 : queue.remainingCapacity();
    }

    /* Determine if message is the smallest unacked message in the unacked queue for a topic:partition. */
    private boolean isSmallestUnacked(StreamRecord rec) {
        for (StreamRecord s : queue) {
            if (s.lessThan(rec)) {
                return false;
            }
        }
        return true;
    }

    /* Collect and pop all the acked messages that are less than the greatest unacked message for a topic:partition */
    private ArrayList<StreamRecord> collapseAcked(StreamRecord rec) {
        ArrayList<StreamRecord> l = new ArrayList<StreamRecord>();
        l.add(rec);
        StreamRecord smallest = null;
        for (StreamRecord s : queue) {
            if (s.greaterThan(rec) && !acked.contains(s) && (smallest == null || s.lessThan(smallest)))
                smallest = s;
        }
        for (StreamRecord s : acked) {
            if ((smallest == null && s.greaterThan(rec)) || (smallest != null && s.lessThan(smallest)))
                l.add(s);
        }
        for (StreamRecord s : l) {
          acked.remove(s);
        }
        return l;
    }

    private void checkTransactionality() throws UnackedBufferFullException {
        if (queue != null && queue.remainingCapacity() == 0)
          throw new UnackedBufferFullException("Acknowledgement buffer full at size: " + queue.size());
    }

    private StreamRecord<K, V> leaseRecord(ConsumerRecord<K, V> rec) {
        StreamRecord<K, V> ret = null;
        try {
            ret = new StreamRecord<K, V>(rec);
            if (queue != null)
                queue.put(ret);
        } catch (Exception e) {}
        return ret;
    }
}

