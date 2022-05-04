package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.Gender;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class GenderChecker implements Checker<Gender>{

    public List<Message> check(Gender gender) {

        String type = gender.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            Gender.valueOf(gender.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such  "
                        + gender.toString(), MessageType.ERROR));
        }

        return result;
    }

}
