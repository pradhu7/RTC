package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.ClinicalCode;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ClinicalCodeChecker implements Checker<ClinicalCode> {

    public List<Message> check(ClinicalCode code) {

        List<Message> result = new ArrayList<Message>();

        if (StringUtils.isBlank(code.getCode())) {
            result.add(new Message(code.getClass().getName() + " code is null",
                        MessageType.ERROR));
        }

        if (StringUtils.isBlank(code.getCodingSystemOID())
                && StringUtils.isBlank(code.getCodingSystem())) {
            result.add(new Message(code.getClass().getName()
                        + " both codingSystemOID and codingSystem are null",
                        MessageType.ERROR));
        } else {

            if (StringUtils.isBlank(code.getCodingSystemOID())) {
                result.add(new Message(code.getClass().getName()
                            + " codingSystemOID is null", MessageType.WARNING));
            }

            if (StringUtils.isBlank(code.getCodingSystem())) {
                result.add(new Message(code.getClass().getName()
                            + " codingSytem is null", MessageType.WARNING));
            }

        }

        if (StringUtils.isBlank(code.getDisplayName())) {
            result.add(new Message(code.getClass().getName()
                        + " displayName is null", MessageType.WARNING));
        }

        return result;
    }

}
