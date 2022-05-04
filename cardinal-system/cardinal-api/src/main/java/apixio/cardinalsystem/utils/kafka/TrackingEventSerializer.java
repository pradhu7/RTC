package apixio.cardinalsystem.utils.kafka;

import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import org.apache.kafka.common.serialization.Serializer;

public class TrackingEventSerializer implements Serializer<TrackingEventRecord> {
    @Override
    public byte[] serialize(final String topic, final TrackingEventRecord data) {
        return data.toByteArray();
    }
}