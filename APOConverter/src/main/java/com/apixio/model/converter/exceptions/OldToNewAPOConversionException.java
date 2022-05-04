package com.apixio.model.converter.exceptions;

/**
 * Created by jctoledo on 3/10/16.
 */
public class OldToNewAPOConversionException extends RuntimeException {
    public OldToNewAPOConversionException(String msg){super (msg);}
    public OldToNewAPOConversionException(Throwable cause) {super(cause);}
    public OldToNewAPOConversionException(String msg, Throwable cause){super(msg,cause);}
}
