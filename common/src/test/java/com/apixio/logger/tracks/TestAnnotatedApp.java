package com.apixio.logger.tracks;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.log4j.MDC;

import com.apixio.logger.EventLogger;
import com.apixio.logger.tracks.TrackMDC;
import com.apixio.logger.tracks.TrackedProxy;
import com.apixio.logger.tracks.app.App;
import com.apixio.logger.tracks.app.AppImpl;

// TODO: factor app package and this test zone to allow 
// both local tests using CaptureAppender and remote tests sending to Fluent/Graphite

@Ignore("These tests have been failing for a long time. Not even clear whether this functionality is used")
public class TestAnnotatedApp {
    // trigger creation of log4j tree
    static EventLogger logger = EventLogger.getLogger("TestAnnotatedApp");
    Callback callback = new Callback();
    Callback callback2 = new Callback();
    Callback callback3 = new Callback();
    Callback callback4 = new Callback();

    @Before
    public void setup() {
        CallbackAppender.setCallbacks(callback, callback2, callback3, callback4);
        CallbackAppender.clear();
        TrackMDC.clear();
        MDC.clear();
    }

    @Test
    public void testTimer() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        proxy.methodTimer();
        assertNotNull(callback3.mdc.get("app.timer1"));
        assertNotNull(callback3.mdc.get("app.timer2"));
        assertTrue(1000 > Long.parseLong(callback3.mdc.get("app.timer1")));
        assertTrue(1000 > Long.parseLong(callback3.mdc.get("app.timer2")));
    }

    @Test
    public void testConstants() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        proxy.methodConstants();
        assertEquals("constant1", callback3.mdc.get("app.call1"));
        assertEquals("constant2", callback3.mdc.get("app.call2"));
    }

    @Test
    public void testParams() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        proxy.methodParams("ignore", "value", "ignore");
        assertEquals("value", callback3.mdc.get("app.method.param"));
    }

    @Test()
    public void testStatusSuccess() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        proxy.methodStatus(false);
        assertEquals("success", callback3.mdc.get("app.status"));
    }
    
    @Test()
    public void testStatusError() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        try {
            proxy.methodStatus(true);
        } catch (Throwable e) {
            assertTrue(callback3.mdc.get("message").length() > 0);
            assertEquals("error", callback4.mdc.get("app.status"));
            assertNotNull(callback4.mdc.get("error.message"));
        }
    }
    
    @Test
    public void testMDC() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        proxy.methodMDC("app.mdc", "mdc");
        assertEquals("mdc", callback3.mdc.get("app.mdc"));
    }

    @Test
    public void testLogged() throws Exception {
        App app = new AppImpl();
        App proxy = (App) TrackedProxy.newInstance(app);
        proxy.methodLogged("app.logged", "logged");
        assertEquals("logged", callback3.mdc.get("app.logged"));
    }

}
