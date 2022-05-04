package com.apixio.datasource.cassandra;

import com.apixio.utility.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * CqlMonitor os a helper class that keeps stats of Cassandra call
 * whenever used.
*/
public class CqlMonitor {
    /**
     ** Stats to keep
     **/
    public static class Metrics
    {
        long       traceID;
        List<UUID> queryTraceIDs;
        long       totalMs;
        long       totalRequests;
        UUID       maxQueryTraceID;
        long       maxMs;
        String     maxMsProcName;

        Metrics(Long traceID)
        {
            this.traceID = traceID;
        }
    }

    private static Logger log = LoggerFactory.getLogger(CqlMonitor.class.getCanonicalName());

    /**
     *
     */
    private static final ThreadLocal<Metrics> tlMetrics  = new ThreadLocal<>();
    private static final Random randomizer = new Random();

    private static AtomicLong globalNumberOfRequests = new AtomicLong(0);
    private static AtomicLong globalTotalTime        = new AtomicLong(0);
    private static AtomicLong globalNumberOfReads    = new AtomicLong(0);

    /**
     * No Cassandra request should take this long, so issue a warning if one
     * does.
     */
    private long maxTimeAllowed = 250L;

    private boolean detailedStats;

    public CqlMonitor(boolean detailed)
    {
        this.detailedStats = detailed;
    }

    public void setDetailedStats(boolean detailedStats)
    {
        this.detailedStats = detailedStats;
    }

    /**
     *
     */

    private void info(Metrics metrics, String format, Object... args)
    {
        log.info("CQL INFO " + metrics.traceID + ": " + StringUtil.subargsPos(format, args));
    }

    private void warn(Metrics metrics, String format, Object... args)
    {
        log.warn("CQL WARN " + metrics.traceID + ": " + StringUtil.subargsPos(format, args));
    }

    public Metrics startMonitoring()
    {
        Metrics metrics = new Metrics(Math.abs(randomizer.nextLong()));

        tlMetrics.set(metrics);

        return metrics;
    }

    public void dumpMonitoring()
    {
        Metrics metrics = tlMetrics.get();

        info(metrics,
                "CqlCrud for transaction: queryTraceIDs={}, total requests={}, macQueryTraceID={}, totalMs={}, maxMs={}, maxMsProc={}",
                metrics.queryTraceIDs, metrics.totalRequests, metrics.maxQueryTraceID, metrics.totalMs, metrics.maxMs, metrics.maxMsProcName);
    }

    public static void startGlobalMonitoring()
    {
        globalNumberOfRequests.set(0);
        globalTotalTime.set(0);
        globalNumberOfReads.set(0);
    }

    public static void dumpGlobalMonitoring()
    {
        log.info(StringUtil.subargsPos("Global CqlCrud info: requests={}, reads={}, Ms={}",
                globalNumberOfRequests, globalNumberOfReads, globalTotalTime));
    }

    public void recordTime(Timer t, UUID queryTraceID)
    {
        Metrics metrics = tlMetrics.get();
        long    taken   = t.time();

        if (metrics == null)
            metrics = startMonitoring();

        if (detailedStats)
            info(metrics, "{}ms for Procname {} and queryTraceID {} ", taken, t.procName, queryTraceID);

        if (taken > maxTimeAllowed)
            warn(metrics, "CqlCrud method {} with queryTraceID {} took > {} ms", t.procName, queryTraceID, maxTimeAllowed);

        metrics.totalRequests++;
        metrics.totalMs += taken;

        if (metrics.maxMs < taken)
        {
            metrics.maxMs = taken;
            metrics.maxMsProcName = t.procName;
            metrics.maxQueryTraceID = queryTraceID;
        }

        if (queryTraceID != null)
        {
            if (metrics.queryTraceIDs == null)
                metrics.queryTraceIDs = new ArrayList<>();

            metrics.queryTraceIDs.add(queryTraceID);
        }
    }

    public static void recordGlobalTime(Timer t, long numberOfReads)
    {
        globalNumberOfRequests.incrementAndGet();
        globalTotalTime.addAndGet(t.time());
        globalNumberOfReads.addAndGet(numberOfReads);
    }

    public Timer getTimer(String proc, Object... args)
    {
        return new Timer(detailedStats, proc, args);
    }

    public static Timer getTimer()
    {
        return new Timer();
    }

    public static class Timer
    {
        String procName;
        long start;

        Timer(boolean detailed, String proc, Object... args)
        {
            this.procName = (detailed) ? StringUtil.subargsPos(proc, args) : proc;
            this.start = System.currentTimeMillis();
        }

        Timer()
        {
            this.start = System.currentTimeMillis();
        }

        long time()
        {
            return System.currentTimeMillis() - start;
        }
    }
}

