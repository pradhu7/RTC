package com.apixio.model.commonparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.apixio.model.commonparser.SerDerObjectMapper.InitializeType;

public abstract class ApixioJSONParser<T>
{
    private static ObjectMapper objectMapper = new ObjectMapper();

    public ApixioJSONParser(InitializeType initializer)
    {
        switch (initializer)
        {
            case NonCompact:
                SerDerObjectMapper.initializeNonCompact(objectMapper);
                break;
            case Compact:
                SerDerObjectMapper.initializeCompact(objectMapper);
                break;
            case Basic:
                SerDerObjectMapper.initializeBasic(objectMapper);
                break;
        }
    }

    protected T parse(String jsonString, Class<T> t)
        throws IOException
    {
        return objectMapper.readValue(jsonString, t);
    }

    public String toJSON(T t)
        throws IOException
    {
        return objectMapper.writeValueAsString(t);
    }

    public  byte[]  toBytes(T t)
            throws IOException
    {
        return objectMapper.writeValueAsBytes(t);
    }

    public void serialize(OutputStream outputStream, T t)
            throws IOException
    {
        objectMapper.writeValue(outputStream, t);
    }

    public abstract T parse(String jsonString)
        throws IOException;
}

