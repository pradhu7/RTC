package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.Source;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class SourceChecker<T extends Source> extends BaseObjectChecker<T>{

    @Override
    public List<Message> check(T source) {

        String type = source.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(source));

        if (StringUtils.isBlank(source.getSourceSystem())) {
            result.add(new Message(type + " sourceSystem is null",
                        MessageType.ERROR));
        }

        if (StringUtils.isBlank(source.getSourceType())) {
            result.add(new Message(type + " sourceType is null",
                        MessageType.WARNING));
        }

        if (source.getClinicalActorId() == null) {
            result.add(new Message(type + " clinicalActorId is null",
                        MessageType.WARNING));
        }

        if (source.getOrganization() == null) {
            result.add(new Message(type + " organization is null",
                        MessageType.WARNING));
        }

        if (source.getCreationDate() == null) {
            result.add(new Message(type + " creationDate is null",
                        MessageType.WARNING));
        }

        return result;
    }

}
