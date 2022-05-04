package com.apixio.model.converter.exceptions;

/**
 * Created by jctoledo on 6/8/16.
 */
public class InsuranceClaimFilteringException extends  RuntimeException {
    public InsuranceClaimFilteringException(String msg){super(msg);}
    public InsuranceClaimFilteringException(Throwable cause){super(cause);}
    public InsuranceClaimFilteringException(String msg, Throwable cause){super(msg,cause);}
}
