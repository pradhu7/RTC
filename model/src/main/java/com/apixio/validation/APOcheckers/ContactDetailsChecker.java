package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.ContactDetails;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ContactDetailsChecker<T extends ContactDetails> extends BaseObjectChecker<T> {

    @Override
    public List<Message> check(T contactDetails) {

        String type = contactDetails.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(contactDetails));

        if ((contactDetails.getPrimaryAddress() == null)
                && StringUtils.isBlank(contactDetails.getPrimaryEmail())
                && contactDetails.getPrimaryPhone() == null) {

            result.add(new Message(type + " contactDetails are empty",
                        MessageType.ERROR));
        } else {

            if (contactDetails.getPrimaryAddress() == null) {
                result.add(new Message(type + " primaryAddress is null",
                            MessageType.WARNING));
            }

            if (StringUtils.isBlank(contactDetails.getPrimaryEmail())) {
                result.add(new Message(type + " primaryEmail is null",
                            MessageType.WARNING));
            }

            if (contactDetails.getPrimaryPhone() == null) {
                result.add(new Message(type + " primaryPhone is null",
                            MessageType.WARNING));
            }
        }

        // private List<Address> alternateAddresses;

        // private List<String> alternateEmails;

        // private List<TelephoneNumber> alternatePhones;

        return result;
    }
}
