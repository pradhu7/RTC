package com.apixio.restbase.apiacl.model;

import com.apixio.restbase.apiacl.UrlTemplate;

/**
 * The canonical runtime representation of an API that needs to be protected.
 */
public class ApiDef {

    private String id;
    private String name;
    private String url;
    private HttpMethod httpMethod;
    private UrlTemplate urlTemplate;

    public ApiDef(String id, String name, String url)
    {
        this.urlTemplate = new UrlTemplate(url);
        this.id          = id;
        this.name        = name;
    }

    public String getApiID()
    {
        return id;
    }

    public String getApiUrl()
    {
        return url;
    }

    public UrlTemplate getUrlTemplate()
    {
        return urlTemplate;
    }

    @Override
    public String toString()
    {
        return "[ApiDef " + urlTemplate + "]";
    }
}
