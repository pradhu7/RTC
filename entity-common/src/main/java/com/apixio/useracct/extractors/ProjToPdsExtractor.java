package com.apixio.useracct.extractors;

import com.apixio.SysServices;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;
import com.apixio.useracct.entity.Project;

public class ProjToPdsExtractor implements Extractor {

    private SysServices sysServices;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        this.sysServices = initInfo.sysServices;
    }

    /**
     * Returns true if the only way for the extractor to return information is by examining
     * the HTTP entity body.
     */
    @Override
    public boolean requiresHttpEntity()
    {
        return false;
    }

    /**
     *
     */
    @Override
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity)
    {
        // prevInChain is assumed to be a projectID

        try
        {
            Project proj = sysServices.getProjectLogic().getProjectByID((String) prevInChain);

            if (proj != null)
                return proj.getPatientDataSetID();
        }
        catch (Exception x)
        {
            ctx.recordWarn("Failure while trying to get PdsID from ProjectID {}:  {}", prevInChain, x.toString());
        }

        return null;
    }

}
