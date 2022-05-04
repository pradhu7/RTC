package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.CareSiteType;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class CareSiteTypeChecker implements Checker<CareSiteType>{

    public List<Message> check(CareSiteType careSiteType) {

        String type = careSiteType.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        try {
            CareSiteType.valueOf(careSiteType.toString());
        } catch (IllegalArgumentException ex) {
            result.add(new Message(type + " no such editType "
                        + careSiteType.toString(), MessageType.ERROR));
        }

        return result;
    }

}
