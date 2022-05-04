package com.apixio.security.exception;

/**
 *
 */
public class ApixioSecurityException extends RuntimeException
{
    public ApixioSecurityException()
    {
        super();
    }

    public ApixioSecurityException(String msg)
    {
        super(msg);
    }

    public ApixioSecurityException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
