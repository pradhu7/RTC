package com.apixio.restbase.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.apixio.restbase.DataServices;
import com.apixio.restbase.apiacl.AccessResult;
import com.apixio.restbase.apiacl.ApiAcls.CheckResults;
import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.HttpRequestInfo;

/**
 * AclChecker performs the API ACL check by calling into the RESTful API access control
 * system.  If the API access can be checked and rejected/allowed at this top-level
 * filter level, then it does the check and either immedicately rejects (403) the request or
 * fully allows the operation through.  There is one case where this immediate go/no-go
 * can't be done at the filter level as the HTTP entity body is needed.  In this case
 * this class transfers info needed from the request to the HTTP headers and sets up
 * an HttpServletRequestWrapper to supply those synthetic headers.
 */
public class AclChecker extends Microfilter<DataServices>
{
    private ApiAcls apiAcls;

    /**
     * In order to support returning a 403 FORBIDDEN from a ReaderInterceptor (which is
     * necessary if we have to extract the ACL Object from the HTTP entity body), the
     * only thing that is common between the (context-less, really) ReaderInterceptor
     * and the top-level code that returns the final code is the thread.  So we create
     * a ThreadLocal here that has the int override (if any).  Note that if this
     * assumption that the same thread is used by the ReaderInterceptor and the
     * ContainerReseponseFilter isn't valid, then we will NOT be able to return a 403
     * for an ACL-deny picked up by the ApiReaderInterceptor.
     */

    /**
     * In order to support efficient testing of ACLs by the ReaderInterceptor code,
     * we need to be able to pass down the already-matched protected API information
     * along with the query parameters.  Without this, the ReaderInterceptor would need
     * to re-match against all protected APIs, etc.
     */

    /**
     * AclContext holds the information needed to support passing information to and
     * from the ReaderInterceptor-based ACL check.  Instances of this class are always
     * and only used as thread locals.
     */
    private static class AclContext {
        int          responseCode = -1;
        CheckResults checkResults;
    }

    private static ThreadLocal<AclContext> tlAclContext = new ThreadLocal<AclContext>();

    /**
     * Sets the ACL enforcer.  If ths sytem is not configured for ACL enforcement, then
     * this filter really doesn't do anything.
     */
    public void setApiAcls(ApiAcls apiAcls)
    {
        this.apiAcls = apiAcls;
    }

    /**
     *
     */
    private static AclContext getAclContext()
    {
        AclContext context = tlAclContext.get();

        if (context == null)
        {
            context = new AclContext();
            tlAclContext.set(context);
        }

        return context;
    }

    private static void clearAclContext()
    {
        tlAclContext.set(null);
    }

    /**
     * Sets the Integer override for the current thread.
     */
    public static void overrideResponseStatus(int sc)
    {
        AclContext ctx = getAclContext();

        if (ctx.responseCode == -1)
            ctx.responseCode = sc;
        else
            System.out.println("ERROR:  AclChecker.overrideResponseStatus already called!");
    }

    /**
     * Returns the override code set via overrideResponseStatus, or -1, if no override
     */
    public static int getOverrideResponseStatus()
    {
        return getAclContext().responseCode;
    }

    private static void setCheckResults(CheckResults results)
    {
        AclContext ctx = getAclContext();

        if (ctx.checkResults == null)
            ctx.checkResults = results;
        else
            System.out.println("ERROR:  AclChecker.setCheckResults already called!");
    }

    public static CheckResults getCheckResults()
    {
        return getAclContext().checkResults;
    }

    /**
     * If there is an ACL enforcer in place, then check the incoming request and either
     * allow, disallow, or delay the check.
     */
    @Override
    public Action beforeDispatch(Context ctx) throws IOException
    {
        if (apiAcls != null)
        {
            HttpRequestInfo ri = new HttpRequestInfo(ctx.req);
            CheckResults    cr = apiAcls.requestAllowed(ri, null);

            // CheckResult.ALLOW handled by not adding Wrapper

            if (cr.access == AccessResult.DISALLOW)
            {
                ctx.res.setStatus(HttpServletResponse.SC_FORBIDDEN);

                return Action.ResponseSent;
            }
            else if (cr.access == AccessResult.NOT_FOUND)
            {
                ctx.res.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return Action.ResponseSent;
            }
            else if (cr.access == AccessResult.NEED_ENTITY)
            {
                setCheckResults(cr);

                // delegate final check to when we have the entity body so replace req/res with wrapped
                ctx.req = new RequestWrapper(ri, ctx.req);
            }
        }

        return Action.NeedsUnwind;
    }

    /**
     * If there is an ACL enforcer in place, then check the incoming request and either
     * allow, disallow, or delay the check.
     */
    @Override
    public Action afterDispatch(Context ctx)
    {
        clearAclContext();

        return Action.NextInChain;
    }

    /**
     * Used to pass down non-header information available only on HttpServletRequest but that
     * is needed by code that only has access to the HTTP headers.
     */
    private static class RequestWrapper extends HttpServletRequestWrapper {

        private HttpRequestInfo requestInfo;

        RequestWrapper(HttpRequestInfo ri, HttpServletRequest req)
        {
            super(req);
            this.requestInfo = ri;
        }

        @Override
        public String getHeader(String name)
        {
            String added = getAddedHeader(name);

            if (added != null)
                return added;
            else
                return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames()
        {
            List<String>        all  = new ArrayList<String>();
            Enumeration<String> real = super.getHeaderNames();

            // simplest way to make a single Enumeration...but ugly

            all.add(HttpRequestInfo.REQUEST_PATHINFO_HEADERNAME);
            all.add(HttpRequestInfo.REQUEST_METHOD_HEADERNAME);
            all.add(HttpRequestInfo.REQUEST_QUERYSTRING_HEADERNAME);
            all.add(HttpRequestInfo.REQUEST_AUTH_HEADERNAME);
            
            while (real.hasMoreElements())
                all.add(real.nextElement());

            return Collections.enumeration(all);
        }

        @Override
        public Enumeration<String> getHeaders(String name)
        {
            String  toAdd = getAddedHeader(name);

            if (toAdd != null)
            {
                List<String> all = new ArrayList<String>(2);

                all.add(toAdd);

                return Collections.enumeration(all);
            }
            else
            {
                return super.getHeaders(name);
            }
        }

        private String getAddedHeader(String name)
        {
            if (HttpRequestInfo.REQUEST_PATHINFO_HEADERNAME.equals(name))
                return requestInfo.getPathInfo();
            else if (HttpRequestInfo.REQUEST_METHOD_HEADERNAME.equals(name))
                return requestInfo.getMethod();
            else if (HttpRequestInfo.REQUEST_QUERYSTRING_HEADERNAME.equals(name))
                return requestInfo.getQueryString();
            else if (HttpRequestInfo.REQUEST_AUTH_HEADERNAME.equals(name))
                return requestInfo.getAuthHeader();
            else
                return null;
        }
    }

}
