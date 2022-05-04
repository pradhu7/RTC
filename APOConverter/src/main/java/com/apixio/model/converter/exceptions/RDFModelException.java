package com.apixio.model.converter.exceptions;

/**
 * Created by jctoledo on 11/1/16.
 */
public class RDFModelException extends RuntimeException {
    public RDFModelException(String msg){super(msg);}
    public RDFModelException(Throwable cause){super(cause);}
    public RDFModelException(String msg, Throwable cause){super(msg,cause);}
}
