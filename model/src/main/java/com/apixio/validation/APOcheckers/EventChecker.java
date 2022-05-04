package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.event.Event;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class EventChecker<T extends Event> extends CodedBaseObjectChecker<T>{

    @Override
    public List<Message> check(T event) {

        String type = event.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(event));

        if (event.getPatientUUID() == null) {
            result.add(new Message(type + " patientUUID is null", MessageType.ERROR));
        }

        if (StringUtils.isBlank(event.getDisplayText())) {
            result.add(new Message(type + " displayText is empty", MessageType.WARNING));
        }

        if (event.getEventType() == null) {
            result.add(new Message(type + " eventType is null", MessageType.WARNING));
        }

        //private UUID objectUUID;
        //private UUID parentEncounterID;
        //private DateTime startDate, endDate;
        //private String objectType;
        //private DateTime validAfter = DateTime.now(), validBefore=null;

        return result;
    }
}
