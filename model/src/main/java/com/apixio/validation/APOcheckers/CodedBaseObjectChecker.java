package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.CodedBaseObject;
import com.apixio.model.patient.Document;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class CodedBaseObjectChecker<T extends CodedBaseObject> extends BaseObjectChecker<T> {

    @Override
    public List<Message> check(T codedBaseObject) {

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(codedBaseObject));

        if (codedBaseObject.getPrimaryClinicalActorId() == null) {
            result.add(new Message(codedBaseObject.getClass().getName()
                        + " primaryClinicalActorId is null", MessageType.WARNING));
        }

        if (codedBaseObject.getSourceEncounter() == null) {
            result.add(new Message(codedBaseObject.getClass().getName()
                        + " sourceEncounter is null", MessageType.WARNING));
        }

        if (codedBaseObject.getCode() == null) {

            if (codedBaseObject instanceof Document) {
                result.add(new Message(codedBaseObject.getClass().getName()
                            + " clinicalCode is null", MessageType.WARNING));
            } else {
                result.add(new Message(codedBaseObject.getClass().getName()
                            + " clinicalCode is null", MessageType.ERROR));
            }


        }

        return result;
    }
}
