package com.apixio.mcs;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This is the DTO for outgoing response data from a query.
 */
public class FullModelMeta extends ModelMeta
{
    /**
     * Non-null only for This will be ignored for incoming data on REST requests
     */
    public List<PartMeta> parts;

    /**
     * Needed for deserializing the server response via jackson
     */
    public FullModelMeta()
    {
        super();
    }

    /**
     * Create from all the details.  Only server code should be calling this.
     */
    public FullModelMeta(String id, Date createdAt, Boolean deleted,
                         String createdBy, String executor, String name,
                         String outputType, String product, String version,
                         Lifecycle state, String pdsId,
                         Map<String,Object> coreJson, Map<String,Object> searchJson,
                         List<PartMeta> parts)
    {
        super(id, createdAt, deleted,
              createdBy, executor, name,
              outputType, product, version,
              state, pdsId, coreJson, searchJson);

        this.parts = parts;
    }
}
