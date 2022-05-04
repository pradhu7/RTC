package com.apixio.useracct.dw.resources;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.util.DateUtil;
import com.apixio.restbase.web.BaseException;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;

import com.apixio.useracct.buslog.*;

import com.apixio.useracct.buslog.ProjectLogic.PropertyBag;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.CanonicalEmail;

import com.apixio.useracct.entity.*;
import com.apixio.useracct.util.DeprecateUtil;
import com.google.common.collect.Sets;

import com.apixio.useracct.UserUtil;

/**
 * Internal service.
 */
@Path("/projects")
@Produces("application/json")
public class ProjectRS extends BaseRS {

    /**
     * Conversion names for client-supplied bag names to PropertyBag enum
     */
    private static final String BAG_GEN   = "gen";
    private static final String BAG_PHASE = "phase";
    private static final String BAG_HCC   = "hcc";
    private static final String BAG_QUALITY = "quality";
    private static final String BAG_ALL   = "all";
    private static final String BAG_PROSPECTIVE = "prospective";
    private static final String BAG_LABEL = "label";
    /**
     * List of bag names of properties to return when the client asks for
     * properties to return.  This is a comma-separated list of PropertyBag
     * enum conversion names (above).
     */
    private static final String ALL_PROPERTY_BAGS = (BAG_GEN + "," + BAG_PHASE + "," + BAG_HCC + ","
            + BAG_QUALITY + "," + BAG_PROSPECTIVE + "," + BAG_LABEL);

    /**
     * Name of Map key that contains the bags of properties when client asks for all
     * properties within a bag (or all bags) returned.
     */
    private static final String PROP_KEYNAME = "properties";

    /**

1) ################ create a new project
POST:/projects
    body:  {"organizationID":"{xuuid}", "patientDataSetID":"{}", "name":"{}", "description":"{}", "type":"{}"}
    returns:  {"id":"..."}

2) ################ get all projects I have access to.  Initially;  ROOT gets all, !ROOT gets none;
GET:/projects?orgID=xxx&pdsID=xxx&type=xxx&inactive=true
    returns:  [
       {"id": "{projID}", "name": "{project.name}", "organizationID": "{XUUID of customer-type Org}", "organizationName": "{name of org}",
          "type":"{type}", "patientDataSetID": "xuuid", "patientDataSetExternalID": {coID}, "patientDataSetName": "{name}"
        }, ...
      ]


3) ################ get all info about given project:
GET:/projects/{projID}
    {"id": "{projID}", "name": "{project.name}", "organizationID": "{XUUID of customer-type Org}", "organizationName": "{name of org}",
      "type":"{type}", "patientDataSetID": "xuuid"
    }


4) ################ update name, desc of project (only those fields are writeable)
PUT:/projects/{projID}
    body:  {"name":"...", "description":"...", ...}


5) ################ deletes the project (and all associations and ACLs)
DELETE:/projects/{projID}


6) ################ adds (associates) the user to the project (if not already added) with the given phase(s)
PUT:/projects/{projID}/members/{userID}
    body:  {"phases":["phase1", ...], "active": true/false}


7) ################ gets all users added to the project via API #6, along with [user,proj] info
GET:/projects/{projID}/members
    returns:  [ {"userid":"{emailAddress}", "projid":"{xuuid}",  "active":true/false,
                 "phases":["phase1", ...] }, ... ]

    note that the objects returned in the list SHOULD be able to be supplied
    as input to API #6


8) ################ removes the user (association) from the project
DELETE:/projects/{projID}/members/{userID}



9) ################ gets all projects the user has been added to (via API #6), along with [user,proj] info
GET:/projects/users/{userID}
    returns:  [ {"userid":"{emailAddress}", "projid":"{xuuid}",  "active":true/false,
                 "phases":["a", ...] }, ... ]


10) ################ assigns user the given role within the project for ACL purposes
PUT:/projects/{projID}/users/{userID}/roles/{roleName}


11) ################ unassigns user the given role within the project for ACL purposes; undoes API #13
DELETE:/projects/{projID}/users/{userID}/roles/{roleName}


12) ################ gets the role(s) the user is assigned to within the given project
GET:/projects/{projID}/users/{userID}/roles
    returns:  ["{role1}", ... ]

12.5) ################ removes ALL members and unassigns ALL roles for the given project (a bit of URL structure overlap with 10,11,12...)
DELETE:/projects/{projID}/users


################ custom properties metadata
13) GET:/projects/propdefs/{bag}
14) POST:/projects/propdefs/{bag}
15) DELETE:/projects/propdefs/{bag}

################ per-instance custom properties value management:
16) PUT:/projects/{someID}/properties/{bag}/{name}
17) DELETE:/projects/{someID}/properties/{bag}/{name}
18) GET:/projects/{someID}/properties/{bag}

################ querying custom properties across all entities of that type:
19) GET:/projects/properties/{bag}
20) GET:/projects/properties/{bag}/{name}

####  summary of above:

    POST:  /projects                                              create a new project
    GET:   /projects?orgID=xxx&pdsID=xxx&type=xxx&inactive=true   get all projects I have access to.  Initially;  ROOT gets all, ~ROOT gets none;
    GET:   /projects/{projID}                                     get all info about given project
    PUT:   /projects/{projID}                                     update name, desc of project (only those fields are writeable)
    DELETE:/projects/{projID}                                     deletes the project (obviously removes membership and role assignments)

    PUT:   /projects/{projID}/members/{userID}                    adds the user to the project (if not already added) with the given phase(s)
    GET:   /projects/{projID}/members                             gets all users added to the project via API #5, along with [user,proj] info
    DELETE:/projects/{projID}/members/{userID}                    removes the user from the project
    X:     /projects/{projID}/members/{userID}/roles/{roleName}
    X:     /projects/{projID}/members/{userID}/roles
    GET:   /projects/users/{userID}                               gets all projects the user has been added to (via API #5), along with [user,proj] info

    PUT:   /projects/{projID}/users/{userID}/roles/{roleName}     assigns user the given role within the project for ACL purposes
    DELETE:/projects/{projID}/users/{userID}/roles/{roleName}     unassigns user the given role within the project for ACL purposes; undoes API #13
    GET:   /projects/{projID}/users/{userID}/roles                gets the role(s) the user is assigned to within the given project

    DELETE:/projects/{projID}/users                               unassigns all users from all roles and removes all user membership for the project
    DELETE:/projects/{projID}/users/{userID}                      unassigns given user from all roles and removes user membership on that project

    GET:   /projects/propdefs/{bag}
    POST:  /projects/propdefs/{bag}
    DELETE:/projects/propdefs/{bag}
    PUT:   /projects/{someID}/properties/{bag}/{name}
    DELETE:/projects/{someID}/properties/{bag}/{name}
    GET:   /projects/{someID}/properties/{bag}
    GET:   /projects/properties/{bag}
    GET:   /projects/properties/{bag}/{name}

    Note (4/16/2019):  new API gateway needed aliases for these endpoints:

       X:/projects/{projID}/users/{userID}/roles/{roleName}
       X:/projects/{projID}/users/{userID}/roles

    The aliases are the same except replacing "/users/" with "/members/".  This is
    shown in the above list with the "X:" for the HTTP verb.

    */
    private PrivSysServices sysServices;

    /**
     * Field names MUST match what's defined in the API spec!  These classes really
     * *are* the JSON (or HTTP PUT entity body) spec.  These are a go-between what
     * the client expresses things in and what the rest of the system expresses things
     * in.  There ideally is a good match between the two but that's not always possible.
     *
     * The overall design pattern is:
     *
     *  * low level entity required properties are any that are:
     *    * truly readonly (meaning they MUST never be changed after construction)
     *    * required for proper reconstruction from the DAO
     *
     *  * low level entity constructor should have ALL and ONLY required parameters
     *
     *  * any default value for low level properties that isn't the Java type default
     *    MUST be set at the business logic level, not the entity level
     *
     *  * any property value adjustment/constraints that are part of the semantics of
     *    the entity (e.g., a Date object that must be at 00:00:00 of the day) MUST
     *    be done at the entity level
     *
     *  * business logic accepts "native" (i.e., already converted from external
     *    representation) parameter values.  any extra validation MUST be done at
     *    the business logic level
     *
     *  * business logic defines a single DTO class that is sufficient to hold all
     *    field values for both creation and modification of the entity.  Java native
     *    types MUST be boxed objects (e.g., Integer instead of int).
     *
     *  * business logic "createXyz" method MUST take a param of the DTO type.
     *    It splits out the required parameters for the entity constructor call and
     *    if there are extra, it then calls entity.setXyz methods
     *
     *  * business logic "modifyXyz" method MUST take a param of the DTO type.
     *    It makes calls to entity setters for non-null param fields.
     *
     *  * XyzProperties is the middle-ground for converting to/from the entity level
     *    representation and the outgoing external model (it could be well-argued that
     *    this functionality could also be moved to the resource area).  It exposes
     *    conversions for enumerations <-> strings, etc.
     *
     *  * resource code defines a JAXB-compatible simple structure class that matches/defines
     *    the JSON spec.  This one class is used for both POST and PUT.
     *
     *  * resource code converts from JAXB struct to business logic classes; validation
     *    on things like enums, etc., is done here.
     *
     */
    public static class ProjParams {
        // String/external form of what's in ProjectLogic.CreateParams

        // all projects
        public String   createdAt;          // ISO8601 format; accepted in JSON but ignored as it's readonly
        public String   name;               // 
        public String   description;        // 
        public String   type;               // 
        public String   organizationID;     // XUUID
        public String   patientDataSetID;   // XUUID
        public Boolean  status;             //
        public String   state;              // ["new", "bundled", "started", "completed"]
        public Double   budget;
        public String   deadline;           // ISO8601 format:  "yyyy-MM-dd'T'HH:mm:ss'Z'"
        public String   datasource;

        // hcc projects
        public String   dosStart;           // ISO8601 format:  "yyyy-MM-dd'T'HH:mm:ss'Z'"
        public String   dosEnd;             // ISO8601 format:  "yyyy-MM-dd'T'HH:mm:ss'Z'"
        public String   paymentYear;        // YYYY
        public String   sweep;              // ["initial", "midYear", "finalReconciliation"]
        public String   passType;           // ["First Pass", "Second Pass"]
        public Double   rawRaf;
        public Double   raf;
        public String   patientList;
        public String   docFilterList;

        // quality projects
        public String  measurementYear;       // YYYY
        public String  measureList;
        public String  lineOfBusiness;
        public String  measureDateRangeOverride;
        public Boolean measureEligibilityFiltering;
        public String  eligibleMeasurePrograms;
        public String  eligibleComplianceStatus;


        //prospective projects
        public Double historicalYears; // required

        // setting of custom properties; while declared String=>Object we assume
        // it's Map<String, Map<String, String>> to force an incoming JSON structure
        // like:
        //
        //  { "name":"something", "properties": { "hcc": {"hccProp1":"val1", ...}, "gen": {...} } }
        public Map<String, Object> properties;

        public Map<String, Object> toLogging()
        {
            Map<String, Object> logs = new HashMap<>();

            put(logs, "createdAt",        createdAt);
            put(logs, "name",             name);
            put(logs, "description",      description);
            put(logs, "type",             type);
            put(logs, "organizationID",   organizationID);
            put(logs, "patientDataSetID", patientDataSetID);
            put(logs, "status",           status);
            put(logs, "state",            state);
            put(logs, "budget",           budget);
            put(logs, "deadline",         deadline);
            put(logs, "datasource",       datasource);

            put(logs, "dosStart",         dosStart);
            put(logs, "dosEnd",           dosEnd);
            put(logs, "paymentYear",      paymentYear);
            put(logs, "sweep",            sweep);
            put(logs, "passType",         passType);
            put(logs, "rawRaf",           rawRaf);
            put(logs, "raf",              raf);
            put(logs, "patientList",      patientList);
            put(logs, "docFilterList",    docFilterList);

            put(logs, "measurementYear",           measurementYear);
            put(logs, "measureList",               measureList);
            put(logs, "lineOfBusiness",            lineOfBusiness);
            put(logs, "measureDateRangeOverride",  measureDateRangeOverride);
            put(logs, "measureEligibilityFiltering", measureEligibilityFiltering);
            put(logs, "eligibleMeasurePrograms",   eligibleMeasurePrograms);
            put(logs, "eligibleComplianceStatus",  eligibleComplianceStatus);

            return logs;
        }

        private void put(Map<String, Object> map, String name, Object value)
        {
            if (value != null)
                map.put(name, value);
        }
    }

    public static class UserProjectParams {
        public String       userid;    // email address or XUUID
        public String       userID;    // only XUUID
        public String       projectID;
        public boolean      active;
        public List<String> phases;
    }

    public ProjectRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Project entity APIs                                            */
    /* ################################################################ */
    /* ################################################################ */

    /*
      POST:  /projects                                              create a new project
      GET:   /projects?orgID=xxx&pdsID=xxx&type=xxx&inactive=true   get all projects I have access to.  Initially;  ROOT gets all, ~ROOT gets none;
      GET:   /projects/{projID}                                     get all info about given project
      coded    PUT:   /projects/{projID}                                     update name, desc of project (only those fields are writeable)
      coded    DELETE:/projects/{projID}                                     deletes the project
    */
    
    @POST
    @Consumes("application/json")
    public Response createProject(@Context HttpServletRequest request, final ProjParams params)
    {
        final ApiLogger           logger = super.createApiLogger(request, "/projects");
        final Map<String, String> json   = new HashMap<>();

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    Project      proj;

                    if (params == null)
                    {
                        badRequestEmptyParam("Project Parameters");
                    }

                    if (QualityProject.QUALITY_TYPE.equalsIgnoreCase(params.type))
                    {
                        proj = pl.createQualityProject(validateQualityProjParams(params, true));
                    }
                    else if (ProspectiveProject.PROSPECTIVE_TYPE.equalsIgnoreCase(params.type))
                    {
                        proj = pl.createProspectiveProject(validateProspectiveProjParams(params, true));
                    }
                    else if (LabelProject.LABEL_TYPE.equalsIgnoreCase(params.type))
                    {
                        proj = pl.createLabelProject(validateLabelProjParams(params, true));
                    }
                    else // default to HccProject
                    {
                        proj = pl.createHccProject(validateHccProjParams(params, true));
                    }

                    String id = proj.getID().toString();
                    logger.addParameter("projectId", id);

                    if (params.properties != null)
                        pl.updateProjCustomProperties(proj, convertCustomProps(params.properties));

                    logParameters(logger, proj.getID(), params);

                    json.put("id", id);
                    return ok(json);
                }});
    }

    /**
     */
    @GET
    public Response getProjects(@Context HttpServletRequest request,
                                @QueryParam("orgID")      final String orgID,
                                @QueryParam("pdsID")      final String pdsID,
                                @QueryParam("type")       final String type,
                                @QueryParam("inactive")   final String inactive,
                                @QueryParam("properties") final String properties
        )
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/projects");
        final XUUID               caller   = RestUtil.getInternalToken().getUserID();

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    List<Map<String, Object>> projs = new ArrayList<>();
                    List<Project> projectList = null;

                    try {
                        projectList = sysServices.getProjectLogic().getProjects(caller, type, orgID, pdsID, Boolean.valueOf(inactive));

                    } catch (IllegalArgumentException iea){
                        badRequest(iea.getMessage());
                    }

                    for (Project proj : projectList)
                    {
                        Map<String, Object> json = ProjectProperties.toMap(proj, getOrganization(proj), getPds(proj));

                        handleReturnProperties(proj, properties, json);
                        projs.add(json);
                    }

                    return ok(projs);
                }});
    }

    /**
     *
     */
    private Organization getOrganization(Project proj)
    {
        return sysServices.getOrganizationLogic().getOrganizationByID(proj.getOrganizationID().toString()); //!!! YUCK .tostring
    }

    private PatientDataSet getPds(Project proj)
    {
        return sysServices.getPatientDataSetLogic().getPatientDataSetByID(proj.getPatientDataSetID());
    }

    /**
     * 
     */
    @GET
    @Path("/{projID}")
    public Response getProject(@Context HttpServletRequest request, @PathParam("projID") final String projID,
                               @QueryParam("properties") final String properties)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/projects/{projID}");

        logger.addParameter("projectId", projID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Project proj = null;

                    try {
                        proj = sysServices.getProjectLogic().getProjectByID(projID);
                    } catch (IllegalArgumentException ex) {
                        badRequest(ex.getMessage());
                    }

                    if (proj != null)
                    {
                        Map<String, Object> json = ProjectProperties.toMap(proj, getOrganization(proj), getPds(proj));

                        handleReturnProperties(proj, properties, json);

                        return ok(json);
                    }
                    else
                    {
                        return notFound("Project", projID);
                    }
                }});
    }

    /**
     * Adds, as requested by the value(s) in "requested", project properties.  The "requested"
     * string is a comma-separated list of bag names; it can also be "all" to return all bags.
     * The returned Map structure always has "properties" and then each bag is a nested Map
     * of name=value.
     */
    private void handleReturnProperties(Project proj, String requested, Map<String, Object> json)
    {
        if (requested != null)
        {
            ProjectLogic        projLogic = sysServices.getProjectLogic();
            Map<String, Object> props     = new HashMap<>();

            json.put(PROP_KEYNAME, props);

            if (requested.equals(BAG_ALL))
                requested = ALL_PROPERTY_BAGS;

            for (String spec : requested.split(","))
            {
                PropertyBag bag = null;

                try
                {
                    bag = getPropertyBag(spec);
                }
                catch (IllegalArgumentException iae)
                {
                    badRequest(iae.getMessage());
                }

                if (bag != null)
                {
                    Map<String, Object> bagProps = projLogic.getProjectProperties(bag, proj);

                    if (bagProps.size() > 0)
                        props.put(spec, bagProps);
                }
            }
        }
    }

    /**
     */
    @PUT
    @Consumes("application/json")
    @Path("/{projID}")
    @Deprecated
    public Response updateProject(@Context HttpServletRequest       request,
                                  @PathParam("projID") final String projID,
                                  final ProjParams                  params
        )
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/projects/{projID}");

        logger.addParameter("projectId", projID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    /**
                     * makekey function is written in the superclass (DaoBase) of projects
                     * Since there is no way to get the DaoBase here, so using projects class for convention
                     */
                    String key = "deprecated_endpoint-updateProject_in_useraccount";
                    if ("true".equals(DeprecateUtil.getRedisValue(sysServices, key))) {
                        String deprecatedMsg = "updateProject method in useraccounts is deprecated and replaced by Projectadmin.\n"
                                + "If you use python-apxapi, please use put_project_basic_property method in Projectadmin.\n"
                                + "If you call the endpoint directly,  please refer to PUT: /projadmin/legacy/{projId} in Projectadmin.\n"
                                + "Please see projectadmin & useraccounts in python-apxapi for more detail\n"
                                + "and refer to Ramo if having any question or finding any bug. Thank you~ :D";
                        return Response.status(405).entity(deprecatedMsg).build();
                    } else {
                        ProjectLogic pl = sysServices.getProjectLogic();
                        Project proj = pl.getProjectByID(projID);

                        if (proj != null) {
                            if (proj.getProjectClass() == Project.ProjectClass.HCC)
                                pl.updateProject((HccProject) proj, validateHccProjParams(params, false));
                            else if (proj.getProjectClass() == Project.ProjectClass.QUALITY)
                                pl.updateProject((QualityProject) proj, validateQualityProjParams(params, false));
                            else if (proj.getProjectClass() == Project.ProjectClass.PROSPECTIVE)
                                pl.updateProject((ProspectiveProject) proj, validateProspectiveProjParams(params, false));
                            else if (proj.getProjectClass() == Project.ProjectClass.LABEL)
                                pl.updateProject((LabelProject) proj, validateLabelProjParams(params, false));

                            if (params.properties != null)
                                pl.updateProjCustomProperties(proj, convertCustomProps(params.properties));

                            logParameters(logger, proj.getID(), params);

                            return ok();
                        } else {
                            return notFound("Project", projID);
                        }
                    }
                }});
    }

    private Map<PropertyBag, Map<String, String>> convertCustomProps(Map<String, Object> props)
    {
        Map<PropertyBag, Map<String, String>> converted = new HashMap<>();

        for (Map.Entry<String, Object> outer : props.entrySet())
        {
            PropertyBag bag  = getPropertyBag(outer.getKey());
            Object      valo = outer.getValue();

            if ((bag != null) && (valo instanceof Map))
            {
                Map<String, String> bagProps = new HashMap<>();
                Map<Object, Object> bagMap   = (Map<Object, Object>) valo;

                for (Map.Entry inner : bagMap.entrySet())
                {
                    Object key  = inner.getKey();
                    Object vali = inner.getValue();

                    if ((key instanceof String) && (vali instanceof String))
                        bagProps.put(((String) key), (String) vali);
                }

                if (bagProps.size() > 0)
                    converted.put(bag, bagProps);
            }
        }

        return converted;
    }

    private HccProjectDTO validateHccProjParams(ProjParams reqParams, boolean forCreate)
    {
        HccProjectDTO params = new HccProjectDTO();

        if (forCreate)
        {
            if (reqParams.organizationID == null)
                badRequestNullParam("organizationID");
            else if (reqParams.patientDataSetID == null)
                badRequestNullParam("patientDataSetID");
            else if (reqParams.type == null)
                badRequestNullParam("type");

            // these fields can't be modified after creation:
            try {
                params.type = HccProject.Type.valueOf(reqParams.type.toUpperCase());
                params.organizationID = XUUID.fromString(reqParams.organizationID);
                params.patientDataSetID = XUUID.fromString(reqParams.patientDataSetID);
            } catch (IllegalArgumentException iae) {
                badRequest(iae.getMessage());
            }
        }

        params.name             = reqParams.name;
        params.description      = reqParams.description;
        params.dosStart         = iso8601("dosStart", reqParams.dosStart);
        params.dosEnd           = iso8601("dosEnd", reqParams.dosEnd);
        params.paymentYear      = reqParams.paymentYear;
        params.sweep            = ProjectProperties.toSweep(reqParams.sweep);
        params.passType         = ProjectProperties.toPassType(reqParams.passType);
        params.status           = reqParams.status;
        params.state            = ProjectProperties.toState(reqParams.state);
        params.rawRaf           = reqParams.rawRaf;
        params.raf              = reqParams.raf;
        params.budget           = reqParams.budget;
        params.deadline         = iso8601("deadline", reqParams.deadline);
        params.datasource       = reqParams.datasource;
        params.patientList      = reqParams.patientList;
        params.docFilterList    = reqParams.docFilterList;

        if (forCreate)
        {
            if (!params.organizationID.getType().equals(Organization.OBJTYPE))
                badRequestParam("organizationID", reqParams.organizationID, "missing/invalid organization value {}");
            else if (!params.patientDataSetID.getType().equals(PatientDataSet.OBJTYPE))
                badRequestParam("patientDataSetID", reqParams.patientDataSetID, "missing/invalid patientDataSet value {}");
        }

        return params;
    }

    private ProspectiveProjectDTO validateProspectiveProjParams(ProjParams reqParams, boolean forCreate)
    {
        ProspectiveProjectDTO params = new ProspectiveProjectDTO();
        if (forCreate)
        {
            if (reqParams.organizationID == null)
                badRequestNullParam("organizationID");
            else if (reqParams.patientDataSetID == null)
                badRequestNullParam("patientDataSetID");
            try
            {
                // these fields can't be modified after creation:
                params.organizationID = XUUID.fromString(reqParams.organizationID);
                params.patientDataSetID = XUUID.fromString(reqParams.patientDataSetID);
            }
            catch (IllegalArgumentException iae)
            {
                badRequest(iae.getMessage());
            }

            if (!params.organizationID.getType().equals(Organization.OBJTYPE))
                badRequestParam("organizationID", reqParams.organizationID, "missing/invalid organization value {}");
            else if (!params.patientDataSetID.getType().equals(PatientDataSet.OBJTYPE))
                badRequestParam("patientDataSetID", reqParams.patientDataSetID, "missing/invalid patientDataSet value {}");
        }


        params.name = reqParams.name;
        params.description      = reqParams.description;
        params.status           = reqParams.status;
        params.state            = ProjectProperties.toProspectiveState(reqParams.state);
        params.budget           = reqParams.budget;
        params.deadline         = iso8601("deadline", reqParams.deadline);
        params.datasource       = reqParams.datasource;
        params.historicalYears  = reqParams.historicalYears;
        params.patientList      = reqParams.patientList;
        params.paymentYear      = reqParams.paymentYear;

        return params;

    }

    private QualityProjectDTO validateQualityProjParams(ProjParams reqParams, boolean forCreate)
    {
        QualityProjectDTO params = new QualityProjectDTO();

        if (forCreate)
        {
            if (reqParams.organizationID == null)
                badRequestNullParam("organizationID");
            else if (reqParams.patientDataSetID == null)
                badRequestNullParam("patientDataSetID");

            try
            {
                // these fields can't be modified after creation:
                params.organizationID = XUUID.fromString(reqParams.organizationID);
                params.patientDataSetID = XUUID.fromString(reqParams.patientDataSetID);
            }
            catch (IllegalArgumentException iae)
            {
                badRequest(iae.getMessage());
            }

            if (!params.organizationID.getType().equals(Organization.OBJTYPE))
                badRequestParam("organizationID", reqParams.organizationID, "missing/invalid organization value {}");
            else if (!params.patientDataSetID.getType().equals(PatientDataSet.OBJTYPE))
                badRequestParam("patientDataSetID", reqParams.patientDataSetID, "missing/invalid patientDataSet value {}");
        }

        params.name             = reqParams.name;
        params.description      = reqParams.description;
        params.status           = reqParams.status;
        params.state            = ProjectProperties.toQualityState(reqParams.state);
        params.budget           = reqParams.budget;
        params.deadline         = iso8601("deadline", reqParams.deadline);
        params.datasource       = reqParams.datasource;
        params.measurementYear  = reqParams.measurementYear;
        params.measureList      = reqParams.measureList;
        params.lineOfBusiness   = reqParams.lineOfBusiness;
        params.measureDateRangeOverride    = reqParams.measureDateRangeOverride;
        params.measureEligibilityFiltering = reqParams.measureEligibilityFiltering;
        params.eligibleMeasurePrograms     = reqParams.eligibleMeasurePrograms;
        params.eligibleComplianceStatus    = reqParams.eligibleComplianceStatus;

        return params;
    }

    /**
     *  Simple validation params for Label project
     */
    private LabelProjectDTO validateLabelProjParams(ProjParams reqParams, boolean forCreate)
    {
        LabelProjectDTO labelParams = new LabelProjectDTO();
        if(forCreate)
        {
            if(reqParams.organizationID == null)
            {
                badRequestNullParam("organizationID");
            }
            else if (reqParams.patientDataSetID == null)
            {
                badRequestNullParam("patientDataSetID");
            }

            try
            {
                labelParams.organizationID = XUUID.fromString(reqParams.organizationID);
                labelParams.patientDataSetID = XUUID.fromString(reqParams.patientDataSetID);
            }
            catch (IllegalArgumentException iae)
            {
                badRequest(iae.getMessage());
            }

            if (!labelParams.organizationID.getType().equals(Organization.OBJTYPE))
            {
                badRequestParam("organizationID", reqParams.organizationID, "missing/invalid organization value {}");
            }
            else if (!labelParams.patientDataSetID.getType().equals(PatientDataSet.OBJTYPE))
                badRequestParam("patientDataSetID", reqParams.patientDataSetID, "missing/invalid patientDataSet value {}");

        }

        labelParams.name = reqParams.name;
        labelParams.description = reqParams.description;
        labelParams.status = reqParams.status;
        labelParams.state = ProjectProperties.toLabelState(reqParams.state);

        return labelParams;
    }

    /**
     * Log the project create/update parameters by adding each one as a logging parameter.
     */
    private void logParameters(ApiLogger logger, XUUID projID, ProjParams params)
    {
        Organization   org = null;
        PatientDataSet pds = null;

        logger.addParameter("projectId", projID.toString());

        if (params.organizationID != null)
            org = sysServices.getOrganizationLogic().getOrganizationByID(params.organizationID);

        if (params.patientDataSetID != null)
            pds = sysServices.getPatientDataSetLogic().getPatientDataSetByID(XUUID.fromString(params.patientDataSetID));

        for (Map.Entry<String, Object> entry : params.toLogging().entrySet())
            logger.addParameter(entry.getKey(), entry.getValue().toString());

        // special cases:
        // "organizationName":  event cloud wants it but it's not an input parameter so we need to get it ourselves
        if (org != null)
            logger.addParameter("organizationName", org.getName());

        if (pds != null)
            logger.addParameter("patientDataSetExternalID", pds.getCOID());
    }

    /**
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{projID}")
    public Response deleteProject(@Context HttpServletRequest request,  @PathParam("projID") final String projID)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/projects/{projID}");

        logger.addParameter("projectId", projID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    Project      proj = pl.getProjectByID(projID);

                    if (proj != null)
                    {
                        pl.deleteProject(proj);

                        return ok();
                    }
                    else
                    {
                        return notFound("Project", projID);
                    }
                }});
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   User/Project membership/associations                           */
    /* ################################################################ */
    /* ################################################################ */

    /*
     * PUT:   /projects/{projID}/members/{userid}                    adds the user to the project (if not already added) with the given phase(s)
     * GET:   /projects/{projID}/members                             gets all users added to the project via API #5, along with [user,proj] info
     * DELETE:/projects/{projID}/members/{userid}                    removes the user from the project
     * GET:   /projects/users/{userid}                               gets all projects the user has been added to (via API #5), along with [user,proj] info
     *
     * The following are aliases for the endpoints that have
     *  /users/ instead of /members/ in the middle:
     *
     *  PUT:   /projects/{projID}/members/{userID}/roles/{roleName}     assigns user the given role within the project for ACL purposes
     *  DELETE:/projects/{projID}/members/{userID}/roles/{roleName}     unassigns user the given role within the project for ACL purposes; undoes API #13
     *  GET:   /projects/{projID}/members/{userID}/roles                gets the role(s) the user is assigned to within the given project
     */

    @PUT
    @Consumes("application/json")
    @Path("/{projID}/members/{userID}")
    public Response addProjectMember(@Context HttpServletRequest       request,
                                     @PathParam("projID") final String projID,
                                     @PathParam("userID") final String userid,     // email addr or XUUID
                                     final UserProjectParams           userProjectInfo
        )
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/members/{userID}");

        logger.addParameter("projectId", projID);
        logger.addParameter("userId", userid);
        logger.addParameter("active", userProjectInfo.active);
        logger.addParameter("phases", userProjectInfo.phases);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();

                    Project      proj = null;

                    try
                    {
                        proj = pl.getProjectByID(projID);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    User         user = findUserByEmailOrID(userid, false);

                    if (userProjectInfo == null)
                    {
                        return badRequest("Expected HTTP entity body");
                    }
                    else if (user == null)
                    {
                        return notFound("User", userid);
                    }
                    else if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        pl.addUserToProject(proj, user.getID(), userProjectInfo.active, userProjectInfo.phases);

                        return ok();
                    }
                }});
    }

    @GET
    @Consumes("application/json")
    @Path("/{projID}/members")
    public Response getProjectMembers(@Context HttpServletRequest request,
                                      @PathParam("projID") final String  projID,
                                      @QueryParam("roles") final Boolean includeRoles
        )
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/members");

        logger.addParameter("projectId", projID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    Project proj = null;

                    try
                    {
                         proj = pl.getProjectByID(projID);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    if (proj != null)
                    {
                        List<UserProject> ups = pl.getUsersInProject(proj);
                        // ups.isActive, .getUserID, .getProjectID, .getPhases

                        // Added for HCC roles
                        ups = filterProjectUsersByUserRoleOrg(projID, ups, sysServices.getProjectLogic(), sysServices.getOrganizationLogic(), UserUtil.getCachedUser());

                        return ok(fillInUserProjectInfo(ups, (includeRoles != null) ? includeRoles.booleanValue() : false));
                    }
                    else
                    {
                        return notFound("Project", projID);
                    }
                }});
    }

    /**
     * Filter project users based on the current login user role
     * on the given Project.
     */
    public List<UserProject> filterProjectUsersByUserRoleOrg(String projectId, List<UserProject> userProjects, ProjectLogic projectLogic, OrganizationLogic orgLogic, User user) throws IOException {
        Set<String> userRolePermittedOrgs = projectLogic.getHccRolePermittedOrgs(user, projectId);
        List<UserProject> filteredUserProjects = userProjects;

        // Filter data for HCC roles orgs
        if (!userRolePermittedOrgs.isEmpty() && !userRolePermittedOrgs.contains("*")) { // filter data for hcc roles
            filteredUserProjects = userProjects.stream().filter(up -> {
                XUUID userId = up.getUserID();
                List<Organization> userOrgs = null;
                try {
                    // get project member orgs
                    userOrgs = orgLogic.getUsersOrganizations(userId);
                } catch (IOException ie) {
                    userOrgs = Collections.EMPTY_LIST;
                }
                // Check if user org and project member org matches
                Set<String> userOrgIds = userOrgs.stream().map(uo -> uo.getID().getID()).collect(Collectors.toSet());
                return !Sets.intersection(userRolePermittedOrgs, userOrgIds).isEmpty();
            }).collect(Collectors.toList());
        }

        return filteredUserProjects;
    }

    @DELETE
    @Path("/{projID}/members/{userID}")
    public Response removeProjectMember(@Context HttpServletRequest       request,
                                        @PathParam("projID") final String projID,
                                        @PathParam("userID") final String userid
        )
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/members/{userID}");

        logger.addParameter("projectId", projID);
        logger.addParameter("userId", userid);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    Project      proj = pl.getProjectByID(projID);
                    User         user = findUserByEmailOrID(userid, false);

                    if (user == null)
                    {
                        return notFound("User", userid);
                    }
                    else if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        pl.removeUserFromProject(proj, user.getID());

                        return ok();
                    }
                }});
    }

    @GET
    @Consumes("application/json")
    @Path("/users/{userID}")
    public Response getProjectsForUser(@Context HttpServletRequest request,
                                       @PathParam("userID") final String  userid,
                                       @QueryParam("roles") final Boolean includeRoles
        )
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/users/{userID}");

        logger.addParameter("userId", userid);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    User         user = findUserByEmailOrID(userid, false);

                    if (user != null)
                    {
                        List<UserProject> ups = pl.getProjectsForUser(user);

                        return ok(fillInUserProjectInfo(ups, (includeRoles != null) ? includeRoles.booleanValue() : false));
                    }
                    else
                    {
                        return notFound("User", userid);
                    }
                }});
    }

    // *  PUT:   /projects/{projID}/members/{userID}/roles/{roleName}     assigns user the given role within the project for ACL purposes
    @PUT
    @Consumes("application/json")
    @Path("/{projID}/members/{userID}/roles/{role}")
    public Response assignProjectRoleAlias(@Context HttpServletRequest       request,
                                      @PathParam("projID") final String projID,
                                      @PathParam("userID") final String userid,     // email addr or XUUID
                                      @PathParam("role")   final String rolename
        )
    {
        return assignProjectRole(request, projID, userid, rolename);
    }
    
    // *  DELETE:/projects/{projID}/members/{userID}/roles/{roleName}     unassigns user the given role within the project for ACL purposes; undoes API #13
    @DELETE
    @Consumes("application/json")
    @Path("/{projID}/members/{userID}/roles/{role}")
    public Response unassignProjectRoleAlias(@Context HttpServletRequest       request,
                                        @PathParam("projID") final String projID,
                                        @PathParam("userID") final String userid,     // email addr or XUUID
                                        @PathParam("role")   final String rolename
        )
    {
        return unassignProjectRole(request, projID, userid, rolename);
    }

    // *  GET:   /projects/{projID}/members/{userID}/roles                gets the role(s) the user is assigned to within the given project
    @GET
    @Path("/{projID}/members/{userID}/roles")
    public Response getRolesForUserAlias(
        @Context             HttpServletRequest request,
        @PathParam("projID") final String       projID,
        @PathParam("userID") final String       userid     // email addr or XUUID
        )
    {
        return getRolesForUser(request, projID, userid);
    }

    /**
     * Given either an account email address or its XUUID, lookup and return the actual User object;
     * return null for any syntax error or for user-not-found.
     *
     * Re forUserUpdate, if false then underlying code checks if caller has VIEWUSER_OPERATION rights.
     */
    private User findUserByEmailOrID(String userid, boolean forUserUpdate) throws IOException
    {
        UserLogic ul     = sysServices.getUserLogic();
        Token     caller = RestUtil.getInternalToken();

        if ((userid == null) || (userid.length() == 0))
            return null;
        else if ("me".equals(userid))
            return ul.getUser(caller, "me", forUserUpdate);

        try
        {
            return ul.getUser(caller, XUUID.fromString(userid, User.OBJTYPE).toString(), forUserUpdate);
        }
        catch (IllegalArgumentException x1)
        {
            try
            {
                return ul.getUserByEmail(caller.getUserID(), (new CanonicalEmail(userid)).getEmailAddress(), forUserUpdate);
            }
            catch (IllegalArgumentException x2)
            {
            }
        }

        return null;
    }

    /**
     * Convert the list of UserProject info to POJOs for what is to be returned as JSON.
     * The only real changes from the raw info are to return the user's email address
     * and to return 'active' as a JSON boolean and 'phases' as a JSON list of strings.
     */
    private List<Map<String, Object>> fillInUserProjectInfo(List<UserProject> ups, boolean includeRoles) throws IOException
    {
        List<XUUID>               userIDs  = new ArrayList<>();
        List<Map<String, Object>> objects  = new ArrayList<>();
        Projects                  projects = sysServices.getProjects();
        Map<String, List<String>> roleMap  = null;
        List<User>                users;
        List<String>              roles;

        projects.cacheLatest();

        if (includeRoles)
            roleMap = createRoleMap(ups);

        for (UserProject up : ups)
            userIDs.add(up.getUserID());

        users = sysServices.getUsers().getUsers(userIDs);

        for (UserProject up : ups)
        {
            Map<String, Object> obj = new HashMap<>();
            Project             prj = projects.findCachedProjectByID(up.getProjectID());

            if (prj != null)
            {
                obj.put("projectStatus", Boolean.valueOf(prj.getStatus()));
                obj.put("projName",      prj.getName());
                obj.put("projId",        up.getProjectID().toString());
                obj.put("active",        Boolean.valueOf(up.isActive()));
                obj.put("phases",        up.getPhases());
                obj.put("userid",        getUserEmail(up.getUserID(), users));
                obj.put("userID",        up.getUserID().toString());

                // Add in the role the user has in each project to avoid massive querying from the front end.
                if (includeRoles &&
                    (roles = roleMap.get(makeRoleMapKey(up.getUserID(), up.getProjectID()))) != null)
                    obj.put("roles", roles);

                objects.add(obj);
            }
        }

        return objects;
    }

    /**
     * Efficiently create the map from [userID+projID] to Role so that we have a
     * fast way of getting the user's role within a given project.  There will be
     * two "modes" for the data coming in:  either the userID will be constant
     * and the projID will vary across the list elements, or the projID will be
     * constant and the userID will vary.  This method won't care about that but
     * will fetch the roles for each distinct project and will get the list of
     * users in each role for each distinct project.
     */
    private Map<String, List<String>> createRoleMap(List<UserProject> ups)  throws IOException // (userID+projID) => rolename
    {
        Map<String, List<String>> roleMap  = new HashMap<>();
        Set<XUUID>                projIDs  = new HashSet<>();
        ProjectLogic              pl       = sysServices.getProjectLogic();
        Projects                  projects = sysServices.getProjects();
        RoleLogic                 rl       = sysServices.getRoleLogic();

        // gross that I'm using both logic and dao level...hack from time pressure

        for (UserProject up : ups)
            projIDs.add(up.getProjectID());

        for (XUUID projID : projIDs)
        {
            Project proj = projects.findCachedProjectByID(projID);

            for (Role role : pl.getRolesForProject(proj))
            {
                for (XUUID userID : rl.getUsersWithRole(null/*caller?*/, projID, role.getID()))
                {
                    String       key   = makeRoleMapKey(userID, projID);
                    List<String> roles = roleMap.get(key);

                    if (roles == null)
                    {
                        roles = new ArrayList<String>();
                        roleMap.put(key, roles);
                    }

                    roles.add(role.getName());
                }
            }
        }

        return roleMap;
    }

    /**
     *
     */
    private String makeRoleMapKey(XUUID userID, XUUID projID)
    {
        return userID.toString() + ":" + projID.toString();
    }

    /**
     * Given an XUUID for a user and a list of Users, return User.emailAddress for
     * the single matching XUUID in the list.
     */
    private String getUserEmail(XUUID userID, List<User> users)
    {
        // just do stupid scan for now as we don't expect many users per project

        for (User user : users)
        {
            if (user.getID().equals(userID))
                return user.getEmailAddr();
        }

        return null;
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Project/user role management                                   */
    /* ################################################################ */
    /* ################################################################ */

    /*
      GET:   /projects/{projID}/roles                               get all roles that user can be assigned to the given org
      PUT:   /projects/{projID}/users/{userID}/roles/{roleName}     assigns user the given role within the project for ACL purposes
      DELETE:/projects/{projID}/users/{userID}/roles/{roleName}     unassigns user the given role within the project for ACL purposes; undoes API #13
      GET:   /projects/{projID}/users/{userID}/roles                gets the role(s) the user is assigned to within the given project
     */

    @GET
    @Path("/{projID}/roles")
    public Response getRolesForProject(
        @Context              HttpServletRequest request,
        @PathParam("projID")  final String       projID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/projects/{projID}/roles");

        logger.addParameter("projectId", projID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Project      proj   = sysServices.getProjectLogic().getProjectByID(projID);

                    if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        List<String> rnames = new ArrayList<>();

                        for (Role role : sysServices.getProjectLogic().getRolesForProject(proj))
                            rnames.add(role.getName());

                        return ok(rnames);
                    }
                }});
    }

    @PUT
    @Consumes("application/json")
    @Path("/{projID}/users/{userID}/roles/{role}")
    public Response assignProjectRole(@Context HttpServletRequest       request,
                                      @PathParam("projID") final String projID,
                                      @PathParam("userID") final String userid,     // email addr or XUUID
                                      @PathParam("role")   final String rolename
        )
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/users/{userID}/roles/{role}");

        logger.addParameter("projectId", projID);
        logger.addParameter("userId", userid);
        logger.addParameter("role", rolename);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    RoleLogic    rl   = sysServices.getRoleLogic();

                    Project      proj = null;
                    try
                    {
                        proj = pl.getProjectByID(projID);
                    }
                    catch(IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    User         user = findUserByEmailOrID(userid, false);

                    if (user == null)
                    {
                        return notFound("User", userid);
                    }
                    else if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        Role role = pl.getRoleByProjectAndName(proj, rolename);

                        if (role == null)
                            return notFound("Role", rolename);

                        rl.assignUserToRole(RestUtil.getInternalToken().getUserID(), user, role, proj.getID());

                        return ok();
                    }
                }});
    }

    @DELETE
    @Consumes("application/json")
    @Path("/{projID}/users/{userID}/roles/{role}")
    public Response unassignProjectRole(@Context HttpServletRequest       request,
                                        @PathParam("projID") final String projID,
                                        @PathParam("userID") final String userid,     // email addr or XUUID
                                        @PathParam("role")   final String rolename
        )
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/users/{userID}/roles/{role}");

        logger.addParameter("projectId", projID);
        logger.addParameter("userId", userid);
        logger.addParameter("role", rolename);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    RoleLogic    rl   = sysServices.getRoleLogic();
                    Project      proj = pl.getProjectByID(projID);
                    User         user = findUserByEmailOrID(userid, false);

                    if (user == null)
                    {
                        return notFound("User", userid);
                    }
                    else if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        Role role = pl.getRoleByProjectAndName(proj, rolename);

                        if (role == null)
                            return notFound("Role", rolename);

                        rl.unassignUserFromRole(RestUtil.getInternalToken().getUserID(), user, role, proj.getID());

                        return ok();
                    }
                }});
    }


    @DELETE
    @Consumes("application/json")
    @Path("/{projID}/users")
    public Response unassignAllProjectRoles(@Context HttpServletRequest request, @PathParam("projID") final String projID)
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/users");

        logger.addParameter("projectId", projID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    RoleLogic    rl   = sysServices.getRoleLogic();
                    Project      proj = pl.getProjectByID(projID);

                    if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        pl.removeAllUsersFromProject(proj);
                        rl.unassignAllUsersFromTarget(RestUtil.getInternalToken().getUserID(),
                                                      pl.getRolesForProject(proj),
                                                      proj.getID());

                        return ok();
                    }
                }});
    }

    @DELETE
    @Consumes("application/json")
    @Path("/{projID}/users/{userID}")
    public Response removeUserFromProject(@Context HttpServletRequest request, @PathParam("projID") final String projID,
                                          @PathParam("userID") final String userID)
    {
        final ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/users/{userID}");

        logger.addParameter("projectId", projID);
        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    RoleLogic    rl   = sysServices.getRoleLogic();
                    Project      proj = pl.getProjectByID(projID);
                    User         user = findUserByEmailOrID(userID, false);

                    if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else if (user == null)
                    {
                        return notFound("User", userID);
                    }
                    else
                    {
                        XUUID userX = user.getID();

                        pl.removeUserFromProject(proj, userX);

                        rl.unassignUserFromTarget(RestUtil.getInternalToken().getUserID(),
                                                  userX, pl.getRolesForProject(proj),
                                                  proj.getID());

                        return ok();
                    }
                }});
    }

    @GET
    @Path("/{projID}/users/{userID}/roles")
    public Response getRolesForUser(
        @Context             HttpServletRequest request,
        @PathParam("projID") final String       projID,
        @PathParam("userID") final String       userid     // email addr or XUUID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/projects/{projID}/users/{userID}/roles");

        logger.addParameter("projectId", projID);
        logger.addParameter("userId", userid);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    ProjectLogic pl   = sysServices.getProjectLogic();
                    Project      proj = sysServices.getProjectLogic().getProjectByID(projID);
                    User         user = findUserByEmailOrID(userid, false);

                    if (user == null)
                    {
                        return notFound("User", userid);
                    }
                    else if (proj == null)
                    {
                        return notFound("Project", projID);
                    }
                    else
                    {
                        List<String> roles = new ArrayList<>();

                        for (Role role : pl.getUserRolesInProject(user, proj))
                            roles.add(role.getName());

                        return ok(roles);
                    }

                }});
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Custom Project properties                                      */
    /* ################################################################ */
    /* ################################################################ */

    /**
     * Eight APIs are supported for managing custom Project properties:
     *
     *  APIs that operate on the set of custom properties (meta-level):
     *  1) POST:/projects/propdefs/{bag}           # creates a new custom Project property
     *  2) GET:/projects/propdefs/{bag}            # returns the set of custom Project properties
     *  3) DELETE:/projects/propdefs/{bag}/{name}  # deletes a custom Project property
     *
     *  APIs that operate on a particular User entity instance:
     *  4) PUT:/projects/{projID}/properties/{bag}/{name}     # sets a custom property value on a Project instance
     *  5) DELETE:/projects/{projID}/properties/{bag}/{name}  # removes a custom property value from a Project instance
     *  6) GET:/projects/{projID}/properties/{bag}            # gets all custom properties on a Project instance
     *
     *  APIs that provide query operations on the entire set of Projects with custom properties:
     *  7) GET:/projects/properties/{bag}         # gets ALL properties of ALL Projects with custom proeprties
     *  8) GET:/projects/properties/{bag}/{name}  # gets all Projects with the given property, along with the value for each Project
     */

    /**
     * Creates a new property definition.  The input HTTP entity body must be of the form
     *
     *  {"name": "auniquename", "type":"one of [STRING, BOOLEAN, INTEGER, DOUBLE, DATE]" }
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     *
     * Arbitrary (string) metadata on the type is specified by appending a ":" and the metadata
     * to the type.  For example:
     *
     * {"name": "count", "type":"INTEGER:immutable" }
     *
     * The metadata string is not returned in GET:/uorgs/propdefs unless the "meta"
     * query parameter is set to true:
     *
     *  GET:/projects/{bag}/propdefs?meta=true
     */
    @POST
    @Path("/propdefs/{bag}")
    @Consumes("application/json")
    public Response createCustomPropertyDef(@Context HttpServletRequest request,
                                            @PathParam("bag") final String bag, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/propdefs/{bag}");

        logger.addParameter("bag", bag);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    String  name = jsonObj.get("name");
                    String  type = jsonObj.get("type");

                    logger.addParameter("propertyName", name);
                    logger.addParameter("propertyType", type);

                    if (emptyString(name))
                        return badRequestEmptyParam("name");
                    else if (emptyString(type))
                        return badRequestEmptyParam("type");

                    try
                    {
                        sysServices.getProjectLogic().addPropertyDef(getPropertyBag(bag), name, type);
                    }
                    catch(IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    return ok();
                }});
    }

    /**
     * Returns a list of all property definition.  The returned JSON is of the form
     *
     *  [ {"propname": "proptype"}, ... ]
     *
     * where "propname" is really the actual property name (which was supplied as the "name"
     * JSON field during the creation) and "proptype" is the type (ditto).  For example:
     *
     *  [ {"count": "INTEGER"} ]
     *
     * If the query parameter "meta" is set to true, then the metadata string is included
     * in the returned type:
     *
     *  [ {"count": "INTEGER:immutable"} ]
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @GET
    @Path("/propdefs/{bag}")
    public Response getCustomPropertiesDefs(@Context HttpServletRequest request,
                                            @PathParam("bag") final String bag, @QueryParam("meta") final String meta)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/propdefs/{bag}");

        logger.addParameter("bag", bag);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(sysServices.getProjectLogic().getPropertyDefinitions(getPropertyBag(bag), Boolean.valueOf(meta)));
                }});
    }

    /**
     * Deletes the given property definition.  All property values across all projects
     * entities are also removed.
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @DELETE
    @Path("/propdefs/{bag}/{name}")
    public Response deleteCustomPropertyDef(@Context HttpServletRequest request,
                                            @PathParam("bag") final String bag, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/propdefs/{bag}/{name}");

        logger.addParameter("bag", bag);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PropertyBag propertyBag = null;
                    try
                    {
                        propertyBag = getPropertyBag(bag);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    sysServices.getProjectLogic().removePropertyDef(propertyBag, name);

                    return ok();
                }});
    }

    /**
     * Adds the given property to the given project entity.  The input HTTP entity
     * must be of the form:
     *
     *  {"value": "theactualvalue"}
     *
     * The name of the property must have already been added via a request to
     * POST:/projects/propdefs/{bag} and the value must be able to be interpreted
     * as valid within the given type.  For example, if the type of "count" is
     * "INTEGER", then the value specified in the HTTP entity body must be able to
     * be parsed as a Java int.
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @PUT
    @Path("/{projID}/properties/{bag}/{name}")
    @Consumes("application/json")
    @Deprecated
    public Response setCustomProperty(@Context HttpServletRequest request, @PathParam("bag") final String bag,
                                      @PathParam("projID") final String entityID,
                                      @PathParam("name") final String name, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/properties/{bag}/{name}");

        logger.addParameter("projectId", entityID);
        logger.addParameter("bag", bag);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    String key = "deprecated_endpoint-setCustomProperty_in_useraccount";
                    if ("true".equals(DeprecateUtil.getRedisValue(sysServices, key))) {
                        String deprecatedMsg = "setCustomProperty method in useraccounts is deprecated and replaced by Projectadmin.\n"
                                + "If you use python-apxapi, please use put_project_custom_property method in Projectadmin.\n"
                                + "If you call the endpoint directly,  please refer to PUT: /projadmin/legacy/{projId}/properties/{bag}/{name} in Projectadmin.\n"
                                + "Please see projectadmin & useraccounts in python-apxapi for more detail\n"
                                + "and refer to Ramo if having any question or finding any bug. Thank you~ :D";
                        return Response.status(405).entity(deprecatedMsg).build();
                    } else {
                        Project proj = checkedGetProject(entityID);
                        String value = jsonObj.get("value");

                        logger.addParameter("value", value);

                        if (emptyString(value))
                            return badRequestEmptyParam("value");
                        else if (proj == null)
                            return notFound("project", entityID);

                        sysServices.getProjectLogic().setProjectProperty(getPropertyBag(bag), proj, name, value);

                        return ok();
                    }
                }});
    }

    /**
     * Removes the given property from the given project entity.
     *
     * The name of the property must have already been added via a request to
     * POST:/projects/propdefs/{bag}.
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @DELETE
    @Path("/{projID}/properties/{bag}/{name}")
    public Response removeCustomProperty(@Context HttpServletRequest request, @PathParam("bag") final String bag,
                                         @PathParam("projID") final String entityID, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/properties/{bag}/{name}");

        logger.addParameter("projectId", entityID);
        logger.addParameter("name", name);
        logger.addParameter("bag", bag);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PropertyBag propertyBag = null;
                    Project  proj  = checkedGetProject(entityID);

                    if (proj == null)
                        return notFound("project", entityID);

                    try
                    {
                        propertyBag = getPropertyBag(bag);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    sysServices.getProjectLogic().removeProjectProperty(propertyBag, proj, name);

                    return ok();
                }});
    }

    /**
     * Returns the full set of properties (name and value) that have been added to the
     * given project.  The returned JSON structure will look like:
     *
     *  { "name1": "value1", "name2": "value2", ...}
     *
     * where "name1" is the actual name of the property, etc.  For example:
     *
     *  { "count": 35 }
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @GET
    @Path("/{projID}/properties/{bag}")
    public Response getAllProjectProperties(@Context HttpServletRequest request, @PathParam("bag") final String bag,
                                            @PathParam("projID") final String entityID)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/{projID}/properties/{bag}");

        logger.addParameter("projectId", entityID);
        logger.addParameter("bag", bag);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Project  proj  = checkedGetProject(entityID);

                    if (proj == null)
                        return notFound("project", entityID);

                    return ok(sysServices.getProjectLogic().getProjectProperties(getPropertyBag(bag), proj));
                }});
    }

    
    /**
     * Gets all properties of all project entities.  The JSON return structure is
     * as follows:
     *
     *  { { "object1ID" : { "prop1": "value1", "prop2": "value2", ...} },
     *    { "object2ID" : { "prop1": "value1", "prop2": "value2", ...} },
     *    ... }
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @GET
    @Path("/properties/{bag}")
    public Response getAllProjectsProperties(@Context HttpServletRequest request, @PathParam("bag") final String bag)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/properties/{bag}");

        logger.addParameter("bag", bag);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PropertyBag propertyBag = null;
                    try
                    {
                        propertyBag = getPropertyBag(bag);
                    }
                    catch(IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    return ok(sysServices.getProjectLogic().getAllProjectProperties(propertyBag));
                }});
    }

    /**
     * Gets a single property across all project entities.  The JSON return structure is
     * as follows:
     *
     *  { "object1ID": "value1", 
     *    "object2ID": "value2",
     *    ... }
     *
     * where the values are for the given property.  If an object has never had the value
     * set on it, that object ID won't be included in the returned JSON.
     *
     * Allowed values of {bag} are:  "gen", "phase", and "hcc".
     */
    @GET
    @Path("/properties/{bag}/{name}")
    public Response getAllProjectsProperty(@Context HttpServletRequest request, @PathParam("bag") final String bag,
                                           @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/projects/properties/{bag}/{name}");

        logger.addParameter("bag", bag);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    if (emptyString(name))
                        return badRequestEmptyParam("name");

                    return ok(sysServices.getProjectLogic().getProjectsProperty(getPropertyBag(bag), name));
                }});
    }

    /**
     * Looks up and returns the Project by ID, verifying the caller has permission to operate
     * on the given Project.
     */
    private Project checkedGetProject(String projID)
    {
        //!!! use caller...        return sysServices.getProjectLogic().getProjectByID(RestUtil.getInternalToken(), projID);

        Project project = null;

        try
        {
            project = sysServices.getProjectLogic().getProjectByID(projID);
        }
        catch (IllegalArgumentException iae)
        {
            badRequest(iae.getMessage());
        }

        return project;
    }

    /**
     * Returns true if the string doesn't contain non-whitespace characters.
     */
    private static boolean emptyString(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     *
     */
    private PropertyBag getPropertyBag(String bag)
    {
        if (bag.equals(BAG_GEN))
            return PropertyBag.GENERIC;
        else if (bag.equals(BAG_PHASE))
            return PropertyBag.PHASE;
        else if (bag.equals(BAG_HCC))
            return PropertyBag.HCC;
        else if (bag.equals(BAG_QUALITY))
            return PropertyBag.QUALITY;
        else if (bag.equals(BAG_PROSPECTIVE))
            return PropertyBag.PROSPECTIVE;
        else if (bag.equals(BAG_LABEL))
            return PropertyBag.LABEL;
        else
            throw new IllegalArgumentException("Unknown property set name [" + bag + "]");
    }

    private static Date iso8601(String parameter, String dateString)
    {
        Date date = null;

        if(dateString!=null)
        {
            date = DateUtil.validateIso8601(dateString);
            if (date == null)
            {
                throw BaseException.badRequest(String.format("Invalid date format [%s] provided for property [%s]", dateString, parameter));
            }
        }
        return date;
    }

}
