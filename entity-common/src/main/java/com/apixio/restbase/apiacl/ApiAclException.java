package com.apixio.restbase.apiacl;

import javax.ws.rs.WebApplicationException;

/**
 * 
 */
public class ApiAclException extends WebApplicationException {

    public ApiAclException(String reason)
    {
        super(reason);
    }

}
