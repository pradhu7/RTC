package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.Demographics;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class DemographicsChecker<T extends Demographics> extends BaseObjectChecker<T>{

    @Override
    public List<Message> check(T demographics) {

        String type = demographics.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(demographics));

        if (demographics.getName() == null) {
            result.add(new Message(type + " name is null", MessageType.ERROR));
        }

        if (demographics.getDateOfBirth() == null) {
            result.add(new Message(type + " dateOfBirth is null", MessageType.ERROR));
        }

        if (demographics.getGender() == null) {
            result.add(new Message(type + " gender is null", MessageType.ERROR));
        }


        //private DateTime dateOfDeath;
        //private ClinicalCode race;
        //private ClinicalCode ethnicity;
        //private MaritalStatus maritalStatus;
        //private List<String> languages = new LinkedList<String>();
        //private String religiousAffiliation;

        return result;
    }
}
