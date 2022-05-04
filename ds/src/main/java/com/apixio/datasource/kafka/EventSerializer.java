
package com.apixio.datasource.kafka;

import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.errors.SerializationException;

import com.apixio.datasource.utility.EventDataUtility;
import com.apixio.model.event.EventType;

/**
 * @author slydon
 */
public final class EventSerializer implements Serializer<EventType> {
    private EventDataUtility util = null;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (this.util == null)
            this.util = new EventDataUtility();
    }

    @Override
    public byte[] serialize(String topic, EventType data) {
        try {
            return util.makeEventBytes(data, true);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
        this.util = null;
    }
}
