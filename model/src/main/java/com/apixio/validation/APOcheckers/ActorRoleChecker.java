package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.ActorRole;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ActorRoleChecker implements Checker<ActorRole>{

    public List<Message> check(ActorRole actorRole) {

        String type = actorRole.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            ActorRole.valueOf(actorRole.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such actorRole"
                        + actorRole.toString(), MessageType.ERROR));
        }

        return result;
    }

}
