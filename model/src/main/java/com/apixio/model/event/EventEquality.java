package com.apixio.model.event;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import com.apixio.model.event.transformer.EventTypeJSONParser;

public final class EventEquality
{
    private static EventTypeJSONParser parser = new EventTypeJSONParser();

    public static boolean equals(EventType e1, EventType e2)
        throws JsonParseException, JsonMappingException, IOException
    {
        if (e1 == null && e2 == null) return true;
        if (e1 == null) return false;
        if (e2 == null) return false;
        if (e1 == e2) return true;

        String st1 = parser.toJSON(e1);
        String st2 = parser.toJSON(e2);

        return st1.equals(st2);
    }
}


