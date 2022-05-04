package com.apixio.datasource.utility;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.apixio.model.event.EventType;
import com.apixio.model.event.transformer.EventTypeJSONParser;
import com.apixio.model.event.transformer.EventTypeListJSONParser;
import com.apixio.security.Security;
import com.apixio.utility.DataCompression;

public class EventDataUtility
{
    private static final Logger logger = LoggerFactory.getLogger(EventDataUtility.class);

    private Security encryptor;

    private static EventTypeListJSONParser eventTypeListJSONParser = new EventTypeListJSONParser();

    private static EventTypeJSONParser parser = new EventTypeJSONParser();


    public EventDataUtility()
    {
        encryptor = Security.getInstance();
    }

    public EventDataUtility(Security sec)
    {
        this.encryptor = sec;
    }

    public List<EventType> getEvents(byte[] eventData, boolean isCompressed)
        throws Exception
    {
        DataCompression compressor = new DataCompression();

        byte[] decryptedBytes = null;
        try
        {
            /* If it's compressed, we decrypt bytes to bytes. else since there is no compression, we will use normal decryption. */
            if (isCompressed)
                decryptedBytes = encryptor.decryptBytesToBytes(eventData);
            else
                decryptedBytes = encryptor.decrypt(new String(eventData, "UTF-8")).getBytes("UTF-8");
        }
        catch (Exception e)
        {
            logger.error("Could not decrypt the event list data sent.", e);
        }

        if (decryptedBytes != null)
        {
            byte[] deCompressedBytes = compressor.decompressData(decryptedBytes, isCompressed);
            if (deCompressedBytes != null)
            {
                return eventTypeListJSONParser.parseEventsData(new String(deCompressedBytes, "UTF-8"));
            }
            else
            {
                throw new Exception("Could not uncompress data.");
            }
        }

        return new ArrayList<EventType>();
    }

    public byte[] makeEventsBytes(List<EventType> events, boolean compress)
        throws Exception
    {
        DataCompression compression = new DataCompression();

        String eventsObjStr = eventTypeListJSONParser.toJSON(events);

        byte[] result = compression.compressData(eventsObjStr.getBytes("UTF-8"), compress);

        if (compress)
            return encryptor.encryptBytesToBytes(result);
        else
            return encryptor.encrypt(new String(result, "UTF-8")).getBytes("UTF-8");
    }

    public EventType getEvent(byte[] eventData, boolean isCompressed)
        throws Exception
    {
        EventType eventType = null;
        DataCompression compressor = new DataCompression();

        byte[] decryptedBytes = null;
        try
        {
            /* If it's compressed, we decrypt bytes to bytes. else since there is no compression, we will use normal decryption. */
            if (isCompressed)
                decryptedBytes = encryptor.decryptBytesToBytes(eventData);
            else
                decryptedBytes = encryptor.decrypt(new String(eventData, "UTF-8")).getBytes("UTF-8");
        }
        catch (Exception e)
        {
            logger.error("Could not decrypt the event list data sent.", e);
        }

        if (decryptedBytes != null)
        {
            byte[] deCompressedBytes = compressor.decompressData(decryptedBytes, isCompressed);
            if (deCompressedBytes != null)
            {
                eventType = parser.parseEventTypeData(new String(deCompressedBytes, "UTF-8"));
            }
            else
            {
                throw new Exception("Could not uncompress data.");
            }
        }

        return eventType;
    }

    public byte[] makeEventBytes(EventType eventType, boolean compress)
        throws Exception
    {
        DataCompression compression = new DataCompression();

        String eventObjStr = parser.toJSON(eventType);

        byte[] compressedData = compression.compressData(eventObjStr.getBytes("UTF-8"), compress);

        if (compress)
            return encryptor.encryptBytesToBytes(compressedData);
        else
            return encryptor.encrypt(new String(compressedData, "UTF-8")).getBytes("UTF-8");
    }

    private ObjectMapper getMapper()
    {
        return parser.getObjectMapper();
    }
}
