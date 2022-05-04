package com.apixio.thread;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadPool is a basic executor map where each executor has a prefix and a pool size.
 */

public class ThreadPool
{
    private static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);

    private static final String DEFAULT_PREFIX = "Apixio-General-";

    public static final int MAX_THREAD_POOL_SIZE = 30;
    public static final int THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors() * 30, MAX_THREAD_POOL_SIZE);

    private static Map<String, ThreadPool> executors = Collections.synchronizedMap(new HashMap<String, ThreadPool>());
    private ScheduledExecutorService executor;

    private static class LoggedScheduledThreadPoolExecutor
        extends ScheduledThreadPoolExecutor
    {
        LoggedScheduledThreadPoolExecutor(int poolSize, ThreadFactory threadFactory)
        {
            super(poolSize, threadFactory);
        }

        public void afterExecute(Runnable r, Throwable t)
        {
            if (t != null)
                logger.error("Throwable terminated dispatched thread in ThreadPool", t);
        }
    }

    public static ThreadPool getThreadPool()
    {
        return getThreadPool(DEFAULT_PREFIX, THREAD_POOL_SIZE);
    }

    public static ThreadPool getThreadPool(String threadPrefix)
    {
        return getThreadPool(threadPrefix, THREAD_POOL_SIZE);
    }

    public static ThreadPool getThreadPool(String threadPrefix, int threadPoolSize)
    {
        if (executors.containsKey(threadPrefix))
        {
            return executors.get(threadPrefix);
        }
        else
        {
            ThreadPool threadPool = new ThreadPool(threadPrefix, threadPoolSize);
            executors.put(threadPrefix, threadPool);

            return threadPool;
        }
    }

    private ThreadPool() {}

    private ThreadPool(String prefix, int threadPoolSize)
    {
        executor = new LoggedScheduledThreadPoolExecutor(threadPoolSize, new ThreadWorkerFactory(prefix));
    }

    public Future<?> submit(Runnable task)
    {
        return executor.submit(task);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
    {
        return executor.schedule(command, delay, unit);
    }

    public <T> Future<T> submit(Callable<T> task)
    {
        return executor.submit(task);
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }
}