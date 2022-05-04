package com.apixio.datasource.cassandra;

public class CqlException
    extends RuntimeException
{
    public CqlException()
    {
        super();
    }

    public CqlException(String message)
    {
        super(message);
    }

    public CqlException(Throwable t)
    {
    	super(t);
    }

    public CqlException(String message, Throwable t)
    {
    	super(message, t);
    }
}
