package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.DocumentType;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class DocumentTypeChecker implements Checker<DocumentType> {

    public List<Message> check(DocumentType documentType) {

        String type = documentType.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            DocumentType.valueOf(documentType.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such documentType"
                        + documentType.toString(), MessageType.ERROR));
        }

        return result;
    }

}
