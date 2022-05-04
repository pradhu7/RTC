package com.apixio.logger.metrics;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

@Metrics(managed = true)
public class MetricAppImpl implements MetricApp {

    @Override
    @Timed(name = "timed", absolute = true)
    public void timer() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    @Override
    @Metered(name = "metered1", absolute = true)
    public void meter() {
    }

    @Override
    @Metered(name = "metered2", absolute = true)
    @ExceptionMetered(name = "exception", absolute = true)
    public void exception(boolean toss) throws Exception {
        if (toss)
            throw new Exception();
    }

}
