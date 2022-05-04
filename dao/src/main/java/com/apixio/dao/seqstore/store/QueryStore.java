package com.apixio.dao.seqstore.store;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.apixio.model.event.EventType;
import com.apixio.model.event.ReferenceType;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.ColumnAndValue;
import com.apixio.datasource.utility.EventDataUtility;
import com.apixio.utility.TimeHelper;
import com.apixio.utility.DataSourceUtility;
import com.apixio.dao.seqstore.utility.*;

public class QueryStore
{
    private int iteratorBufferSize = 1000;

    private EventDataUtility dataUtility = new EventDataUtility();

    private CqlCrud cqlCrud;

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud = cqlCrud;
    }

    public QueryStore(int iteratorBufferSize)
    {
        this.iteratorBufferSize = iteratorBufferSize;
    }

    public List<EventType> getSequence(QueryType queryType, ReferenceType subject, String path, String value, long qst, long qet, String columnFamily)
        throws Exception
    {
        String  qs         = qst != 0 ? TimeHelper.convertToString(qst) : null;
        String  qe         = qet != 0 ? TimeHelper.convertToString(qet) : null;
        String  eqs        = qs != null ? TimeHelper.convertToString(SeqStoreUtility.mapDateToEndDate(TimeHelper.stringToDate(qs))) : null;
        String  subjectId  = SeqStoreUtility.makeSubjectId(subject);

        List<String> filtered = queryPeriods(queryType, subjectId, path, value, eqs, columnFamily);

        return queryEvents(queryType, subjectId, path, value, filtered, qs, qe, columnFamily);
    }

    public Iterator<EventType> getIteratorSequence(QueryType queryType, ReferenceType subject, String path, String value, long qst, long qet, String columnFamily)
        throws Exception
    {
        String  qs         = qst != 0 ? TimeHelper.convertToString(qst) : null;
        String  qe         = qet != 0 ? TimeHelper.convertToString(qet) : null;
        String  eqs        = qs != null ? TimeHelper.convertToString(SeqStoreUtility.mapDateToEndDate(TimeHelper.stringToDate(qs))) : null;
        String  subjectId  = SeqStoreUtility.makeSubjectId(subject);

        List<String> filtered    = queryPeriods(queryType, subjectId, path, value, eqs, columnFamily);
        List<String> SubjectKeys = makeSubjectKeys(queryType, subjectId, filtered, path, value);

        return getIterator(SubjectKeys, qs, qe, columnFamily);
    }

    private List<String> queryPeriods(QueryType queryType, String subjectId, String path, String value, String qs, String columnFamily)
        throws Exception
    {
        String  periodKey = makePeriodKey(queryType, subjectId, path, value);

        SortedMap<String, ByteBuffer> datetoX = DataSourceUtility.readColumns(cqlCrud, periodKey, null, null, columnFamily);

        List<String> filtered = new ArrayList<>();
        for (Map.Entry<String, ByteBuffer> element : datetoX.entrySet())
        {
            String point = element.getKey();
            if ( (qs == null) || (qs.compareTo(point) <= 0) )
            {
                filtered.add(point);
            }
        }

        return filtered;
    }

    private List<EventType> queryEvents(QueryType queryType, String subjectId, String path, String value, List<String> periods, String qs, String qe, String columnFamily)
        throws Exception
    {
        List<EventType> eventTypes = new ArrayList<>();

        for (String period : periods)
        {
            String subjectKey = makeSubjectKey(queryType, subjectId, period, path, value);
            SortedMap<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, subjectKey, null, null, columnFamily);

            for (Map.Entry<String, ByteBuffer> element : results.entrySet())
            {
                ByteBuffer buffer = element.getValue();
                List<EventType> events = dataUtility.getEvents(DataSourceUtility.getBytes(buffer), true);
                List<EventType> filtered;

                filtered = new ArrayList<>();
                for (EventType eventType : events)
                {
                    if ( (qe == null || qe.compareTo(TimeHelper.convertToString(SeqStoreUtility.getStartTime(eventType))) >= 0)
                            && (qs == null || qs.compareTo(TimeHelper.convertToString(SeqStoreUtility.getEndTime(eventType))) <= 0) )
                    {
                        filtered.add(eventType);
                    }
                }

                eventTypes.addAll(filtered);
            }
        }

        return eventTypes;
    }

    private String makePeriodKey(QueryType queryType, String subjectId, String path, String value)
    {
        switch(queryType)
        {
            case QuerySubjectStore:
                return SeqStoreKeyUtility.prepareSubjectPeriodKey(subjectId);

            case QuerySubjectPathStore:
                return SeqStoreKeyUtility.prepareSubjectPathPeriodKey(subjectId, path);

            case QuerySubjectPathValueStore:
                return SeqStoreKeyUtility.prepareSubjectPathValuePeriodKey(subjectId, SeqStoreUtility.buildPathValue(path, value));
        }

        return null;
    }

    private String makeSubjectKey(QueryType queryType, String subjectId, String period, String path, String value)
    {
        switch(queryType)
        {
            case QuerySubjectStore:
                return SeqStoreKeyUtility.prepareSubjectKey(subjectId, period);

            case QuerySubjectPathStore:
                return SeqStoreKeyUtility.prepareSubjectPathKey(subjectId, path, period);

            case QuerySubjectPathValueStore:
                return SeqStoreKeyUtility.prepareSubjectPathValueKey(subjectId, SeqStoreUtility.buildPathValue(path, value), period);
        }

        return null;
    }

    private List<String> makeSubjectKeys(QueryType queryType, String subjectId, List<String> periods, String path, String value)
    {
        List<String> subjectKeys = new ArrayList<>();

        for (String period: periods)
        {
            String subjectKey = makeSubjectKey(queryType, subjectId, period, path, value);
            subjectKeys.add(subjectKey) ;
        }

        return subjectKeys;
    }

    public List<String> getSequencePaths(ReferenceType subject, String columnFamily)
        throws Exception
    {
        String subjectId  = SeqStoreUtility.makeSubjectId(subject);
        String subjectKey = SeqStoreKeyUtility.preparePathValueKey(subjectId);

        SortedMap<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, subjectKey, null, null, columnFamily);

        Set<String> paths = new HashSet<>();

        for (Map.Entry<String, ByteBuffer> element : results.entrySet())
        {
            String column = element.getKey();
            String path = SeqStoreUtility.getPath(column);

            paths.add(path);
        }

        return new ArrayList<>(paths);
    }

    public List<String> getSequenceValues(ReferenceType subject, String path, String columnFamily)
       throws Exception
    {
        String subjectId  = SeqStoreUtility.makeSubjectId(subject);
        String subjectKey = SeqStoreKeyUtility.preparePathValueKey(subjectId);

        SortedMap<String, ByteBuffer> results = DataSourceUtility.readColumnsWithPattern(cqlCrud, subjectKey, path, path, columnFamily);

        Set<String> values = new HashSet<>();

        for (Map.Entry<String, ByteBuffer> element : results.entrySet())
        {
            String column = element.getKey();
            String value = SeqStoreUtility.getValue(column);

            values.add(value);
        }

        return new ArrayList<>(values);
    }

    public List<EventType> getAddressableSequenceElements(String addressId, String columnFamily)
        throws Exception
    {
        String key = SeqStoreKeyUtility.prepareAddressKey(addressId);

        List<EventType> eventTypes = new ArrayList<>();

        SortedMap<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, key, null, null, columnFamily);
        if (results.isEmpty())
            return eventTypes;

        for (Map.Entry<String, ByteBuffer> entry: results.entrySet())
        {
            EventType eventType = dataUtility.getEvent(DataSourceUtility.getBytes(entry.getValue()), true);
            eventTypes.add(eventType);
        }

        return eventTypes;
    }

    public EventType getAddressableSequenceElement(String addressId, String columnFamily)
            throws Exception
    {
        String key = SeqStoreKeyUtility.prepareAddressKey(addressId);

        SortedMap<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, key, null, null, columnFamily);
        if (results.isEmpty())
            return null;

        return dataUtility.getEvent(DataSourceUtility.getBytes(results.get(results.lastKey())), true);
    }

    private Iterator<EventType> getIterator(List<String> keys, String qs, String qe, String columnFamily) throws Exception
    {
        return new EventIterator(keys, qs, qe, columnFamily);
    }

    private class EventIterator implements Iterator<EventType>
    {
        private List<String> keys;
        private String qs;
        private String qe;
        private String columnFamily;

        private Iterator<ColumnAndValue> eventColumnAndValue;
        private int position;
        private int index;
        private List<EventType> nextEvents;

        /**
         * hasNext returns true if and only if there should be another column left to return.
         */
        public boolean hasNext()
        {
            if (nextEvents != null && !nextEvents.isEmpty() && (index < nextEvents.size())) return true;

            if (eventColumnAndValue != null && eventColumnAndValue.hasNext())
            {
                ColumnAndValue columnAndValue = eventColumnAndValue.next();

                try
                {
                    List<EventType> eventTypes = dataUtility.getEvents(DataSourceUtility.getBytes(columnAndValue.value), true);
                    nextEvents = filterEventTypes(eventTypes);
                    index = 0;
                    return hasNext();
                }
                catch (Exception e)
                {
                    return false;
                }
            }

            if ( position >= (keys.size() - 1) ) return false;

            try
            {
                eventColumnAndValue = cqlCrud.getAllColumns(columnFamily, keys.get(++position), "", iteratorBufferSize);
                return hasNext();
            }
            catch(Exception e)
            {
                return false;
            }
        }

        /**
         * next returns the next List of Keys
         */
        public EventType next()
        {
            if (!hasNext())
                return null;

            return nextEvents.get(index++);
        }

        private List<EventType> filterEventTypes(List<EventType> eventTypes)
        {
            List<EventType> filtered = new ArrayList<>();
            for (EventType eventType : eventTypes)
            {
                if ( (qe == null || qe.compareTo(TimeHelper.convertToString(SeqStoreUtility.getStartTime(eventType))) >= 0)
                            && (qs == null || qs.compareTo(TimeHelper.convertToString(SeqStoreUtility.getEndTime(eventType))) <= 0) )
                {
                    filtered.add(eventType);
                }
            }

            return filtered;
        }

        /**
         * remove throws an exception since we don't allow keys to be removed via this interface.
         */
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        /**
         * Constructs a new EventIterator by setting up the query parameters and relying on underlying
         * functionality of getAllColumns
         *
         * @throws Exception
         */
        private EventIterator(List<String> keys, String qs, String qe, String columnFamily) throws Exception
        {
            this.position = -1;

            this.keys = keys;
            this.qs = qs;
            this.qe = qe;
            this.columnFamily = columnFamily;
        }
    }

}
