package com.apixio.useracct.dw.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.apixio.XUUID;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.AuthState;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PrivUserLogic;
import com.apixio.useracct.buslog.RoleAssignment;
import com.apixio.useracct.buslog.UserLogic;
import com.apixio.useracct.dao.OrgTypes;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.dw.util.XUUIDUtil;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.OldRole;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.PasswordPolicy;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.eprops.OrganizationProperties;
import com.apixio.useracct.eprops.PasswordPolicyProperties;
import com.apixio.useracct.eprops.UserProperties.ModifyUserParams;

/**
 * Internal User entity RESTful Service.
 */
@Path("/users")
@Produces("application/json")
public class UserRS extends BaseRS {

    /**

       GET:/users?orgID=xyz
       GET:/users/{userID}
       GET:/users/me
       GET:/users/me/{detail}
       PUT:/users/{userID}/activation        # activate (set status to ACTIVE)
       GET:/users/{userID}/org               # return orgIDs of user (should always be just 1 org)
       GET:/users/{userID}/roles             # return list of role assignments
       POST:/users/{userID}/pass_email       # sends reset password email
       GET:/users/{userID}/detail
       POST:/users                           # creates a new user
       DELETE:/users/{userID}                # testing only
       POST:/users/forgot                    # send forgot password email
       GET:/users/me/policy
       PUT:/users/me/{detail}
       PUT:/users/{userID}/{detail}
       PUT:/users/{userID}
       PUT:/users/me
       GET:/users/token                      # returns internal token for currently logged in user; test only?
       POST:/users/code                      # resend a opt code for 2FA for a user
       ... custom property endpoints ...

     */

    final static String PROP_ID               = "userID";
    final static String PROP_ACCOUNTSTATE     = "accountState";
    final static String PROP_USERROLES        = "roles";
    final static String PROP_ALLOWEDROLES     = "allowedRoles";
    final static String PROP_EMAILADDR        = "emailAddress";
    final static String PROP_TIMEOUTOVERRIDE  = "timeoutOverride";
    public final static String PROP_NEEDSTWOFACTOR   = "needsTwoFactor";
    private final Cookie cookieKiller;

    private PrivSysServices sysServices;

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class CreateUserParams {
        public String  email;
        public boolean resendOnly;
        public boolean skipEmail;
        public String  organizationID;
        public String  _password;              // leading "_" as this field is a bit of a hack
        public String cellPhone;              // field required for two factor
        public Boolean needsTwoFactor;
    }
    public static class ActivateUserParams {
        public boolean state;
    }
    public static class PasswordEmailParams {
        public String templateName;
    }
    public static class ForgotPasswordParams {
        public String  email;
    }
    public static class UnlockAccountParams {
        public String email;
        public String caller;
    }
    public static class SetUserPrivileged {
        public String password;
    }

    public UserRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
        String authCookieName = configuration.getAuthConfig().getAuthCookieName();
        if(authCookieName == null)
            throw new IllegalStateException("Missing required configuration for authConfig.authCookieName");

        cookieKiller = CookieUtil.externalCookie(authCookieName, "", 0);
    }

    /**
     * Returns all properties of the given user in the form of a JSON object.
     */
    @GET
    @Path("/{userID}")
    public Response getUser(@Context final HttpServletRequest request, @PathParam("userID") final String userID)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/{userID}");

        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
            public Response operation() throws IOException
                {
                    Map<String, Object> json = new HashMap<>();
                    User                user = findUserByEmailOrID(userID, false);

                    if (user != null)
                    {
                        UserUtil.copyUserProperties(user, json, null);

                        // special properties:
                        json.put(PROP_ACCOUNTSTATE, user.getState().toString());
                        UserUtil.addRoles(json, user.getRoleNames());
                        UserUtil.addAllowedRoles(json, user.getAllowedRoles());

                        return ok(json);
                    }
                    else
                    {
                        return notFound("userID", userID);
                    }
                }
            });
    }

    //Scott:  supposedly the generic /{userid} wasn't picking up 'me' (from what Michael said)...
    @GET
    @Path("/me")
    public Response getMe(@Context HttpServletRequest request) 
    {
        return getUser(request, "me");
    }

    @GET
    @Path("/me/{detail}")
    public Response getMeDetail(@Context HttpServletRequest request, @PathParam("detail") String detail)
    {
        return getUserDetail(request, "me", detail);
    }

    /**
     * set activation state of a user
     * @param payload json object with key state set to true (active) or false (disabled)
     * @param request ditto
     * @param userID ditto
     * @return 200 ok with no body if succeeds
     */
    @PUT
    @Path("/{userID}/activation")
    @Consumes("application/json")
    public Response activateUser(final ActivateUserParams payload,
                                 @Context HttpServletRequest request,
                                 @PathParam("userID") final String userID
        )
    {
        ApiLogger logger = super.createApiLogger(request,"/users/{userID}/activation");

        logger.addParameter("Active", payload.state);
        logger.addParameter("userId", userID);
        
        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
                {
                    User user = findUserByEmailOrID(userID, true);

                    user.setState(payload.state ? AccountState.ACTIVE : AccountState.DISABLED);
                    sysServices.getPrivUsers().update(user);

                    return ok();
                }
            });
    }

    /**
     * Returns the org to which the user is assigned
     * @param request ditto
     * @param userID ditto
     * @return array of json objects for orgs to which this user is assigned, or an empty array
     */
    @GET
    @Path("/{userID}/org")
    public Response getUserOrg(@Context HttpServletRequest request, @PathParam("userID") final String userID)
    {
        ApiLogger logger = super.createApiLogger(request,"/users/{userID}/org");

        logger.addParameter("userId",userID);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
                {
                    User user = findUserByEmailOrID(userID, false);

                    if (user != null)
                    {
                        OrgTypes                  types  = sysServices.getOrgTypes();
                        List<Map<String, Object>> result = new ArrayList<>();

                        for (Organization org : sysServices.getOrganizationLogic().getUsersOrganizations(user.getID()))
                            result.add(OrganizationProperties.toJson(org, types.findOrgTypeByID(org.getOrgType())));

                        return ok(result);
                    }
                    else
                    {
                        return notFound("User", userID);
                    }
                }
            });
    }

    /**
     * Returns the roles to which the given user has been assigned.
     */
    @GET
    @Path("/{userID}/roles")
    public Response getUserRoles(@Context HttpServletRequest request, @PathParam("userID") final String userID)
    {
        ApiLogger logger = super.createApiLogger(request,"/users/{userID}/roles");

        logger.addParameter("userId",userID);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
                {
                    User user = findUserByEmailOrID(userID, false);

                    if (user != null)
                    {
                        List<Map<String, Object>> result = new ArrayList<>();

                        for (RoleAssignment ra : sysServices.getRoleLogic().getRolesAssignedToUser(user))
                        {
                            Map<String, Object> json = new HashMap<>();

                            json.put("roleID",   ra.role.getID());
                            json.put("roleName", ra.role.getName());
                            json.put("target",   ra.targetID);

                            result.add(json);
                        }

                        return ok(result);
                    }
                    else
                    {
                        //!! HACK:  CORE-1535 wants a 403 return but we can't distinguish (right now)
                        // between "no such user" and "no permission".
                        return forbidden();
                        //                        return notFound("User", userID);
                    }
                }
            });
    }

    /**
     * Sends a reset password email
     * @param payload optional json structure with a key called templateName containing the name of the template to create email message
     * @param request ditto
     * @param userID ditto
     * @return 200 ok, no message body.
     */
    @POST
    @Path("/{userID}/pass_email")
    @Consumes("application/json")
    public Response sendResetPasswordEmail(final PasswordEmailParams payload,
                                           @Context HttpServletRequest request, @PathParam("userID") final String userID)
    {
        ApiLogger    logger       = super.createApiLogger(request, "/users/{userID}/pass_email");
        final String templateName = (payload.templateName == null) ? "forgotpassword" : payload.templateName;

        logger.addParameter("userId", userID);
        logger.addParameter("templateName", templateName);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
                {
                    User user = findUserByEmailOrID(userID, false);

                    if (user != null)
                        sysServices.getPrivUserLogic().sendResetPassword(user.getEmailAddress().getEmailAddress(),templateName);

                    return ok();
                }
            });
    }

    /**
     * Returns the given property of the user in a JSON object.
     */
    @GET
    @Path("/{userID}/{detail}")
    public Response getUserDetail(@Context HttpServletRequest request, @PathParam("userID") final String userID,
                                  @PathParam("detail") final String detail)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/{userID}/{detail}");

        logger.addParameter("userId", userID);
        logger.addParameter("detail", detail);

        return super.restWrap(new RestWrap(logger) {
            public Response operation() throws IOException
                {
                    User                user = findUserByEmailOrID(userID, false);
                    Map<String, Object> json = new HashMap<>();

                    if (user != null)
                    {
                        UserUtil.copyUserProperties(user, json, detail);

                        // special properties:
                        if (detail.equals(PROP_ACCOUNTSTATE))
                            json.put(PROP_ACCOUNTSTATE, user.getState().toString());

                        return ok(json);
                    }
                    else
                    {
                        return notFound("userID", userID);
                    }
                }
            });
    }

    /**
     * create a new account with the given email address.  A side effect of
     * successful creation is an email that is sent to the email address.
     * This email has an activation link that the user must click on in order
     * to verify the email address.  No access is granted to the user until
     * that link is clicked on.
     *
     * This is also the method used to resend a verification link for a new
     * account that hasn't yet had its email address verified.  (There is
     * no other good place for this from a RESTful perspective...).  This
     * "resend verification email" is triggered by a URL parameter of
     * "resendOnly=true".
     *
     * API Details:
     *
     *  * must be called with ADMIN role (=> token required)
     */
    @POST
    @Consumes("application/json")
    public Response createUser(
        @Context                  HttpServletRequest request,
        @QueryParam("resendOnly") String             resendOnly,
        final CreateUserParams                       createParams
        )
    {
        ApiLogger logger   = super.createApiLogger(request, "/users");

        createParams.resendOnly |= Boolean.valueOf(resendOnly);

        if (createParams.email != null)
            logger.addParameter("email", createParams.email);
        if (createParams.organizationID != null)
            logger.addParameter("organizationID", createParams.organizationID);

        logger.addParameter("resendOnly", createParams.resendOnly);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    Map<String, String> json  = new HashMap<>();
                    PrivUserLogic       pul   = sysServices.getPrivUserLogic();
                    User                user;

                    if (createParams.resendOnly)
                    {
                        user = pul.resendVerification(createParams.email);
                    }
                    else
                    {
                        boolean setPassword = !emptyString(createParams._password);

                        if (setPassword)
                            createParams.skipEmail = true;

                        user = pul.createUser(createParams.email, XUUID.fromString(createParams.organizationID, Organization.OBJTYPE),
                                              sysServices.getOldRoleLogic().getRole(OldRole.USER),
                                              !createParams.skipEmail, createParams.cellPhone, createParams.needsTwoFactor);

                        if (setPassword)
                        {
                            pul.setInitialPassword(RestUtil.getInternalToken(), user, createParams._password);
                            logger.addParameter("initialPasswordSpecified", "true");
                        }
                    }

                    json.put("id", user.getID().toString());

                    return ok(json);
                }
            });
    }

    /**
     *
     */
    @GET
    @Consumes("application/json")
    public Response getAllUsers(
        @Context                  HttpServletRequest request, @QueryParam("orgID") final String orgID)
    {
        ApiLogger logger   = super.createApiLogger(request, "/users");

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    List<Map<String, Object>> jsons = new ArrayList<>();
                    List<XUUID>               orgs  = new ArrayList<>();

                    if (orgID != null)
                        orgs.add(XUUID.fromString(orgID, Organization.OBJTYPE));

                    for (User user : sysServices.getUserLogic().getUsersByManageUserPermission(RestUtil.getInternalToken().getUserID(), orgs,
                                                                                               null, AccountState.CLOSED))
                    {
                        Map<String, Object> props = new HashMap<>();

                        UserUtil.copyUserProperties(user, props, null);

                        jsons.add(props);
                    }

                    return ok(jsons);
                }
            });
    }

    @PUT
    @Path("/priv/{userID}")
    @Consumes("application/json")
    public Response setUserPrivileged(@Context HttpServletRequest request,
                                      @PathParam("userID") final String userIDstr, final SetUserPrivileged privInfo)
    {
        ApiLogger logger   = super.createApiLogger(request, "/users/priv/{userID}");

        logger.addParameter("userId", userIDstr);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    PrivUsers users  = sysServices.getPrivUsers();
                    XUUID     userID = XUUID.fromString(userIDstr, User.OBJTYPE);
                    User      user   = users.findUserByID(userID);

                    if (user == null)
                        return notFound("User", userIDstr);

                    if (privInfo.password != null)
                        sysServices.getPrivUserLogic().forcePasswordSet(userID, privInfo.password);

                    return ok();
                }
            });
    }

    @DELETE
    @Path("/{userID}")
    public Response deleteUser(
        @Context                   HttpServletRequest request,
        @PathParam("userID") final String             userID
        )
    {
        ApiLogger logger   = super.createApiLogger(request, "/users/{userID}");

        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    PrivUserLogic  pul   = sysServices.getPrivUserLogic();

                    pul.deleteUser(XUUIDUtil.getXuuid(userID, User.OBJTYPE));

                    return ok();
                }
            });
    }

    /**
     * This is for priv users only
     * @param detail it supports only cellphone
     * @return
     */
    @DELETE
    @Path("/priv/{userID}/{detail}")
    public Response deleteUserDetail(
            @Context                   HttpServletRequest request,
            @PathParam("userID") final String             userID,
            @PathParam("detail") final String             detail
    )
    {
        ApiLogger logger   = super.createApiLogger(request, "/users/{userID}/{detail}");

        logger.addParameter("userId", userID);
        logger.addParameter("detail", detail);

        return super.restWrap(new RestWrap(logger) {
            public Response operation() throws IOException
            {
                PrivUserLogic  pul   = sysServices.getPrivUserLogic();

                if ("cellPhone".equals(detail))
                {
                    boolean b = pul.deleteCellPhone(XUUIDUtil.getXuuid(userID, User.OBJTYPE));
                    if (!b)
                    {
                        badRequest("delete failed: user does not exist");
                    }
                }

                return ok();
            }
        });
    }

    // ################################################################

    /**
     *
     */
    @POST
    @Path("/forgot")
    public Response forgotPassword(
        @Context              HttpServletRequest request,
        @FormParam("email")   String             emailAddress         // required
        )
    {
        ForgotPasswordParams params = new ForgotPasswordParams();

        params.email = emailAddress;

        return forgotPasswordCommon(request, params);
    }

    @POST
    @Path("/forgot")
    @Consumes("application/json")
    public Response forgotPasswordJson(
        @Context    HttpServletRequest request,
        ForgotPasswordParams           params
        )
    {
        return forgotPasswordCommon(request, params);
    }

    private Response forgotPasswordCommon(
        HttpServletRequest         request,
        final ForgotPasswordParams params
        ) {
        ApiLogger logger = super.createApiLogger(request, "/users/forgot");

        if (params.email != null)
            logger.addParameter("email", params.email);

        return super.restWrap(new RestWrap(logger) {
            public Response operation() {
                if (params.email == null)
                    return badRequestNullParam("email");

                PrivUserLogic pul = sysServices.getPrivUserLogic();

                // To prevent the "user enumeration" attack
                // we don't wont to return an error when the provided email doesn't exist in the system
                // catching the user logic exceptions here & log without returning to front end
                try {
                    pul.sendResetPassword(params.email);
                } catch (UserLogic.UserException ex) {
                    logger.addException(ex);
                }

                return ok();
            }
        });
    }

    @POST
    @Path("/unlockaccount")
    public Response unlockAccount(@Context HttpServletRequest request,
                                  @FormParam("email") final String email,
                                  @FormParam("caller") final String caller) {
        UnlockAccountParams params = new UnlockAccountParams();
        params.email = email;
        params.caller = caller;
        return unlockAccountCommon(request, params);
    }

    @POST
    @Path("/unlockaccount")
    @Consumes("application/json")
    public Response unlockAccount(@Context HttpServletRequest request,
                                  final UnlockAccountParams params) {
        return unlockAccountCommon(request, params);
    }

    private Response unlockAccountCommon(HttpServletRequest request, final UnlockAccountParams params) {
        ApiLogger    logger       = super.createApiLogger(request, "/users/{userID}/unlock_email");
        final String templateName = "unlockaccount";

        logger.addParameter("email", params.email);
        logger.addParameter("caller", params.caller);
        logger.addParameter("templateName", templateName);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception
            {
                if (params.email == null)
                    return badRequestNullParam("email");

                sysServices.getPrivUserLogic().sendUnlockAccountEmail(params.email, params.caller);
                return ok();
            }
        });
    }


    // ################################################################

    /**
     * Returns the user's password policy details.  An empty JSON object is returned
     * if there is no password policy for the user.
     */
    @GET
    @Path("/me/passpolicy")
    public Response getUsersPasswordPolicy(@Context HttpServletRequest request)
    {
        return getUsersPasswordPolicyAux(request, UserLogic.SELF_ID);
    }

    @GET
    @Path("/{userID}/passpolicy")
    public Response getUsersPasswordPolicy(@Context HttpServletRequest request, @PathParam("userID") final String userID)
    {
        return getUsersPasswordPolicyAux(request, userID);
    }

    private Response getUsersPasswordPolicyAux(@Context HttpServletRequest request, final String userID)
    {
        ApiLogger     logger   = super.createApiLogger(request, "/users/{userID}/passpolicy}");
        final Token   caller   = RestUtil.getInternalToken();

        logger.addParameter("userId", userID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    User   user = sysServices.getUserLogic().getUser(caller, userID, false);

                    if (user != null)
                    {
                        PasswordPolicy  policy = sysServices.getUserLogic().getUserPasswordPolicy(user);

                        if (policy != null)
                            return ok(PasswordPolicyProperties.toJson(policy));
                        else
                            return ok(new HashMap());
                    }
                    else
                    {
                        return badRequest();
                    }
                }
            });
    }

    // ################################################################

    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Path("/me/{detail}")
    public Response updateMeDetail(
        @Context               HttpServletRequest request,
        @Context               HttpServletResponse response,
        @PathParam("detail")   String             detail,
        MultivaluedMap<String, String>            formParams
        ) throws IOException
    {
        return updateUserDetail(request, response, "me", detail, formParams);
    }

    /**
     *
     */
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Path("/{userid}/{detail}")
    public Response updateUserDetail(
        @Context               HttpServletRequest request,
        @Context               HttpServletResponse response,
        @PathParam("userid")   String             userID,             // could be the special ID of "me"
        @PathParam("detail")   String             detail,
        MultivaluedMap<String, String>            formParams
        ) throws IOException
    {
        Map<String, String> params = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : formParams.entrySet())
            params.put(entry.getKey(), entry.getValue().get(0));

        return updateUserDetailCommon(request, response, userID, detail, params);
    }

    @PUT
    @Consumes("application/json")
    @Path("/me/{detail}")
    public Response updateMeDetailJson(
        @Context               HttpServletRequest request,
        @Context               HttpServletResponse response,
        @PathParam("detail")   String             detail,
        Map<String, String>                       updateParams
        ) throws IOException
    {
        return updateUserDetailJson(request, response, "me", detail, updateParams);
    }

    @PUT
    @Consumes("application/json")
    @Path("/{userid}/{detail}")
    public Response updateUserDetailJson(
        @Context               HttpServletRequest request,
        @Context               HttpServletResponse response,
        @PathParam("userid")   String             userID,             // could be the special ID of "me"
        @PathParam("detail")   String             detail,
        Map<String, String>                       updateParams
        ) throws IOException
    {
        return updateUserDetailCommon(request, response, userID, detail, updateParams);
    }

    private Response updateUserDetailCommon(
        HttpServletRequest   request,
        HttpServletResponse  response,
        String               userID,             // could be the special ID of "me"
        String               detail,             // value MUST be (currently) one of the params keys
        Map<String, String>  updateParams
        ) throws IOException
    {
        Token   token   = RestUtil.getInternalToken();

        if (token == null)
            throw new IllegalStateException("Attempt to call UserAcct API that requires authentication but no token is on the thread!");

        Map<String, String> params = new HashMap<>();

        // semantics are to update ONLY the thing requested without modifying anything else.
        // this is compared to PUT:/users/me, which will replace all with what was provided

        if (detail.equals("password"))
        {
            String val;

            if ((val = updateParams.get("existing")) != null)
                params.put("existing", val);

            if ((val = updateParams.get("password")) != null)
                params.put("password", val);

            if ((val = updateParams.get("nonce")) != null)
                params.put("nonce", val);
            
            if ((val = updateParams.get("test")) != null)
                params.put("test", val);
        }
        else
        {
            String prop = updateParams.get(detail);

            if (prop != null)
                params.put(detail, prop);
        }

        return updateUserJson(request, response, userID, params);
    }

    // ################################################################
    // ################################################################

    /**
     * updateUser updates ALL of the properties of a User with the values given in the
     * parameters.  If the property doesn't exist in the Map<> the ...
     */
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Path("/{userID}")
    public Response updateUser(
        @Context               HttpServletRequest request,
        @Context               HttpServletResponse response,
        @PathParam("userID")   String             userID,
        MultivaluedMap<String, String>            formParams
        ) throws IOException
    {
        Map<String, String> params = new HashMap<>();

        // copy over non-empty ones, otherwise how would we allow blanking of the fields?
        for (Map.Entry<String, List<String>> entry : formParams.entrySet())
        {
            String val = entry.getValue().get(0);

            if (val.length() > 0)
                params.put(entry.getKey(), val);
        }

        return updateUserJson(request, response, userID, params);
    }

    @PUT
    @Consumes("application/json")
    @Path("/me")
    public Response updateMeJson(
        @Context             HttpServletRequest request,
        @Context             HttpServletResponse response,
        Map<String, String>  updateParams               // ALL in here will overwrite, so don't put "" unless desired!
        ) throws IOException
    {
        return updateUserJson(request, response, "me", updateParams);
    }

    @PUT
    @Consumes("application/json")
    @Path("/{userID}")
    public Response updateUserJson(
        @Context             HttpServletRequest request,
        @Context             HttpServletResponse response,
        @PathParam("userID") String             userID,             // could be the special ID of "me" or userID or emailID
        Map<String, String>                     updateParams        // ALL in here will overwrite, so don't put "" unless desired!
        ) throws IOException
    {
        Token             caller = RestUtil.getInternalToken();
        User user = null;

        if("me".equals(userID))
        {
            user   = sysServices.getUserLogic().getUser(caller, userID, true);
        }
        else
        {   // get the user by email for modification
            user = findUserByEmailOrID(userID, true);
        }

        ModifyUserParams  modify = new ModifyUserParams();
        String            val;

        // this is a bit inefficient if user==null.  we do this just so there's a single point of handling
        // exceptions and creating the final JSON return...

        if (user != null)
        {
            // first load the current user data and then overwrite with supplied data
            modify.copyFrom(user);
            modify.copyFrom(updateParams);

            // accountState is special case
            if ((val = updateParams.get(PROP_ACCOUNTSTATE)) != null)
            {
                try
                {
                    modify.accountState = AccountState.valueOf(val);
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                    // tried to update to something that wasn't a known enum name
                }
            }

            // password is special case:  setting it requires current and new
            if ((val = updateParams.get("password")) != null)
            {
                modify.newPassword     = val;
                modify.currentPassword = updateParams.get("existing");
                modify.nonce           = updateParams.get("nonce");
                modify.test            = updateParams.get("test");
            }

            // role is special case
            if (((val = updateParams.get(PROP_USERROLES)) != null) && ((val = val.trim()).length() > 0))
                modify.roles = Arrays.asList(val.split(","));

            // allowedrole is special case
            if (((val = updateParams.get(PROP_ALLOWEDROLES)) != null) && ((val = val.trim()).length() > 0))
                modify.allowedRoles = Arrays.asList(val.split(","));

            // timeoutOverride is special case
            modify.timeoutOverride = user.getTimeoutOverride();
            if ((val = updateParams.get(PROP_TIMEOUTOVERRIDE)) != null)
            {
                if ((val = val.trim()).length() > 0)
                    modify.timeoutOverride = Integer.valueOf(val);
                else
                    modify.timeoutOverride = null;
            }

            // user needs two factor is a special case
            modify.needsTwoFactor = user.getNeedsTwoFactor();
            if ((val = updateParams.get(PROP_NEEDSTWOFACTOR)) != null)
            {
                val = val.trim();
                // to be able to set a unset the needs two factor field
                if("".equals(val))
                {
                    modify.needsTwoFactor = null;
                }
                else
                {
                    modify.needsTwoFactor = Boolean.valueOf(val);
                }
            }

        }

        return updateUserCommon(request, response, caller, userID, user, modify);
    }

    /**
     * Get token details, including the user ID
     * @return token
     */
    @GET
    @Produces("application/json")
    @Path("/token")
    public Response getToken(@Context HttpServletRequest request)
    {
        ApiLogger logger = super.createApiLogger(request, "/users/token");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(RestUtil.getInternalToken());
                }
            });
    }

    /**
     * Resend the OTP details, getting the user from the token
     */
    @POST
    @Produces("application/json")
    @Path("/code")
    public Response resendOTP(@Context HttpServletRequest request)
    {
        ApiLogger logger = super.createApiLogger(request, "/users/code");

        return super.restWrap(new RestWrap(logger) {
            public Response operation() throws IOException {
                // this can only be internal token
                final Token itoken = RestUtil.getInternalToken();

                if (itoken.getAuthState() != AuthState.PASSWORD_AUTHENTICATED)
                {
                    badRequest("user not password authenticated");
                }

                User user = sysServices.getUsers().findUserByID(itoken.getUserID());
                Organization org = sysServices.getOrganizationLogic().getUsersOrganization(user.getID());

                if ((user == null) || (org == null))
                {
                    badRequest("Invalid User or Organization");
                }

                sysServices.getPrivUserLogic().sendOTP(user, org, true);

                return ok();
            }
        });
    }

    private Response updateUserCommon(
        @Context HttpServletRequest       request,
        @Context HttpServletResponse      response,
        final Token              caller,
        final String             userID,
        final User               user,
        final ModifyUserParams   params
        )
    {
        ApiLogger           logger   = super.createApiLogger(request, "/users/{userID}");

        logger.addParameter("userId", userID);
        logUserParmeters(logger, params);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    if (user != null)
                    {
                        PrivUserLogic.ModStatus modStatus = sysServices.getPrivUserLogic().modifyUser(caller, userID, user, params);

                        // when a password is changed we kill the cookie
                        // and make the user login again
                        if(modStatus.passwordChanged)
                        {
                             response.addCookie(cookieKiller);
                        }
                        return ok();
                    }
                    else
                    {
                        return notFound("userID", userID);
                    }
                }
            });
    }

    /**
     * Methos to log the updates to the user,
     * not logging password here
     * @param logger
     * @param params
     */
    private void logUserParmeters(ApiLogger logger, ModifyUserParams params)
    {
        logger.addParameter("firstName", params.firstName);
        logger.addParameter("lastName", params.lastName);
        logger.addParameter("dateOfBirth", params.dateOfBirth);
        logger.addParameter("officePhone", params.officePhone);
        logger.addParameter("cellPhone", params.cellPhone);
        logger.addParameter("emailAddr", params.emailAddr);

        logger.addParameter("test", params.test);
        logger.addParameter("createdAt", params.createdAt);
        logger.addParameter("state", params.state);
        logger.addParameter("accountState", params.accountState);
        logger.addParameter("roles", params.roles);
        logger.addParameter("allowedRoles", params.allowedRoles);
        logger.addParameter("timeoutOverride", params.timeoutOverride);
        logger.addParameter("needsTwoFactor", params.needsTwoFactor);
    }

    /* ################################################################ */
    /* ################################################################ */
    /*   custom User properties                                         */
    /* ################################################################ */
    /* ################################################################ */

    /**
     * Eight APIs are supported for managing custom User properties:
     *
     *  APIs that operate on the set of custom properties (meta-level):
     *  1) POST:/users/cproperties                # creates a new custom User property
     *  2) GET:/users/cproperties                 # returns the set of custom User properties
     *  3) DELETE:/users/cproperties/{name}       # deletes a custom User property
     *
     *  APIs that operate on a particular User entity instance:
     *  4) PUT:/users/{entityID}/properties/{name}       # sets a custom property value on a User instance
     *  5) DELETE:/users/{entityID}/properties/{name}    # removes a custom property value from a User instance
     *  6) GET:/users/{entityID}/properties              # gets all custom properties on a User instance
     *
     *  APIs that provide query operations on the entire set of Users with custom properties:
     *  7) GET:/users/properties                # gets ALL properties of ALL Users with custom proeprties
     *  8) GET:/users/properties/{name}         # gets all Users with the given property, along with the value for each user
     */

    /**
     * Implementation of #1.  HTTP entity body is JSON that gets mapped to Map<String,String> where
     * there are 2 fields:  "name" and "type".  Name must be unique within User custom properties
     * and type (when uppercased) must be one of the enum values in PropertyType.
     */
    @POST
    @Path("/cproperties")
    @Consumes("application/json")
    public Response createCustomPropertyDef(@Context HttpServletRequest request, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/cproperties");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    String name = jsonObj.get("name");
                    String type = jsonObj.get("type");

                    if (emptyString(name))
                        return badRequestEmptyParam("name");
                    else if (emptyString(type))
                        return badRequestEmptyParam("type");

                    logger.addParameter("name", name);
                    logger.addParameter("type", type);

                    sysServices.getUserLogic().addProperty(name, PropertyType.valueOf(type.toUpperCase()));

                    return ok();
                }});
    }

    /**
     * Implementation of #2.
     */
    @GET
    @Path("/cproperties")
    public Response getCustomPropertiesDefs(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/cproperties");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(sysServices.getUserLogic().getPropertyDefinitions(false));
                }});
    }

    /**
     * Implementation of #3
     */
    @DELETE
    @Path("/cproperties/{name}")
    public Response deleteCustomPropertyDef(@Context HttpServletRequest request, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/cproperties/{name}");

        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    sysServices.getUserLogic().removeProperty(name);

                    return ok();
                }});
    }

    /**
     * Implementation of #4
     */
    @PUT
    @Path("/{userID}/properties/{name}")
    @Consumes("application/json")
    public Response setCustomProperty(@Context HttpServletRequest request, @PathParam("userID") final String entityID,
                                      @PathParam("name") final String name, final Map<String, String> jsonObj)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/{userID}/properties/{name}");

        logger.addParameter("userId", entityID);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    User      user  = findUserByEmailOrID(entityID, true);
                    String    value = jsonObj.get("value");

                    logger.addParameter("value", value);

                    if (emptyString(value))
                        return badRequestEmptyParam("value");
                    else if (user == null)
                        return notFound("user", entityID);

                    sysServices.getUserLogic().setUserProperty(user, name, value);

                    return ok();
                }});
    }

    /**
     * Implementation of #5
     */
    @DELETE
    @Path("/{userID}/properties/{name}")
    public Response removeCustomProperty(@Context HttpServletRequest request,
                                         @PathParam("userID") final String entityID, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/{userID}/properties/{name}");

        logger.addParameter("userId", entityID);
        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    User  user  = findUserByEmailOrID(entityID, true);

                    if (user == null)
                        return notFound("user", entityID);

                    sysServices.getUserLogic().removeUserProperty(user, name);

                    return ok();
                }});
    }

    /**
     * Implementation of #6.  Get all custom properties on the given object
     */
    @GET
    @Path("/{userID}/properties")
    public Response getAllUserProperties(@Context HttpServletRequest request, @PathParam("userID") final String entityID)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/{userID}/properties");

        logger.addParameter("userId", entityID);

        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws IOException
                {
                    User  user  = findUserByEmailOrID(entityID, false);

                    if (user == null)
                        return notFound("user", entityID);

                    return ok(sysServices.getUserLogic().getUserProperties(user));
                }});
    }

    
    /**
     * Implementation of #7:  aggregate query to get all custom properties across all Users.
     */
    @GET
    @Path("/properties")
    public Response getAllUsersProperties(@Context HttpServletRequest request)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/properties");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    return ok(sysServices.getUserLogic().getAllUserProperties());
                }});
    }

    /**
     * Implementation of #8:  aggregate query to get a single custom property across all Users.
     */
    @GET
    @Path("/properties/{name}")
    public Response getAllUsersProperty(@Context HttpServletRequest request, @PathParam("name") final String name)
    {
        ApiLogger  logger   = super.createApiLogger(request, "/users/properties/{name}");

        logger.addParameter("name", name);

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    if (emptyString(name))
                        return badRequestEmptyParam("name");

                    return ok(sysServices.getUserLogic().getUsersProperty(name));
                }});
    }

    /**
     * Attempts to get the User from the XUUID-based userID, testing that the caller should be
     * allowed to retrieve that User object (only "self" and ROOT users can do this).
     */
    private User findUserByEmailOrID(String userid, boolean forUserUpdate) throws IOException
    {
        if ((userid == null) || (userid.length() == 0))
            return null;

        if (!userid.equals("me"))
        {
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
                    badRequest(x1.getMessage() + " and " + x2.getMessage());
                }
            }

            if (userXid != null)
                userid = userXid.toString();
        }

        return sysServices.getUserLogic().getUser(RestUtil.getInternalToken(), userid, forUserUpdate);
    }

    /**
     * Returns true if the string doesn't contain non-whitespace characters.
     */
    private static boolean emptyString(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }
    
}
