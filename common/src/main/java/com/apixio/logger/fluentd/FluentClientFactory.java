//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2013 OZAWA Tsuyoshi
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.apixio.logger.fluentd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import com.apixio.logger.fluentd.client.Constants;
import com.apixio.logger.fluentd.client.FluentClient;
import com.apixio.logger.fluentd.sender.RawSocketSender;
import com.apixio.logger.fluentd.sender.Sender;

public class FluentClientFactory {
	
    private final Map<String, FluentClient> loggers;

    public FluentClientFactory() {
        loggers = new WeakHashMap<String, FluentClient>();
    }

    public FluentClient getLogger(String tagPrefix) throws Exception {
        return getLogger(tagPrefix, "localhost", 24224);
    }

    public FluentClient getLogger(String tagPrefix, String host, int port) throws Exception {
        return getLogger(tagPrefix, host, port, 3 * 1000, 1 * 1024 * 1024);
    }

    public synchronized FluentClient getLogger(
            String tagPrefix, String host, int port, int timeout, int bufferCapacity) throws Exception {
        String key = String.format("%s_%s_%d_%d_%d",
                new Object[] { tagPrefix, host, port, timeout, bufferCapacity });
        if (loggers.containsKey(key)) {
            return loggers.get(key);
        } else {
            Sender sender = null;
            Properties props = System.getProperties();
            if (!props.containsKey(Constants.FLUENT_SENDER_CLASS)) {
                // create default sender object
                sender = new RawSocketSender(host, port, timeout, bufferCapacity);
            } else {
                String senderClassName = props.getProperty(Constants.FLUENT_SENDER_CLASS);
                    try
                    {
                        sender = createSenderInstance(senderClassName,
                                new Object[] { host, port, timeout, bufferCapacity });
                    } catch (Exception e)
                    {
                        throw e;
                    }
            }
            FluentClient logger = new FluentClient(tagPrefix, sender);
            loggers.put(key, logger);
            return logger;
        }
    }

    @SuppressWarnings("unchecked")
    private Sender createSenderInstance(final String className,
            final Object[] params) throws ClassNotFoundException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Class<Sender> cl = (Class<Sender>)
                FluentClient.class.getClassLoader().loadClass(className);
        Constructor<Sender> cons = cl.getDeclaredConstructor(
                new Class[] { String.class, int.class, int.class, int.class });
        return (Sender) cons.newInstance(params);
    }

    /**
     * the method is for testing
     */
    public Map<String, FluentClient> getLoggers() {
        return loggers;
    }

    public synchronized void closeAll() {
        for (FluentClient logger : loggers.values()) {
            logger.close();
        }
        loggers.clear();
    }

    public synchronized void flushAll() {
        for (FluentClient logger : loggers.values()) {
            logger.flush();
        }
    }

}

