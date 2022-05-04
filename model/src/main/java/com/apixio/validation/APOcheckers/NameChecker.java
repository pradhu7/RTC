package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.Name;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class NameChecker<T extends Name> extends BaseObjectChecker<T> {

    @Override
    public List<Message> check(T name) {

        String type = name.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(name));

        if ((name.getGivenNames() == null || name.getGivenNames().size() == 0)
                && (name.getFamilyNames() == null || name.getFamilyNames()
                    .size() == 0)) {
            result.add(new Message(type + " names is empty", MessageType.ERROR));
        } else {

            if (name.getGivenNames() == null
                    || name.getGivenNames().size() == 0) {
                result.add(new Message(type + " givenNames is empty",
                            MessageType.WARNING));
                    }

            if (name.getFamilyNames() == null
                    || name.getFamilyNames().size() == 0) {
                result.add(new Message(type + " familyNames is empty",
                            MessageType.WARNING));
                    }

        }

        if (name.getNameType() == null) {
            result.add(new Message(type + " nameType is null", MessageType.WARNING));
        }
        //private List<String> prefixes;
        //private List<String> suffixes;

        return result;
    }
}
