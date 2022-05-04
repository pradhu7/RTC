package com.apixio.useracct.dw.resources;

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

import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PasswordPolicyLogic.PasswordPolicyException;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.PasswordPolicy;
import com.apixio.useracct.eprops.PasswordPolicyProperties;

/**
 * Internal service.
 */
@Path("/passpolicies")
@Produces("application/json")
public class PasswordPolicyRS extends BaseRS {

    private PrivSysServices sysServices;

    /**
     * Field names MUST match what's defined in the API spec!
     */
    public static class CreatePasswordPolicyParams {
        public String   name;
        public String   maxDays;   // comes with optional unit:  d=day, w=week, m=month, z=minute
        public Integer  minChars;
        public Integer  maxChars;
        public Integer  minLower;
        public Integer  minUpper;
        public Integer  minDigits;
        public Integer  minSymbols;
        public Integer  noReuseCount;
        public Boolean  noUserID;

        @Override
        public String toString()
        {
            return ("[createppp name=" + name +
                    ", maxDays=" + maxDays +
                    "]");
        }
    }

    public static class UpdatePasswordPolicyParams {
        public String   name;
        public String   maxDays;   // comes with optional unit:  d=day, w=week, m=month, z=minute
        public Integer  minChars;
        public Integer  maxChars;
        public Integer  minLower;
        public Integer  minUpper;
        public Integer  minDigits;
        public Integer  minSymbols;
        public Integer  noReuseCount;
        public Boolean  noUserID;

        @Override
        public String toString()
        {
            return ("[updateppp name=" + name +
                    ", maxDays=" + maxDays +
                    "]");
        }
    }

    public PasswordPolicyRS(ServiceConfiguration configuration, PrivSysServices sysServices)
    {
        super(configuration, sysServices);

        this.sysServices = sysServices;
    }

    @POST
    @Consumes("application/json")
    public Response createPasswordPolicy(
        @Context  HttpServletRequest     request,
        final CreatePasswordPolicyParams params
        )
    {
        ApiLogger           logger   = super.createApiLogger(request, "/passpolicies");

        logger.addParameter("name",         params.name);
        logger.addParameter("maxDays",      params.maxDays);
        logger.addParameter("minChars",     params.minChars);
        logger.addParameter("maxChars",     params.maxChars);
        logger.addParameter("minLower",     params.minLower);
        logger.addParameter("minUpper",     params.minUpper);
        logger.addParameter("minDigits",    params.minDigits);
        logger.addParameter("minSymbols",   params.minSymbols);
        logger.addParameter("noReuseCount", params.noReuseCount);
        logger.addParameter("noUserID",     params.noUserID);

        // this is kind of a non-standard creation pattern.  I chose to do it this way to avoid
        // passing in a large number of method args (one for each max/min param)...
        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PasswordPolicy      policy = new PasswordPolicy(params.name);

                    if (params.maxDays      != null) setMaxTime(policy, params.maxDays);
                    if (params.minChars     != null) policy.setMinChars(params.minChars);
                    if (params.maxChars     != null) policy.setMaxChars(params.maxChars);
                    if (params.minLower     != null) policy.setMinLower(params.minLower);
                    if (params.minUpper     != null) policy.setMinUpper(params.minUpper);
                    if (params.minDigits    != null) policy.setMinDigits(params.minDigits);
                    if (params.minSymbols   != null) policy.setMinSymbols(params.minSymbols);
                    if (params.noReuseCount != null) policy.setNoReuseCount(params.noReuseCount);
                    if (params.noUserID     != null) policy.setNoUserID(params.noUserID);

                    policy = sysServices.getPasswordPolicyLogic().createPasswordPolicy(policy);

                    return ok(PasswordPolicyProperties.toJson(policy));
                }});
    }

    /**
     * Returns password policy details for all password policies.
     */
    @GET
    public Response getAllPasswordPolicy(@Context HttpServletRequest request)
    {
        ApiLogger   logger   = super.createApiLogger(request, "/passpolicies");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    List<Map<String, Object>> jsons = new ArrayList<>();

                    for (PasswordPolicy pp : sysServices.getPasswordPolicyLogic().getAllPasswordPolicies())
                        jsons.add(PasswordPolicyProperties.toJson(pp));

                    return ok(jsons);
                }});
    }

    /**
     * Returns the password policy details.
     */
    @GET
    @Path("/{policyName}")
    public Response getPasswordPolicy(@Context HttpServletRequest request, @PathParam("policyName") final String name)
    {
        ApiLogger           logger   = super.createApiLogger(request, "/passpolicies/{name}}");

        return super.restWrap(new RestWrap(logger) {
                public Response operation()
                {
                    PasswordPolicy      policy = sysServices.getPasswordPolicyLogic().getPasswordPolicy(name);

                    if (policy != null)
                        return ok(PasswordPolicyProperties.toJson(policy));
                    else
                        return notFound();
                }});
    }

    /**
     *
     */
    @PUT
    @Consumes("application/json")
    @Path("/{policyName}")
    public Response updatePasswordPolicyJson(
        @Context                 final HttpServletRequest request,
        @PathParam("policyName") final String             policyName,
        final UpdatePasswordPolicyParams                  updateParams
        )
    {
        final ApiLogger        logger = super.createApiLogger(request, "/passpolicies/{policyName}");
        final PasswordPolicy   policy = sysServices.getPasswordPolicyLogic().getPasswordPolicy(policyName);

        logger.addParameter("policyName", (policy != null) ? policy.getName() : "<null>");

        if (policy == null)
        {
            return notFound("PasswordPolicy", policyName);
        }
        else
        {
            return super.restWrap(new RestWrap(logger) {
                    public Response operation()
                    {
                        if (updateParams.name         != null) policy.setName(updateParams.name);
                        if (updateParams.minChars     != null) policy.setMinChars(updateParams.minChars.intValue());
                        if (updateParams.maxChars     != null) policy.setMaxChars(updateParams.maxChars.intValue());
                        if (updateParams.minLower     != null) policy.setMinLower(updateParams.minLower.intValue());
                        if (updateParams.minUpper     != null) policy.setMinUpper(updateParams.minUpper.intValue());
                        if (updateParams.minDigits    != null) policy.setMinDigits(updateParams.minDigits.intValue());
                        if (updateParams.minSymbols   != null) policy.setMinSymbols(updateParams.minSymbols.intValue());
                        if (updateParams.noReuseCount != null) policy.setNoReuseCount(updateParams.noReuseCount.intValue());
                        if (updateParams.noUserID     != null) policy.setNoUserID(updateParams.noUserID.booleanValue());

                        if (updateParams.maxDays != null)
                            setMaxTime(policy, updateParams.maxDays);

                        sysServices.getPasswordPolicyLogic().modifyPasswordPolicy(policy);

                        return ok();
                    }});
        }
    }

    /**
     * Interprets the maxDays string (format of # followed by unit) and pushes values into
     * PasswordPolicy appropriately
     */
    private void setMaxTime(PasswordPolicy policy, String maxDays)
    {
        int len = maxDays.length();

        if (len > 0)
        {
            char    ch   = maxDays.charAt(len - 1);
            int     unit = 0;
            boolean sub  = false;

            if (ch == 'd')
            {
                unit = PasswordPolicy.UNIT_DAY;
                sub  = true;
            }
            else if (ch == 'w')
            {
                unit = PasswordPolicy.UNIT_WEEK;
                sub  = true;
            }
            else if (ch == 'm')
            {
                unit = PasswordPolicy.UNIT_MONTH;
                sub  = true;
            }
            else if (ch == 'z')
            {
                unit = PasswordPolicy.UNIT_MINUTE;
                sub  = true;
            }
            else
            {
                unit = PasswordPolicy.UNIT_DAY;
            }

            if (sub)
                maxDays = maxDays.substring(0, len - 1);

            try
            {
                policy.setMaxTime(Integer.parseInt(maxDays)); // do this before so it can exception out first
                policy.setTimeUnit(unit);
            }
            catch (NumberFormatException x)
            {
                throw PasswordPolicyException.badRequest("Unparseable number {}", maxDays);
            }
        }
    }

}
