package com.apixio.useracct.dw.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.OrganizationLogic;
import com.apixio.useracct.eprops.UserProperties;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dao.OrgTypes;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.OrgType;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.eprops.OrganizationProperties;
import com.apixio.useracct.eprops.PdsProperties;

/**
 * RESTful endpoints to manage Organization resources.  An Organization entity
 * can have members (e.g., Users who are employees) and it can have Users who
 * are assigned Roles within the organization.  Users who are assigned roles
 * do NOT need to be members of that organization.
 *
 * Organizations have a type; this type is one of System, Vendor, and Customer.
 * Each type has its own set of roles.  Roles that are conceptually the same
 * across the different types (e.g., "ADMIN") are actually distinct as they
 * could have different privileges associated with them.
 *
 * Organizations of type "Customer" can also own one or more PatientDataSets.
 *
 * Finally, the system supports adding custom properties to organization entities.
 */
@Path("/uorgs")
@Produces("application/json")
public class OrganizationRS extends BaseRS {

    /**

       // organization entity management:
       POST:/uorgs                                             # create new org
       GET:/uorgs                                              # get all orgs
       GET:/uorgs/{orgid}                                      # get info on one org (externalID)
       PUT:/uorgs/{orgid}                                      # update info on one org

       // member-of management
       PUT:/uorgs/{orgID}/members/{userID}                     # add user to org as member-of
       DELETE:/uorgs/{orgID}/members/{userID}                  # remove user from org as member-of.   WARNING:  no ACL cleanup is done!  Use judiously
       GET:/uorgs/{orgID}/members                              # get all users that are member-of org

       // assign-user-to-org-with-role stuff:
       GET:/uorgs/{orgID}/roles                                # get all roles that user can be assigned to the given org
       PUT:/uorgs{orgID}/roles/{roleName}/{userID}             # assign user the given role in given org
       GET:/uorgs/{orgID}/roles/{roleName}                     # get all users with given role in given org
       DELETE:/uorgs{orgID}/roles/{roleName}/{userID}          # unassign user the given role in given org

       // PDS list management
       PUT:/uorgs/{orgID}/patientdatasets/{pdsID}              # associates the given PDS with the (customer) org
       GET:/uorgs/{orgID}/patientdatasets                      # gets list of PDSs associated with (customer) org
       DELETE:/uorgs/{orgID}/patientdatasets/{pdsID}           # unassociates PDS from org

       // property definition management:
       POST:/uorgs/propdefs                                    # create property def for organizations
       GET:/uorgs/propdefs                                     # get all property defs for organizations
       DELETE:/uorgs/propdefs/{name}                           # delete property def for organizations

       // property value management:
       PUT:/uorgs/{orgID}/properties/{name}                    # assign property value to one organization instance
       GET:/uorgs/properties                                   # get all prop values for all organizations (expensive)
       GET:/uorgs/properties/{name}                            # get one prop value for all organizations
       GET:/uorgs/{orgID}/properties                           # get all prop values for one organization
       DELETE:/uorgs/{orgID}/properties/{name}                 # delete property value from one organization instance

     */

    private PrivSysServices sysServices;

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class CreateOrgParams {
        public String name;
        public String description;
        public String type;
        public Map<String, String> properties;  // custom properties

        // orgID and externalID are the SAME; having both is solely
        // intended to support both old (those that submit "orgID") and
        // new ("externalID") until all old clients supply externalID.
        // During creation we prefer externalID over orgID (and no check
        // is done to make sure they're equal if both are supplied)
        public String   orgID;
        public String   externalID;
        public Boolean  needsTwoFactor;
        public Integer  maxFailedLogins;
    }

    public static class UpdateOrgParams {
        public String              name;
        public String              description;
        public Map<String, String> properties;  // custom properties
        public Boolean             isActive;

        // these ought to be available on create but that isn't necessary at this point
        public String              passwordPolicy;
        public String[]            ipWhitelist;
        public Integer             activityTimeoutOverride;
        public Boolean             needsTwoFactor;
        public Integer             maxFailedLogins;
    }

    public OrganizationRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Organization entity management                                 */
    /* ################################################################ */
    /* ################################################################ */

    /*
      POST:/uorgs                                             # create new org
      GET:/uorgs                                              # get all orgs
      GET:/uorgs/{orgid}                                      # get info on one org (externalID)
      PUT:/uorgs/{orgid}                                      # update info on one org
    */

    /**
     * Creates a new Organization entity.  The input HTTP entity JSON must be like:
     *
     *  { "name": "organizationname", "description": "thedescription",
     *    "type": "one of [System, Vendor, Customer]",
     *    "externalID":  "client-defined identifier" }
     *
     * A successful creation will return a JSON object with the submitted information
     * along with an XUUID identifier:
     *
     *  { "id": "UO_958e517d-6c4f-4747-b54f-9f0c285b8599",
     *    "name": "organizationname", "description": "thedescription",
     *    "type": "one of [System, Vendor, Customer]",
     *    "externalID":  "client-defined identifier" }
     */
    @POST
    @Consumes("application/json")
    public Response createOrg(@Context HttpServletRequest request, final CreateOrgParams params)
    {
        final ApiLogger           logger = super.createApiLogger(request, "/uorgs");

        logger.addParameter("name",             params.name);
        logger.addParameter("description",      params.description);
        logger.addParameter("type",             params.type);
        logger.addParameter("needsTwoFactor",   params.needsTwoFactor);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    String            ext  = (params.externalID != null) ? params.externalID : params.orgID;  // see comment in CreateOrgParams class...
                    OrganizationLogic ol   = sysServices.getOrganizationLogic();
                    Organization      org  = ol.createOrganization(params.type, params.name, params.description,
                            params.orgID, params.needsTwoFactor != null && params.needsTwoFactor);

                    if (org != null)
                    {
                        logger.addParameter("orgId", org.getExternalID());
                    }

                    OrgType           type = sysServices.getOrgTypes().findOrgTypeByID(org.getOrgType());

                    if (params.properties != null)
                    {
                        ol.updateOrgCustomProperties(org, params.properties);
                        logProperties(logger, params.properties);
                    }
                    if(params.maxFailedLogins != null)
                        org.setMaxFailedLogins(params.maxFailedLogins);

                    return ok(OrganizationProperties.toJson(org, type));
                }});
    }

    /**
     * Returns all properties of the given organization in the form of a JSON object.  It does
     * NOT return user lists.
     */
    @GET
    @Consumes("application/json")
    public Response getAllOrganizations(@Context HttpServletRequest request,
                                        @QueryParam("properties") final Boolean includeProps,
                                        @QueryParam("type")       final String  requestedType,
                                        @QueryParam("inactive")   final Boolean includeInactive)
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/uorgs");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    OrgTypes                  types  = sysServices.getOrgTypes();
                    OrgType                   rqType = (requestedType != null) ? types.findOrgTypeByName(requestedType) : null;
                    List<Map<String, Object>> orgs   = new ArrayList<>();
                    boolean                   props  = (includeProps != null) && includeProps;

                    for (Organization org : sysServices.getOrganizationLogic().getAllOrganizations((includeInactive != null) && includeInactive))
                    {
                        OrgType type = sysServices.getOrgTypes().findOrgTypeByID(org.getOrgType());

                        if ((rqType == null) || type.getID().equals(rqType.getID()))
                        {
                            Map<String, Object> json = OrganizationProperties.toJson(org, type);

                            if (props)
                                json.put("properties", sysServices.getOrganizationLogic().getOrganizationProperties(org));

                            orgs.add(json);
                        }
                    }

                    return ok(orgs);
                }});
    }

    /**
     * Returns all properties of the given organization in the form of a JSON object.  It does
     * NOT return user lists.
     */
    @GET
    @Path("/{orgID}")
    public Response getOrganization(@Context HttpServletRequest request, @PathParam("orgID") final String orgID,
                                    @QueryParam("properties") final Boolean includeProps
        )
    {
        final ApiLogger           logger   = super.createApiLogger(request, "/uorgs/{orgID}");

        logger.addParameter("orgId", orgID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Organization  org = null;

                    try
                    {
                        org = sysServices.getOrganizationLogic().getOrganizationByID(orgID);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    if (org != null)
                    {
                        OrgType             type = sysServices.getOrgTypes().findOrgTypeByID(org.getOrgType());
                        Map<String, Object> json = OrganizationProperties.toJson(org, type);

                        if ((includeProps != null) && includeProps)
                            json.put("properties", sysServices.getOrganizationLogic().getOrganizationProperties(org));

                        // special handling due to String[] not supported by E2Properties; key
                        // should match UpdateOrgParams.ipWhitelist...
                        json.put("ipWhitelist", org.getIpAddressWhitelist());

                        return ok(json);
                    }
                    else
                    {
                        return notFound("Organization", orgID);
                    }
                }});
    }

    /**
     * updateOrg updates ALL of the properties of an Org with the values given in the
     * parameters.  If the property doesn't exist in the Map<> the ...
     */
    @PUT
    @Consumes("application/json")
    @Path("/{orgID}")
    public Response updateOrg(
        @Context               HttpServletRequest request,
        @PathParam("orgID")    final String       orgID,
        final UpdateOrgParams                     updateParams
        )
    {
        final ApiLogger    logger = super.createApiLogger(request, "/uorgs/{orgID}");

        logger.addParameter("orgId", orgID);
        logger.addParameter("name", updateParams.name);
        logger.addParameter("description", updateParams.description);
        logger.addParameter("isActive", updateParams.isActive);

        logger.addParameter("passwordPolicy", updateParams.passwordPolicy);
        logger.addParameter("ipWhitelist", updateParams.ipWhitelist);
        logger.addParameter("activityTimeoutOverride", updateParams.activityTimeoutOverride);
        logger.addParameter("needsTwoFactor", updateParams.needsTwoFactor);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Organizations      os  = sysServices.getOrganizations();
                    OrganizationLogic  ol  = sysServices.getOrganizationLogic();
                    Organization       org = os.findOrganizationByExternalID(orgID);

                    // ugly but I screwed up the API def by requiring XUUID at first and I'm not sure what clients
                    // use it vs externalID.
                    if (org == null)
                        org = ol.getOrganizationByID(orgID);

                    if (org != null)
                    {
                        if (updateParams.name != null)
                            org.setName(updateParams.name);
                        if (updateParams.description != null)
                            org.setDescription(updateParams.description);
                        if (updateParams.isActive != null)
                            org.setIsActive(updateParams.isActive);
                        if (updateParams.passwordPolicy != null)
                            org.setPasswordPolicy(updateParams.passwordPolicy);
                        if (updateParams.ipWhitelist != null)
                            org.setIpAddressWhitelist(updateParams.ipWhitelist);
                        if (updateParams.activityTimeoutOverride != null)
                            org.setActivityTimeoutOverride(updateParams.activityTimeoutOverride);
                        if(updateParams.needsTwoFactor != null){
                            org.setNeedsTwoFactor(updateParams.needsTwoFactor);
                        }
                        if(updateParams.maxFailedLogins != null){
                            org.setMaxFailedLogins(updateParams.maxFailedLogins);
                        }

                        if (updateParams.properties != null)
                        {
                            ol.updateOrgCustomProperties(org, updateParams.properties);
                            logProperties(logger, updateParams.properties);
                        }

                        sysServices.getOrganizationLogic().updateOrganization(org);

                        return ok();
                    }
                    else
                    {
                        return notFound("Organization", orgID);
                    }
                }});
    }

    @DELETE
    @Consumes("application/json")
    @Path("/{orgID}")
    public Response deleteOrg(@Context HttpServletRequest request, @PathParam("orgID") final String orgID)
    {
        final ApiLogger           logger = super.createApiLogger(request, "/uorgs/{orgID}");

        logger.addParameter("orgId", orgID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    sysServices.getOrganizationLogic().deleteOrganization(XUUID.fromString(orgID, Organization.OBJTYPE));

                    return ok();
                }});
    }

    private void logProperties(ApiLogger logger, Map<String, String> properties)
    {
        for (Map.Entry<String, String> entry : properties.entrySet())
            logger.addParameter(entry.getKey(), entry.getValue());
    }

    /* ################################################################ */
    /* ################################################################ */
    /*    Member-of management                                          */
    /* ################################################################ */
    /* ################################################################ */

    /*
      PUT:/uorgs/{orgID}/members/{userID}                     # add user to org as member-of
      DELETE:/uorgs/{orgID}/members/{userID}                  # remove user from org as member-of.   WARNING:  no ACL cleanup is done!  Use judiously
      GET:/uorgs/{orgID}/members                              # get all users that are member-of org
     */

    @PUT
    @Path("/{orgID}/members/{userID}")
    public Response addMemberOf(
        @Context             HttpServletRequest request,
        @PathParam("orgID")  final String       orgID,
        @PathParam("userID") final String       userID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/members/{userID}");

        logger.addParameter("orgId",  orgID);
        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization org  = getOrganizationFromID(orgID);
                    User         user = getUserFromID(userID);

                    sysServices.getOrganizationLogic().addMemberToOrganization(org.getID(), user.getID());

                    return ok();
                }});
    }

    @DELETE
    @Path("/{orgID}/members/{userID}")
    public Response removeMemberOf(
        @Context             HttpServletRequest request,
        @PathParam("orgID")  final String       orgID,
        @PathParam("userID") final String       userID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/members/{userID}");

        logger.addParameter("orgId",  orgID);
        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization org  = getOrganizationFromID(orgID);
                    User         user = getUserFromID(userID);

                    sysServices.getOrganizationLogic().removeMemberFromOrganization(org.getID(), user.getID());

                    return ok();
                }});
    }

    @GET
    @Path("/{orgID}/members")
    public Response getMembersOf(
        @Context             HttpServletRequest request,
        @PathParam("orgID")  final String       orgID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/members");

        logger.addParameter("orgId", orgID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization              org   = getOrganizationFromID(orgID);
                    List<Map<String, String>> jsons = new ArrayList<>();

                    if (org == null)
                        return notFound("Organization", orgID);

                    for (User user : sysServices.getUsers().getUsers(sysServices.getOrganizationLogic().getUsersBelongingToOrg(org.getID())))
                    {
                        Map<String, String> json = new HashMap<>();

                        //!! what should be returned?
                        json.put("id",           user.getID().toString());
                        json.put("emailAddress", user.getEmailAddress().getEmailAddress());

                        jsons.add(json);
                    }

                    return ok(jsons);
                }});
    }

    /**
     *
     */
    private Organization getOrganizationFromID(String orgID)
    {
        Organization org = null;

        try
        {
            org = sysServices.getOrganizationLogic().getOrganizationByID(orgID);
        }
        catch (IllegalArgumentException iae)
        {
            badRequest(iae.getMessage());
        }

        return org;
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Role Assignment methods                                        */
    /* ################################################################ */
    /* ################################################################ */

    /*
      GET:   /uorgs/{orgID}/roles                             # get all roles that user can be assigned to the given org
      PUT:   /uorgs/{orgID}/roles/{roleName}/{userID}         # assign user the given role in given org
      DELETE:/uorgs/{orgID}/roles/{roleName}/{userID}         # unassign user the given role in given org
      GET:   /uorgs/{orgID}/roles/{roleName}                  # get all users with given role in given org
    */

    @GET
    @Path("/{orgID}/roles")
    public Response getRolesForOrganization(
        @Context             HttpServletRequest request,
        @PathParam("orgID")  final String       orgID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/roles");

        logger.addParameter("orgId", orgID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization org    = getOrganizationFromID(orgID);
                    List<String> rnames = new ArrayList<>();

                    for (Role role : sysServices.getOrganizationLogic().getRolesForOrg(org))
                        rnames.add(role.getName());  // kinda ambiguous; relies on the fact that an orgtype has a single roleset

                    return ok(rnames);
                }});
    }

    /**
     * Assign the given user the given role within the given org
     */
    @PUT
    @Path("/{orgID}/roles/{roleName}/{userID}")
    public Response assignUserToRole(
        @Context               HttpServletRequest request,
        @PathParam("orgID")    final String       orgID,
        @PathParam("roleName") final String       roleName,
        @PathParam("userID")   final String       userID          // either email addr or XUUID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/roles/{roleName}/{userID}");

        logger.addParameter("orgId",  orgID);
        logger.addParameter("role",   roleName);
        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization org  = getOrganizationFromID(orgID);
                    User         user = getUserFromID(userID);
                    Role         role = sysServices.getOrganizationLogic().getRoleFromOrgAndName(org, roleName);

                    if (org == null)
                        notFound("organization", orgID);
                    else if (user == null)
                        notFound("user", userID);
                    else if (role == null)
                        notFound("role", roleName);

                    sysServices.getRoleLogic().assignUserToRole(RestUtil.getInternalToken().getUserID(), user, role, org.getID());

                    return ok();
                }});
    }

    /**
     * Assign the given user the given role within the given org
     */
    @DELETE
    @Path("/{orgID}/roles/{roleName}/{userID}")
    public Response unassignUserToRole(
        @Context               HttpServletRequest request,
        @PathParam("orgID")    final String       orgID,
        @PathParam("roleName") final String       roleName,
        @PathParam("userID")   final String       userID          // either email addr or XUUID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/roles/{roleName}/{userID}");

        logger.addParameter("orgId",  orgID);
        logger.addParameter("role",   roleName);
        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization org  = getOrganizationFromID(orgID);
                    User         user = getUserFromID(userID);
                    Role         role = sysServices.getOrganizationLogic().getRoleFromOrgAndName(org, roleName);

                    sysServices.getRoleLogic().unassignUserFromRole(RestUtil.getInternalToken().getUserID(), user, role, org.getID());

                    return ok();
                }});
    }

    @GET
    @Path("/{orgID}/roles/{roleName}")
    public Response getUsersInRoles(
        @Context               HttpServletRequest request,
        @PathParam("orgID")    final String       orgID,
        @PathParam("roleName") final String       roleName
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/roles/{roleName}");

        logger.addParameter("orgId", orgID);
        logger.addParameter("role",  roleName);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Organization              org   = getOrganizationFromID(orgID);
                    Role                      role  = sysServices.getOrganizationLogic().getRoleFromOrgAndName(org, roleName);
                    List<Map<String, String>> jsons = new ArrayList<>();

                    if (org == null)
                        return notFound("Organization", orgID);

                    if (role == null)
                        return notFound("Role", roleName);

                    for (User user : sysServices.getUsers().getUsers(
                             sysServices.getRoleLogic().getUsersWithRole(RestUtil.getInternalToken().getUserID(), org.getID(), role.getID())))
                    {
                        Map<String, String> json = new HashMap<>();

                        UserProperties.autoProperties.copyFromEntity(user, json);

                        json.put("id", user.getID().toString());

                        jsons.add(json);
                    }

                    return ok(jsons);
                }});
    }

    /**
     * Accepts either a User XUUID or a users' email address and returns the actual User
     * for it.
     */
    private User getUserFromID(String emailOrID)
    {
        Users users = sysServices.getUsers();

       try
       {
           XUUID xid = XUUID.fromString(emailOrID, User.OBJTYPE);

           return users.findUserByID(xid);
       }
       catch (IllegalArgumentException x)
       {
           try
           {
               CanonicalEmail ce = new CanonicalEmail(emailOrID);

               return users.findUserByEmail(emailOrID);
           }
           catch (IllegalArgumentException y)
           {
               notFound("user", emailOrID);

               return null; // never hit
           }
       }
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   PDS association methods                                        */
    /* ################################################################ */
    /* ################################################################ */

    /**
       PUT:/uorgs/{orgID}/patientdatasets/{pdsID}              # associates the given PDS with the (customer) org
       GET:/uorgs/{orgID}/patientdatasets                      # gets list of PDSs associated with (customer) org
       DELETE:/uorgs/{orgID}/patientdatasets/{pdsID}           # unassociates PDS from org
     */

    /**
     *
     */
    @PUT
    @Path("/{orgID}/patientdatasets/{pdsID}")
    public Response assignPdsToOrg(
        @Context             HttpServletRequest request,
        @PathParam("orgID") final String        orgID,
        @PathParam("pdsID") final String        pdsID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/patientdatasets/{pdsID}");

        logger.addParameter("orgId",  orgID);
        logger.addParameter("pdsId",  pdsID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    // This will return false if already associated (even to this org)
                    sysServices.getOrganizationLogic().addPdsToCustomerOrg(XUUID.fromString(orgID, Organization.OBJTYPE),
                                                                           XUUID.fromString(pdsID, PatientDataSet.OBJTYPE));

                    return ok();
                }});
    }

    /**
     *
     */
    @DELETE
    @Path("/{orgID}/patientdatasets/{pdsID}")
    public Response unassignPdsToOrg(
        @Context             HttpServletRequest request,
        @PathParam("orgID") final String        orgID,
        @PathParam("pdsID") final String        pdsID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/patientdatasets/{pdsID}");

        logger.addParameter("orgId",  orgID);
        logger.addParameter("pdsId",  pdsID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    sysServices.getOrganizationLogic().removePdsFromCustomerOrg(XUUID.fromString(orgID, Organization.OBJTYPE),
                                                                                XUUID.fromString(pdsID, PatientDataSet.OBJTYPE));

                    return ok();
                }});
    }

    /**
     *
     */
    @GET
    @Path("/{orgID}/patientdatasets")
    public Response getOrgPdsList(
        @Context             HttpServletRequest request,
        @PathParam("orgID") final String        orgID
        )
    {
        ApiLogger logger = super.createApiLogger(request, "/uorgs/{orgID}/patientdatasets");

        logger.addParameter("orgId",  orgID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    List<Map<String, Object>> jsons = new ArrayList<>();

                    List<PatientDataSet> customerOrgPdsList = null;
                    try
                    {
                        customerOrgPdsList = sysServices.getOrganizationLogic().getCustomerOrgPdsList(XUUID.fromString(orgID, Organization.OBJTYPE));
                    }
                    catch (IllegalArgumentException iae)
                    {
                        badRequest(iae.getMessage());
                    }

                    for (PatientDataSet pds : customerOrgPdsList)
                        jsons.add(PdsProperties.toJson(pds));

                    return ok(jsons);
                }});
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   Custom Organization properties                                 */
    /* ################################################################ */
    /* ################################################################ */

    /**
     * Eight APIs are supported for managing custom Organization properties:
     *
     *  APIs that operate on the set of custom properties (meta-level):
     *  1) POST:/uorgs/propdefs                # creates a new custom Organization property
     *  2) GET:/uorgs/propdefs                 # returns the set of custom Organization properties
     *  3) DELETE:/uorgs/propdefs/{name}       # deletes a custom Organization property
     *
     *  APIs that operate on a particular User entity instance:
     *  4) PUT:/uorgs/{entityID}/properties/{name}       # sets a custom property value on a Organization instance
     *  5) DELETE:/uorgs/{entityID}/properties/{name}    # removes a custom property value from a Organization instance
     *  6) GET:/uorgs/{entityID}/properties              # gets all custom properties on a Organization instance
     *
     *  APIs that provide query operations on the entire set of Organizations with custom properties:
     *  7) GET:/uorgs/properties                # gets ALL properties of ALL Organizations with custom properties
     *  8) GET:/uorgs/properties/{name}         # gets all Organizations with the given property, along with the value for each Organization
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
     * The metadata string is not returned in GET:/uorgs/propdefs unless the "meta"
     * query parameter is set to true:
     *
     *  GET uorgss/propdefs?meta=true
     */
    @POST
    @Path("/propdefs")
    @Consumes("application/json")
    public Response createCustomPropertyDef(@Context HttpServletRequest request, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/propdefs");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    String name = jsonObj.get("name");
                    String type = jsonObj.get("type");

                    logger.addParameter("name", name);
                    logger.addParameter("type", type);

                    if (emptyString(name))
                        return badRequestEmptyParam("name");
                    else if (emptyString(type))
                        return badRequestEmptyParam("type");

                    sysServices.getOrganizationLogic().addPropertyDef(name, type);

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
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/propdefs");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    
                    return ok(sysServices.getOrganizationLogic().getPropertyDefinitions(Boolean.valueOf(meta)));
                }});
    }

    /**
     * Deletes the given property definition.  All property values across all organization
     * entities are also removed.
     */
    @DELETE
    @Path("/propdefs/{name}")
    public Response deleteCustomPropertyDef(@Context HttpServletRequest request, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/propdefs/{name}");

        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    sysServices.getOrganizationLogic().removePropertyDef(name);

                    return ok();
                }});
    }

    /**
     * Adds the given property to the given organization entity.  The input HTTP entity
     * must be of the form:
     *
     *  {"value": "theactualvalue"}
     *
     * The name of the property must have already been added via a request to
     * POST:/uorgs/propdefs and the value must be able to be interpreted
     * as valid within the given type.  For example, if the type of "count" is
     * "INTEGER", then the value specified in the HTTP entity body must be able to
     * be parsed as a Java int.
     */
    @PUT
    @Path("/{orgID}/properties/{name}")
    @Consumes("application/json")
    public Response setCustomProperty(@Context HttpServletRequest request,
                                      @PathParam("orgID") final String entityID,
                                      @PathParam("name") final String name, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/{orgID}/properties/{name}");

        logger.addParameter("orgId", entityID);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Organization   userOrg = checkedGetOrganization(entityID);
                    String    value   = jsonObj.get("value");

                    logger.addParameter("value", value);

                    if (emptyString(value))
                        return badRequestEmptyParam("value");
                    else if (userOrg == null)
                        return notFound("userOrg", entityID);

                    sysServices.getOrganizationLogic().setOrganizationProperty(userOrg, name, value);

                    return ok();
                }});
    }

    /**
     * Removes the given property from the given organization entity.
     *
     * The name of the property must have already been added via a request to
     * POST:/uorgs/propdefs.
     */
    @DELETE
    @Path("/{orgID}/properties/{name}")
    public Response removeCustomProperty(@Context HttpServletRequest request,
                                         @PathParam("orgID") final String entityID, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/{orgID}/properties/{name}");

        logger.addParameter("orgId", entityID);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Organization  userOrg  = checkedGetOrganization(entityID);

                    if (userOrg == null)
                        return notFound("userOrg", entityID);

                    sysServices.getOrganizationLogic().removeOrganizationProperty(userOrg, name);

                    return ok();
                }});
    }

    /**
     * Returns the full set of properties (name and value) that have been added to the
     * given organization.  The returned JSON structure will look like:
     *
     *  { "name1": "value1", "name2": "value2", ...}
     *
     * where "name1" is the actual name of the property, etc.  For example:
     *
     *  { "count": 35 }
     */
    @GET
    @Path("/{orgID}/properties")
    public Response getAllOrganizationProperties(@Context HttpServletRequest request, @PathParam("orgID") final String entityID)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/{orgID}/properties");

        logger.addParameter("orgId", entityID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    Organization  userOrg  = checkedGetOrganization(entityID);

                    if (userOrg == null)
                        return notFound("userOrg", entityID);

                    return ok(sysServices.getOrganizationLogic().getOrganizationProperties(userOrg));
                }});
    }

    
    /**
     * Gets all properties of all organization entities.  The JSON return structure is
     * as follows:
     *
     *  { { "object1ID" : { "prop1": "value1", "prop2": "value2", ...} },
     *    { "object2ID" : { "prop1": "value1", "prop2": "value2", ...} },
     *    ... }
     */
    @GET
    @Path("/properties")
    public Response getAllOrganizationsProperties(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/properties");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(sysServices.getOrganizationLogic().getAllOrganizationProperties());
                }});
    }

    /**
     * Gets a single property across all organization entities.  The JSON return structure is
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
    public Response getAllOrganizationsProperty(@Context HttpServletRequest request, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/uorgs/properties/{name}");

        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    if (emptyString(name))
                        return badRequestEmptyParam("name");

                    return ok(sysServices.getOrganizationLogic().getOrganizationsProperty(name));
                }});
    }

    /**
     * Looks up and returns the Organization by ID, verifying the caller has permission to operate
     * on the given Organization.
     * Also make sure that Organization ID is correct
     */
    private Organization checkedGetOrganization(String userOrgID)
    {
        Organization organization = null;
        try
        {
            organization = sysServices.getOrganizationLogic().getOrganizationByID(userOrgID);
        }
        catch (IllegalArgumentException iae)
        {
            badRequest(iae.getMessage());
        }

        return organization;
    }

    /**
     * Returns true if the string doesn't contain non-whitespace characters.
     */
    private static boolean emptyString(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }
    
}
