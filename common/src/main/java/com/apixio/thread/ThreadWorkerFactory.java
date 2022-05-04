package com.apixio.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;

/**
 * ThreadWorkerFactory extends the java ThreadFactory by giving the thread a name made of (prefix and incremental counter).
 */

public class ThreadWorkerFactory implements ThreadFactory
{
    private AtomicInteger counter = new AtomicInteger(0);
    private String prefix;

    public ThreadWorkerFactory(String prefix)
    {
        if (StringUtils.isBlank(prefix))
            throw new RuntimeException("ThreadWorkerFactory: empty prefix not allowed");

        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r)
    {
        Thread t = new Thread(r);

        counter.incrementAndGet();
        t.setDaemon(true);
        t.setName(prefix + counter.toString());

        return t;
    }
}
