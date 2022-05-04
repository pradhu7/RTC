package com.apixio.useracct.dw.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.RoleSetLogic;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.Privilege;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.RoleSet.RoleType;
import com.apixio.useracct.entity.RoleSet;
import com.apixio.useracct.eprops.RoleProperties;
import com.apixio.useracct.eprops.RoleSetProperties;

/**
 * Internal service.
 */
@Path("/rolesets")
@Produces("application/json")
public class RoleSetsRS extends BaseRS {

    private PrivSysServices sysServices;

    /*
         create a new RoleSet:
         * POST:/rolesets                  {"type":"[organization, project]", "nameID":"...e.g. System...", "name":"...System...", "description":"...something..."}
                              returns      {"id", "xuuid", "type":"...", "nameID":"...", "name":"...", "description":"..."}

         create a new Role
         * POST:/rolesets/{nameID}         {"name": "...e.g. ROOT...", "description":"root is god", "privileges": [ ... ] }
                              returns      {"id", "xuuid", "name":"...", "description":"...", "privileges":[...]}

         * GET:/rolesets/{nameID}          -> {"type", "nameID", "name", "description",
                                                 "roles":[ {"id", "name":"..root,admin,etc...", "description", "...blah...",
                                                          "privileges": [ {"forMember":true/false, "operation":"blah", "acltarget":"blah"}, ... ] // end of list of privileges
                                                 }, ...        // more roles
                                               ]               // end of list of roles
                                             }

         // completely replace (this is the only option) privileges on a role:
         * PUT:/rolesets/{nameID}/{role}  payload: [ {"forMember":true/false, "operation":"blah", "acltarget":"blah"}, ... ]
                              returns     ok/not-ok
     */

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class CreateRoleSetParams {
        public String type;          // "organization" or "project"
        public String nameID;
        public String name;
        public String description;
    }

    public static class RolePrivilege {
        public boolean forMember;
        public String  operation;
        public String  aclTarget;

        public RolePrivilege()
        {
        }

        RolePrivilege(boolean forMember, String operation, String target)
        {
            this.forMember = forMember;
            this.operation = operation;
            this.aclTarget = target;
        }
    }
    public static class CreateRoleParams {
        public String              name;
        public String              description;
        public List<RolePrivilege> privileges;
    }
    public static class UpdatePrivilegesParam {
        public List<RolePrivilege> privileges;
    }


    public RoleSetsRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    /**
     *
     */
    @POST
    @Consumes("application/json")
    public Response createRoleset(@Context HttpServletRequest request, final CreateRoleSetParams createParams)
    {
        ApiLogger           logger   = super.createApiLogger(request, "/rolesets");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    RoleSet rs = sysServices.getRoleSetLogic().createRoleSet(convertType(createParams.type),
                                                                             createParams.nameID, createParams.name,
                                                                             createParams.description);

                    return ok(RoleSetProperties.toJson(rs));
                }});
    }

    @POST
    @Consumes("application/json")
    @Path("/{nameID}")
    public Response createRole(@Context HttpServletRequest request, @PathParam("nameID") final String nameID, final CreateRoleParams createParams)
    {
        ApiLogger           logger   = super.createApiLogger(request, "/rolesets/{nameID}");

        logger.addParameter("nameId", nameID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    RoleSetLogic rsl = sysServices.getRoleSetLogic();
                    RoleSet      rs  = rsl.getRoleSetByNameID(nameID);

                    if (rs == null)
                    {
                        return notFound("roleset" , nameID);
                    }
                    else
                    {
                        Role role = rsl.createRole(rs, createParams.name, createParams.description,
                                                   convertPrivilegesIn(createParams.privileges));
                        Map<String, Object> json;

                        json = RoleProperties.toJson(role);
                        json.put("privileges", convertPrivilegesOut(role.getPrivileges()));

                        return ok(json);
                    }
                }});
    }

    /**
     *
     */
    @GET
    public Response getAllRolesets(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/rolesets");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    RoleSetLogic              rsl    = sysServices.getRoleSetLogic();
                    List<Map<String, Object>> rsJson = new ArrayList<>();

                    for (RoleSet rs : rsl.getAllRoleSets())
                        rsJson.add(roleSetToJson(rs));

                    return ok(rsJson);
                }});
    }

    /**
     *
     */
    @GET
    @Path("/{nameID}")
    public Response getRoleset(
        @Context               HttpServletRequest request,
        @PathParam("nameID")   final String       nameID
        )
    {
        ApiLogger           logger   = super.createApiLogger(request, "/rolesets/{nameID}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    RoleSetLogic rsl = sysServices.getRoleSetLogic();
                    RoleSet      rs  = rsl.getRoleSetByNameID(nameID);

                    if (rs == null)
                        return notFound("roleset", nameID);
                    else
                        return ok(roleSetToJson(rs));
                }});
    }

    private Map<String, Object> roleSetToJson(RoleSet rs)
    {
        RoleSetLogic              rsl     = sysServices.getRoleSetLogic();
        Map<String, Object>       json    = RoleSetProperties.toJson(rs);
        List<Map<String, Object>> rsRoles = new ArrayList<>();

        for (Role role : rsl.getRolesInSet(rs))
        {
            Map<String, Object> roleJson = RoleProperties.toJson(role);

            roleJson.put("privileges", convertPrivilegesOut(role.getPrivileges()));

            rsRoles.add(roleJson);
        }

        json.put("roles", rsRoles);

        return json;
    }

    /**
     *aaa
     */
    @PUT
    @Consumes("application/json")
    @Path("/{nameID}/{role}")
    public Response updateRole(
        @Context    HttpServletRequest request,
        @PathParam("nameID") final String nameID,
        @PathParam("role")   final String roleName,
        final  List<RolePrivilege>        updateParams
        )
    {
        ApiLogger    logger  = super.createApiLogger(request, "/rolesets/{nameID}/{role}");
        final XUUID  caller  = RestUtil.getInternalToken().getUserID();

        logger.addParameter("nameId", nameID);
        logger.addParameter("roleName", roleName);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    String fullName = nameID + "/" + roleName;
                    Role   role     = sysServices.getRoleSetLogic().lookupRoleByName(fullName);

                    if (role == null)
                    {
                        return notFound("role", fullName);
                    }
                    else
                    {
                        List<Privilege> privs = convertPrivilegesIn(updateParams);

                        sysServices.getPrivRoleLogic().setRolePrivileges(caller, role, privs);

                        return ok(updateParams);
                    }
                }});
    }

    private List<Privilege> convertPrivilegesIn(List<RolePrivilege> privs)
    {
        List<Privilege> newPrivs = new ArrayList<>();

        for (RolePrivilege rp : privs)
            newPrivs.add(new Privilege(rp.forMember, rp.operation, rp.aclTarget));

        return newPrivs;
    }

    private List<RolePrivilege> convertPrivilegesOut(List<Privilege> privs)
    {
        List<RolePrivilege> newPrivs = new ArrayList<>();

        for (Privilege p : privs)
            newPrivs.add(new RolePrivilege(p.forMember(), p.getOperationName(), p.getObjectResolver()));

        return newPrivs;
    }

    private RoleType convertType(String typeName)
    {
        if (RoleSetProperties.RST_ORGANIZATION.equals(typeName))
            return RoleType.ORGANIZATION;
        else if (RoleSetProperties.RST_PROJECT.equals(typeName))
            return RoleType.PROJECT;
        else
            throw new IllegalArgumentException("Unknown role set type [" + typeName + "]; must be 'organization' or 'project'");
    }

}
