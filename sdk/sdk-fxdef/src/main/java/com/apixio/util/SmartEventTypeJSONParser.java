package com.apixio.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import com.apixio.model.event.EventType;
import com.apixio.model.event.transformer.EventTypeJSONParser;

/**
 * The current EventTypeJSONParser in com.apixio.model doesn't handle both old (up to around 2014)
 * and new (post 2014) with the same instance due to the initialization model it has and rather than
 * modify that code and risk breaking existing clients, this class was created to support both
 * old and new in a single instance.
 */
public class SmartEventTypeJSONParser
{

    /**
     * 
     */
    private static EventTypeJSONParser oldParser = EventTypeJSONParser.getDeprecatedParser();
    private static EventTypeJSONParser newParser = EventTypeJSONParser.getStandardParser();

    /**
     * Parse JSON, preferring "new" format and falling back to "old" format parsing if parsing with
     * the new format looks like it didn't pick up the fields.
     */
    public static EventType parseEventTypeJSON(String json) throws JsonParseException, JsonMappingException, IOException
    {
        // new parser shouldn't throw an exception on valid old and new JSON
        EventType event = newParser.parseEventTypeData(json);

        // old JSON parsed with new parser will not have most fields, so choose subject as a marker
        if (event.getSubject() == null)
            event = oldParser.parseEventTypeData(json);

        return event;
    }

}
