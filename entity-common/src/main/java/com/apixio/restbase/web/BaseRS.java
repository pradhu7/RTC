package com.apixio.restbase.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletResponse;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.logger.EventLogger;
import com.apixio.logger.LogHelper;
import com.apixio.logger.StandardMetricsLayout;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.entity.Token;

/**
 * BaseRS is the base class for REST resource classes.  Currently it provides
 * help for API logging.
 */
public class BaseRS
{
    /**
     * A simple framework to invert control flow of RESTful methods in order to template-ize
     * things with respect to try/catch error handling.
     */
    public static abstract class RestWrap
    {
        /**
         * The logger that must be finished at the end of the operation
         */
        ApiLogger logger;

        /**
         * The HttpServletResponse.SC_* codes to return on Exception and BaseException
         */
        int serverErrorCode;
        int badRequestCode;

        public RestWrap(ApiLogger logger)
        {
            this.logger = logger;
            this.serverErrorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            this.badRequestCode  = HttpServletResponse.SC_BAD_REQUEST;
        }

        public RestWrap serverErrorCode(int code)
        {
            this.serverErrorCode = code;
            return this;
        }

        public RestWrap badRequestCode(int code)
        {
            this.badRequestCode = code;
            return this;
        }

        public abstract Response operation() throws Exception;
    }

    /**
     * The logger, if configured.
     */
    private EventLogger logger;
    private boolean     loggingEnabled;

    /**
     *
     */
    private SysServices sysServices;

    /**
     * ApiLogger is intended to be used as a local variable within the scope of
     * a RESTful API method call.  It can be retrieved via the method
     * BaseRS.createApiLogger.  While it is safe (no resource leaks) if
     * apiEnd() is not called, no logging will occur for a method that does that.
     * The expected pattern is for the REST method code to call createApiLogger,
     * perform its REST function, then before returning the Response object, calling
     * apiEnd().
     *
     * Extra information relevant to logging can be added along the way by calling
     * methods such as addUser() or addException().
     */
    public class ApiLogger
    {
        private long                startTime;
        private String              apiName;
        private String              method;
        private String              uri;
        private String              endpoint;
        private XUUID               userID;
        private Exception           exception;
        private String              failureDetails;
        private Map<String, Object> params;

        /**
         * Create an ApiLogger, pulling a lot of the logged information directly
         * from the HttpServletRequest.
         */
        ApiLogger(HttpServletRequest request)
        {
            if (loggingEnabled)
            {
                Token  token = RestUtil.getExternalToken();

                this.method    = request.getMethod();
                this.uri       = RestUtil.getRequestPathInfo(request);
                this.startTime = System.currentTimeMillis();
                this.apiName   = "user_account"; // default app name for api logs // TODO: This is a bad default and should probably be changed. I chose it for backwards compatibility in user account code. -lilith
                this.params    = new HashMap<String, Object>();

                if (token == null)
                    token = RestUtil.getInternalToken();

                if (token != null)
                    this.userID = token.getUserID();
            }
        }

        public void setApiUrl(String url)
        {
            this.uri = url;
        }
        public void setApiName(String name)
        {
            this.apiName = name;
        }
        public void setApiEndpoint(String path)
        {
            this.endpoint = path;
        }

        public void addFailureDetails(String info)
        {
            if (loggingEnabled)
                this.failureDetails = info;
        }
        public void addException(Exception x)
        {
            this.exception = x;
        }

        public void addParameter(String name, Object value)
        {
            String prefix = "app."+apiName+".request.parameters.";
            params.put(prefix + name, value);
        }

        /**
         * Logs the API call data, if logging is enabled.
         */
        public void apiEnd(Response response)
        {
            if (loggingEnabled)
            {
                int    code   = response.getStatus();
                String prefix = "app." + apiName + ".request.";

                if (userID != null)
                    params.put(prefix + "userId", userID);
                if (failureDetails != null || exception != null)
                {
                    params.put(prefix + "status", "error");
                    if (failureDetails != null)
                        params.put(prefix + "failureReason", failureDetails);
                    if (exception != null)
                        params.put(prefix + "error", exception.getMessage());
                }
                else
                {
                    params.put(prefix + "status", "success");
                }

                params.put(prefix + "endpoint",   endpoint != null ? endpoint : uri);
                params.put(prefix + "uri",        uri);
                params.put(prefix + "method",     method);
                params.put(prefix + "code",       Integer.valueOf(code));
                params.put(prefix + "millis",     Long.valueOf(System.currentTimeMillis() - startTime));

                StandardMetricsLayout.addJVMMetrics(params);

                logger.event(params);

                //                System.out.println("---> APILOG: " + params);
            }
        }
    }

    /**
     * Base method that grabs logging configuration and sets up the logger.
     */
    protected BaseRS(MicroserviceConfig config, SysServices sysServices)
    {
        this(config, sysServices, null);
    }

    protected BaseRS(MicroserviceConfig config, SysServices sysServices, String loggerName)
    {
        this.sysServices    = sysServices;
        this.loggingEnabled = ((logger = sysServices.getEventLogger(loggerName)) != null);
    }

    /**
     * For that code that doesn't directly use ApiLogger.
     */
    protected EventLogger getEventLogger2()
    {
        return logger;
    }
    protected EventLogger getEventLogger(String loggerName)
    {
        return sysServices.getEventLogger(loggerName);
    }

    /**
     * Creates an ApiLogger from the given HttpServletRequest.
     */
    protected ApiLogger createApiLogger(HttpServletRequest request)
    {
        return createApiLogger(request, null, null);
    }
    protected ApiLogger createApiLogger(String name, HttpServletRequest request) { return createApiLogger(request, null, name); }
    protected ApiLogger createApiLogger(HttpServletRequest request, String path) { return createApiLogger(request, path, null); }
    protected ApiLogger createApiLogger(HttpServletRequest request, String path, String name)
    {
        ApiLogger al = new ApiLogger(request);

        if (path != null)
            al.setApiEndpoint(path);
        if (name != null)
            al.setApiName(name);

        return al;
    }

    /**
     * Convert Map<String, String> to its equivalent Properties object.
     */
    private static Properties toProperties(Map<String, String> map)
    {
        Properties props = new Properties();

        for (Map.Entry<String, String> entry : map.entrySet())
            props.setProperty(entry.getKey(), entry.getValue());

        return props;
    }

    /**
     * Convenience method for Redis Transaction support.
     *
     * If a resource java method wants to NOT persist any redis changes, then it should
     * call this method.
     */
    protected void setRequestError(boolean error)
    {
        RestUtil.setRequestError(error);
    }

    /**
     * Easy/cheap support methods for returning standard things in a standard way.
     */

    /**
     * "ok" means all worked as expected, return 200 code.
     */
    protected Response ok()
    {
        return Response.ok().build();
    }
    protected Response ok(Object result)
    {
        return Response.ok().entity(result).build();
    }
    protected Response.ResponseBuilder okBuilder(Object result)
    {
        return Response.ok().entity(result);
    }

    /**
     * forbidden method will return a SC_FORBIDDEN (403) code.
     */
    protected Response forbidden()
    {
        return Response.status(HttpServletResponse.SC_FORBIDDEN).build();
    }

    /**
     * "created" means all worked as expected, return 201 code.
     */
    protected Response created(String location)
    {
        try
        {
            return created(new URI(location));
        }
        catch (Exception e)
        {
            throw BaseException.badRequest();
        }
    }

    protected Response created(String location, Object body)
    {
        try
        {
            return created(new URI(location), body);
        }
        catch (Exception e)
        {
            throw BaseException.badRequest();
        }
    }

    protected Response created(URI location)
    {
        return Response.created(location).build();
    }

    protected Response created(URI location, Object body)
    {
        return Response.created(location).entity(body).build();
    }

    /**
     * "not found" is intended solely for the case where the URL has an entity ID in it
     * but that actual ID supplied by the client doesn't refer to a known object.
     */
    protected Response notFound()
    {
        throw BaseException.notFound();
    }
    protected Response notFound(String type, String id)
    {
        throw BaseException.notFound().description("Entity of type {} with ID {} was not found", type, id);
    }

    /**
     * "badRequest" means that the client did something wrong (e.g., didn't send valid
     * parameters, etc.).  This is NOT to be used when a client-supplied entity ID (that
     * is part of the URL as a real resource identifier) is not found--notFound() shouldl
     * be used for that case.
     */
    protected Response badRequest()
    {
        throw BaseException.badRequest();
    }
    protected Response badRequest(String reason)
    {
        throw BaseException.badRequest(reason);
    }
    protected Response badRequestParam(String paramName, Object paramValue, String format)
    {
        throw BaseException.badRequestParam(paramName, paramValue, format);
    }
    protected Response badRequestEmptyParam(String paramName)
    {
        throw BaseException.badRequest().description("'{}' cannot be empty", paramName);
    }
    protected Response badRequestNullParam(String paramName)
    {
        throw BaseException.badRequest().description("'{}' cannot be null", paramName);
    }
    protected void validateParamNotEmpty(String paramName, String paramValue)
    {
        if ((paramValue == null) || (paramValue.trim().length() == 0))
            throw BaseException.badRequest().description("'{}' cannot be null", paramName);
    }

    /**
     * This method provides the proper and standardized error handling for RESTful API method
     * implementations.  This method does NOT need to be used but it does provide automatic
     * handling of the following:
     *
     *  1.  determination of "bad request" (something the client can do something about) and
     *      "server error" (something the client can't do anything about)
     *
     *  2.  logging of exception/error information
     *
     *  3.  aborting of pending redis transactions
     *
     *  4.  closing/finishing of the ApiLogger.
     */
    protected Response restWrap(RestWrap op)
    {
        Response response = null;

        try
        {
            response = op.operation();
        }
        catch (BaseException bx)
        {
            if (bx.isNotFound())
            {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND).build();
            }
            else
            {
                Map<String, String> json = new HashMap<String, String>(2);

                RestUtil.setRequestError(true);

                json.putAll(bx.getDetails());

                if (!bx.isBadRequest())
                {
                    op.logger.addException(bx);
                    op.logger.addFailureDetails(bx.getDetails().get(BaseException.P_REASON));
                }

                response = Response.status(op.badRequestCode).type("application/json").entity(json).build();
            }
        }
        catch (Exception x)
        {
            x.printStackTrace();
            Map<String, String> json = new HashMap<String, String>(2);

            RestUtil.setRequestError(true);

            json.put(BaseException.P_REASON, x.getMessage());  //!! getMessage value should not be the reason!!

            op.logger.addException(x);

            response = Response.status(op.serverErrorCode).type("application/json").entity(json).build();
        }

        op.logger.apiEnd(response);

        return response;
    }

}
