package com.apixio.logger.tracks;

import static org.junit.Assert.*;

import org.junit.Test;

import com.apixio.logger.tracks.TrackedProxy;

/**
 * verify dynamic proxy worked, not LoggedProxy features
 * TestLoggedImpl has doLog() false, so don't have to set up CallbackAppender 
 *
 * @author lance
 *
 */

public class TestLoggedProxy {

    @Test
    public void testProxyCreate() throws Exception {
        TestLogged fixture = new TestLoggedImpl();
        fixture.test1("life");
        assertEquals("life", fixture.getThug1());
        TestLogged proxy = (TestLogged) TrackedProxy.newInstance(fixture);
        assertTrue(proxy instanceof TestLogged);
        proxy.test1("death");
        assertEquals("death", fixture.getThug1());
    }

}
