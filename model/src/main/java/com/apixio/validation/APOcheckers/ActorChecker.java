package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.Actor;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ActorChecker<T extends Actor> extends BaseObjectChecker<T>{

    @Override
    public List<Message> check(T actor) {

        String type = actor.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(actor));

        if (actor.getActorGivenName() == null) {
            result.add(new Message(type + " actorGivenName is null", MessageType.ERROR));
        }

        if (StringUtils.isBlank(actor.getTitle())) {
            result.add(new Message(type + " title is null", MessageType.WARNING));
        }

        if (actor.getOriginalId() == null) {
            result.add(new Message(type + " originalId is null", MessageType.ERROR));
        }

        if (actor.getAssociatedOrg() == null) {
            result.add(new Message(type + " associatedOrg is null", MessageType.WARNING));
        }

        //private List<Name> actorSupplementalNames;

        //private List<ExternalID> alternateIds = new LinkedList<ExternalID>();

        return result;
    }
}
