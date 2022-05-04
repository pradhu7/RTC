package com.apixio.restbase.config;

/**
 */
public class ApiaclConfig {
    private String          apiAclDefs;    // file path
    private String          aclColumnFamilyName;
    private String          aclDebug;
    private Long            aclHpCacheTimeout;

    public void setApiAclDefs(String defs)
    {
        this.apiAclDefs = defs;
    }
    public String getApiAclDefs()
    {
        return apiAclDefs;
    }

    public void setAclColumnFamilyName(String cfName)
    {
        this.aclColumnFamilyName = cfName;
    }
    public String getAclColumnFamilyName()
    {
        return aclColumnFamilyName;
    }

    public void setAclDebug(String aclDebug)
    {
        this.aclDebug = aclDebug;
    }
    public String getAclDebug()
    {
        return aclDebug;
    }

    public void setAclHpCacheTimeout(Long timeout)
    {
        this.aclHpCacheTimeout = timeout;
    }
    public Long getAclHpCacheTimeout()
    {
        return aclHpCacheTimeout;
    }

    @Override
    public String toString()
    {
        return ("AclConfig: " +
                "aclDefs=" + apiAclDefs +
                "aclCfName=" + aclColumnFamilyName +
                "aclDebug=" + aclDebug +
                "cacheTimeout=" + aclHpCacheTimeout
            );
    }

}
