package com.apixio.logger.tracks.app;

import org.apache.log4j.MDC;

import com.apixio.logger.tracks.Clock;
import com.apixio.logger.tracks.Logged;
import com.apixio.logger.tracks.TrackParam;
import com.apixio.logger.tracks.Tracked;
import com.apixio.logger.tracks.Tracks;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

@Tracked(isFrame=true,doLog=true)
public class AppImpl implements App {

    @Override
    @Tracks(timer = "app.timer2")
    public void methodTimer() {
        Clock t = Clock.getTimer("app.timer1");
        t.tick();
    }

    @Override
    @Tracks({"app.call1","constant1", "app.call2","constant2"})
    public void methodConstants() {
    }

    @Override
    @Tracks
    public void methodLogged(String name, String value) {
        Logged four = new Logged(name, null);
        four.set(value);
    }

    @Override
    @Tracks
    public void methodMDC(String name, String value) {
        MDC.put(name, value);
    }

    @Override
    @Tracks
    public void methodParams(String abc, @TrackParam("app.method.param") String def, String ghi) {

    }

    @Override
    @Tracks(status = "app.status")
    public void methodStatus(boolean fail) throws Exception {
        if (fail)
            throw new Exception();
    }
    
    @Override
    @Metered(name = "app.metered", absolute = true)
    public void methodMetered() {
    }

    @Override
    @Metered(name = "app.metered", absolute = true)
    @ExceptionMetered(name = "app.exception_metered", absolute = true)
    public void methodExceptionMetered(boolean throwIt) throws Exception {
        if (throwIt)
            throw new Exception();
    }

    @Override
    @Timed(name = "app.timed", absolute = true)
    public void methodTimed() {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            
        }

    }

    @Override
    @Metered(name = "name", absolute = false)
    public void methodName() {
    }

}
