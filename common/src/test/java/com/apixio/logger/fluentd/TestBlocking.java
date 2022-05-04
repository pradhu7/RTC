//package com.apixio.logger.fluentd;
//
//import static org.junit.Assert.*;
//
//import org.apache.log4j.AsyncAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.spi.AppenderAttachable;
//import org.apache.log4j.spi.LoggingEvent;
//import org.junit.Before;
//import org.junit.Test;
//
//
///**
// * verify dynamic proxy worked, not LoggedProxy features
// * TestLoggedImpl has doLog() false, so don't have to set up CallbackAppender 
// *
// * @author lance
// *
// */
//
//public class TestBlocking {
//    MockAppender mockapp = null;
//    AsyncAppender aapp = null;
//    long start;
//
//    @Before
//    public void setup() {
//        mockapp = new MockAppender(false);
//        aapp = new AsyncAppender();
//        ((AppenderAttachable)aapp).addAppender(mockapp);
//        start = System.currentTimeMillis();
//        mockapp.start = start;
//    }
//
//    @Test
//    public void testBlocking() throws Exception {
//        int bufsize = 4;
//        int mocksleep = 3000;
//        print("Test AsyncAppender in blocking mode. Buffer size " + bufsize);
//        mockapp.blocking = true;
//        mockapp.blocktime = mocksleep;
//        aapp.setBlocking(true);
//        aapp.setBufferSize(bufsize);
//        assert(mockapp.lastMessage == null);
//        Logger logger = Logger.getLogger("mock");
//        LoggingEvent event = null;
//        for(int i = 1; i <= 20; i++) {
//            event = new LoggingEvent("mock", logger, 
//                            Level.INFO, "message " + i, null);
//            print("Sending: " + event.getMessage());
//            aapp.append(event);
//        }
//        print("Done sending");
//        // let aa's threads kick over
//        Thread.sleep(mocksleep * (bufsize *2));
//        assert(mockapp.lastMessage.equals("message 10"));
//        print("Done receiving");
//    }
//
//    @Test
//    public void testNonBlocking() throws Exception {
//        int bufsize = 2;
//        int mocksleep = 5000;
//        print("Test AsyncAppender in non-blocking mode. Buffer size " + bufsize);
//        mockapp.blocking = true;
//        mockapp.blocktime = mocksleep;
//        aapp.setBlocking(false);
//        aapp.setBufferSize(bufsize);
//        assert(mockapp.lastMessage == null);
//        LoggingEvent event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 1", null);
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        assert(mockapp.lastMessage.equals("message 1"));
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 2", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 3", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 4", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 5", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 6", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 7", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 8", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 9", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        event = new LoggingEvent("mock", Logger.getLogger("mock"), 
//                        Level.INFO, "message 10", null); 
//        print("Sending: " + event.getMessage());
//        aapp.append(event);
//        print("Done");
//        Thread.sleep(mocksleep * (bufsize +2));
//        assertTrue(mockapp.lastMessage.startsWith("Discarded"));
//    }
//
//    private void print(String message) {
//        System.out.println(Long.toString(System.currentTimeMillis() - start) + "\t" + message);
//        System.out.flush();
//    }
//
//}
