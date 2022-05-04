package com.apixio.restbase.apiacl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.apixio.SysServices;
import com.apixio.logger.EventLogger;
import com.apixio.logger.StandardMetricsLayout;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.apiacl.RestEnforcer.BooleanOp;
import com.apixio.restbase.apiacl.acctrole.RestAccountRoleEnforcer;
import com.apixio.restbase.apiacl.acctstate.RestAccountStateEnforcer;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.RestPermissionEnforcer;
import com.apixio.useracct.UserUtil;
import com.apixio.useracct.entity.User;

/**
 * Main entry and control class for outside clients interacting with the API access control
 * module/subsystem.
 *
 * Due to the desire to log access information in general, and to allow finer-grained
 * info for debugging purposes, there's some fancy stuff to support this.  There are
 * the following levels/behaviors of logging:
 *
 *  NONE:  no logging of anything at all.  No EventLogger is required
 *  ACCESS:  summary logging of access checks.  EventLogger is required
 *  DETAILED:  ACCESS + why access was allowed/denied.  EventLogger is required.
 *             Info is also put to System.out
 *
 * NONE is the default as the others require the app code to supply an EventLogger.
 *
 * When logging to EventLogger, a call to requestAllowed will result in a single call
 * to EventLogger.event() for reasonableness.  What's logged is:
 *
 *   * userID
 *   * permission:  [one of OP_* permissions]
 *   * authStatus:  ALLOW/DISALLOW
 *   * error:       NO_ACL_MATCH
 *
 * When logging to System.out, multiple lines are written (but just gathered along the
 * way).  The information is a superset of what's put to EventLogger.
 */
public class ApiAcls {

    /**
     * Required JSON structure is:
     *
     *  { "fields we don't care about right now": "blah", "role-config":       { "field1": "blah", ... }   // "role-config" defined in knownEnforcers
     *  { "fields we don't care about right now": "blah", "state-config":      { "field1": "blah", ... }   // ditto
     *  { "fields we don't care about right now": "blah", "permission-config": { "field1": "blah", ... }
     *
     *  --OR the following--
     *
     *  { "fields we don't care about right now": "blah", "permission-config":      // or "role-config" or "state-config"
     *     { "or": [ {"field1": "blah", ... }, {"field1": "blah", ...}, ... ] }
     *
     *  --OR the following--
     *
     *  { "fields we don't care about right now": "blah", "permission-config":      // or "role-config" or "state-config"
     *     { "and": [ {"field1": "blah", ... }, {"field1": "blah", ...}, ... ] }
     *
     * Note that the difference in structure is that the value is an object that has either
     * a single key of "or" or "and" which has a list of objects as its value, or it's a
     * that object that is in the list.
     */

    /**
     * The two allowed JSON keys that indicate a nested list structure with a BooleanOp applied
     * to the entire list evaluation.
     */
    private static final String OP_OR  = "or";
    private static final String OP_AND = "and";

    /**
     * For logging, with love
     */
    public enum LogLevel { NONE, ACCESS, DETAILED }

    /**
     * Class to capture all info needed for initialization from .json file.  This SHOULD
     * be extended as needed for clients to add their own init info.
     */
    public static class InitInfo {
        public SysServices sysServices;
    }

    /**
     * CheckResults packages up two things needed to communicate access info back to client.
     * The "match" value is used iff access == AccessResult.NEED_ENTITY as the code that
     * gets invoked for that test should have to re-match the request URL against the list
     * of protected APIs (etc.).
     */
    public static class CheckResults {
        public MatchResults match;
        public AccessResult access;

        /**
         * Because of NEED_ENTITYs handling flow (which returns from requestAllowed
         * and goes through all the rest of the web filters only to end up in the
         * reader interceptor), we need to be able to pass the CheckContext object
         * around to that reader interceptor.  Including the context here allows
         * that to happen.
         */
        public CheckContext ctx;
    }

    /**
     * JSON field names of the API definition.
     */
    private final static String FIELD_API_ID    = "api-id";
    private final static String FIELD_API_NAME  = "api-name";
    private final static String FIELD_API_URL   = "api-url";

    /**
     * There are 3 (now) types of enforcers.  We keep metadata on these types (allowing easier
     * expansion later, if needed).  The first piece of metadata is the JSON field name (which
     * must be a subfield of the larger API JSON object), the second is the optional Java class
     * name of the enforcer, and the third is is the default class (if not specified).
     *
     * ORDER IS IMPORTANT as the ACL logging system logs only the last ACL tested!
     */
    private static EnforcerJsonMeta[] knownEnforcers = new EnforcerJsonMeta[] {
        new EnforcerJsonMeta("role-config",       "role-processor",       RestAccountRoleEnforcer.class),
        new EnforcerJsonMeta("state-config",      "state-processor",      RestAccountStateEnforcer.class),
        new EnforcerJsonMeta("permission-config", "permission-processor", RestPermissionEnforcer.class)
    };

    /**
     * Combining the above, the basic JSON object structure is:
     *
     *  { "api-id":             "...",
     *    "api-name":           "...",
     *    "api-url":            "...",
     *    "state-config":       { "state-processor": "JavaClassName", ... },
     *    "role-config":        { ... }
     *    "permission-config":  { ... }
     *  }
     */

    private LogLevel    logLevel;
    private EventLogger logger;        // this can be null even if logLevel != NONE (to support older clients)
    private SysServices sysServices;

    /**
     * There is  1 enforcer for each distinct type declared in the knownEnforcers array.
     * The key on this Map is the class name.
     */
    private Map<String, RestEnforcer> enforcers = new HashMap<String, RestEnforcer>();

    /**
     * The set of APIs that are protected.
     */
    private ProtectedApiSet protectedApis = new ProtectedApiSet();

    /**
     * Load the ApiAcls configuration from the given JSON-formatted configuration file and
     * return the resultant ApiAcls object
     */
    static public ApiAcls fromJsonFile(SysServices sysServices, String jsonFile) throws FileNotFoundException, IOException
    {
        return fromJsonFile(null, sysServices, jsonFile);
    }

    static public ApiAcls fromJsonFile(InitInfo initInfo, SysServices sysServices, String jsonFile) throws FileNotFoundException, IOException
    {
        if (jsonFile== null)
            throw new IllegalArgumentException("Attempt to read from null jsonfile path");

        ApiAcls     aa   = new ApiAcls(sysServices);
        InputStream json = null;

        if (initInfo == null)
            initInfo = new InitInfo();

        initInfo.sysServices = sysServices;

        try
        {
            JsonFactory   factory      = (new JsonFactory()).configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            ObjectMapper  objectMapper = new ObjectMapper(factory);

            json = new FileInputStream(jsonFile);

            aa.processJsonList(initInfo, objectMapper.readTree(json));
        }
        finally
        {
            if (json != null)
            {
                try
                {
                    json.close();
                }
                catch (IOException iox)
                {
                }
            }
        }

        return aa;
    }

    @Deprecated
    public void setDebug(boolean debug)
    {
        logLevel = LogLevel.DETAILED;
    }

    @Deprecated
    public boolean getDebug()
    {
        return (logLevel == LogLevel.DETAILED);
    }

    public void setLogLevel(LogLevel level, EventLogger logger)
    {
        this.logLevel = level;
        this.logger   = (level != LogLevel.NONE) ? logger : null;
    }

    public RestPermissionEnforcer getPermissionEnforcer()
    {
        return (RestPermissionEnforcer) getEnforcer(RestPermissionEnforcer.class.getName());
    }

    public RestAccountRoleEnforcer getRoleEnforcer()
    {
        return (RestAccountRoleEnforcer) getEnforcer(RestAccountRoleEnforcer.class.getName());
    }

    public RestAccountStateEnforcer getStateEnforcer()
    {
        return (RestAccountStateEnforcer) getEnforcer(RestAccountStateEnforcer.class.getName());
    }

    /**
     * hasMatch just determines if the given URL (method:url) matches something in the protected API list.
     */
    public boolean hasMatch(String method, String url)
    {
        return (protectedApis.selectBestMatch(method, url) != null);
    }

    /**
     * Match the given URL (method:url) against protected APIs and actually test the access against
     * the configured protections.
     */
    public CheckResults requestAllowed(HttpRequestInfo ri, Object httpEntity) throws IOException
    {
        long         start   = System.currentTimeMillis();
        User         user    = UserUtil.getCachedUser();
        CheckResults results = new CheckResults();
        CheckContext ctx     = new CheckContext(logLevel);
        String       rId     = RestUtil.getRequestId();

        results.ctx   = ctx;
        results.match = protectedApis.selectBestMatch(ri.getMethod(), ri.getPathInfo());

        if (results.match != null)
        {
            ProtectedApi api = results.match.api;

            ctx.recordDetail("(Request: {}) matched ApiDef {} with template {}", rId, api.getApiDef().getApiID(), api.getUrlTemplate());
            results.access = api.checkPermission(ctx, ri, results.match, httpEntity);
        }
        else
        {
            ctx.recordWarn("(Request: {}) API definition for URL [{}:{}] was not found.  Disallowing (with a 403) request.", rId, ri.getMethod(), ri.getPathInfo());
            ctx.recordAccess(false, "no match for URL in ACL configuration");

            results.access = AccessResult.DISALLOW;
        }

        if (logLevel != LogLevel.NONE)
        {
            if (logger != null)
            {
                CheckContext.AccessInfo info = ctx.getAccess();

                if (info != null)
                {
                    Map<String, Object> params = new HashMap<>();

                    StandardMetricsLayout.addJVMMetrics(params);

                    if (user != null)
                    {
                        params.put("acl.userID",    user.getID().toString());
                        params.put("acl.userEmail", user.getEmailAddress().toString());
                    }

                    if (info.operation != null)
                        params.put("acl.operation", info.operation);
                    if (info.subject != null)
                        params.put("acl.subject", info.subject);
                    if (info.object != null)
                        params.put("acl.object", info.object);
                    if (info.reason != null)
                        params.put("acl.reason", info.reason);

                    params.put("acl.endpoint",  ri.getMethod() + ":" + ri.getPathInfo());
                    params.put("acl.allowed",   Boolean.toString(info.allowed));
                    params.put("acl.requestId", rId);
                    params.put("acl.millis",    Long.toString(System.currentTimeMillis() - start));

                    logger.event(params);
                }
            }

            if (logLevel == LogLevel.DETAILED)
                System.out.println(ctx.getDetails());
        }

        return results;
    }

    /**
     *
     */
    private ApiAcls(SysServices sysServices)
    {
        this.sysServices = sysServices;

        for (EnforcerJsonMeta em : knownEnforcers)
            loadEnforcer(em.enforcerClass.getName());
    }

    private void processJsonList(InitInfo initInfo, JsonNode node)
    {
        if (node.isArray())
        {
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); )
                processJsonApiDef(initInfo, it.next());
        }
        else
        {
            throw new IllegalArgumentException("Expected JsonArray in processJsonList");
        }
    }

    /**
     * Gets the enforcer indicated by the class name, creating the single instance if it's
     * not already there.
     */
    private RestEnforcer getEnforcer(String cls)
    {
        return enforcers.get(cls);
    }

    /**
     *
     */
    private void loadEnforcer(String cls)
    {
        try
        {
            RestEnforcer re = (RestEnforcer) Class.forName(cls).newInstance();

            re.init(sysServices);

            enforcers.put(cls, re);
        }
        catch (Exception x)
        {
            x.printStackTrace();
            throw new IllegalArgumentException("Problem creating RestEnforcer of class " + cls);
        }
    }

    /**
     * Process a single JSON API definition object by getting its enforcer(s) and adding it
     * to its (their) list(s).
     */
    private void processJsonApiDef(InitInfo initInfo, JsonNode node)
    {
        ProtectedApi pa = new ProtectedApi(new ApiDef(getRequiredField(node, FIELD_API_ID),
                                                      getRequiredField(node, FIELD_API_NAME),
                                                      getRequiredField(node, FIELD_API_URL)));
        boolean      atLeastOne = false;

        protectedApis.addApi(pa);

        for (EnforcerJsonMeta em : knownEnforcers)
        {
            JsonNode config = node.get(em.jsonConfigFieldName);

            if (config != null)
            {
                JsonNode                  clsNode = node.get(em.jsonEnforcerFieldClsName);
                RestEnforcer              re      = getEnforcer((clsNode != null) ? clsNode.asText() : em.enforcerClass.getName());
                JsonNode                  or      = config.get("or");
                JsonNode                  and     = config.get("and");
                List<Map<String, String>> configs = new ArrayList<>();
                BooleanOp                 op;

                if ((or != null) && (and != null))
                {
                    throw new IllegalArgumentException("Invalid JSON:  both 'and' and 'or' are present in " + config.toString());
                }
                else if (or != null)
                {
                    op = BooleanOp.OR;
                }
                else if (and != null)
                {
                    or = and;             // redefine the boolean operator world...or just reuse the local var...
                    op = BooleanOp.AND;
                }
                else
                {
                    op = null;
                }

                if (op != null)
                {
                    int      idx = 0;
                    JsonNode ele;

                    if (!or.isArray())
                        throw new IllegalArgumentException("The 'and' or 'or' JSON field value MUST be an array");

                    while (true)
                    {
                        if ((ele = or.get(idx++)) == null)
                            break;

                        configs.add(configToMap(ele));
                    }
                }
                else
                {
                    configs.add(configToMap(config));
                }

                atLeastOne = true;

                pa.addCheck(re.createApiCheck(initInfo, pa, op, configs));
            }
        }

        if (!atLeastOne)
            System.out.println("WARN:  RESTful API \"" + pa.getApiDef().getApiID() + "\" (" + pa.getUrlTemplate().toString() +
                               ") has no access checks:  ALL requests to this API will be denied");
    }

    /**
     *
     */
    private String getRequiredField(JsonNode json, String name)
    {
        JsonNode field = json.get(name);

        if (field == null)
            throw new IllegalArgumentException("JSON API def object is missing the required '" + name + "' field");

        return field.asText();
    }

    /**
     * Converts a JSON object to its corresponding Map of string->string.  Needed since internal
     * config is not JSON specific.
     */
    private Map<String, String> configToMap(JsonNode config)
    {
        Map<String, String> mapConfig = new HashMap<String, String>();

        for (Iterator<Map.Entry<String, JsonNode>> it = config.fields(); it.hasNext(); )
        {
            Map.Entry<String, JsonNode> field = it.next();

            mapConfig.put(field.getKey(), field.getValue().asText());
        }

        return mapConfig;
    }

    public static void main(String[] args) throws Exception
    {
        ApiAcls acls = fromJsonFile(null, args[0]);

        for (int i = 1; i < args.length; i++)
        {
            String[]     split   = args[i].split(":");
            CheckResults results = new CheckResults();

            System.out.println("\n===========================");

            results.match = acls.protectedApis.selectBestMatch(split[0], split[1]);

            System.out.println("INFO:  match ApiDef " + results.match.api.getApiDef().getApiID());
        }
    }

}
