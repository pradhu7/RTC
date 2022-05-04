package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.EditType;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class EditTypeChecker implements Checker<EditType>{

    public List<Message> check(EditType editType) {

        String type = editType.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            EditType.valueOf(editType.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such editType "
                        + editType.toString(), MessageType.ERROR));
        }

        return result;
    }

}
