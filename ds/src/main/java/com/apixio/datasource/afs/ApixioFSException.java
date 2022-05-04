package com.apixio.datasource.afs;

import java.io.IOException;

public class ApixioFSException
    extends IOException
{
    public ApixioFSException()
    {
        super();
    }

    public ApixioFSException(String message)
    {
        super(message);
    }

    public ApixioFSException(Throwable t)
    {
    	super(t);
    }

    public ApixioFSException(String message, Throwable t)
    {
    	super(message, t);
    }
}
