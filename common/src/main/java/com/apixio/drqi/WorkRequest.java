package com.apixio.drqi;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WorkRequest {
    private static final String DEFAULT_PRIORITY = "2";
    private String organization;
    private String operation;
    private String batchID;
    private String hdfsInput;
    private String category = null;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void setOrganization(String org)
    {
        this.organization = org;
    }
    public void setOperation(String op)
    {
        this.operation = op;
    }
    public void setBatchID(String id)
    {
        this.batchID = id;
    }
    public void setHdfsInput(String hdfsInput)
    {
        this.hdfsInput = hdfsInput;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String toString() {
        ObjectNode evt = getObject();
        return evt.toString();
    }

    public String serialize()
    {
        ObjectNode evt = getObject();

        return evt.toString();
    }
    
    private ObjectNode getObject()
    {
        ObjectNode  evt  = objectMapper.createObjectNode();
        ObjectNode  data = objectMapper.createObjectNode();

        evt.put("type", "SUBMIT_WORK_ORDER");
        evt.put("uuid", UUID.randomUUID().toString());
        evt.put("data", data);

        // all values must be present
        data.put("cx.org",  organization.toString());
        data.put("cx.bid",  batchID.toString());
        data.put("hdfsdir", hdfsInput.toString());
        data.put("operation", operation.toString());

        // hardcoded (hidden)
        data.put("cx.app",    category.toString());
        data.put("cx.pri",    DEFAULT_PRIORITY);
        data.put("workflow",  "main");
        data.put("jobrunner", "com.apixio.coordinator.GenericJobRunner");
        data.put("activity",  "_");
        return evt;
    }

}