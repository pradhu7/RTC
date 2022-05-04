package com.apixio.dao.seqstore.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.model.event.EventType;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.utility.EventDataUtility;
import com.apixio.utility.TimeHelper;
import com.apixio.utility.DataSourceUtility;
import com.apixio.dao.seqstore.utility.*;

public class SubjectStore
{
    private EventDataUtility dataUtility = new EventDataUtility();

    private CqlCache cqlCache;
    private CqlCrud cqlCrud;

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud  = cqlCrud;
        this.cqlCache = cqlCrud.getCqlCache();
    }

    public void put(List<EventType> eventTypes, String insertionTime, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        Map<String, List<EventType>> group = groupEvents(eventTypes);

        for (Map.Entry<String, List<EventType>> element : group.entrySet())
        {
            String          key    = element.getKey();
            List<EventType> events = element.getValue();

            String subjectId       = getSubjectId(key);
            String endTimePeriodSt = getEndTimePeriod(key);

            byte[] data = dataUtility.makeEventsBytes(events, true);

            createSubjectIndex(subjectId, endTimePeriodSt, columnFamily, localCqlCache);
            addEventsForSubject(subjectId, endTimePeriodSt, insertionTime, data, columnFamily, localCqlCache);
        }
    }

    private void createSubjectIndex(String subjectId, String endTimePeriodSt, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        String key    = SeqStoreKeyUtility.prepareSubjectPeriodKey(subjectId);
        String column = SeqStoreKeyUtility.prepareSubjectPeriodColumn(endTimePeriodSt);

        SeqStoreUtility.writeIndex(localCqlCache, cqlCache, cqlCrud, columnFamily, key, column);
    }

    private void addEventsForSubject(String subjectId, String endTimePeriodSt, String insertionTime, byte[] data, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        String key    = SeqStoreKeyUtility.prepareSubjectKey(subjectId, endTimePeriodSt);
        String column = SeqStoreKeyUtility.prepareSubjectColumn(endTimePeriodSt, insertionTime);

        if (localCqlCache == null)
            DataSourceUtility.saveRawData(cqlCache, key, column, data, columnFamily);
        else
            DataSourceUtility.saveRawData(localCqlCache, key, column, data, columnFamily);
    }

    private Map<String, List<EventType>> groupEvents(List<EventType> eventTypes)
    {
        Map<String, List<EventType>> groupedEvents = new HashMap<>();

        for (EventType  eventType : eventTypes)
        {
            String subjectId       = SeqStoreUtility.makeSubjectId(eventType);
            Date   endTime         = SeqStoreUtility.getEndTime(eventType);
            long   endTimePeriod   = SeqStoreUtility.mapDateToEndDate(endTime);
            String endTimePeriodSt = TimeHelper.convertToString(endTimePeriod);

            String          key    = subjectId + ":::" + endTimePeriodSt;
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

    private String getSubjectId(String key)
    {
        return key.split(":::")[0];
    }

    private String getEndTimePeriod(String key)
    {
        return key.split(":::")[1];
    }
}
