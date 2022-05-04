package com.apixio.restbase.apiacl.model;

/**
 * The canonical runtime representation of the supported HTTP methods, including
 * the wildcard one ("ANY").
 */
public enum HttpMethod {

    GET, POST, PUT, DELETE, PATCH, ANY;

    public static HttpMethod fromString(String name)
    {
        if (name.equals("*"))
            return ANY;
        else
            return valueOf(name.toUpperCase());
    }

}
