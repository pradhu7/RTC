package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.ClinicalActor;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ClinicalActorChecker<T extends ClinicalActor> extends ActorChecker<T>{

    @Override
    public List<Message> check(T clinicalActor) {

        String type = clinicalActor.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(clinicalActor));

        if (clinicalActor.getRole() == null) {
            result.add(new Message(type + " role is null", MessageType.ERROR));
        }

        if (clinicalActor.getContactDetails() == null) {
            result.add(new Message(type + " contactDetails is null", MessageType.WARNING));
        }

        return result;
    }
}
