package com.apixio.jobs.util;

public class NoPageException extends RuntimeException {

    public static final long serialVersionUID = 0x08103834;

    public NoPageException()
    {
        super();
    }

    public NoPageException(String message)
    {
        super(message);
    }

    public NoPageException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NoPageException(Throwable cause)
    {
        super(cause);
    }
}
