package com.apixio.mcs;

import java.util.Date;
import java.util.Map;

import com.apixio.restbase.util.DateUtil;

/**
 * Metadata about parts
 */
public class PartMeta
{
    public String modelId;
    public String name;
    public String s3Path;
    public String md5Digest;
    public String createdBy;
    public String createdAt;
    public String mimeType;

    public Map<String,Object> core;
    public Map<String,Object> search;

    /**
     * For jackson deserialization of response metadata on client
     */
    public PartMeta()
    {
    }

    /**
     * PartMeta instances are created only by server code.
     */
    public PartMeta(String modelId, String name, String s3Path, String md5Digest, String createdBy, Date createdAt,
                    String mimeType, Map<String,Object> core, Map<String,Object> search)
    {
        this.modelId   = modelId;
        this.name      = name;
        this.s3Path    = s3Path;
        this.md5Digest = md5Digest;
        this.createdBy = createdBy;
        this.createdAt = DateUtil.dateToIso8601(createdAt);
        this.mimeType  = mimeType;
        this.core      = core;
        this.search    = search;
    }

    @Override
    public String toString()
    {
        return ("PartMeta(" +
                "modelId=" + modelId +
                "; name=" + name +
                "; s3Path=" + s3Path +
                "; md5Digest=" + md5Digest +
                "; createdBy=" + createdBy +
                "; createdAt=" + createdAt +
                "; mimeType=" + mimeType +
                ")");
    }
}
