package com.apixio.restbase.apiacl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Contains the information needed by all Extractors.  It has to be fillable from both
 * ReaderInterceptor's MultivaluedMap as well as from an HttpServletRequest
 */
public class HttpRequestInfo {

    /**
     * These are the names of the (added, hidden) HTTP headers used to pass down
     * the extractable information from HttpServletRequest to the ReaderInterceptor
     * via adding to the list of HTTP headers.
     */
    public static final String REQUEST_PATHINFO_HEADERNAME    = "_pathInfo";
    public static final String REQUEST_METHOD_HEADERNAME      = "_method";
    public static final String REQUEST_QUERYSTRING_HEADERNAME = "_queryString";
    public static final String REQUEST_AUTH_HEADERNAME        = "_auth";

    /**
     * The pulled values
     */
    private String pathInfo;
    private String method;
    private String queryString;
    private String authHeader;
    private String contentType;
    private Map<String, String> queryParams;

    /**
     * Create an instance by pulling the needed info directly from HttpServletRequest.
     */
    public HttpRequestInfo(HttpServletRequest req)
    {
        pathInfo    = req.getPathInfo();
        method      = req.getMethod();
        queryString = req.getQueryString();
        authHeader  = req.getHeader("Authorization");
        contentType = req.getHeader(HttpHeaders.CONTENT_TYPE);
    }

    /**
     * Create an instance by pulling them from the (wrapped) HTTP header list that's
     * available via MultivaluedMap.
     */
    public HttpRequestInfo(MultivaluedMap<String, String> map)
    {
        pathInfo    = map.getFirst(REQUEST_PATHINFO_HEADERNAME);
        method      = map.getFirst(REQUEST_METHOD_HEADERNAME);
        queryString = map.getFirst(REQUEST_QUERYSTRING_HEADERNAME);
        authHeader  = map.getFirst(REQUEST_AUTH_HEADERNAME);
        contentType = map.getFirst(HttpHeaders.CONTENT_TYPE);
    }

    /**
     * Getters
     */
    public String getPathInfo()
    {
        return pathInfo;
    }

    public String getMethod()
    {
        return method;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public String getQueryParameter(String name)
    {
        if(this.queryString == null) {
            return null;
        } else
        {
            if (queryParams == null)
                queryParams = parseQueryString(queryString);

            return queryParams.get(name);
        }
    }

    public String getAuthHeader()
    {
        return authHeader;
    }

    public String getContentType()
    {
        return contentType;
    }

    /**
     * Unfortunately we can't rely on HttpServletRequest.getParameter("name") because we have no
     * access to it via the HTTP headers (we can only set a HTTP header value to a string, which,
     * in this case is the query string).
     */
    private Map<String, String> parseQueryString(String query)
    {
        Map<String, String> map = new HashMap<String, String>();

        for (NameValuePair nvp : URLEncodedUtils.parse(query, StandardCharsets.UTF_8))
            map.put(nvp.getName(), nvp.getValue());

        return map;
    }
}
