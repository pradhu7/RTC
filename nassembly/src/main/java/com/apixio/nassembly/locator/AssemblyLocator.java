package com.apixio.nassembly.locator;

import com.apixio.model.nassembly.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The assembly locator locates the implementation of assembly interfaces
 *
 * IMPORTANT: In order to discover your interfaces, you have to enumerate them under the resources classes
 * (see your maven plugin and corresponding resources directory)
 */

public class AssemblyLocator
{
    private static final Logger  logger         = LoggerFactory.getLogger(AssemblyLocator.class);
    private static final int     retryThreshold = 10;
    private static final Boolean lock           = Boolean.TRUE;

    private static ServiceLoader<CPersistable>     cPersistableLoader     = ServiceLoader.load(CPersistable.class);
    private static ServiceLoader<BlobPersistable>  blobPersistableLoader  = ServiceLoader.load(BlobPersistable.class);
    private static ServiceLoader<Exchange>         exchangeLoader         = ServiceLoader.load(Exchange.class);
    private static ServiceLoader<GarbageCollector> garbageCollectorLoader = ServiceLoader.load(GarbageCollector.class);
    private static ServiceLoader<Combiner>         combineLoader          = ServiceLoader.load(Combiner.class);
    private static ServiceLoader<Indexer>          indexLoader            = ServiceLoader.load(Indexer.class);

    /**
     *
     * Given a dataTypeName, return the corresponding Persistable Class
     `*
     * Note: Pure functional class
     *
     * @param dataTypeName
     * @return
     */
    public static CPersistable getPersistable(String dataTypeName)
    {
        return asStreamWithRetry(cPersistableLoader, retryThreshold)
                .filter(g -> g.matchClass(dataTypeName))
                .findFirst().orElseThrow(() -> new UnsupportedOperationException("Not yet Implemented Persistable Service for: " + dataTypeName));
    }

    /**
     * Return the List of all Cassandra Persistable Classes
     *
     * @return
     */
    public static List<CPersistable> allCPersistables()
    {
        return asStreamWithRetry(cPersistableLoader, retryThreshold).collect(Collectors.toList());
    }

    /**
     * Return the List of all Blob Persistable Classes
     *
     * @return
     */
    public static List<BlobPersistable> allBlobPersistables()
    {

        return asStreamWithRetry(blobPersistableLoader, retryThreshold).collect(Collectors.toList());
    }

    /**
     *
     * Given a dataTypeName, return the corresponding Garbage Collector Class
     `*
     * Note: Pure functional class
     *
     * @param dataTypeName
     * @return
     */
    public static GarbageCollector getGarbageCollector(String dataTypeName)
    {
        return asStreamWithRetry(garbageCollectorLoader, retryThreshold)
                .filter(g -> g.matchClass(dataTypeName))
                .findFirst().orElseThrow(() -> new UnsupportedOperationException("Not yet Implemented Garbage Collector Service for: " + dataTypeName));
    }

    /**
     * Return the List of all Garbage Collector Classes
     *
     * @return
     */
    public static List<GarbageCollector> allGarbageCollectors()
    {
        return asStreamWithRetry(garbageCollectorLoader, retryThreshold).collect(Collectors.toList());
    }

    /**
     *
     * Given a dataTypeName, return the corresponding Combiner Class
     `*
     * Note: Pure functional class
     *
     * @param dataTypeName
     * @return
     */
    public static Combiner getCombine(String dataTypeName)
    {
        return asStreamWithRetry(combineLoader, retryThreshold)
                .filter(g -> g.matchClass(dataTypeName))
                .findFirst().orElseThrow(() -> new UnsupportedOperationException("Not yet Implemented Combiner Service for: " + dataTypeName));
    }

    /**
     * Return the List of all Combine Classes
     *
     * @return
     */
    public static List<Combiner> allCombines()
    {

        return asStreamWithRetry(combineLoader, retryThreshold).collect(Collectors.toList());
    }

    /**
     *
     * Given a dataTypeName, return the corresponding Exchange Class
     `*
     * Note: Pure data class
     *
     * @param dataTypeName
     * @return
     */
    public static Exchange getExchange(String dataTypeName)
    {
        return asStreamWithRetryForExchange(exchangeLoader, retryThreshold)
                .filter(g -> g.matchClass(dataTypeName))
                .findFirst().orElseThrow(() -> new UnsupportedOperationException("Not yet Implemented Exchange Service for: " + dataTypeName));
    }

    /**
     * Return the List of all Exchange Classes
     *
     * @return
     */
    public static List<Exchange> allExchanges()
    {
        return asStreamWithRetryForExchange(exchangeLoader, retryThreshold).collect(Collectors.toList());
    }

    public static Indexer getIndexer(String dataTypeName)
    {
        return asStreamWithRetry(indexLoader, retryThreshold)
                .filter(g -> g.matchClass(dataTypeName))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Not yet Implemented Indexer Service for: " + dataTypeName));
    }

    public static List<Indexer> allIndexers()
    {
        return asStreamWithRetry(indexLoader, retryThreshold).collect(Collectors.toList());
    }

    private static <T> Stream<T> asStreamWithRetry(ServiceLoader<T> loader, int retryThreshold) {
        return asStreamWithRetryGut(loader, retryThreshold, false);
    }

    private static <T> Stream<T> asStreamWithRetryForExchange(ServiceLoader<T> loader, int retryThreshold) {
        return asStreamWithRetryGut(loader, retryThreshold, true);
    }

    private static <T> Stream<T> asStreamWithRetryGut(ServiceLoader<T> loader, int retryThreshold, boolean forceReload) {
        List list;
        int retry = 0;

        synchronized (lock) {
            if (forceReload) {
                loader.reload();
                list = asStream(loader).collect(Collectors.toList());
            } else {
                list = asStream(loader).collect(Collectors.toList());
            }
            while (list.size() <= 0 && retry++ < retryThreshold) {
                if (list.size() <= 0) {
                    loader.reload();
                    list = asStream(loader).collect(Collectors.toList());
                }

                // this is a hack to sleep (1000 ms) for the last few retries and
                // not for all of them in order not to screw the performance when it is not needed
                if ((retryThreshold - retry) < 2) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignore) {
                    }
                }
                logger.debug("List size after reload: " + list.size());
            }
        }

        //convert the list back to a stream.
        Iterable<T> iterable = list::iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private static <T> Stream<T> asStream(ServiceLoader<T> loader)
    {
        Iterable<T> iterable = () -> loader.iterator();
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}