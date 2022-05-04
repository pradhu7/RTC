package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.event.EventType;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class EventTypeChecker implements Checker<EventType> {

    public List<Message> check(EventType eventType) {

        String type = eventType.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            EventType.valueOf(eventType.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such eventType"
                        + eventType.toString(), MessageType.ERROR));
        }

        return result;
    }
}
