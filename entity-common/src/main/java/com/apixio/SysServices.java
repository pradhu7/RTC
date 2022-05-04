package com.apixio;

import com.apixio.aclsys.buslog.AccessTypeLogic;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.aclsys.buslog.OperationLogic;
import com.apixio.aclsys.buslog.UserGroupLogic;
import com.apixio.aclsys.dao.AccessTypes;
import com.apixio.aclsys.dao.AclDao;
import com.apixio.aclsys.dao.Operations;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.aclsys.dao.UserGroupDaoFactory;
import com.apixio.aclsys.dao.redis.RedisAclDao;
import com.apixio.aclsys.dao.redis.RedisUserGroupDaoFactory;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.DataServices;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.buslog.OldRoleLogic;
import com.apixio.useracct.buslog.OrganizationLogic;
import com.apixio.useracct.buslog.PasswordPolicyLogic;
import com.apixio.useracct.buslog.ProjectLogic;
import com.apixio.useracct.buslog.RoleLogic;
import com.apixio.useracct.buslog.RoleSetLogic;
import com.apixio.useracct.buslog.TextBlobLogic;
import com.apixio.useracct.buslog.UserLogic;
import com.apixio.useracct.buslog.UserProjectLogic;
import com.apixio.useracct.dao.OldRoles;
import com.apixio.useracct.dao.OrgTypes;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dao.PasswordPolicies;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.dao.RoleSets;
import com.apixio.useracct.dao.Roles;
import com.apixio.useracct.dao.TextBlobs;
import com.apixio.useracct.dao.UserProjects;

/**
 * SysServices is the central location/class to store all system level singleton objects
 * that provide a service to another object (or objects).
 *
 * The basic types of objects/services that belong here are:
 *
 *  1.  data access services/objects
 *  2.  utility objects
 *  3.  business logic objects
 *
 * One architectural goal is to have some ordering/layering of these services so the initialization
 * of these objects should be done in the above numbered order:  first DAOs, then utilities, then
 * business logic.  No DAO should depend on a business object, etc.  (NOTE!  it might be necessary
 * to have a small set of utility objects initialized prior to DAOs...)
 */
public class SysServices extends DataServices
{
    /**
     * We keep multiple UserGroupDao objects as we need to have multiple "namespaces"
     * for them to facilitate the ACL/RBAC system.  These constants are to be used in
     * a generic way to access the desired namespace.
     */
    public  final static int UGDAO_MEMBEROF = 0;
    public  final static int UGDAO_ACL      = 1;
    private final static int UGDAO_MAX      = 2;

    // DataAccessObjects:
    private AccessTypes       accessTypes;
    private AclDao            aclDao;
    private OldRoles          roles;
    private Operations        operations;
    private OrgTypes          orgTypes;
    private Organizations     organizations;
    private PasswordPolicies  passwordPolicies;
    private Projects          projects;
    private RoleSets          roleSets;
    private Roles             orgRoles;
    private TextBlobs         textBlobs;
    private Tokens            tokens;
    private UserGroupDao      userGroupDao;
    private UserGroupDao[]    userGroupDaoExtended;
    private UserProjects      userProjects;

    // BusinessLogic:
    private AccessTypeLogic     accessTypeLogic;
    private AclLogic            aclLogic;
    private OldRoleLogic        oldRoleLogic;
    private OperationLogic      operationLogic;
    private OrganizationLogic   organizationLogic;
    private ProjectLogic        projectLogic;
    private PasswordPolicyLogic passwordPolicyLogic;
    private RoleLogic           roleLogic;
    private RoleSetLogic        roleSetLogic;
    private TextBlobLogic       textBlobLogic;
    private UserGroupLogic      userGroupLogic;
    private UserGroupLogic[]    userGroupLogicExtended;
    private UserLogic           userLogic;
    private UserProjectLogic    userProjectLogic;

    /**
     * Services that need Cassandra will pass a non-null cqlCrud in
     */
    public SysServices(DaoBase seed, MicroserviceConfig config)
    {
        super(seed, null);
        ConfigSet aclCs     = config.getAclConfig();
        String    aclCfName = (aclCs != null) ? aclCs.getString(MicroserviceConfig.ACL_CFNAME) : null;

        // ################################################################
        // Required order of initialization:
        //
        //  1.  Data Access Objects.  These MUST be initialized first and must depend ONLY
        //      on persistence-level objects
        //
        //  2.  Miscellaneous objects that business objects depend on (e.g., utilities)
        //
        //  3.  Business Logic objects.  These MUST NOT depend on anything other than
        //      DAOs and utilities
        //
        // Note that SysServices collects all the above into a single object.  Initialization
        // of anything other than DAOs will pass in 'this' because of the dependence on
        // SysServices.

        // ################################################################
        //  DAO initialization

        // A bit of a hack (I really do prefer Spring's IoC model...) in that the
        // DAO model suggests that we use an already-setup Persistable (meaning
        // that it has RedisOps and key prefix) to set up more Persistable-derived
        // classes.
        this.tokens           = new Tokens(seed, config.getTokenConfig());
        this.accessTypes      = new AccessTypes(seed);
        this.operations       = new Operations(seed);
        this.orgRoles         = new Roles(seed, dataVersions);
        this.orgTypes         = new OrgTypes(seed, dataVersions);
        this.organizations    = new Organizations(seed, dataVersions);
        this.projects         = new Projects(seed, dataVersions);
        this.roleSets         = new RoleSets(seed, dataVersions);
        this.roles            = new OldRoles(seed);
        this.textBlobs        = new TextBlobs(seed, dataVersions);
        this.userProjects     = new UserProjects(seed);

        // ACLs/UserGroups:  Cassandra vs Redis:
        //
        //  * stage 0:  ACLs and UserGroups are only in Cassandra; the Cassandra DAOs are used only.
        //              This was the system up until May 2017 (prior to version 5.3.0 of entity-common).
        //              The initialization of ACL subsystem was triggered off of whether or not the
        //              config included Cassandra connection info
        //
        //  * stage 1:  ACLs and UserGroups have been migrated to Redis; we use the "Combo" DAOs so that
        //              changes are written to both but only read from Redis; version 5.3.0 of entity-common.
        //              Initialization of ACLs and UserGroups is always done (since Redis is always available).
        //
        //  * stage 2:  ACLs and UserGroups are only in Redis; the Redis DAOs are used only; version 5.4.0
        //              of entity-common

        if (aclCfName != null)
        {
            UserGroupDaoFactory ugDaoFactory = new RedisUserGroupDaoFactory();

            this.aclDao              = new RedisAclDao(seed, aclCfName);
            this.userGroupDao        = ugDaoFactory.getDefaultUserGroupDao(seed, aclCfName);
            this.userGroupLogic      = new UserGroupLogic(this, userGroupDao);

            // mixing in business logic initialization...

            this.userGroupDaoExtended   = new UserGroupDao[UGDAO_MAX];
            this.userGroupLogicExtended = new UserGroupLogic[UGDAO_MAX];

            for (int i = 0; i < UGDAO_MAX; i++)
            {
                this.userGroupDaoExtended[i]   = ugDaoFactory.getExtendedUserGroupDao(seed, i, aclCfName);
                this.userGroupLogicExtended[i] = new UserGroupLogic(this, userGroupDaoExtended[i]);
            }
        }

        // the next set of initializations are a bit yucky because we're passing in dataVersions, etc.
        this.passwordPolicies = new PasswordPolicies(seed, this.dataVersions);

        // ################################################################
        //  Utility initialization

        // ################################################################
        //  Business logic initialization
        this.operationLogic      = new OperationLogic(this);  // this MUST be before AclLogic, due to stupidity and lack of IoC
        this.accessTypeLogic     = new AccessTypeLogic(this);
        this.aclLogic            = new AclLogic(this);

        this.oldRoleLogic        = addPostInit(new OldRoleLogic(this));
        this.organizationLogic   = addPostInit(new OrganizationLogic(this));
        this.passwordPolicyLogic = addPostInit(new PasswordPolicyLogic(this));
        this.projectLogic        = addPostInit(new ProjectLogic(this));
        this.roleLogic           = addPostInit(new RoleLogic(this));
        this.roleSetLogic        = addPostInit(new RoleSetLogic(this));
        this.textBlobLogic       = addPostInit(new TextBlobLogic(this));
        this.userLogic           = addPostInit(new UserLogic(this));
        this.userProjectLogic    = addPostInit(new UserProjectLogic(this));

        doPostInit();
    }

    // DAO
    public AccessTypes       getAccessTypes()      { return accessTypes;      }
    public AclDao            getAclDao()           { return aclDao;           }
    public OldRoles          getOldRoles()         { return roles;            }
    public Operations        getOperations()       { return operations;       }
    public OrgTypes          getOrgTypes()         { return orgTypes;         }
    public Organizations     getOrganizations()    { return organizations;    }
    public PasswordPolicies  getPasswordPolicies() { return passwordPolicies; }
    public Projects          getProjects()         { return projects;         }
    public RoleSets          getRoleSets()         { return roleSets;         }
    public Roles             getRoles()            { return orgRoles;         }
    public TextBlobs         getTextBlobs()        { return textBlobs;        }
    public Tokens            getTokens()           { return tokens;           }
    public UserGroupDao      getUserGroupDao()     { return userGroupDao;     }
    public UserProjects      getUserProjects()     { return userProjects;     }

    // odd one:
    public UserGroupDao      getUserGroupDaoExt(int n)    { return (userGroupDaoExtended   != null) ? userGroupDaoExtended[n]   : null; }
    public UserGroupLogic    getUserGroupLogicExt(int n)  { return (userGroupLogicExtended != null) ? userGroupLogicExtended[n] : null; }

    // utility

    // business logic
    public AccessTypeLogic     getAccessTypeLogic()     { return accessTypeLogic;      }
    public AclLogic            getAclLogic()            { return aclLogic;             }
    public OldRoleLogic        getOldRoleLogic()        { return oldRoleLogic;         }
    public OperationLogic      getOperationLogic()      { return operationLogic;       }
    public OrganizationLogic   getOrganizationLogic()   { return organizationLogic;    }
    public ProjectLogic        getProjectLogic()        { return projectLogic;         }
    public PasswordPolicyLogic getPasswordPolicyLogic() { return passwordPolicyLogic;  }
    public RoleLogic           getRoleLogic()           { return roleLogic;            }
    public RoleSetLogic        getRoleSetLogic()        { return roleSetLogic;         }
    public TextBlobLogic       getTextBlobLogic()       { return textBlobLogic;        }
    public UserGroupLogic      getUserGroupLogic()      { return userGroupLogic;       }
    public UserLogic           getUserLogic()           { return userLogic;            }
    public UserProjectLogic    getUserProjectLogic()    { return userProjectLogic;     }

    /**
     * Protected setters for setting privileged logic objects.
     */
    protected void setAccessTypeLogic(AccessTypeLogic accessTypeLogic)
    {
        if (accessTypeLogic != null)
            this.accessTypeLogic = accessTypeLogic;
    }
    protected void setOperationLogic(OperationLogic operationLogic)
    {
        if (operationLogic != null)
            this.operationLogic = operationLogic;
    }
    protected void setRoleLogic(RoleLogic roleLogic)
    {
        if (roleLogic != null)
            this.roleLogic = roleLogic;
    }
    protected void setUserLogic(UserLogic userLogic)
    {
        if (userLogic != null)
            this.userLogic = userLogic;
    }

}
