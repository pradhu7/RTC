package apixio.cardinalsystem.clients;

import apixio.cardinalsystem.api.model.CardinalModelBase;
import apixio.cardinalsystem.utils.kafka.TrackingEventSerializer;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KafkaProducerClient {
    private Properties config = new Properties();
    private KafkaProducer<String, TrackingEventRecord> producer;
    private final static Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final static int ASYNC_PRODUCE_TIMEOUT_MS = 5;

    /**
     * @param clientConfig kafka producer properties to have set
     */
    public KafkaProducerClient(Properties clientConfig) {
        this.config.putAll(clientConfig);
        this.producer = new KafkaProducer<>(this.config, new StringSerializer(), new TrackingEventSerializer());
    }

    /**
     * Sends events to kafka and waits for the result to ensure it is produced
     *
     * @param events List of {@link TrackingEventRecord} to produce to kafka
     * @param topic  the topic name to produce to
     * @return false if there are errors, true if there are no errors
     */
    public boolean sendSync(List<TrackingEventRecord> events, String topic) {
        List<Future<RecordMetadata>> producedFutures = send(events, topic);
        producer.flush();
        for (Future<RecordMetadata> future : producedFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.severe(String.format("Error producing %d kafka events %s", producedFutures.size(), e.getMessage()));
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * Sends events to kafka and waits for the result to ensure it is produced
     *
     * @param events List of {@link TrackingEventRecord} to produce to kafka
     * @param topic  the topic name to produce to
     * @return false if there are errors, true if there are no errors
     */
    public boolean sendAsync(List<TrackingEventRecord> events, String topic) {
        List<Future<RecordMetadata>> producedFutures = send(events, topic);
        producer.flush();
        for (Future<RecordMetadata> future : producedFutures) {
            try {
                future.get(ASYNC_PRODUCE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOG.log(Level.SEVERE, String.format("Error producing %d kafka events", producedFutures.size()), e);
                return false;
            } catch (TimeoutException e) {
                LOG.warning(String.format("kafka future took more than %d ms", ASYNC_PRODUCE_TIMEOUT_MS));
            }
        }

        return true;
    }

    /**
     * @param events batch of {@link TrackingEventRecord} to send to kafka
     * @param topic  the topic name to produce to
     * @return list of {@link Future} that can be used to check if the message is produced
     */
    public List<Future<RecordMetadata>> send(List<TrackingEventRecord> events, String topic) {
        List<Future<RecordMetadata>> producedFutures = new ArrayList<>();
        for (TrackingEventRecord event : events) {
            producedFutures.add(send(event, topic));
        }
        return producedFutures;
    }

    /**
     * @param event single {@link TrackingEventRecord} to send to kafka
     * @param topic the topic name to produce to
     * @return {@link Future} that can be used to check if the message is produced
     */
    public Future<RecordMetadata> send(TrackingEventRecord event, String topic) {
        return producer.send(new ProducerRecord<>(topic, CardinalModelBase.getEventId(event), event));
    }
}
