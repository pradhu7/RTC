package com.apixio.useracct.buslog;

import java.util.List;

import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.LogicBase;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.UserProjects;
import com.apixio.useracct.entity.UserProject;

/**
 */
public class UserProjectLogic extends LogicBase<SysServices> {

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for modifyOrganization:
         */
        SOMETHING
    }

    /**
     * distributed lock to prevent concurrent same user adds to same project
     */
    private DistLock distLock;

    /**
     * If organization operations fail they will throw an exception of this class.
     */
    public static class UserProjectException extends BaseException {

        private FailureType failureType;

        public UserProjectException(FailureType failureType)
        {
            super(failureType);
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * System Services used
     */
    private UserProjects userProjects;

    /**
     * Constructor.
     */
    public UserProjectLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    @Override
    public void postInit()
    {
        userProjects = sysServices.getUserProjects();
        distLock = new DistLock(sysServices.getRedisOps(), "user-project-lock-");
    }

    /**
     * addUserToProject adds or updates (if exists) the [userID, projID] information,
     */
    public void addUserToProject(XUUID userID, XUUID projectID, boolean active, List<String> phases)
    {
        String key = userID.toString() + "-" + projectID.toString();
        String lock = distLock.lock(key, 3000);
        UserProject upa = null;

        if (lock !=null)
        {
            upa = userProjects.findUserProject(userID, projectID);

            if (upa == null)
                upa = userProjects.createUserProject(userID, projectID);

            distLock.unlock(lock, key);
        }
        else
        {
            String reason = String.format("Invalid same user [%s] is currently being added to this project [%s]",
                    userID.toString(), projectID.toString());
            throw BaseException.badRequest(reason);
        }
        upa.setActive(active);
        upa.setPhases(phases);

        userProjects.update(upa);
    }

    /**
     *
     */
    public List<UserProject> getUserProjectForUser(XUUID userID)
    {
        return userProjects.getProjectsForUser(userID);
    }

    /**
     *
     */
    public List<UserProject> getUserProjectForProject(XUUID projID)
    {
        return userProjects.getUsersForProject(projID);
    }

    /**
     * removeUserFromProject removes the [userID, projID] association and related info.
     */
    public void removeUserFromProject(XUUID userID, XUUID projectID)
    {
        UserProject up = userProjects.findUserProject(userID, projectID);

        if (up != null)
            userProjects.delete(up);
    }

    public void removeAllUsersFromProject(XUUID projectID)
    {
        for (UserProject up : userProjects.getUsersForProject(projectID))
            userProjects.delete(up);
    }

    /**
     *
     */
    public void deleteByProject(XUUID projID)
    {
        for (UserProject up : userProjects.getUsersForProject(projID))
            userProjects.delete(up);
    }

}
