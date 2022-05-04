package com.apixio.mcs;

import java.util.Date;
import java.util.Map;

import com.apixio.restbase.util.DateUtil;

/**
 * This is the DTO for external clients and will be mapped to/from JSON in
 * REST APIs.  Note that this is also used as the DTO for returned information
 * so it has some fields that the client can theoretically supply but that will
 * be ignored on input.
 */
public class ModelMeta
{
    // maintained by server (i.e., client could supply during creation but they are ignored)
    public String  id;
    public String  createdAt;   // ISO8601 format; can't use more natural Date here w/o special config
    public Boolean deleted;

    // required metadata:
    public String    createdBy;
    public String    executor;
    public String    name;
    public String    outputType;
    public String    product;
    public String    version;
    public Lifecycle state;   // can be updated only via separate endpoint from rest of meta update

    // optional metadata:
    public String  pdsId;

    /**
     * Unfortunately Jackson doesn't automatically convert to JSONObject so
     * we take maps here and convert as needed
     */
    public Map<String,Object> core;
    public Map<String,Object> search;

    /**
     * Constructor for jackson
     */
    public ModelMeta()
    {
    }

    /**
     * Create from details.  Server code is likely the big user of this
     */
    public ModelMeta(String id, Date createdAt, Boolean deleted,
                     String createdBy, String executor, String name,
                     String outputType, String product, String version,
                     Lifecycle state, String pdsId,
                     Map<String,Object> coreJson, Map<String,Object> searchJson)
    {
        this.id         = id;
        this.createdAt  = DateUtil.dateToIso8601(createdAt);
        this.deleted    = deleted;
        this.createdBy  = createdBy;
        this.executor   = executor;
        this.name       = name;
        this.outputType = outputType;
        this.product    = product;
        this.version    = version;
        this.state      = state;
        this.pdsId      = pdsId;
        this.core       = coreJson;
        this.search     = searchJson;
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("ModelMeta(id=" + id +
                ", createdAt=" + createdAt +
                ", deleted=" + deleted +
                ", createdBy=" + createdBy +
                ", executor=" + executor +
                ", name=" + name +
                ", outputType=" + outputType +
                ", product=" + product +
                ", version=" + version +
                ", state=" + state +
                ", pdsId=" + pdsId +
                ", core=" + core +
                ", search=" + search +
                ")");
    }

}
