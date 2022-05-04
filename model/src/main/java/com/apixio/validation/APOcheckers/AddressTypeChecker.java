package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.AddressType;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class AddressTypeChecker implements Checker<AddressType> {

    public List<Message> check(AddressType addressType) {

        String type = addressType.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            AddressType.valueOf(addressType.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such addressType "
                        + addressType.toString(), MessageType.ERROR));
        }

        return result;
    }

}
