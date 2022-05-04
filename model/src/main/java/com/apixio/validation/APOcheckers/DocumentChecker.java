package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.Document;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class DocumentChecker<T extends Document> extends CodedBaseObjectChecker<T>{

    @Override
    public List<Message> check(T document) {

        String type = document.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(document));

        // not overwrite super.check
        // if no origianlId, add an error, since BaseObjectChecker set it as WARNING
        if (document.getOriginalId() == null) {
            result.add(new Message(type + " document originalId is null", MessageType.ERROR));
        }

        if (document.getDocumentTitle() == null) {
            result.add(new Message(type + " documentTitle is null", MessageType.ERROR));
        }

        if (document.getDocumentDate() == null) {
            result.add(new Message(type + " documentDate is null", MessageType.ERROR));
        }

        if (StringUtils.isBlank(document.getStringContent())) {
            result.add(new Message(type + " stringContent is empty", MessageType.WARNING));
        }

        if (document.getDocumentContents() == null || document.getDocumentContents().size() == 0) {
            result.add(new Message(type + " documentContents is null or empty", MessageType.ERROR));
        }

        return result;
    }

}
