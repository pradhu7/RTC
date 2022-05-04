package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.apixio.datasource.redis.DistLock;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.LogicBase;
import com.apixio.Datanames;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.PropertyUtil;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.dao.RoleSets;
import com.apixio.useracct.dao.Roles;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.entity.HccProject.State;
import org.apache.commons.lang.NullArgumentException;

import static com.apixio.CommonUtil.getCurrentUTCTime;
import static com.apixio.CommonUtil.mapObjToString;


/**
 */
public class ProjectLogic extends LogicBase<SysServices> {

    private final static String ROLESET_PREFIX = "Project.";

    /**
     * Versioning Redis key and some default values
     */
    public static String prjVersionPrefix     = "Project_version_data:";
    public static String prjVersionNumPostfix = ":";
    public static String prjGetCurNumPrefix   = "Project_cur_version_num:";
    public static String LAST_UPDATED_BY = "lastUpdatedBy";
    public static String LAST_CREATED_AT = "lastCreatedAt";
    public static String COMMENT = "comment";
    public static String VERSION = "version";
    public static String PROJECT_DATA = "projData";
    public static String METHOD = "method";
    public static List<String> VERSION_BLACK_LIST = Arrays.asList("mf-last-update","mf-user-list");
    public static String EMAIL_NOT_PROVIDED = "";
    public static String METHOD_NOT_PROVIDED = "";
    public static String COMMENT_NOT_PROVIDED = "";
    public static String UNKNOWN_USER_EMAIL = "unknown_user_email";
    public static Boolean DEFAULT_VERSIONING = true;

    /**
     * PropertyUtils help manage per-Project CustomProperties.  There are two property "bags"
     * exposed by Projects:  a generic "properties" and a "phase" bag.
     */
    private PropertyUtil genericPropertyUtil;
    private PropertyUtil phasePropertyUtil;
    private PropertyUtil hccPropertyUtil;
    private PropertyUtil qualityPropertyUtil;
    private PropertyUtil prospectivePropertyUtil;
    private PropertyUtil labelPropertyUtil;

    /**
     * System Services used
     */
    private Projects         projects;
    private Users            users;
    private UserProjectLogic userProjectLogic;
    private OrganizationLogic orgLogic;
    private RoleLogic roleLogic;
    private PatientDataSetLogic pdsLogic;
    private RedisOps redisOps;
    private String serviceName;

    /**
     * distributed lock to prevent concurrent same user adds to same project
     */
    private DistLock distLock;
    public final static String VERSIONING_LOCK_FOR_PROJECT_PROPERTIES = "temp_versioning_lock_for_project_property";
    private static final int LOCK_MAX_ATTEMPT = 5;
    private static final long LOCK_THREAD_SLEEPING_TIME = 500;  // ms
    private static final long LOCK_DURATION_TIME = 3000;  // ms

    /**
     *
     */
    public enum PropertyBag { GENERIC, PHASE, HCC, QUALITY, PROSPECTIVE, LABEL }
    public static final PropertyBag[] ALL_PROPERTY_BAGS = PropertyBag.values();
    public static final String PROP_KEYNAME = "properties";
    public static final String GEN_BAG = "gen";

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for modifyProject:
         */
        NAME_USED,
        MISSING_REQUIRED_VALUE,
        BAD_BAG_NAME
    }

    /**
     * If organization operations fail they will throw an exception of this class.
     */
    public static class ProjectException extends BaseException {

        public ProjectException(FailureType failureType, String details, Object... args)
        {
            super(failureType);
            super.description(details, args);
        }
    }

    /**
     * Constructor.
     */
    public ProjectLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    @Override
    public void postInit()
    {
        projects         = sysServices.getProjects();
        users            = sysServices.getUsers();
        userProjectLogic = sysServices.getUserProjectLogic();

        genericPropertyUtil     = new PropertyUtil(Datanames.SCOPE_PROJ_GENERIC, sysServices);
        phasePropertyUtil       = new PropertyUtil(Datanames.SCOPE_PROJ_PHASE,   sysServices);
        hccPropertyUtil         = new PropertyUtil(Datanames.SCOPE_PROJ_HCC,     sysServices);
        qualityPropertyUtil     = new PropertyUtil(Datanames.SCOPE_PROJ_QUALITY, sysServices);
        prospectivePropertyUtil = new PropertyUtil(Datanames.SCOPE_PROJ_PROSPECTIVE, sysServices);
        labelPropertyUtil       = new PropertyUtil(Datanames.SCOPE_PROJ_LABEL,   sysServices);
        redisOps = projects.getRedisOps();
        orgLogic = sysServices.getOrganizationLogic();
        roleLogic = sysServices.getRoleLogic();
        pdsLogic = sysServices.getPatientDataSetLogic();
        serviceName = sysServices.getMicroServiceName();
        distLock = new DistLock(redisOps, "projectlogic-lock-");
    }

    /**
     * Creates an Project of the given type with the given name and description.  The
     * underlying "ACL UserGroup" and "Member UserGroup" are not created here as they're
     * an "on demand" creation.
     */
    public HccProject createHccProject(final HccProjectDTO params)
    {
        checkRequired(params);  // throws exception if missing params

        HccProject proj = new HccProject(params.name, params.type, params.organizationID, params.patientDataSetID);

        return commonCreateHCC(proj, params);
    }

    public QualityProject createQualityProject(final QualityProjectDTO params)
    {
        checkRequired(params);  // throws exception if missing params

        QualityProject proj = new QualityProject(params.name, params.organizationID, params.patientDataSetID);

        return commonCreateQuality(proj, params);
    }

    public ProspectiveProject createProspectiveProject(final ProspectiveProjectDTO params)
    {
        checkRequired(params);

        ProspectiveProject proj = new ProspectiveProject(params.name, params.organizationID, params.patientDataSetID);

        return commonCreateProspective(proj, params);
    }

    public LabelProject createLabelProject(final LabelProjectDTO params)
    {
        checkRequired(params);

        LabelProject proj = new LabelProject(params.name, params.organizationID, params.patientDataSetID);

        return commonCreateLabel(proj, params);
    }

    /**
     * Creates an Project of the given type with the given name and description.  The
     * projectID is forced to be what's supplied.
     */
    public HccProject createHccProject(final XUUID projectID, final HccProjectDTO params)
    {
        checkRequired(params);  // throws exception if missing params

        HccProject proj = new HccProject(projectID, params.name, params.type.toString(), params.organizationID, params.patientDataSetID, params.status);

        return commonCreateHCC(proj, params);
    }

    private HccProject commonCreateHCC(final HccProject proj, final HccProjectDTO hccProjectDTO)
    {
        // defaults:
        proj.setState(State.NEW);

        // defaults
        proj.setStatus(true);

        // copy over what was supplied; redundant set of hccProjectDTO.name...
        hccProjectDTO.dtoToEntity(proj);

        projects.create(proj);

        projects.associateProject(proj.getID(), hccProjectDTO.patientDataSetID);

        return proj;
    }

    private QualityProject commonCreateQuality(final QualityProject proj, final QualityProjectDTO qualityProjectDTO)
    {
        // defaults:
        proj.setState(QualityProject.State.NEW);

        // defaults
        proj.setStatus(true);

        // copy over what was supplied; redundant set of qualityProjectDTO.name...
        qualityProjectDTO.dtoToEntity(proj);

        projects.create(proj);

        projects.associateProject(proj.getID(), qualityProjectDTO.patientDataSetID);

        return proj;
    }

    private ProspectiveProject commonCreateProspective(final ProspectiveProject proj, final ProspectiveProjectDTO prospectiveProjectDTO)
    {

        prospectiveProjectDTO.dtoToEntity(proj);

        proj.setState(ProspectiveProject.State.NEW);

        proj.setStatus(true);

        projects.create(proj);

        projects.associateProject(proj.getID(), prospectiveProjectDTO.patientDataSetID);

        return proj;
    }

    private LabelProject commonCreateLabel(final LabelProject proj, final LabelProjectDTO labelProjectDTO)
    {
        labelProjectDTO.dtoToEntity(proj);

        proj.setState(LabelProject.State.NEW);

        proj.setStatus(true);

        projects.create(proj);

        projects.associateProject(proj.getID(), labelProjectDTO.patientDataSetID);

        return proj;
    }

    private void checkRequired(final HccProjectDTO params)
    {
        if (params.name == null)             throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "name");
        if (params.type == null)             throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "type");
        if (params.organizationID == null)   throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "organizationID");
        if (params.patientDataSetID == null) throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "patientDatasetID");
    }

    private void checkRequired(final QualityProjectDTO params)
    {
        if (params.name == null)             throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "name");
        if (params.organizationID == null)   throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "organizationID");
        if (params.patientDataSetID == null) throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "patientDatasetID");
        if (params.measurementYear == null)  throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "measurementYear");
        if (params.measureList == null)      throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "measureList");
        if (params.lineOfBusiness == null)   throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "lineOfBusiness");
    }

    private void checkRequired(final ProspectiveProjectDTO params)
    {
        if (params.name == null)             throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "name");
        if (params.organizationID == null)   throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "organizationID");
        if (params.patientDataSetID == null) throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "patientDatasetID");
        if (params.historicalYears == null)  throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "historicalYears");
        if (params.patientList == null)      throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "patientList");
        if (params.paymentYear == null)      throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "paymentYear");
    }

    private void checkRequired(final LabelProjectDTO params)
    {
        if (params.name == null)             throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "name");
        if (params.organizationID == null)   throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "organizationID");
        if (params.patientDataSetID == null) throw new ProjectException(FailureType.MISSING_REQUIRED_VALUE, "patientDataSetID");
    }

    private String composeServiceNameWithlMethod(String method)
    {
        return serviceName + "::" + method;
    }

    private String getUserEmail()
    {
        String userEmail = UNKNOWN_USER_EMAIL;
        try {
            userEmail = UserUtil.getCachedUser().getEmailAddr();
        }
        catch (Exception e){
            Token rToken = RestUtil.getInternalToken();
            if (rToken == null) {
            }
            else {
                userEmail = users.findUserByID(rToken.getUserID()).getEmailAddr();
            }
        }
        return userEmail;
    }

    public String getProjectByIDLikeUseracctString(String projectId)
    {
        Project project = getProjectByID(projectId);
        return getProjectByIDLikeUseracctString(project);
    }

    public String getProjectByIDLikeUseracctString(Project project)
    {
        Map<String, Object> data = getProjectByIDLikeUseracct(project);
        return mapObjToString(data);
    }

    /*
        Lots of existing services still use useracct get_project method to retrieve project complete data, but useracct get_project method
        will be deprecated soon. In order to be backward compatible, providing the method here that has the useraccrt return structure
        if your purpose is just to get the basic data (ex. pds, projectId, orgId), getProjectByID method is preferable for better performance
    */
    public Map<String, Object> getProjectByIDLikeUseracct(Project project)
    {
        if (project == null) {
            return null;
        }
        else {
            Organization   org = orgLogic.getOrganizationByID(project.getOrganizationID().toString());
            PatientDataSet pds = pdsLogic.getPatientDataSetByID(project.getPatientDataSetID());
            Map<String, Object> basicProject = ProjectProperties.toMap(project, org, pds);
            Map<String, Object> completeProject = composeCustomPropertiesInProject(project, ALL_PROPERTY_BAGS, basicProject);
            return completeProject;
        }
    }

    public void updateProject(Project project, final ProjectDTO params)
    {
        updateProject(project, params, EMAIL_NOT_PROVIDED, METHOD_NOT_PROVIDED, COMMENT_NOT_PROVIDED);
    }

    public void updateProject(Project project, final ProjectDTO params, String userEmail, String method, String comment)
    {
        updateProject(project, params, userEmail, method, comment, DEFAULT_VERSIONING);
    }

    public void updateProject(Project project, final ProjectDTO params, String userEmail, String method, String comment, Boolean doVersioning)
    {
        if (project == null) {
            throw new NullArgumentException("Input project object should not be NULL");
        }
        if (project.getProjectClass() == Project.ProjectClass.HCC) {
            updateHCCProject((HccProject) project, (HccProjectDTO) params, userEmail, method, comment, doVersioning);
        }
        else if (project.getProjectClass() == Project.ProjectClass.QUALITY) {
            updateQualityProject((QualityProject) project, (QualityProjectDTO) params, userEmail, method, comment, doVersioning);
        }
        else if (project.getProjectClass() == Project.ProjectClass.PROSPECTIVE) {
            updateProspectiveProject((ProspectiveProject) project, (ProspectiveProjectDTO) params, userEmail, method, comment, doVersioning);
        }
        else if (project.getProjectClass() == Project.ProjectClass.LABEL){
            updateLabelProject((LabelProject) project, (LabelProjectDTO) params, userEmail, method, comment, doVersioning);
        }
        else {
            throw new IllegalArgumentException(String.format("project: %s is not the standard project class. Project class: $s", project.getID().getID(), project.getProjectClass().toString()));
        }
    }

    /**
     * if you don't know who the userEmail is, parse empty string and here will try to get the userEmail for you :)
     * These three updateProjects should be able to be combined, will figure out at next PR
     */
    private void updateHCCProject(HccProject project, final HccProjectDTO params, String userEmail, String method, String comment, Boolean doVersioning)
    {
        params.dtoToEntity(project);
        projects.update(project);
        if (doVersioning) {
            createProjectVersion(project, userEmail, method == null || method.equals(METHOD_NOT_PROVIDED) ? "projectlogic.updateHccProject": method, comment);
        }
    }

    private void updateQualityProject(QualityProject project, final QualityProjectDTO params, String userEmail, String method, String comment, Boolean doVersioning)
    {
        params.dtoToEntity(project);
        projects.update(project);
        if (doVersioning) {
            createProjectVersion(project, userEmail, method == null || method.equals(METHOD_NOT_PROVIDED) ? "projectlogic.updateQualityProject": method, comment);
        }
    }

    private void updateProspectiveProject(ProspectiveProject project, final ProspectiveProjectDTO params, String userEmail, String method, String comment, Boolean doVersioning)
    {
        params.dtoToEntity(project);
        projects.update(project);
        if (doVersioning) {
            createProjectVersion(project, userEmail, method == null || method.equals(METHOD_NOT_PROVIDED) ? "projectlogic.updateProspectiveProject": method, comment);
        }
    }

    private void updateLabelProject(LabelProject project, final LabelProjectDTO params, String userEmail, String method, String comment, Boolean doVersioning)
    {
        params.dtoToEntity(project);
        projects.update(project);
        if (doVersioning) {
            createProjectVersion(project, userEmail, method == null || method.equals(METHOD_NOT_PROVIDED) ? "projectlogic.updateLabelProject": method, comment);
        }
    }

    public HccProject getHccProjectByID(String id)
    {
        Project proj = projects.findProjectByID(XUUID.fromString(id));

        if ((proj != null) && (proj.getProjectClass() == Project.ProjectClass.HCC))
            return (HccProject) proj;
        else
            return null;
    }

    public QualityProject getQualityProjectByID(String id)
    {
        Project proj = projects.findProjectByID(XUUID.fromString(id));

        if ((proj != null) && (proj.getProjectClass() == Project.ProjectClass.QUALITY))
            return (QualityProject) proj;
        else
            return null;
    }

    public ProspectiveProject getProspectiveProjectByID(String id)
    {
        Project proj = projects.findProjectByID(XUUID.fromString(id));

        if ((proj != null) && (proj.getProjectClass() == Project.ProjectClass.PROSPECTIVE))
            return (ProspectiveProject) proj;
        else
            return null;
    }

    public LabelProject getLabelProjectByID(String id)
    {
        Project proj = projects.findProjectByID(XUUID.fromString(id));

        if ((proj != null) && (proj.getProjectClass() == Project.ProjectClass.LABEL))
            return (LabelProject) proj;
        else
            return null;
    }

    public Project getProjectByID(String id)
    {
        return projects.findProjectByID(XUUID.fromString(id));
    }
    public Project getProjectByID(XUUID xid)
    {
        return projects.findProjectByID(xid);
    }

    /**
     * Returns a list of all Project objects.
     */
    public List<Project> getAllProjects()
    {
        return projects.getAllProjects();
    }

    /**
     * Returns a list of all Project objects of the given type.
     */
    public List<Project> getProjects(XUUID caller, String type, String org, String pds, boolean inactive)
    {
        List<Project> projs = new ArrayList<>();
        XUUID         orgID = (org != null) ? XUUID.fromString(org, Organization.OBJTYPE) : null;
        XUUID         pdsID = (pds != null) ? XUUID.fromString(pds, PatientDataSet.OBJTYPE) : null;

        type = Project.normalizeType(type);

        for (Project proj : projects.getAllProjects())
        {
            if (meetsFilter(proj, caller, type, orgID, pdsID))
                projs.add(proj);
        }

        return projs;
    }

    /**
     * Deletes the entire project, relationships, ACLS, etc., as though no prior APIs calls related
     * to that project were ever called.  Ugly and complicated in the end.
     */
    public void deleteProject(Project project) throws IOException
    {
        XUUID projID = project.getID();

        userProjectLogic.deleteByProject(projID);

        removeAcls(project);

        projects.unassociateProject(projID);

        projects.delete(project);
    }

    /**
     *
     */
    private void removeAcls(Project proj) throws IOException
    {
        RoleLogic rl     = sysServices.getRoleLogic();
        XUUID     projID = proj.getID();

        // note that deleteRoleTarget deletes groups which destroys membership so we
        // don't need to remove users at this level

        for (Role role : getRolesForProject(proj))
            rl.deleteRoleTarget(null, role, projID);
    }

    /**
     * Return the list of roles for the project.  This uses the type of the project
     * as the nameID of the role set (prefixed so they don't overlap org role set names).
     */
    public List<Role> getRolesForProject(Project proj)
    {
        RoleSets     roleSets = sysServices.getRoleSets();
        Roles        roles    = sysServices.getRoles();
        RoleSet      rs       = roleSets.findRoleSetByNameID(getRoleSetNameID(proj));
        List<Role>   rRoles   = new ArrayList<>();

        if (rs != null)
        {
            List<Role>   allRoles = roles.getAllRoles();
            for (XUUID role : roleSets.getRolesBySet(rs))
                rRoles.add(roles.findCachedRoleByID(role));
        }

        return rRoles;
    }

    /**
     * Static method to return the fully scoped RoleSet nameID that can be used to look up
     * the Role.  This is useful because the role set nameID is prefixed for projects.
     */
    public static String getRoleSetNameID(Project proj)
    {
        return ROLESET_PREFIX + proj.getType();
    }

    /**
     * Given a project and the name of a role within the project, look up and return the Role.
     */
    public Role getRoleByProjectAndName(Project proj, String rolename)
    {
        return sysServices.getRoleLogic().lookupRoleByName(getRoleSetNameID(proj) + "/" + rolename);
    }

    /**
     * Return a list of roles that the user has within the given project.
     */
    public List<Role> getUserRolesInProject(User user, Project proj) throws IOException
    {
        List<RoleAssignment> assignments = sysServices.getRoleLogic().getRolesAssignedToUser(user);
        List<Role>           roles       = new ArrayList<>();

        for (RoleAssignment ra : assignments)
        {
            if (ra.targetID.equals(proj.getID()))
                roles.add(ra.role);
        }

        return roles;
    }

    /**
     * Adds or updates the user->project association information with the given information.
     */
    public void addUserToProject(Project project, XUUID userID, boolean active, List<String> phases)
    {
        userProjectLogic.addUserToProject(userID, project.getID(), active, phases);
    }

    /**
     * Returns list of users that have been added to project via addUserToProject.
     */
    public List<UserProject> getUsersInProject(Project project)
    {
        return userProjectLogic.getUserProjectForProject(project.getID());
    }

    /**
     * Returns list of projects that the given user has been added to.
     */
    public List<UserProject> getProjectsForUser(User user)
    {
        return userProjectLogic.getUserProjectForUser(user.getID());
    }

    /**
     * Removes the user->project association
     */
    public void removeUserFromProject(Project project, XUUID userID)
    {
        userProjectLogic.removeUserFromProject(userID, project.getID());
    }

    public void removeAllUsersFromProject(Project project)
    {
        userProjectLogic.removeAllUsersFromProject(project.getID());
    }

    /**
     *
     */
    private boolean meetsFilter(Project proj, XUUID caller, String type, XUUID orgID, XUUID pdsID)
    {
        boolean  fine = true;

        //!! ignore caller for now...

        fine = fine && ((type == null)  || type.equals(proj.getType()));
        fine = fine && ((orgID == null) || orgID.equals(proj.getOrganizationID()));
        fine = fine && ((pdsID == null) || pdsID.equals(proj.getPatientDataSetID()));

        return fine;
    }

    // ################################################################
    //  Association with PDS instances
    // ################################################################

    public List<Project> getProjectListForPds(XUUID pdsID)
    {
        return projects.findProjectsByIDs(projects.getProjectsAssociatedWithPds(pdsID));
    }

    // ################################################################
    //  Custom Properties
    // ################################################################


    public void updateProjCustomProperties(Project proj, Map<PropertyBag, Map<String, String>> properties)
    {
        updateProjCustomProperties(proj, properties, EMAIL_NOT_PROVIDED, METHOD_NOT_PROVIDED, COMMENT_NOT_PROVIDED);
    }
    /**
     * Bulk update/set of custom property values on the given project.  Do NOT ignore errors on
     * individual setting.
     */
    public void updateProjCustomProperties(Project proj, Map<PropertyBag, Map<String, String>> properties, String userEmail, String method, String comment)
    {
        for (Map.Entry<PropertyBag, Map<String, String>> outer : properties.entrySet())
        {
            PropertyBag bag = outer.getKey();

            for (Map.Entry<String, String> inner : outer.getValue().entrySet())
            {
                String key   = inner.getKey();
                String value = inner.getValue();

                if (value != null)
                    // Do the versioning after bulk update work finishes, so no need to do versioning here
                    setProjectProperty(bag, proj, key, value, null, null, null, false);
                else
                    removeProjectProperty(bag, proj, key);
            }
        }
        createProjectVersion(proj, userEmail, method == null || method.equals(METHOD_NOT_PROVIDED) ? "projectlogic.updateProjCustomProperties": method, comment);
    }

    /**
     * Add a new custom property to the global set of properties.  The name must be unique
     * when lowercased.
     */ 
    public void addPropertyDef(PropertyBag bag, String name, String typeMeta)   // throws exception if name.trim.tolowercase is not unique
    {
        getBag(bag).addPropertyDef(name, typeMeta);
    }

    /**
     * Returns a map from unique property name to the declared PropertyType of that property.
     */
    public Map<String, String> getPropertyDefinitions(PropertyBag bag, boolean includeMeta) // <propName, propType>
    {
        return getBag(bag).getPropertyDefs(includeMeta);
    }

    /**
     * Removes the custom property definition.  This removal will cascade to a deletion of
     * property values on Project objects.
     */
    public void removePropertyDef(PropertyBag bag, String name)
    {
        getBag(bag).removePropertyDef(name);
    }

    /**
     * Add a property value to the given Project.  The actual value
     * will be converted--as possible--to the declared property type.
     */
    public void setProjectProperty(PropertyBag bag, Project project, String propertyName, String valueStr)
    {
        setProjectProperty(bag, project, propertyName, valueStr, EMAIL_NOT_PROVIDED, METHOD_NOT_PROVIDED, COMMENT_NOT_PROVIDED, DEFAULT_VERSIONING);
    }

    public void setProjectProperty(PropertyBag bag, Project project, String propertyName, String valueStr, String userEmail, String method, String comment)
    {
        setProjectProperty(bag, project, propertyName, valueStr, userEmail, method, comment, DEFAULT_VERSIONING);
    }
    /**
     * Updates the project with the non-null values in the properties
     * Plus, if you don't know who the userEmail is, parse empty string and here will try to get the userEmail for you :)
     */
    public void setProjectProperty(PropertyBag bag, Project project, String propertyName, String valueStr, String userEmail, String method, String comment, Boolean doVersioning) // throws exception if name not known
    {
        getBag(bag).setEntityProperty(project.getID(), propertyName, valueStr);
        if (doVersioning && !VERSION_BLACK_LIST.contains(propertyName)) {
            createProjectVersion(project, userEmail, method == null || method.equals(METHOD_NOT_PROVIDED) ? "projectlogic.setProjectProperty": method, comment);
        }
    }

    /**
     * Remove a custom property value from the given Project.
     */
    public void removeProjectProperty(PropertyBag bag, Project project, String propertyName) // throws exception if name not known
    {
        getBag(bag).removeEntityProperty(project.getID(), propertyName);
    }

    /**
     * Returns a map from ProjectXUUIDs to a map that contains all the name=value pairs for each Project.
     */
    public Map<XUUID, Map<String, Object>> getAllProjectProperties(PropertyBag bag)                 // <ProjectID, <propname, propvalue>>
    {
        return getBag(bag).getAllCustomProperties();
    }

    /**
     * Given a property name, return a map from ProjectXUUID to the property value for that Project.
     */
    public Map<XUUID, Object> getProjectsProperty(PropertyBag bag, String propName)     // <ProjectID, propvalue>
    {
        return getBag(bag).getCustomProperty(propName);
    }

    /**
     * Given a Project, return a map from property name to the property value for that Project.
     */
    public Map<String, Object> getProjectProperties(PropertyBag bag, Project project)   // <propname, propvalue>
    {
        return getBag(bag).getEntityProperties(project.getID());
    }

    /**
     * Given a project XUUID, return a map from property name to the property value for that Project.
     *
     * This method is replicated from method above to optimize getting properties
     * for single project by skipping loading all projects into local cache
     */
    public Map<String, Object> getProjectProperties(PropertyBag bag, XUUID projectId)   // <propname, propvalue>
    {
        return getBag(bag).getEntityPropertiesForOneEntity(projectId);
    }

    /**
     *
     */
    private PropertyUtil getBag(PropertyBag bag)
    {
        if (bag == PropertyBag.GENERIC)
            return genericPropertyUtil;
        else if (bag == PropertyBag.PHASE)
            return phasePropertyUtil;
        else if (bag == PropertyBag.HCC)
            return hccPropertyUtil;
        else if (bag == PropertyBag.QUALITY)
            return qualityPropertyUtil;
        else if (bag == PropertyBag.PROSPECTIVE)
            return prospectivePropertyUtil;
        else if (bag == PropertyBag.LABEL)
            return labelPropertyUtil;
        else
            throw new ProjectException(FailureType.BAD_BAG_NAME, "Unknown PropertyBag name {}", bag);
    }

    public PropertyBag getPropertyBag(String bagName)
    {
        if (bagName.equalsIgnoreCase("gen")) {
            bagName = "GENERIC";
        }
        return PropertyBag.valueOf(bagName.toUpperCase());
    }

    private Map<String, Object> composeCustomPropertiesInProject(Project proj, PropertyBag requested, Map<String, Object> json) {
        PropertyBag[] requestedList = {requested};
        return composeCustomPropertiesInProject(proj, requestedList, json);
    }

    /*
        A "complete" project is composed of two parts -- one is the basic project with the real class Object and another part is the customized project data with Map structure
        In order to get the complete project, need to get the basic project first and then compose with the customized parts
     */
    private Map<String, Object> composeCustomPropertiesInProject(Project proj, PropertyBag[] requested, Map<String, Object> json)
    {
        if (requested == null) {
            return json;
        }
        else {
            Map<String, Object> completeProject = new HashMap<>(json);
            Map<String, Object> props = new HashMap<>();

            for (PropertyBag bag : requested)
            {
                Map<String, Object> bagProps = getProjectProperties(bag, proj);

                if (bagProps.size() > 0) {
                    // people and lots of services get used to "gen" as the field name, so need to backward compatible
                    if (bag == PropertyBag.GENERIC) {
                        props.put(GEN_BAG, bagProps);
                    } else {
                        props.put(bag.toString().toLowerCase(), bagProps);
                    }
                }
            }
            completeProject.put(PROP_KEYNAME, props);
            return completeProject;
        }
    }

    // ###################  Versioning used method  #####################

    public void createProjectVersion(String projectId, String userEmail, String method, String comment)
    {
        Project project = getProjectByID(projectId);
        createProjectVersion(project, userEmail, method, comment);
    }

    public void createProjectVersion(Project project, String userEmail, String method, String comment)
    {

        String projectId = project.getID().toString();
        String tempVersionKey = projectId + "-" + VERSIONING_LOCK_FOR_PROJECT_PROPERTIES;
        String lock = distLock.lock(tempVersionKey, LOCK_DURATION_TIME);
        int attempt = 0;

        // To prevent a race condition, busy-wait until the `DistLock` returns.
        while (attempt < LOCK_MAX_ATTEMPT && lock == null) {
            attempt++;
            distLock.sleep(LOCK_THREAD_SLEEPING_TIME);
            lock = distLock.lock(tempVersionKey, LOCK_DURATION_TIME);
        }
        try {
            String insertVersionNum = getProjNextVersionNum(projectId);
            String projDataVersionInsertKey = getProjDataByVersionKey(projectId, insertVersionNum);
            String projCurVersionKey = getProjCurVersionKey(projectId);
            String realUserEmail = userEmail;
            if (userEmail == null || userEmail.isEmpty() || userEmail.equals(EMAIL_NOT_PROVIDED)) {
                realUserEmail = getUserEmail();
            }
            Map<String, String> storingMap = buildStoringMap(project, insertVersionNum, realUserEmail, composeServiceNameWithlMethod(method), comment);
            redisOps.hmset(projDataVersionInsertKey, storingMap);
            redisOps.set(projCurVersionKey, insertVersionNum);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Error occurs while trying to create project version for project: %s. Error: %s", projectId, e.getMessage()), e);
        } finally {
            distLock.unlock(lock, tempVersionKey);
        }

    }

    public Map<String, String> getProjectWithVersion(String projectId, String version)
    {
        String key = getProjDataByVersionKey(projectId, version);
        return redisOps.hgetAll(key);
    }

    public int getProjCurVersionNum(String projectId)
    {
        String key = getProjCurVersionKey(projectId);
        try {
            return Integer.parseInt(redisOps.get(key));
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Unable to get current Project Version number for project: %s, highly possible it is not existed yet. Error: %s", projectId, e.getMessage()), e);
        }
    }

    private String getProjNextVersionNum(String projectId)
    {
        try {
            return String.valueOf(getProjCurVersionNum(projectId)+1);
        } catch (Exception e) {
            return "1";
        }
    }



    private String getProjCurVersionKey(String projectId)
    {
        return projects.makeKey(prjGetCurNumPrefix + projectId);
    }

    private String getProjDataByVersionKey(String projectId, String version)
    {
        return projects.makeKey(prjVersionPrefix + projectId + prjVersionNumPostfix + version);
    }

    public Map<String, String> buildStoringMap(Project project, String insertVersionNum, String userEmail, String method, String comment)
    {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put(PROJECT_DATA, getProjectByIDLikeUseracctString(project));
        dataMap.put(LAST_CREATED_AT, getCurrentUTCTime());
        dataMap.put(LAST_UPDATED_BY, userEmail);
        dataMap.put(METHOD, method);
        dataMap.put(COMMENT, comment);
        dataMap.put(VERSION, insertVersionNum);
        return dataMap;
    }

    /**
     * Gets list of organizations (or * for all) that the user's roles permit the user to supervise.
     * @param user
     * @param projectId
     * @return
     */
    public Set<String> getHccRolePermittedOrgs(User user, String projectId) throws IOException {
        List<RoleAssignment> rolesAssigned = roleLogic.getRolesAssignedToUser(user);
        Set<String> roleNames = rolesAssigned.stream().map(role -> role.role.getName()).collect(Collectors.toSet());

        // For roles: CUSTOMEROPS, this roles will have full access to project data
        if (roleNames.contains(Role.CUSTOMEROPS)) { // Can see everything
            // all organizations
            return Collections.singleton("*");
        }

        // For roles: SUPERVISOR, QALEAD this roles will have full access to project data
        Set<String> supervisedProjectIds = rolesAssigned.stream()
                .filter(r -> r.role.getName().equals(Role.SUPERVISOR) || r.role.getName().equals(Role.QALEAD))
                .map(r -> r.targetID.toString())
                .collect(Collectors.toSet());
        if (supervisedProjectIds.contains(projectId)) {
            return Collections.singleton("*");
        }

        // For roles: VENDOR_SUPERVISOR or CLIENT_SUPERVISOR or REVIEWER; return user own org
        Set<String> hccRolesSupervisedProjectIds = rolesAssigned.stream()
                .filter(r -> r.role.getName().equals(Role.VENDOR_SUPERVISOR) || r.role.getName().equals(Role.CLIENT_SUPERVISOR)|| r.role.getName().equals(Role.REVIEWER))
                .map(r -> r.targetID.toString())
                .collect(Collectors.toSet());
        if (hccRolesSupervisedProjectIds.contains(projectId)) {
            Set<String> userOrgIds = orgLogic.getUsersOrganizations(
                    user.getID()).stream()
                    .map(uo -> uo.getID().getID())
                    .collect(Collectors.toSet());
            return userOrgIds;
        }

        // For roles: ORG_SUPERVISOR, return targeted assigned orgs
        Set<String> orgLevelSupervisedOrgIds = rolesAssigned.stream()
                .filter(r -> r.role.getName().equals(Role.ORG_SUPERVISOR))
                .map(r -> r.targetID.toString())
                .collect(Collectors.toSet());
        if (!orgLevelSupervisedOrgIds.isEmpty()) {
            return orgLevelSupervisedOrgIds;
        }

        // For any other roles; this never will be the case
        return Collections.EMPTY_SET;
    }
}
