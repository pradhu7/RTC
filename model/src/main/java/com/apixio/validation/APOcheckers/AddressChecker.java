package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.Address;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class AddressChecker implements Checker<Address> {

    public List<Message> check(Address address) {

        String type = address.getClass().getName();
        List<Message> result = new ArrayList<Message>();

        if (StringUtils.isBlank(address.getCity())
                && StringUtils.isBlank(address.getState())
                && StringUtils.isBlank(address.getZip())
                && (address.getStreetAddresses() == null || address
                    .getStreetAddresses().size() == 0)) {
            result.add(new Message(type + " address is empty",
                        MessageType.ERROR));
        } else {
            if (address.getStreetAddresses() == null
                    || address.getStreetAddresses().size() == 0) {
                result.add(new Message(type + " streetAddresses is empty",
                            MessageType.WARNING));
                    }

            if (StringUtils.isBlank(address.getCity())) {
                result.add(new Message(type + " city is empty",
                            MessageType.WARNING));
            }

            if (StringUtils.isBlank(address.getState())) {
                result.add(new Message(type + " state is empty",
                            MessageType.WARNING));
            }

            if (StringUtils.isBlank(address.getZip())) {
                result.add(new Message(type + " zip is empty",
                            MessageType.WARNING));
            }

            if (address.getAddressType() == null) {
                result.add(new Message(type + " addressType is null",
                            MessageType.WARNING));
            }
        }

        return result;

        // private String country;
        // private String county;

    }

}
