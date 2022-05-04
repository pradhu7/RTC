package com.apixio.datasource.s3;

/**
 * Created by mramanna on 11/3/17.
 */
public class KeyNotFoundException extends RuntimeException {

    public static final long serialVersionUID = 0x4263212;

    public KeyNotFoundException()
    {
    }

    public KeyNotFoundException(String message)
    {
        super(message);
    }

    public KeyNotFoundException(Throwable cause)
    {
        super(cause);
    }

    public KeyNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
