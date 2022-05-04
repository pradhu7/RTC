package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.CareSite;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class CareSiteChecker<T extends CareSite> extends BaseObjectChecker<T> {

    @Override
    public List<Message> check(T careSite) {

        String type = careSite.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(careSite));

        if (StringUtils.isBlank(careSite.getCareSiteName())) {
            result.add(new Message(type + " careSiteName is null",
                        MessageType.WARNING));
        }

        if (careSite.getAddress() == null) {
            result.add(new Message(type + " address is null",
                        MessageType.WARNING));
        }

        if (careSite.getCareSiteType() == null) {
            result.add(new Message(type + " careSiteType is null",
                        MessageType.WARNING));
        }

        return result;
    }
}
