package com.apixio.dao.seqstore.utility;

import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import com.apixio.model.event.EventType;
import com.apixio.model.event.ReferenceType;
import com.apixio.model.event.transformer.EventTypeJSONParser;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.utility.DataSourceUtility;

public class SeqStoreUtility
{
    private static CacheLoader<String, String> loader = new CacheLoader<String, String>()
        {
                @Override
                public String load(String key) {
                    return key;
        }
    };

    // Cache Size - 4M keys - that corresponds to 1M logical inserts.
    // If we assume each key is 100B, the total cache storage is 400M

    private static LoadingCache<String, String> columnCache = CacheBuilder.newBuilder().maximumSize(1000 * 1000 * 4L).build(loader);

    private static EventTypeJSONParser parser = new EventTypeJSONParser();

    public static String makeSubjectId(EventType eventType)
    {
        return eventType.getSubject().getType() + "=" + eventType.getSubject().getUri();
    }

    public static String makeSubjectId(ReferenceType referenceType)
    {
        return referenceType.getType() + "=" + referenceType.getUri();
    }

    public static Date getStartTime(EventType eventType)
    {
        Date endTime = eventType.getFact().getTime().getStartTime();

        return endTime;
    }

    public static Date getEndTime(EventType eventType)
    {
        Date endTime = eventType.getFact().getTime().getEndTime();

        return endTime;
    }

    public static long mapDateToEndDate(Date eventTime)
    {
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-M-d").withZoneUTC();
        DateTime start = format.parseDateTime("1900-1-1");  // To DO

        DateTime time = new DateTime(eventTime, DateTimeZone.UTC);
        DateTime end  = start;

        while (true)
        {
            if (time.isBefore(end))
            {
                return end.getMillis();
            }

            end = end.plusMonths(12);
        }
    }

    public static String buildPathValue(String path, String value)
    {
        return path + "=" +  value;
    }

    public static String getPath(String pathValue)
    {
        return pathValue.split("=")[0];
    }

    public static String getValue(String pathValue)
    {
        return pathValue.split("=")[1];
    }

    public static Map<String, String> getPathValue(EventType event, List<String> paths)
        throws Exception
    {
        Map<String, String> pathToValue = new HashMap<>();

        String json = parser.toJSON(event);

        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);

        for (String path : paths)
        {
            String value = null;
            try { value = JsonPath.read(document, "$." + path); } catch (Exception e) { }
            if (value != null)
            {
                pathToValue.put(path, value);
            }
        }

        return pathToValue;
    }

    public static void writeIndex(LocalCqlCache localCqlCache, CqlCache cqlCache, CqlCrud cqlCrud,
                                  String columnFamily, String key, String column) throws Exception
    {
        /**
         * in cache implies it is in cassandra. But, the opposite is not true.
         */

        boolean inCache     = false;
        boolean inCassandra = false;

        inCache = (columnCache.getIfPresent(columnFamily+key+column) != null);

        if (inCache)
        {
            inCassandra = true;
        }
        else
        {
            inCassandra = (DataSourceUtility.readColumnValue(cqlCrud, key, column, columnFamily) != null);
        }

        if (inCassandra)
        {
            if (!inCache)
                columnCache.put(columnFamily+key+column, "x");
        }
        else
        {
            if (localCqlCache == null)
                DataSourceUtility.saveRawData(cqlCache, key, column, "x".getBytes("UTF-8"), columnFamily);
            else
                DataSourceUtility.saveRawData(localCqlCache, key, column, "x".getBytes("UTF-8"), columnFamily);

        }
    }


    public static ThreadPoolExecutor createDefaultExecutorService(int threadPoolSize)
    {
        ThreadFactory threadFactory = new ThreadFactory()
        {
            private int threadCount = 1;

            public Thread newThread(Runnable r)
            {
                Thread thread = new Thread(r);
                thread.setName("seqStore-executorService-worker-" + threadCount++);
                return thread;
            }
        };
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    }

    /**
     * Special conversion method, to adjust for our business logic, that a 0 qet is
     * the same as setting the end to inifinity (LONG.MAX_VALUE) - the largest date
     *
     * @param criteria
     * @return
     */
    static public Range getAdjustedRange(Criteria criteria)
    {
        Range range = criteria.getRange();

        //Optimization, we don't need to clone unless the end range is 0
        if(range.getEnd() != 0) return range;

        // if we need to clone, then do it!
        range = criteria.getRange().clone();
        range.setEnd(Long.MAX_VALUE);

        return range;
    }
}
