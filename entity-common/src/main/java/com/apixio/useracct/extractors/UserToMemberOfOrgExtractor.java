package com.apixio.useracct.extractors;

import java.io.IOException;
import java.util.List;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.User;

public class UserToMemberOfOrgExtractor implements Extractor
{
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
        // prevInChain is assumed to be a UserID (usually retrieved via using "token:" in apiacls.json;
        // returns external ID of the Organization that user is a member-of.  A user is a member-of exactly
        // one organization

        try
        {
            User user = findUserByEmailOrID((String) prevInChain);

            if (user != null)
            {
                List<Organization> memberOfOrgs = sysServices.getOrganizationLogic().getUsersOrganizations(user.getID());  // will be length 1

                return memberOfOrgs.get(0).getExternalID();
            }
        }
        catch (Exception x)
        {
            ctx.recordWarn("Failure while trying to get member-of organization from UserID {}:  {}", prevInChain, x.toString());
        }

        return null;
    }

    /**
     * Attempts to get the User from the XUUID-based userID, testing that the caller should be
     * allowed to retrieve that User object (only "self" and ROOT users can do this).
     */
    private User findUserByEmailOrID(String userid) throws IOException
    {
        if ((userid == null) || (userid.length() == 0))
            return null;

        XUUID userXid = null;

        try
        {
            userXid = XUUID.fromString(userid, User.OBJTYPE);
        }
        catch (IllegalArgumentException x1)
        {
            try
            {
                userXid = sysServices.getUsers().findUserIDByEmail((new CanonicalEmail(userid)).getEmailAddress());
            }
            catch (IllegalArgumentException x2)
            {
            }
        }

        if (userXid != null)
            return sysServices.getUsers().findUserByID(userXid);
        else
            return null;
    }
}
