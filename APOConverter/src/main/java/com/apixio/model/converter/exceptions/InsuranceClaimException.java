package com.apixio.model.converter.exceptions;

/**
 * Created by jctoledo on 12/13/16.
 */
public class InsuranceClaimException extends  RuntimeException {
    public InsuranceClaimException(String msg){super(msg);}
    public InsuranceClaimException(Throwable cause){super(cause);}
    public InsuranceClaimException(String msg, Throwable cause){super(msg,cause);}
}
