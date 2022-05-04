package com.apixio.restbase.web;

import com.apixio.restbase.DataServices;
import com.apixio.restbase.RestUtil;

/**
 * Simple Microfilter to report on how long request processing takes.
 */
public class RequestTimer extends Microfilter<DataServices>
{
    private static final ThreadLocal<Long> tlTimer = new ThreadLocal<Long>();

    @Override
    public Action beforeDispatch(Context ctx)
    {
        tlTimer.set(Long.valueOf(System.currentTimeMillis()));

        return Action.NeedsUnwind;
    }

    @Override
    public Action afterDispatch(Context ctx)
    {
        Long   start   = tlTimer.get();
        String reqInfo = "[RequestTimer] " + ctx.req.getMethod() + ": " + RestUtil.getRequestPathInfo(ctx.req);

        if (start == null)
            System.out.println("BAD BAD:  no start timer for " + reqInfo);
        else
            System.out.println("#### Timing [" + reqInfo + "] took " + (System.currentTimeMillis() - start.longValue()) + " ms");

        tlTimer.set(null);

        return Action.NextInChain;
    }

}
