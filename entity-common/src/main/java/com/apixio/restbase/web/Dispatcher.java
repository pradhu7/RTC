package com.apixio.restbase.web;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;

import com.apixio.restbase.RestUtil;
import com.apixio.restbase.web.Microfilter.Action;
import com.apixio.restbase.web.Microfilter.Context;

/**
 * Dispatcher is the main controller for a series of smaller "microfilters".  The
 * intent of this construct (as compared to just configuring a bunch of web Filters)
 * is both to be somewhat more efficient than a Filter and to take control over
 * the configuration of them, given that they must be deployed in multiple app
 * servers (dropwizard and tomcat/jetty, for example).
 */
public class Dispatcher implements Filter {

    /**
     * The filter configuration object we are associated with. If this value is
     * null, this filter instance is not currently configured.
     */
    private FilterConfig filterConfig = null;

    /**
     * The list of already-configured microfilters.
     */
    private List<Microfilter> microfilters;

    public void setMicrofilters(List<Microfilter> microfilters)
    {
        this.microfilters = microfilters;
    }

    /**
     * Place this filter into service.
     * 
     * @param filterConfig
     *          The filter configuration object
     */
    @Override
    public void init(
        FilterConfig filterConfig
        )
    {
        this.filterConfig = filterConfig;
    }

    /**
     * Take this filter out of service.
     */
    @Override
    public void destroy()
    {
        filterConfig = null;
    }

    /**
     * Perform the microfilter dispatch.  The dispatch model is to go through the
     * configured list of microfilters and passing control to the beforeDispatch
     * method.  If that method handles the full request (e.g., it returns a 401
     * code), then the filter does an early exit/return, otherwise it moves on
     * to the next in the list, optionally remembering if the microfilter is to
     * be called on the way back out.
     */ 
    public void doFilter(
        ServletRequest  request,
        ServletResponse response,
        FilterChain     chain
        )
        throws IOException,
               ServletException
    {
        if ((request instanceof HttpServletRequest) && (microfilters != null))
        {
            HttpServletRequest  req    = (HttpServletRequest)  request;
            HttpServletResponse res    = (HttpServletResponse) response;
            String              path   = req.getServletPath();
            List<Microfilter>   unwind = new ArrayList<Microfilter>(microfilters.size());
            Context             ctx    = new Context(req, res);
            boolean             exc    = false;

            RestUtil.setDispatchContext(ctx);

            try
            {
                String   pi   = RestUtil.getRequestPathInfo(req);
                boolean  next = true;

                for (int i = 0, m = microfilters.size(); i < m; i++)
                {
                    Microfilter mf     = microfilters.get(i); 
                    Action      action = mf.beforeDispatch(ctx);

                    if (action == Action.ResponseSent)
                    {
                        next = false;
                        //                        System.out.println("Response already sent by microfilter " + mf);
                        break;
                    }
                    else if (action == Action.NeedsUnwind)
                    {
                        unwind.add(mf);
                    }
                    else if (action == Action.NextInChain)
                    {
                    }
                    else
                    {
                        // bad return value => log it
                    }
                }

                if (next)
                    chain.doFilter(ctx.req, ctx.res);  // ctx.req/res to allow wrapping
            }
            catch (Exception x)
            {
                ctx.error = true;
                exc       = true;   // so we really force a 503 to be logged.
                x.printStackTrace();
            }
            finally
            {
                RestUtil.setDispatchContext(null);

                for (int i = unwind.size() - 1; i >= 0; i--)
                {
                    // we don't want a failure in one unwind level to prevent other unwinding
                    try
                    {
                        unwind.get(i).afterDispatch(ctx);
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                }

                if (exc)  // have dropwizard record a 503 only if we really got an exception up here
                    res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }
    }
    

}
