package com.apixio.model.event.transformer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.apixio.model.event.EventType;

public class EventTypeListJSONParser
{
    private static EventTypeJSONParser parser = new EventTypeJSONParser();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventTypeList
    {
        // so that json parser works!!!
        public EventTypeList()
        {
        }

        public EventTypeList(List<EventType> eventTypes)
        {
            this.eventTypes = eventTypes;
        }

        public List<EventType> eventTypes;
    }

    public EventTypeListJSONParser()
    {
    }

    public String toJSON(List<EventType> events)
            throws Exception
    {
        EventTypeList eventTypeList = new EventTypeList(events);

        return getMapper().writeValueAsString(eventTypeList);
    }

    public List<EventType> parseEventsData(String jsonString)
            throws Exception
    {
        EventTypeList eventTypeList = getMapper().readValue(jsonString, EventTypeList.class);

        return ( (eventTypeList == null) ? new ArrayList<EventType>() : eventTypeList.eventTypes);
    }

    private ObjectMapper getMapper()
    {
        return parser.getObjectMapper();
    }
}
