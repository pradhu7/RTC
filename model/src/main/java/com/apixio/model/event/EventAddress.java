package com.apixio.model.event;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * EventAddress is a way to get a unique id (an address) for an event based on its contents.
 * currently, an address for the event is a string with two parts - the address generator's
 * version and the hash based on contents of the event. (version:hash_algorithm:hash)
 *
 * The current version of the event addressing mechanism uses the following properties of the events
 * to create an adress
 *  - subject
 *  - source
 *  - fact (including all fact values)
 *  - evidence source
 *
 * Created by vvyas on 4/1/14.
 */
public final class EventAddress {

    public static final String ADDRESSING_VERSION        = "1.1";
    public static final String ADDRESSING_VERSION_2_0    = "2.0";
    public static final String ADDRESSING_VERSION_LATEST = ADDRESSING_VERSION_2_0;
    public static final String separator = ":";
    public static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Given an event, use its contents to create a hash and use that to
     * create an event address.
     * @param event - The event we want an address for.
     * @return - An address for this event.
     */
    public static String getEventAddress(EventType event)
        throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String addressingVersion = getAddressingVersion(event);
        return getEventAddress(event, addressingVersion);
    }

    /**
     * Given an event, use its contents to create a hash and use that to
     * create an event address. Starting with version 2.0, collections are sorted before hashing.
     * @param event - The event we want an address for.
     * @param addressingVersion - The version of the event.
     * @return - An address for this event.
     */
    public static String getEventAddress(EventType event, String addressingVersion)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if ( !(addressingVersion.equals(ADDRESSING_VERSION) || addressingVersion.equals(ADDRESSING_VERSION_2_0)) )
            throw new UnsupportedEncodingException();

        boolean sortCollection = addressingVersion.equals(ADDRESSING_VERSION_2_0) ? true : false;

        return getEventAddressGuts(event, addressingVersion, sortCollection);
    }

    /**
     * Get the addressing version used to generate an address for the event.
     * create an event address. Starting with version 2.0, collections are sorted before hashing.
     * @param event - The event we want an address for.
     * @return - Addressing version of the event
     */
    public static String getAddressingVersion(EventType event) {
        AttributesType attributesType = event.getAttributes();
        List<AttributeType> attributeTypeList = (attributesType == null)? null: attributesType.getAttribute();

        if (attributeTypeList == null || attributeTypeList.isEmpty())
            return ADDRESSING_VERSION;

        for (AttributeType attributeType : attributeTypeList)
        {
            String name = attributeType.getName();

            // This will be set in the pipeline
            if (name.equals("$addressingVersion"))
                return attributeType.getValue();
        }

        return ADDRESSING_VERSION;
    }

    private static String getEventAddressGuts(EventType event, String addressingVersion, boolean sortAttributes)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(addressingVersion);
        sb.append(separator);
        sb.append(HASH_ALGORITHM);
        sb.append(separator);


        MessageDigest digestor = MessageDigest.getInstance(HASH_ALGORITHM);

        // digest the subject
        digestor.update(event.getSubject().getType().getBytes("utf-8"));
        digestor.update(event.getSubject().getUri().getBytes("utf-8"));

        // digest the source
        digestor.update(event.getSource().getType().getBytes("utf-8"));
        digestor.update(event.getSource().getUri().getBytes("utf-8"));

        // digest the fact code
        digestor.update(event.getFact().getCode().getCode().getBytes("utf-8"));
        digestor.update(event.getFact().getCode().getCodeSystem().getBytes("utf-8"));

        if(event.getFact().getCode().getCodeSystemName() != null)
            digestor.update(event.getFact().getCode().getCodeSystemName().getBytes("utf-8"));
        if(event.getFact().getCode().getCodeSystemVersion() != null)
            digestor.update(event.getFact().getCode().getCodeSystemVersion().getBytes("utf-8"));
        if(event.getFact().getCode().getDisplayName() != null)
            digestor.update(event.getFact().getCode().getDisplayName().getBytes("utf-8"));

        // digest the fact times
        digestor.update(Longs.toByteArray(event.getFact().getTime().getStartTime().getTime()));
        if(event.getFact().getTime().getEndTime() != null)
            digestor.update(Longs.toByteArray(event.getFact().getTime().getEndTime().getTime()));

        // digest the fact values
        if(event.getFact().getValues() != null && event.getFact().getValues().getAttribute() != null) {
            List<AttributeType> attributes  = event.getFact().getValues().getAttribute();
            if (sortAttributes) sort(attributes);
            for(AttributeType at : attributes) {
                digestor.update(at.getName().getBytes("utf-8"));
                digestor.update(at.getValue().getBytes("utf-8"));
            }
        }

        // digest the evidence type & source
        if(event.getEvidence() != null) {
            digestor.update(event.getEvidence().isInferred() ? Ints.toByteArray(1) : Ints.toByteArray(0));
            if(event.getEvidence().getSource() != null) {
                digestor.update(event.getEvidence().getSource().getType().getBytes("utf-8"));
                digestor.update(event.getEvidence().getSource().getUri().getBytes("utf-8"));
            }
        }

        if(event.getEvidence().getAttributes() != null && event.getEvidence().getAttributes().getAttribute() != null) {
            List<AttributeType> attributes =  event.getEvidence().getAttributes().getAttribute();
            if (sortAttributes) sort(attributes);
            for(AttributeType at : attributes) {
                digestor.update(at.getName().getBytes("utf-8"));
                digestor.update(at.getValue().getBytes("utf-8"));
            }
        }

        sb.append(BaseEncoding.base16().encode(digestor.digest()));
        return sb.toString();
    }

    private static void sort(List<AttributeType> attributes)
    {
        Collections.sort(attributes, new Comparator<AttributeType>() {
            @Override
            public int compare(AttributeType a1, AttributeType a2) {
                if (a1.getName().equals(a2.getName())) return a1.getValue().compareTo(a2.getValue());
                return (a1.getName().compareTo(a2.getName()));
            }
        });
    }
}
