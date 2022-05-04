package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.BaseObject;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class BaseObjectChecker<T extends BaseObject> implements Checker<T>{

    public List<Message> check(T baseObject) {

        String type = baseObject.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        if (baseObject.getSourceId() == null) {
            result.add(new Message(type + " not have sourceId", MessageType.ERROR));
        }

        if (baseObject.getParsingDetailsId() == null) {
            result.add(new Message(type + " not have parsingDetailsId", MessageType.ERROR));
        }

        if (baseObject.getInternalUUID() == null) {
            result.add(new Message(type + " not have internalUUID", MessageType.ERROR));
        }

        if (baseObject.getOriginalId() == null) {
            result.add(new Message(type + " not have origianlId", MessageType.WARNING));
        }

        if (baseObject.getEditType() == null) {
            result.add(new Message(type + " not have editType", MessageType.WARNING));
        }

        if (baseObject.getLastEditDateTime() == null) {
            result.add(new Message(type + " not have lastEditDateTime", MessageType.ERROR));
        }

        return result;
    }

}
