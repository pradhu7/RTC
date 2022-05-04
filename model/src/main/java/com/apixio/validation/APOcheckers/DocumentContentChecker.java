package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.DocumentContent;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class DocumentContentChecker<T extends DocumentContent> extends BaseObjectChecker<T> {

    @Override
    public List<Message> check(T documentContent) {

        String type = documentContent.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(documentContent));

        if (StringUtils.isBlank(documentContent.getHash())) {
            result.add(new Message(type + " hash is empty", MessageType.ERROR));
        }

        if (StringUtils.isBlank(documentContent.getMimeType())) {
            result.add(new Message(type + " mimeType is empty",
                        MessageType.WARNING));
        }

        if (documentContent.getUri() == null) {
            result.add(new Message(type + " URI is null", MessageType.WARNING));
        }

        if (documentContent.getContent() != null) {
            result.add(new Message(type
                        + " don't use content to store document", MessageType.ERROR));
        }
        // current don't use length
        // private long length;

        return result;
    }
}
