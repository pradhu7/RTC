//package com.apixio.logger.tracks.app;
//
//import java.util.Map;
//
//import junit.framework.TestCase;
//
//import org.junit.Test;
//import org.apache.log4j.MDC;
//
//import com.apixio.logger.EventLogger;
//import com.apixio.logger.tracks.Callback;
//import com.apixio.logger.tracks.CallbackAppender;
//import com.apixio.logger.tracks.TrackMDC;
//import com.apixio.logger.tracks.TrackedProxy;
//
///**
// * Test the tracks left by calling up and down a stack of multiple @Tracked classes.
// * Top and Middle are loggers, Bottom is not. Top is a frame, Middle and Bottom are not.
// * Not sure this stays this complex in mutant version. 
// * @author lance
// *
// */
//
//public class TestMultipleLevels extends TestCase {
//    // trigger creation of log4j tree
//    static EventLogger logger = EventLogger.getLogger("TestMultipleLevels");
//
//    Callback[] callbacks;
//    private Top top;
//    private Middle middle;
//    private Bottom bottom;
//    
//    @Override
//    protected void setUp() throws Exception {
//        logger.info("Test setup");
//        top = (Top) TrackedProxy.newInstance(new TopImpl());
//        middle = (Middle) TrackedProxy.newInstance(new MiddleImpl());
//        bottom = (Bottom) TrackedProxy.newInstance(new BottomImpl());
//        Top top2 = (Top) TrackedProxy.newInstance(new TopImpl());
//        Middle middle2 = (Middle) TrackedProxy.newInstance(new MiddleImpl());
//        top.setName("top1");
//        top2.setName("top2");
//        middle.setName("middle1");
//        middle2.setName("middle2");
//        top.knit(top2, middle, bottom);
//        middle.knit(top, middle2, bottom);
//        bottom.knit(middle);
//        callbacks = new Callback[10];
//        for(int i = 0; i < 10; i++)
//            callbacks[i] = new Callback();
//        CallbackAppender.setCallbacks(callbacks);
//        TrackMDC.clear();
//        MDC.clear();
//    }
//    
//    @Test
//    public void testNothing() {
//        top.doNothing();
//        checkAndReset("top.name", "top1", "top.nothing", "true");
//        middle.doNothing();
//        checkAndReset("middle.nothing", "true", "middle.name", "middle1");
//        bottom.doNothing();
//        // no reports from bottom
//        checkAndReset();
//    }
//    
//    @Test
//    public void testTop() {
//        top.callSelf();
//        checkAndReset("top.self", "true", "top.name", "top1"); // call this does not trigger top.nothing annotation
//        top.callKid();
//        checkAndReset("middle.name", "middle1", "middle.nothing", "true", "top.kid", "true");
//        top.callGrandKid();
//        checkAndReset("top.grandkid", "true", "bottom.nothing", "true");
//        top.callPeer();
//        checkValues(0, "top.name", "top2", "top.nothing", "true");
//        checkValues(1, "top.peer", "true");
//        reset();
//    }
//    
//    @Test
//    public void testMiddle() {
//        middle.callSelf();
//        checkAndReset("middle.name", "middle1", "middle.self", "true");
//        middle.callKid();
//        checkAndReset("middle.kid", "true", "bottom.nothing", "true");
//        middle.callPeer();
//        checkAndReset("middle.name", "middle2", "middle.nothing", "true",  "middle.peer", "true");
//        middle.callKidParent();
//        checkAndReset("middle.nothing", "true", "middle.name", "middle1", "bottom.parent", "true", "middle.kidparent", "true");
//    }
//    
//    @Test
//    public void testBottom() {
//        
//        MDC.put("junk", "data");
//        checkMDCReset("junk", "data");
//        bottom.callSelf();
//        checkFrameReset("bottom.self", "true");
//        bottom.callParent();
//        checkValues(0, "bottom.parent", "true", "middle.nothing", "true", "middle.name", "middle1");
//        reset();
//    }
//    
//    void checkAndReset(String... values) {
//        checkValues(0, values);
//        reset();
//    }
//    
//    void checkValues(int index, String... values) {
//        Map<String,String> mdc = callbacks[index].mdc;
//        mdc.remove("level");
//     
////        System.out.println("Match:");
////        System.out.println("\t" + Arrays.toString(values));
////        System.out.println("\t" + mdc.toString());
//        int size = mdc.size();
//        int length = values.length/2;
//        assertTrue("mdc.length: " + mdc.size() + ", values.length: " + values.length/2, size == length);
//        for(int i = 0; i < values.length; i+= 2) {
//            assertTrue(mdc.containsKey(values[i]) && mdc.get(values[i]).equals(values[i+1]));
//        }
//    }
//    
//    void checkFrameReset(String... values) {
//        Map<String, Object> map = TrackMDC.getCopyOfContext();
//        for(int i = 0; i < values.length; i+= 2) {
//            assertTrue(map.get(values[i]) != null && map.get(values[i]).toString().equals(values[i+1]));
//        }
//        TrackMDC.clear();
//    }
//    
//   void checkMDCReset(String... values) {
//        for(int i = 0; i < values.length; i+= 2) {
//            assertTrue(MDC.get(values[i]) != null && MDC.get(values[i]).equals(values[i+1]));
//        }
//        MDC.clear();
//    }
//    
//    void reset() {
//        CallbackAppender.clear();
//        TrackMDC.clear();
//        MDC.clear();
//    }
//
//}
