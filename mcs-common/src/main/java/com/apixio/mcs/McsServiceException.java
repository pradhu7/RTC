package com.apixio.mcs;

import java.net.URI;

public class McsServiceException extends Exception
{

    public McsServiceException(String msg)
    {
        super(msg);
    }   

    public McsServiceException(String msg, Throwable cause)
    {
        super(msg, cause);
    }   

    /**
     * Constructors specific to making REST requests
     */
    public McsServiceException(int restStatusCode, String httpMethod, URI requestUri)
    {
        this(makeMessage(restStatusCode, httpMethod, requestUri));
    }

    public McsServiceException(int restStatusCode, String httpMethod, URI requestUri, Throwable cause)
    {
        this(makeMessage(restStatusCode, httpMethod, requestUri), cause);
    }

    private static String makeMessage(int restStatusCode, String httpMethod, URI requestUri)
    {
        return "Failed REST request of " + httpMethod + " on " + requestUri + ": returned code " + restStatusCode;
    }

}
