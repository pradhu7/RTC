package com.apixio.dao.seqstore.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apixio.model.event.EventType;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.utility.EventDataUtility;
import com.apixio.utility.TimeHelper;
import com.apixio.utility.DataSourceUtility;
import com.apixio.dao.seqstore.utility.*;

public class SubjectPathValueStore
{
    private EventDataUtility dataUtility = new EventDataUtility();

    private CqlCache cqlCache;
    private CqlCrud cqlCrud;

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud  = cqlCrud;
        this.cqlCache = cqlCrud.getCqlCache();
    }

    public void put(List<EventType> eventTypes, List<String> paths, String insertionTime, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        Map<String, List<EventType>> group = groupEvents(eventTypes, paths);

        for (Map.Entry<String, List<EventType>> element : group.entrySet())
        {
            String          key    = element.getKey();
            List<EventType> events = element.getValue();

            String subjectId       = getSubjectId(key);
            String pathValue       = getPathValue(key);
            String endTimePeriodSt = getEndTimePeriod(key);

            byte[] data = dataUtility.makeEventsBytes(events, true);

            createSubjectPathValueIndex(subjectId, pathValue, endTimePeriodSt, columnFamily, localCqlCache);
            addEventsForSubjectPathValue(subjectId, pathValue, endTimePeriodSt, insertionTime, data, columnFamily, localCqlCache);
        }
    }

    private void createSubjectPathValueIndex(String subjectId, String pathValue, String endTimePeriodSt, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        String key    = SeqStoreKeyUtility.prepareSubjectPathValuePeriodKey(subjectId, pathValue);
        String column = SeqStoreKeyUtility.prepareSubjectPathValuePeriodColumn(endTimePeriodSt);

        SeqStoreUtility.writeIndex(localCqlCache, cqlCache, cqlCrud, columnFamily, key, column);
    }

    private void addEventsForSubjectPathValue(String subjectId, String pathValue, String endTimePeriodSt, String insertionTime, byte[] data, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        String key    = SeqStoreKeyUtility.prepareSubjectPathValueKey(subjectId, pathValue, endTimePeriodSt);
        String column = SeqStoreKeyUtility.prepareSubjectPathValueColumn(endTimePeriodSt, insertionTime);

        if (localCqlCache == null)
            DataSourceUtility.saveRawData(cqlCache, key, column, data, columnFamily);
        else
            DataSourceUtility.saveRawData(localCqlCache, key, column, data, columnFamily);
    }

    private Map<String, List<EventType>> groupEvents(List<EventType> eventTypes, List<String> paths)
        throws Exception
    {
        Map<String, List<EventType>> groupedEvents = new HashMap<>();

        for (EventType  eventType : eventTypes)
        {
            String subjectId       = SeqStoreUtility.makeSubjectId(eventType);
            Date   endTime         = SeqStoreUtility.getEndTime(eventType);
            long   endTimePeriod   = SeqStoreUtility.mapDateToEndDate(endTime);
            String endTimePeriodSt = TimeHelper.convertToString(endTimePeriod);
            Set<String> pathValues = getPathValues(eventType, paths);

            for (String pathValue : pathValues)
            {
                String          key    = subjectId + ":::" + pathValue + ":::" + endTimePeriodSt;
                List<EventType> events = groupedEvents.get(key);

                if (events == null)
                {
                    events = new ArrayList<>();
                    groupedEvents.put(key, events);
                }

                events.add(eventType);
            }
        }

        return groupedEvents;
    }

    private Set<String> getPathValues(EventType event, List<String> paths)
        throws Exception
    {
        Set<String> pathValues = new HashSet<>();

        Map<String, String> pathToValue = SeqStoreUtility.getPathValue(event, paths);
        for (Map.Entry<String, String> element: pathToValue.entrySet())
        {
            String pathValue = SeqStoreUtility.buildPathValue(element.getKey(), element.getValue());
            pathValues.add(pathValue);
        }

        return pathValues;
    }


    private String getSubjectId(String key)
    {
        return key.split(":::")[0];
    }

    private String getPathValue(String key)
    {
        return key.split(":::")[1];
    }

    private String getEndTimePeriod(String key)
    {
        return key.split(":::")[2];
    }
}
