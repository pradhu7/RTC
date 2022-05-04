package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.NameType;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class NameTypeChecker implements Checker<NameType> {

    public List<Message> check(NameType nameType) {

        String type = nameType.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            NameType.valueOf(nameType.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such nameType"
                        + nameType.toString(), MessageType.ERROR));
        }

        return result;
    }

}
