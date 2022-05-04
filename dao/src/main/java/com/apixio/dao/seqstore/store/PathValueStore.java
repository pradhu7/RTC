package com.apixio.dao.seqstore.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.apixio.model.event.EventType;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.dao.seqstore.utility.*;

public class PathValueStore
{
    private CqlCache cqlCache;
    private CqlCrud cqlCrud;

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud  = cqlCrud;
        this.cqlCache = cqlCrud.getCqlCache();
    }

    public void put(List<EventType> eventTypes, List<String> paths, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        Map<String, List<EventType>> group = groupEvents(eventTypes);

        for (Map.Entry<String, List<EventType>> element : group.entrySet())
        {
            List<EventType> events = element.getValue();

            String subjectId       = element.getKey();
            Set<String> pathValues = getPathValues(events, paths);

            for (String pathValue : pathValues)
            {
                createPathValueIndex(subjectId, pathValue, columnFamily, localCqlCache);
            }
        }
    }

    private void createPathValueIndex(String subjectId, String pathValue, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        String key    = SeqStoreKeyUtility.preparePathValueKey(subjectId);
        String column = SeqStoreKeyUtility.preparePathValueColumn(pathValue);

        SeqStoreUtility.writeIndex(localCqlCache, cqlCache, cqlCrud, columnFamily, key, column);
    }

    private Map<String, List<EventType>> groupEvents(List<EventType> eventTypes)
    {
        Map<String, List<EventType>> groupedEvents = new HashMap<>();

        for (EventType  eventType : eventTypes)
        {
            String subjectId = SeqStoreUtility.makeSubjectId(eventType);

            String          key    = subjectId;
            List<EventType> events = groupedEvents.get(key);

            if (events == null)
            {
                events = new ArrayList<>();
                groupedEvents.put(key, events);
            }

            events.add(eventType);
        }

        return groupedEvents;
    }

    private Set<String> getPathValues(List<EventType> events, List<String> paths)
        throws Exception
    {
        Set<String> pathValues = new HashSet<>();

        for (EventType event : events)
        {
            Map<String, String> pathToValue = SeqStoreUtility.getPathValue(event, paths);
            for (Map.Entry<String, String> element: pathToValue.entrySet())
            {
                String pathValue = SeqStoreUtility.buildPathValue(element.getKey(), element.getValue());
                pathValues.add(pathValue);
            }
        }

        return pathValues;
    }
}
