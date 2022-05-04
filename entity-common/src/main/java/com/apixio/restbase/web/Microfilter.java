package com.apixio.restbase.web;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.apixio.restbase.DataServices;

/**
 * A Microfilter defines a small request-level filter that's invoked prior
 * to the real RESTful/jersey method is invoked.  It is intended to do things
 * like checking access control, or for validating tokens, etc.
 */
public class Microfilter<S extends DataServices>
{
    /**
     * Lots of microfilters need these services
     */
    protected S  services;

    /**
     * Context allows expansion of what's passed to before/afterDispatch without
     * forcing a method sig change.
     */
    public static class Context {
        public HttpServletRequest  req;
        public HttpServletResponse res;
        public boolean error;

        Context(HttpServletRequest req, HttpServletResponse res)
        {
            this.req = req;
            this.res = res;
        }
    }

    /**
     * Action tells what to do after beforeDispatch() returns.
     */
    public enum Action {
        ResponseSent,     // beforeDispatch has already sent something; force immediate return
        NeedsUnwind,      // afterDispatch() needs to be called after dispatch
        NextInChain       // pass control to next microfilter in chain
        }

    /**
     * Configure the microfilter at this level by recording the srvices and Redis info.
     *
     * Note that ALL classes that extend Microfilter should do a super.configure() in
     * their configure method.
     */
    public void configure(Map<String, Object> filterConfig, S services)
    {
        this.services = services;
    }

    /**
     * Default method body for handling the beforeDispatch case
     */
    public Action beforeDispatch(Context ctx) throws IOException
    {
        return Action.NextInChain;
    }

    /**
     * Default method body for handling the afterDispatch case
     */
    public Action afterDispatch(Context ctx)
    {
        return Action.NextInChain;
    }

}
