package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.ExternalID;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ExternalIDChecker implements Checker<ExternalID>{

    public List<Message> check(ExternalID myId) {

        String type = myId.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        if (myId.getSource() != null) {
            result.add(new Message(type + " source field is obselete, use assignAuthority instead", MessageType.WARNING));
        }
        if (StringUtils.isBlank(myId.getType())){
            result.add(new Message(type + " type is empty", MessageType.WARNING));
        }
        if (StringUtils.isBlank(myId.getId()) && StringUtils.isBlank(myId.getAssignAuthority())){
            result.add(new Message(type + " has both id and assignAuthority empty", MessageType.ERROR));
        } else {
            if (StringUtils.isBlank(myId.getId())){
                result.add(new Message(type + " id is empty", MessageType.WARNING));
            }
            if (StringUtils.isBlank(myId.getAssignAuthority())){
                result.add(new Message(type + " type is empty", MessageType.WARNING));
            }
        }

        return result;
    }

}
