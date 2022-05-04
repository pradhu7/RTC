
package com.apixio.datasource.kafka;

import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.apixio.datasource.utility.EventDataUtility;
import com.apixio.model.event.EventType;

/**
 * @author slydon
 */
public final class EventDeserializer implements Deserializer<EventType> {
    private EventDataUtility util = null;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (this.util == null)
            this.util = new EventDataUtility();
    }

    @Override
    public EventType deserialize(String topic, byte[] data) {
        try {
            return util.getEvent(data, true);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
        this.util = null;
    }
}
