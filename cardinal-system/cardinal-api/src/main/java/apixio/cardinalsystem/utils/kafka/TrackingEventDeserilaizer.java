package apixio.cardinalsystem.utils.kafka;

import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingEventDeserilaizer implements Deserializer<TrackingEventRecord> {
    private static final Logger LOG = LoggerFactory.getLogger(TrackingEventDeserilaizer.class);

    @Override
    public TrackingEventRecord deserialize(final String topic, byte[] data) {
        try {
            return TrackingEventRecord.parseFrom(data);
        } catch (final InvalidProtocolBufferException e) {
            LOG.error("Received unparseable message", e);
            throw new RuntimeException("Received unparseable message " + e.getMessage(), e);
        }
    }
}
