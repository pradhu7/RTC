package com.apixio.logger.fluentd;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class MockAppender extends AppenderSkeleton {
    boolean blocking;
    public String lastMessage = null;
    long blocktime = 100000000;
    long start;
    
    public MockAppender(boolean blocking) {
        this.blocking = blocking;
    }

    @Override
    public void close() {
        
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(LoggingEvent message) {
        System.out.println(Long.toString(System.currentTimeMillis() - start) + "\tMockAppender: " + message.getMessage());
        System.out.flush();

        lastMessage = message.getMessage().toString();
        if (blocking) {
            try {
                Thread.sleep(blocktime);
            } catch (InterruptedException e) {
            }
        } else {
            return;
        }

    }

}
