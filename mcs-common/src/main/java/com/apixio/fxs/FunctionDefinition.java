package com.apixio.fxs;

/**
 * DTO for REST endpoints
 */
public class FunctionDefinition
{
    public String createdBy;   // email address
    //    public String createdAt;   // ISO8601 format
    public String name;        // name of function
    public String description;

    public String fxdef;  // parseable by com.apixio.sdk.util.FxIdlParser

    public FunctionDefinition()
    {
    }

    public FunctionDefinition(String idl, String name, String description, String creator)
    {
        this.fxdef       = idl;
        this.name        = name;
        this.description = description;
        this.createdBy   = creator;
    }
}
