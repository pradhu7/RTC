package com.apixio.sdk.builtin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.sdk.ArgsEvaluator;
import com.apixio.sdk.DataUriManager;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.protos.EvalProtos.ArgList;

/**
 * ApxGroupingDataUriManager deals with data URIs that identify the data via a groupingID,
 * which is the underlying storage identifier for persisted data.
 *
 * The structure of such a URI is:
 *
 *  apxdata://{domain}/gid/{id}
 *
 * where "gid" means groupingID and {id} is the actual groupingID to be used to restore
 * data from ApxData.getData()
 *
 * The actual production of the groupingID is, by definition, done at the application level.  In
 * order to support generic construction, the idea of ArgList is used to produce the components
 * of the groupingID given a set of accessors to environment+request (possibly others, over time?).
 * The two SDK-supplied accessors are:  environment(string) and request(string) and they can
 * be used like:
 *
 *    request("docuuid"),environment("fxname")
 *
 * The construction of the final groupingID is just a concatenation of these evaluated args, with
 * the values being URL-escape in order to be used as URI elements.
 *
 * Note that this class is intended to be general and support an arbitrary ArgList--it's up to
 * the clients of this class to consistently use a set of field names.
 */
public class ApxGroupingDataUriManager implements DataUriManager
{

    private final static String URI_SCHEME = "apxdata";
    private final static String URI_BASE   = URI_SCHEME + "://";            // must be followed by {domain}
    private final static String URI_PATH   = "/gid/";

    /**
     * The parsed ArgList used to create groupingID
     */
    private ArgsEvaluator argsEval;
    private ArgsEvaluator partitionEval;
    private String        domain;

    /**
     * Used to set up args evaluator
     */
    private FxEnvironment env;

    public void setEnvironment(FxEnvironment env)
    {
        this.env    = env;
        this.domain = env.getAttribute("apx.domain");
    }

    /**
     * These args are evaluated as needed to produce query keys, etc.  The order of the args given
     * in ArgList is 
     */
    public void setPrimaryKeyArgs(ArgList args, List<String> allowedKeys) throws Exception
    {
        if (this.env == null)
            throw new IllegalStateException("Setting context args requires setEnvironment to be called first");
        else if (argsEval != null)
            throw new IllegalStateException("Already initialized");
        else if (allowedKeys != null)
            throw new IllegalStateException("ApxGrouping DataURI doesn't support allowedKeys specification");

        this.argsEval = new ArgsEvaluator(args, env, Arrays.asList(new EnvironmentAccessor(), new RequestAccessor()), null);
    }

    /**
     * Set parsed partition eval that's used to form the partitionID when saving.
     */
    public void setPartitionArgs(ArgList args) throws Exception
    {
        if (this.env == null)
            throw new IllegalStateException("Setting partition args requires setEnvironment to be called first");
        else if (partitionEval != null)
            throw new IllegalStateException("Already initialized");
        else if (args.getArgsList().size() != 1)
            throw new IllegalStateException("Partition arglist must be size 1 but is " + args.getArgsList().size() + ": " + args);

        this.partitionEval = new ArgsEvaluator(args, env, Arrays.asList(new EnvironmentAccessor(), new RequestAccessor()), null);
    }

    /**
     * GroupingID form a DataURI doesn't require query keys
     */
    public QueryKeys makeQueryKeys(FxRequest req) throws Exception
    {
        return null;
    }

    /**
     * Create the raw groupingID for the request.
     */
    public String makeGroupingID(FxRequest req) throws Exception
    {
        List<Object>  vals = evalArgs(req);
        StringBuilder sb   = new StringBuilder();

        for (int i = 0, m = vals.size(); i < m; i++)
        {
            if (i > 0)
                sb.append("-");

            sb.append(URLEncoder.encode(vals.get(i).toString()));
        }

        return sb.toString();
    }

    /**
     * Form the partition ID from the previously set partitionEval.
     */
    public String makePartitionID(FxRequest req) throws Exception
    {
        if (partitionEval != null)
            return partitionEval.evaluate(req).get(0).toString();  // guaranteed to be size 1

        env.getLogger().warn("A partitionID was requested for FxRequest %s but no partitionEval was set", req);

        return null;
    }        

    /**
     * Create the URI that can be used to later retrieve the data
     */
    public URI makeDataURI(FxRequest req) throws Exception
    {
        String id = makeGroupingID(req);

        try
        {
            return new URI(URI_BASE + domain + URI_PATH + id);
        }
        catch (URISyntaxException x)
        {
            throw new IllegalArgumentException("Bad URI construction for dataURI with ID " + id , x);
        }
    }

    /**
     * Pull out the groupingID from a valid-to-this-scheme 
     */
    public String getGroupingID(URI dataURI)
    {
        String path = dataURI.getPath();

        if (!dataURI.getScheme().equals(URI_SCHEME))
            throw new IllegalArgumentException("ApxGrouping DataURI works only with scheme " + URI_SCHEME + " in " + dataURI);
        else if (!path.startsWith(URI_PATH))
            throw new IllegalArgumentException("ApxGrouping DataURI must have a path that begins with " + URI_PATH + " in " + dataURI);

        return path.substring(URI_PATH.length());
    }

    /**
     * Deconstruct the URI into its query keys and return a QueryKeys object
     */
    public QueryKeys getQueryKeys(URI dataURI) throws Exception
    {
        throw new IllegalStateException("ApxGrouping DataURI by definition doesn't have any query keys");
    }

    /**
     * Evaluate the list of args for the request
     */
    private List<Object> evalArgs(FxRequest req) throws Exception
    {
        return argsEval.evaluate(req);
    }

}
