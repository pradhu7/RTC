package com.apixio.useracct.dw.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.web.BaseRS;
import com.apixio.security.Security;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.OrganizationLogic;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.dao.PatientDataSets;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.useracct.eprops.PdsProperties;

/**
 * RESTful endpoints to manage PatientDataSet resources.  A PatientDataSet historically
 * was known as a Customer, which was termed a CustomerOrg in CareOptimizer so the
 * XUUID prefix is "O_" (for Organization).
 *
 * PatientDataSets can either be attached (associated) with
 * a Customer-type of Organization or can be unassociated.  Once associated, this association
 * must be removed before the PDS can be associated with another Organization.
 */
@Path("/patientdatasets")
@Produces("application/json")
public class PatientDataSetRS extends BaseRS
{
    /**
     * ok GET:/patientdatasets                       returns PDSs that are NOT owned by a Customer-type Organization
     *
     * ok POST:/patientdatasets                      not needed until CareOptimizer is removed
     * GET:/patientdataset/{pdsID}
     * PUT:/patientdataset/{pdsID}
     *
     * Standard custom property APIs:
     *
     * ok POST:/patientdatasets/propdefs
     * ok GET:/patientdataset/propdefs
     * ok DELETE:/patientdatasets/propdefs/{name}
     *
     * ok GET:/patientdataset/{pdsID}/properties
     * ok GET:/patientdataset/{pdsID}/property/{name}
     * ok GET:/patientdataset/property/{name}
     * ok GET:/patientdataset/{pdsID}
     * ok DELETE:/patientdataset/{pdsID}/property   "primaryAssignAuthority", ... (org properties/ pds properties)
     */

    private PrivSysServices            sysServices;
    private PatientDataSetLogic        pdsLogic;
    private PatientDataSets            patientDatasets;
    private OrganizationLogic          orgLogic;
    private Security                   security;

    public static class CreatePdsParams
    {
        public String  name;
        public String  description;
        public String  owningOrgID;  // optional

        // coID and externalID are the SAME (minus Long vs String);
        // having both is solely intended to support both old (those
        // that submit "coID") and new ("externalID") until all old
        // clients supply externalID.  During creation we prefer
        // externalID over coID (and no check is done to make sure
        // they're equal if both are supplied)

//        public Long    coID;         // optional Care Optimizer ID.  MUST be "Long" and not "long"
//        public String  externalID;   // optional Care Optimizer ID.  SHOULD match coID
    }

    public static class UpdatePdsParams
    {
        public String  name;
        public String  description;
    }

    public PatientDataSetRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices      = sysServices;
        this.pdsLogic         = sysServices.getPatientDataSetLogic();
        this.patientDatasets  = sysServices.getPatientDataSets();
        this.orgLogic         = sysServices.getOrganizationLogic();

        this.security         = Security.getInstance();
    }

    /* ################################################################ */
    /* ################################################################ */
    /*                                                                  */
    /* ################################################################ */
    /* ################################################################ */

    /**
     * Returns the list of all PatientDataSets, with the option of returning just those
     * that are NOT associated with a PDS-type Organization.  The list of newly
     * created PDS objects as a JSON list.  Each JSON object has the form:
     *
     *  { id: 'O_61d89e25-e438-450e-8326-72dfb211fd91',
     *   isActive: true,
     *   description: 'yoyo',
     *   name: 'Some PDS' }
     */
    @GET
    public Response getPdses(@Context HttpServletRequest request, @QueryParam("unowned") final Boolean unowned)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/patientdatasets");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    List<Map<String, Object>> jsons = new ArrayList<>();
                    List<PatientDataSet>      pdsList;

                    if ((unowned != null) && unowned.booleanValue())
                        pdsList = pdsLogic.getUnassociatedPds();
                    else
                        pdsList = pdsLogic.getAllPatientDataSets(false);  // include non-active

                    for (PatientDataSet pds : pdsList)
                        jsons.add(PdsProperties.toJson(pds));

                    return ok(jsons);
                }});
    }

    /**
     * Creates a new PatientDataSet entity.  The required input JSON is:
     *
     *  { "name": "thename",
     *    "description": "thedescription",
     *    "owningOrgID": "XUUID of owningOrg" }
     *
     * The newly created PDS is returned as a JSOn object with the form:
     *
     *  {'coID': 'Some long'
     *   'description': 'yoyo',
     *   'externalID': 'Some int',
     *   'id': 'O_61d89e25-e438-450e-8326-72dfb211fd91',
     *   'isActive': False
     *   'name': 'Some PDS',
     *   'ownerOrg': 'UO_6ececf9e-2b54-4acf-893c-4c947a14d238'}
     */
    @POST
    @Consumes("application/json")
    public Response createPDS(
        @Context HttpServletRequest request,
        final CreatePdsParams       params
        )
    {
        final ApiLogger           logger = super.createApiLogger(request, "/patientdatasets");

        logger.addParameter("name", params.name);
        logger.addParameter("description", params.description);
        logger.addParameter("owningOrgID", params.owningOrgID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PatientDataSet pds;

                    if (params.owningOrgID != null)
                        pds = orgLogic.createPatientDataSetWithOwningOrg(params.name, params.description, params.owningOrgID);
                    else
                        pds = pdsLogic.createPatientDataSet(params.name, params.description, patientDatasets.getNextPdsID(true));

                    return ok(PdsProperties.toJson(pds));
                }});
    }

    /**
     * Gets information about the given PatientDataSet.  The information is returned
     * as a JSON object with the form:
     *
     *  { id: 'O_61d89e25-e438-450e-8326-72dfb211fd91',
     *   isActive: true,
     *   description: 'yoyo',
     *   name: 'Some PDS' }
     */
    @GET
    @Path("/{pdsID}")
    public Response getPDS(@Context HttpServletRequest request, @PathParam("pdsID") final String pdsID)
    {
        final ApiLogger           logger = super.createApiLogger(request, "/patientdatasets/{pdsID}");

        logger.addParameter("pdsId", pdsID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    XUUID xuuid = getPdsXuuid(pdsID);

                    PatientDataSet       pds   = pdsLogic.getPatientDataSetByID(xuuid);

                    if (pds != null)
                    {
                        Map<String, Object> json = PdsProperties.toJson(pds);

                        json.put("properties", pdsLogic.getPatientDataSetProperties(pds));
                        return ok(json);
                    }
                    else
                    {
                        return notFound("PatientDataSet", pdsID);
                    }
                }});
    }

    /**
     * Updates information about the given PatientDataSet.  The required input JSON is:
     *
     *  { "name": "thename", "description": "thedescription" }
     *
     * Both fields are optional (but obviously it makes sense to supply at least one of them).
     */
    @PUT
    @Path("/{pdsID}")
    public Response updatePDS(@Context HttpServletRequest request, @PathParam("pdsID") final String pdsID, final UpdatePdsParams params)
    {
        final ApiLogger logger = super.createApiLogger(request, "/patientdatasets/{pdsID}");

        logger.addParameter("pdsId", pdsID);
        logger.addParameter("name", params.name);
        logger.addParameter("description", params.description);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PatientDataSet       pds   = pdsLogic.getPatientDataSetByID(XUUID.fromString(pdsID, PatientDataSet.OBJTYPE));

                    if (pds != null)
                    {
                        if (params.name != null)
                            pds.setName(params.name);
                        if (params.description != null)
                            pds.setDescription(params.description);

                        pdsLogic.updatePatientDataSet(pds);

                        return ok();
                    }
                    else
                    {
                        return notFound("PatientDataSet", pdsID);
                    }
                }});
    }

    /**
     * Activates the given PatientDataSet.  The input HTTP entity body must be
     * like:
     *
     *  { "primary_assign_authority": "theassignauthority" }
     *
     * A successful activation will have created the required Cassandra column families
     * and will have set the properties that other (outside this service) code uses.
     *
     * Should the activation was successful, the call would return a HTTP 200.
     */
    @POST
    @Path("/{pdsID}/activate")
    public Response activatePDS(@Context HttpServletRequest request, @PathParam("pdsID") final String pdsIDStr, final Map<String, String> params)
    {
        final ApiLogger logger = super.createApiLogger(request, "/patientdatasets/{pdsID}/activate");

        logger.addParameter("pdsId", pdsIDStr);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    XUUID          pdsId = getPdsXuuid(pdsIDStr);
                    PatientDataSet pds   = pdsLogic.getPatientDataSetByID(pdsId);

                    if (pds == null)
                    {
                        return notFound("PatientDataSet", pdsIDStr);
                    }

                    String aauth = (params != null) ? params.get(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY) : null;
                    logger.addParameter(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY, aauth);

                    if (aauth == null)
                    {
                        return badRequestNullParam("JSON body {\"" + PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY + "\":\"whatever\"}");
                    }

                    // create a per-PDS Vault datakey so each PDS has its own managed AES key
                    security.createScopeKey(pdsIDStr);

                    // Everything is valid at this point, proceed...
                    pdsLogic.activatePatientDataSet(pds, aauth);

                    return ok();
                }});
    }

    private XUUID getPdsXuuid(String pdsIDStr)
    {
        XUUID pdsId = null;

        try
        {
            pdsId = XUUID.fromString(pdsIDStr, PatientDataSet.OBJTYPE);
        }
        catch (IllegalArgumentException iae)
        {
            badRequest(iae.getMessage());
        }

        return pdsId;
    }

    /**
     * Deactivates the given PatientDataSet.  No HTTP entity body is expected.
     */
    @POST
    @Path("/{pdsID}/markActivate")
    public Response markActivatePDS(@Context HttpServletRequest request, @PathParam("pdsID") final String pdsIDStr)
    {
        final ApiLogger logger = super.createApiLogger(request, "/patientdatasets/{pdsID}/markActivate");

        logger.addParameter("pdsId", pdsIDStr);

        return super.restWrap(new RestWrap(logger) {
            public Response operation() throws Exception
            {
                XUUID    pdsID = XUUID.fromString(pdsIDStr, PatientDataSet.OBJTYPE);

                pdsLogic.markActivatePatientDataSet(pdsID);

                return ok();
            }});
    }


    /**
     * Deactivates the given PatientDataSet.  No HTTP entity body is expected.
     */
    @POST
    @Path("/{pdsID}/activateInternalCluster")
    public Response activateInternalCluster(@Context HttpServletRequest request, @PathParam("pdsID") final String pdsIDStr)
    {
        final ApiLogger logger = super.createApiLogger(request, "/patientdatasets/{pdsID}/activateInternalCluster");

        logger.addParameter("pdsId", pdsIDStr);

        return super.restWrap(new RestWrap(logger) {
            public Response operation() throws Exception
            {
                PatientDataSet       pds   = pdsLogic.getPatientDataSetByID(XUUID.fromString(pdsIDStr, PatientDataSet.OBJTYPE));

                pdsLogic.activateInternalCluster(pds);

                return ok();
            }});
    }


    /**
     * Deactivates the given PatientDataSet.  No HTTP entity body is expected.
     */
    @POST
    @Path("/{pdsID}/deactivate")
    public Response deactivatePDS(@Context HttpServletRequest request, @PathParam("pdsID") final String pdsIDStr)
    {
        final ApiLogger logger = super.createApiLogger(request, "/patientdatasets/{pdsID}/deactivate");

        logger.addParameter("pdsId", pdsIDStr);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    XUUID    pdsID = getPdsXuuid(pdsIDStr);

                    pdsLogic.deactivatePatientDataSet(pdsID);

                    return ok();
                }});
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Custom PatientDataSet properties                                      */
    /* ################################################################ */
    /* ################################################################ */

    /**
     * Eight APIs are supported for managing custom PatientDataSet properties:
     *
     *  APIs that operate on the set of custom properties (meta-level):
     *  1) POST:/patientdatasets/propdefs                # creates a new custom PatientDataSet property
     *  2) GET:/patientdatasets/propdefs                 # returns the set of custom PatientDataSet properties
     *  3) DELETE:/patientdatasets/propdefs/{name}       # deletes a custom PatientDataSet property
     *
     *  APIs that operate on a particular User entity instance:
     *  4) PUT:/patientdatasets/{entityID}/properties/{name}       # sets a custom property value on a PatientDataSet instance
     *  5) DELETE:/patientdatasets/{entityID}/properties/{name}    # removes a custom property value from a PatientDataSet instance
     *  6) GET:/patientdatasets/{entityID}/properties              # gets all custom properties on a PatientDataSet instance
     *
     *  APIs that provide query operations on the entire set of PatientDataSets with custom properties:
     *  7) GET:/patientdatasets/properties                # gets ALL properties of ALL PatientDataSets with custom proeprties
     *  8) GET:/patientdatasets/properties/{name}         # gets all PatientDataSets with the given property, along with the value for each PatientDataSet
     */

    /**
     * Creates a new property definition.  The input HTTP entity body must be of the form
     *
     *  {"name": "auniquename", "type":"one of [STRING, BOOLEAN, INTEGER, DOUBLE, DATE]" }
     *
     * Arbitrary (string) metadata on the type is specified by appending a ":" and the metadata
     * to the type.  For example:
     *
     * {"name": "count", "type":"INTEGER:immutable" }
     *
     * The metadata string is not returned in GET:/patientdataset/propdefs unless the "meta"
     * query parameter is set to true:
     *
     *  GET /patientdatasets/propdefs?meta=true
     */
    @POST
    @Path("/propdefs")
    @Consumes("application/json")
    public Response createCustomPropertyDef(@Context HttpServletRequest request, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/propdefs");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    String  name = jsonObj.get("name");
                    String  type = jsonObj.get("type");

                    logger.addParameter("name", name);
                    logger.addParameter("type", type);

                    if (emptyString(name))
                        return badRequestEmptyParam("name");
                    else if (emptyString(type))
                        return badRequestEmptyParam("type");

                    pdsLogic.addPropertyDef(name, PropertyType.valueOf(type.toUpperCase()));

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
     */
    @GET
    @Path("/propdefs")
    public Response getCustomPropertiesDefs(@Context HttpServletRequest request, @QueryParam("meta") final String meta)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/propdefs");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(pdsLogic.getPropertyDefinitions(Boolean.valueOf(meta)));
                }});
    }

    /**
     * Deletes the given property definition.  All property values across all patient data set
     * entities are also removed.
     */
    @DELETE
    @Path("/propdefs/{name}")
    public Response deleteCustomPropertyDef(@Context HttpServletRequest request, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/propdefs/{name}");

        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    pdsLogic.removePropertyDef(name);

                    return ok();
                }});
    }

    /**
     * Adds the given property to the given patient data set entity.  The input HTTP entity
     * must be of the form:
     *
     *  {"value": "theactualvalue"}
     *
     * The name of the property must have already been added via a request to
     * POST:/patientdatasets/propdefs and the value must be able to be interpreted
     * as valid within the given type.  For example, if the type of "count" is
     * "INTEGER", then the value specified in the HTTP entity body must be able to
     * be parsed as a Java int.
     */
    @PUT
    @Path("/{pdsID}/properties/{name}")
    @Consumes("application/json")
    public Response setCustomProperty(@Context HttpServletRequest request, @PathParam("pdsID") final String entityID, @PathParam("name") final String name, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/{pdsID}/properties/{name}");

        logger.addParameter("pdsId", entityID);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    PatientDataSet   pds   = checkedGetPatientDataSet(entityID);
                    String           value = jsonObj.get("value");

                    logger.addParameter("value", value);

                    if (emptyString(value))
                        return badRequestEmptyParam("value");
                    else if (pds == null)
                        return notFound("pds", entityID);

                    // pdsLogic.setPatientDataSetProperty(pds, name, value);
                    pdsLogic.setPdsProperty(name, value, pds.getID());

                    return ok();
                }});
    }

    /**
     * Removes the given property from the given patient data set entity.
     *
     * The name of the property must have already been added via a request to
     * POST:/patientdatasets/propdefs.
     */
    @DELETE
    @Path("/{pdsID}/properties/{name}")
    public Response removeCustomProperty(@Context HttpServletRequest request, @PathParam("pdsID") final String entityID, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/{pdsID}/properties/{name}");

        logger.addParameter("pdsId", entityID);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PatientDataSet  pds  = checkedGetPatientDataSet(entityID);

                    if (pds == null)
                        return notFound("pds", entityID);

                    if (pdsLogic.isUndeletablePdsProperty(name))
                    {
                        return badRequest("Can't remove an immutable pds value [" + name + "]");
                    }
                    else
                    {
                        pdsLogic.removePatientDataSetProperty(pds, name);

                        return ok();
                    }
                }});
    }

    /**
     * Removes the given seq store from the given patient data set and cassandra.
     *
     * The name of the model should be in patient data set
     */
    @DELETE
    @Path("/{pdsID}/seqstore/{model}")
    public Response removeSeqStoreProperty(@Context HttpServletRequest request, @PathParam("pdsID") final String entityID, @PathParam("model") final String model)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/{pdsID}/seqstore/{model}");

        logger.addParameter("pdsId", entityID);
        logger.addParameter("model", model);

        return super.restWrap(new RestWrap(logger) {
            public Response operation()
            {
                PatientDataSet  pds  = checkedGetPatientDataSet(entityID);

                if (pds == null)
                    return notFound("pds", entityID);

                pdsLogic.removeSeqStore(pds, model);

                return ok();
            }});
    }

    /**
     * Returns the full set of properties (name and value) that have been added to the
     * given patient data set.  The returned JSON structure will look like:
     *
     *  { "name1": "value1", "name2": "value2", ...}
     *
     * where "name1" is the actual name of the property, etc.  For example:
     *
     *  { "count": 35 }
     */
    @GET
    @Path("/{pdsID}/properties")
    public Response getAllPatientDataSetProperties(@Context HttpServletRequest request, @PathParam("pdsID") final String entityID)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/{pdsID}/properties");

        logger.addParameter("pdsId", entityID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PatientDataSet  pds  = checkedGetPatientDataSet(entityID);

                    if (pds == null)
                        return notFound("pds", entityID);

                    return ok(pdsLogic.getPatientDataSetProperties(pds));
                }});
    }

    /**
     * Gets all properties of all patient data set entities.  The JSON return structure is
     * as follows:
     *
     *  { { "object1ID" : { "prop1": "value1", "prop2": "value2", ...} },
     *    { "object2ID" : { "prop1": "value1", "prop2": "value2", ...} },
     *    ... }
     */
    @GET
    @Path("/properties")
    public Response getAllPatientDataSetsProperties(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/properties");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(pdsLogic.getAllPatientDataSetProperties());
                }});
    }

    /**
     * Gets a single property across all patient data set entities.  The JSON return structure is
     * as follows:
     *
     *  { "object1ID": "value1",
     *    "object2ID": "value2",
     *    ... }
     *
     * where the values are for the given property.  If an object has never had the value
     * set on it, that object ID won't be included in the returned JSON.
     */
    @GET
    @Path("/properties/{name}")
    public Response getAllPatientDataSetsProperty(@Context HttpServletRequest request, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/patientdatasets/properties/{name}");

        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    if (emptyString(name))
                        return badRequestEmptyParam("name");

                    return ok(pdsLogic.getPatientDataSetsProperty(name));
                }});
    }

    /**
     * Looks up and returns the PatientDataSet by ID, verifying the caller has permission to operate
     * on the given PatientDataSet.
     */
    private PatientDataSet checkedGetPatientDataSet(String pdsID)
    {
        //!!! use caller...        return pdsLogic.getPatientDataSetByID(RestUtil.getInternalToken(), projID);

        XUUID xuiId = getPdsXuuid(pdsID);
        return pdsLogic.getPatientDataSetByID(xuiId);
    }

    /**
     * Returns true if the string doesn't contain non-whitespace characters.
     */
    private static boolean emptyString(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

}
