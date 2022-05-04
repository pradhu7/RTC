package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apixio.restbase.LogicBase;
import com.apixio.Datanames;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.ConfigConstants;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.PropertyUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.PasswordPolicy;
import com.apixio.useracct.entity.User;

/**
 * Contains reusable user-account level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 * This level allows more than one type of access into the system, with RESTful
 * access being just one type (command line access is also possible).
 */
public class UserLogic extends LogicBase<SysServices> {

    public final static String SELF_ID = "me";

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for createUser:
         */ 
        BAD_EMAIL_SYNTAX,
        EMAILADDR_ALREADYUSED,
        ALREADY_VERIFIED,
        MISSING_ORG,

        /**
         * for modifyUser:
         */
        EXISTING_PASSWORD_MISMATCH,
        PASSWORD_POLICY_FAILURE,
        NOT_ACTIVE,
        NO_SUCH_USER,
        PRIVILEGED_OPERATION,
        CONCURRENT_USER_CREATION,
        ERR_PHONE_MISSING,
        INVALID_PHONE_NUMBER;
    }

    /**
     * If user operations fail they will throw an exception of this class.
     */
    public static class UserException extends BaseException {

        public UserException(FailureType failureType)
        {
            super(failureType);
        }

        public UserException(FailureType failureType, Map<String, String> details)
        {
            super.detail(details);
            super.detail(P_REASON, failureType.toString());
        }
    }

    /**
     * PropertyUtil helps manage per-UserOrg CustomProperties
     */
    private PropertyUtil propertyUtil;

    private Users             usersDAO;
    private OrganizationLogic orgLogic;
    private AclLogic          aclLogic;

    /**
     * Constructor.
     */
    public UserLogic(SysServices sysServices)
    {
        super(sysServices);

        propertyUtil = new PropertyUtil(Datanames.SCOPE_USER, sysServices);
    }

    @Override
    public void postInit()
    {
        usersDAO = sysServices.getUsers();
        orgLogic = sysServices.getOrganizationLogic();
        aclLogic = sysServices.getAclLogic();
    }

    /**
     * Looks up the user by its ID and returns it if the user is either the caller (as identified
     * by the token that's in the thread local) or if the caller has root or admin privileges.
     *
     * The special ID of "me" can be used to specify the current caller.
     */
    public User getUser(
        Token    caller,
        String   userID,
        boolean  forWrite
        ) throws IOException
    {
        boolean isSelf  = SELF_ID.equals(userID);
        XUUID   tUserID = caller.getUserID();

        return checkCallerAccessToUser(tUserID,
                                       usersDAO.findUserByID((isSelf) ? tUserID : XUUID.fromString(userID, User.OBJTYPE)),
                                       forWrite);

    }

    /**
     * Returns the User object that has the given email address if and only if the User
     * is a member of an Organization that the caller has ManageUser permission on.
     */
    public User getUserByEmail(XUUID caller, String emailAddress, boolean forWrite) throws IOException
    {
        return checkCallerAccessToUser(caller, usersDAO.findUserByEmail(emailAddress), forWrite);
    }

    /**
     * Returns the User object  if and only if the User
     * is a member of an Organization that the caller has ManageUser permission on.
     */
    private User checkCallerAccessToUser(XUUID caller, User user, boolean forWrite) throws IOException
    {
        List<String>       aclObjects;
        List<Organization> userOrgs;

        // do simple check first before we do all the ACL stuff
        if (user == null)
            return null;

        // if caller == user, all's good
        if (caller.equals(user.getID()))
            return user;

        // permission on everything
        if (aclLogic.hasPermission(caller, ConfigConstants.MANAGEUSER_OPERATION, AclLogic.ANY_OBJECT))
            return user;

        aclObjects = aclLogic.getBySubjectOperation(caller, ConfigConstants.MANAGEUSER_OPERATION, true);

        if (!forWrite)
            aclObjects.addAll(aclLogic.getBySubjectOperation(caller, ConfigConstants.VIEWUSER_OPERATION, true));

        // permission on nothing
        if (aclObjects.size() == 0)
            return null;

        // any overlap?
        for (Organization org : sysServices.getOrganizationLogic().getUsersOrganizations(user.getID()))
        {
            if (aclObjects.contains(org.getID().toString()))
                return user;
        }

        return null;
    }

    /**
     * Return the list of Users in Organizations that the caller has ManageUser permission on.
     * If the caller has ManagUser permission any object, then return all users (since each user
     * must be a member-of exactly one organization);
     */
    public List<User> getUsersByManageUserPermission(XUUID caller, List<XUUID> limitToOrgs, AccountState include, AccountState exclude) throws IOException
    {
        AclLogic   aclLogic = sysServices.getAclLogic();
        List<User> users;

        // get list of orgs caller has ManageUser permission on;
        // if limitToOrgs is null/empty, return all, otherwise just
        // the ones in the given orgs

        if (aclLogic.hasPermission(caller, ConfigConstants.MANAGEUSER_OPERATION, AclLogic.ANY_OBJECT))
        {
            users = usersDAO.getAllUsers();
        }
        else
        {
            // get list of Organizations that caller can ManageUser on
            List<String> aclObjects = aclLogic.getBySubjectOperation(caller, ConfigConstants.MANAGEUSER_OPERATION, true);

            aclObjects.addAll(aclLogic.getBySubjectOperation(caller, ConfigConstants.VIEWUSER_OPERATION, true));

            users = new ArrayList<>();

            if (aclObjects.size() > 0)
            {
                Set<XUUID>  userIDs = new HashSet<>();
                boolean     empty   = (limitToOrgs == null) || (limitToOrgs.size() == 0);

                for (String orgID : aclObjects)
                {
                    try
                    {
                        XUUID orgIDx = XUUID.fromString(orgID, Organization.OBJTYPE);

                        if (empty || limitToOrgs.contains(orgIDx))
                            userIDs.addAll(orgLogic.getUsersBelongingToOrg(orgIDx));
                    }
                    catch (IllegalArgumentException iax)
                    {
                        // skip as it could be that ManageUser isn't for an org... (in the future)
                    }
                }

                users = usersDAO.getUsers(userIDs);
            }
        }

        // now subtract out those whose account state doesn't match
        if ((include != null) || (exclude != null))
        {
            for (Iterator<User> it = users.iterator(); it.hasNext(); )
            {
                AccountState as = it.next().getState();

                if ( ((include != null) && (as != include)) ||
                     ((exclude != null) && (as == exclude)) )
                    it.remove();
            }
        }

        return users;
    }

    /**
     * Return the list of rolenames that the given user has.
     */
    public List<String> getUserRoles(XUUID userID)
    {
        User  user = usersDAO.findUserByID(userID);

        if (user != null)
            return user.getRoleNames();
        else
            return null;
    }

    /**
     * Returns the PasswordPolicy that governs the user's password.
     */
    public PasswordPolicy getUserPasswordPolicy(User user) throws IOException
    {
        PasswordPolicy      policy   = null;
        List<Organization>  orgs     = sysServices.getOrganizationLogic().getUsersOrganizations(user.getID());
        PasswordPolicyLogic polLogic = sysServices.getPasswordPolicyLogic();

        if ((orgs != null) && (orgs.size() == 1))
            policy = polLogic.getPasswordPolicy(orgs.get(0).getPasswordPolicy());  // by name

        if (policy == null)
            policy = polLogic.getPasswordPolicy("Default");
        
        return policy;
    }

    // ################################################################
    //  Custom Properties
    // ################################################################

    /**
     * The Custom Property model allows a set of uniquely named properties that are
     * then available to have values associated with those property names added to
     * a User object.  Custom properties also have a type that limits the types
     * of values that can be added to customer objects.
     *
     * Custom Properties have a lifetime independent of Users and their property values.
     * Actual custom property values on Users are tied to the lifetime of Custom Properties
     * as a deletion of the global Custom Property definition will delete all properties
     * of that type/name from all Users.
     */

    /**
     * Add a new custom property to the global set of properties.  The name must be unique
     * when lowercased.
     */ 
    public void addProperty(String name, PropertyType type)   // throws exception if name.trim.tolowercase is not unique
    {
        propertyUtil.addPropertyDef(name, type);
    }

    /**
     * Returns a map from unique property name to the declared PropertyType of that property.
     */
    public Map<String, String> getPropertyDefinitions(boolean includeMeta) // <propName, propType>
    {
        return propertyUtil.getPropertyDefs(includeMeta);
    }

    /**
     * Removes the custom property definition.  This removal will cascade to a deletion of
     * property values on User objects.
     */
    public void removeProperty(String name)
    {
        propertyUtil.removePropertyDef(name);
    }

    /**
     * Add a property value to the given User.  The actual value
     * will be converted--as possible--to the declared property type.
     */
    public void setUserProperty(User user, String propertyName, String valueStr) // throws exception if name not known
    {
        propertyUtil.setEntityProperty(user.getID(), propertyName, valueStr);
    }

    /**
     * Remove a custom property value from the given User.
     */
    public void removeUserProperty(User user, String propertyName) // throws exception if name not known
    {
        propertyUtil.removeEntityProperty(user.getID(), propertyName);
    }

    /**
     * Returns a map from UserXUUIDs to a map that contains all the name=value pairs for each User.
     */
    public Map<XUUID, Map<String, Object>> getAllUserProperties()                 // <UserID, <propname, propvalue>>
    {
        return propertyUtil.getAllCustomProperties();
    }

    /**
     * Given a property name, return a map from UserXUUID to the property value for that User.
     */
    public Map<XUUID, Object> getUsersProperty(String propName)     // <UserID, propvalue>
    {
        return propertyUtil.getCustomProperty(propName);
    }

    /**
     * Given a User, return a map from property name to the property value for that User.
     */
    public Map<String, Object> getUserProperties(User user)   // <propname, propvalue>
    {
        return propertyUtil.getEntityProperties(user.getID());
    }

}
