package com.apixio.restbase.apiacl.perm.extractors;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;
import com.apixio.useracct.buslog.ProjectLogic;
import com.apixio.useracct.entity.Project;

import java.io.IOException;

public class ProjectToOrgID implements Extractor {
    private SysServices sysServices;
    private ProjectLogic projectLogic;

    @Override
    public void init(ApiAcls.InitInfo initInfo, ApiDef apiDef, String s) {
        this.sysServices = initInfo.sysServices;
        this.projectLogic = sysServices.getProjectLogic();
    }

    @Override
    public boolean requiresHttpEntity() {
        return false;
    }

    @Override
    public Object extract(CheckContext ctx, MatchResults matchres, HttpRequestInfo info, Object prevInChain, Object httpEntity) throws IOException {
        if (prevInChain == null) {
            return null;
        } else {
            try {
                Project proj = projectLogic.getProjectByID(String.valueOf(prevInChain));
                XUUID orgId = proj.getOrganizationID();
                return orgId.toString();
            } catch (Exception exp) {
                return null;
            }
        }
    }
}
