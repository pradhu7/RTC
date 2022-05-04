package com.apixio.model.converter.exceptions;

/**
 * Created by jctoledo on 4/22/16.
 */
public class RDFModelToEventTypeException extends RuntimeException{
    public RDFModelToEventTypeException(String msg){super(msg);}
    public RDFModelToEventTypeException(Throwable cause){super(cause);}
    public RDFModelToEventTypeException(String msg, Throwable cause){super(msg,cause);}
}
