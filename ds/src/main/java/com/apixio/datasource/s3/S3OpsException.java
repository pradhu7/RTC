package com.apixio.datasource.s3;

import java.io.IOException;

public class S3OpsException
    extends IOException
{
    public S3OpsException()
    {
        super();
    }

    public S3OpsException(String message)
    {
        super(message);
    }

    public S3OpsException(Throwable t)
    {
    	super(t);
    }

    public S3OpsException(String message, Throwable t)
    {
    	super(message, t);
    }
}
