package com.apixio.logger.tracks;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.MDC;

/**
 * Log4j Appender that copies MDC and saves it in a preset location.
 * So, unit tests have to be run with log4j. Erg.
 * 
 * @author lance
 *
 */
public class CallbackAppender extends AppenderSkeleton {
    static CallbackAppender instance;
    
    private final Callback[] callbacks;
    private int index;
    

     public CallbackAppender() {
        System.out.println("Construct CallbackAppender");
        instance = this;
        callbacks = new Callback[10];
        Callback.hello();
        clearList();
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean requiresLayout() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void append(LoggingEvent event) {
        if (index >= callbacks.length) {
            System.err.println("CallbackAppender: index = " + index);
            index++;
            return;
        }
        Map mdc = MDC.getContext();
        if (mdc == null) {
            mdc = new HashMap<String, String>();
        } else {
            mdc = new HashMap(mdc);
        }
        if (callbacks[index] != null)
            callbacks[index++].mdc = mdc;
    }

    public static void setCallbacks(Callback ... callbacks) {
        for(int i = 0; i < callbacks.length; i++)
            CallbackAppender.instance.callbacks[i] = callbacks[i];
        CallbackAppender.instance.index = 0;
    }
    
   public static void clear() {
        instance.clearList();
    }
    
    void clearList() {
        for(int i = 0; i < callbacks.length; i++)
            if (callbacks[i] != null && callbacks[i].mdc != null) {
                callbacks[i].mdc.clear();
            }
        index = 0;
    }

}
