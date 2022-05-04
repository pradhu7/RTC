package com.apixio.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadWorker extends the java ThreadFactory by giving the thread a name made of (prefix and incremental counter).
 */

public class ThreadWorker
{
    private static Logger logger = LoggerFactory.getLogger(ThreadWorker.class);

    private static ThreadPool genericExecutor = ThreadPool.getThreadPool();

    public static void runTasks(List<Runnable> tasks)
    {
        runTasks(genericExecutor, tasks);
    }

    public static void runTasks(ThreadPool pool, List<Runnable> tasks)
    {
        List<Callable<Object>> callables = new ArrayList<Callable<Object>>(tasks.size());

        for (Runnable task : tasks)
        {
            callables.add(Executors.callable(task));
        }
            
        try
        {
            List<Future<Object>> results = pool.getExecutor().invokeAll(callables);

            for (Future<Object> result : results)
            {
                Object done = result.get();
            }
        }
        catch (InterruptedException ex)
        {
            logger.error("Interrupted: {}", ex);
        }
        catch (CancellationException ex)
        {
            logger.error("Timeout Error: {}", ex);
        }
        catch (ExecutionException ex)
        {
            logger.error("Execution Error: {}", ex);
        }
        catch (Exception ex)
        {
            logger.error("Unknown Execution Error: {}", ex);
        }
    }
}
