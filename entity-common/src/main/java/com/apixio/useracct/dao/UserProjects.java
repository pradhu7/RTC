package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.restbase.dao.NAssocs;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.UserProject;

public final class UserProjects extends NAssocs {

    // NEVER CHANGE THESE constants as they are used in redis keys
    private static final String AK_USER    = "us";
    private static final String AK_PROJECT = "pr";

    private static AssocConfig UpConfig = new AssocConfig(UserProject.ASSOC_TYPE, new String[] {AK_USER, AK_PROJECT});

    public UserProjects(DaoBase seed)
    {
        super(seed, UpConfig);
    }

    public UserProject createUserProject(XUUID userID, XUUID projID)
    {
        UserProject upa = new UserProject(userID, projID);

        super.createAssoc(upa);

        return upa;
    }

    public UserProject findUserProject(XUUID userID, XUUID projID)
    {
        ParamSet ps = findAssoc(UserProject.makeElementList(userID, projID));

        if (ps != null)
            return new UserProject(ps);
        else
            return null;
    }

    /**
     *
     */
    public List<UserProject> getProjectsForUser(XUUID userID)
    {
        return buildUserProjects(AK_USER, userID.toString());
    }

    /**
     *
     */
    public List<UserProject> getUsersForProject(XUUID projID)
    {
        return buildUserProjects(AK_PROJECT, projID.toString());
    }

    /**
     *
     */
    private List<UserProject> buildUserProjects(String eleName, String eleValue)
    {
        List<ParamSet>     psets = getEntitiesByIDs(findByElement(new Assoc(eleName, eleValue)));
        List<UserProject>  ups   = new ArrayList<>(psets.size());

        for (ParamSet ps : psets)
            ups.add(new UserProject(ps));

        return ups;
    }

}
