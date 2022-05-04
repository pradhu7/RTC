package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.Encounter;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class EncounterChecker<T extends Encounter> extends CodedBaseObjectChecker<T>{

    @Override
    public List<Message> check(T encounter) {

        String type = encounter.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(encounter));

        if (encounter.getEncounterStartDate() == null) {
            result.add(new Message(type + " encounterStartDate is null", MessageType.WARNING));
        }

        if (encounter.getEncounterEndDate() == null) {
            result.add(new Message(type + " encounterEndDate is null", MessageType.WARNING));
        }

        if (encounter.getEditType() == null) {
            result.add(new Message(type + " encType is null", MessageType.WARNING));
        }

        if (encounter.getSiteOfService() == null) {
            result.add(new Message(type + " siteOfService is null", MessageType.WARNING));
        }

        if (encounter.getChiefComplaints() == null) {
            result.add(new Message(type + " chiefComplaints is null", MessageType.WARNING));
        }

        return result;
    }


}
