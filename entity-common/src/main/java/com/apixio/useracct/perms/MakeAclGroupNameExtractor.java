package com.apixio.useracct.perms;

import java.io.IOException;

import com.apixio.SysServices;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;
import com.apixio.useracct.buslog.RoleLogic;

/**
 * MakeAclGroupNameExtractor accepts a string from the previous extractor in the chain
 * (string must be of the form {userID};{roleName}) and produces a string the identifies
 * the AclGroupName (see com.apixio.useracct.buslog.RoleLogic.makeAclGroupName).
 */
public class MakeAclGroupNameExtractor implements Extractor {

    private RoleLogic roleLogic;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        roleLogic = initInfo.sysServices.getRoleLogic();
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
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity) throws IOException
    {
        if (prevInChain == null)
        {
            ctx.recordWarn("MakeAclGroupNameExtractor requires a non-null input from previous element in extractor chain");
            return null;
        }

        String[]  fields = prevInChain.toString().split(";");

        if (fields.length != 3)
        {
            ctx.recordWarn("MakeAclGroupNameExtractor requires an input whose .toString() has three ';'-separated fields:  UserXID;RoleName;TargetXID but got {}", prevInChain);
            return null;
        }

        return roleLogic.makeAclGroupName(fields[0], fields[1], fields[2]);
    }

}
