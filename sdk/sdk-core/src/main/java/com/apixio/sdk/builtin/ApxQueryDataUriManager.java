package com.apixio.sdk.builtin;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.sdk.ArgsEvaluator;
import com.apixio.sdk.DataUriManager;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.protos.EvalProtos.ArgList;

/**
 * ApxQueryDataUriManager deals with data URIs that identify the data via query parameters in
 * the URI that define the ApxDataDao's QueryKeys values
 *
 * The structure of such a URI is:
 *
 *  apxquery://{domain}/q?field=value&...
 *
 * where {domain} is something like [staging|production] and the set of field=value pairs form
 * the QueryKeys, which is then used to retrieve the data.  In the context of the ApxDataDao
 * system, this would mean that the groupingID is retrieved for that QueryKeys set and then
 * that's used to retrieve the data.
 *
 * Domain is taken from FxEnvironment as the attribute value of "apx.domain".
 *
 * The "context args" is just an ArgList that is evaluated against the environment+request when
 * any of the 3 type of data ref is required.  There is an automatically supplied accessor for
 * each of those sources of data:  the accessor 'environment(string)' is used for pulling
 * string values from the FxEnvironment, and the accessor 'request(string)' is used for
 * pulling string values from the FxRequest.  So, a valid argEval string would be:
 *
 *    environment("mcid"),request("docuuid")
 *
 * Instances of ApxQueryDataUriManager MUST have the allowed/known set of field names (or query key
 * names, etc.) defined as it's this set that dictates what values are pulled from context and
 * it uses them to form the groupingID.  For example, using the above argEval, two reasonable
 * field names could be "mcid" and "doc".  The assignment of the evaluated value to these
 * known field names is done by matching the List<Object> from the evaluation with the List<String>
 * of known names; any mismatch will cause an exception to be thrown.
 *
 * Note that this class is intended to be general and support an arbitrary set of query keys--
 * it's up to the clients of this class to consistently use a set of field names.
 */
public class ApxQueryDataUriManager implements DataUriManager
{

    private final static String URI_SCHEME = "apxquery";
    private final static String URI_BASE   = URI_SCHEME + "://";           // must be followed by {domain}
    private final static String URI_PATH   = "/q?";

    /**
     * These MUST be the same size as the values for each element of allowedKeys is taken
     * from the evaluation of the Arg in the corresponding index of the ArgList.
     */
    private List<String>  allowedKeys;
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
        else if (this.argsEval != null)
            throw new IllegalStateException("Already initialized");

        this.argsEval    = new ArgsEvaluator(args, env, Arrays.asList(new EnvironmentAccessor(), new RequestAccessor()), null);
        this.allowedKeys = allowedKeys;
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
     * Produces the QueryKeys that are to be added when saving data.  An implicit "and"
     * operation must be done when querying with the same query keys on reading.
     */
    public QueryKeys makeQueryKeys(FxRequest req) throws Exception
    {
        List<Object>   vals = evalArgs(req);
        QueryKeys      qk   = new QueryKeys();

        for (int i = 0, m = vals.size(); i < m; i++)
            qk.with(allowedKeys.get(i), vals.get(i));

        return qk;
    }

    /**
     * Operational assumption is that the ECC/SDK will use the results of makeQueryKeys() and makeGroupingID()
     * for in the call to ApxDataDao.putData().  Doing so will allow getGroupingID(URI) to work.
     */
    public String makeGroupingID(FxRequest req) throws Exception
    {
        List<Object>  vals = evalArgs(req);
        StringBuilder sb   = new StringBuilder();

        for (int i = 0, m = vals.size(); i < m; i++)
        {
            if (i > 0)
                sb.append("-");

            sb.append(allowedKeys.get(i));
            sb.append(":");
            sb.append(vals.get(i));
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
        List<Object>        vals = evalArgs(req);
        Map<String,String>  map = new HashMap<>();

        for (int i = 0, m = vals.size(); i < m; i++)
            map.put(allowedKeys.get(i), vals.get(i).toString());

        return makeDataURI(domain, map);
    }

    /**
     * Return null as this URI doesn't directly record groupingID.  This requires that we
     * return valid query keys for the given dataURI.
     * @throws Exception
     */
    public String getGroupingID(URI dataURI)
    {
        validateScheme(dataURI);

        return null;
    }

    /**
     * Deconstruct the URI into its query keys and return a QueryKeys object
     */
    public QueryKeys getQueryKeys(URI dataURI) throws Exception
    {
        validateScheme(dataURI);

        Map<String,List<String>> qp = decodeQueryParams(dataURI.getQuery());
        QueryKeys                qk = new QueryKeys();

        for (Map.Entry<String,List<String>> entry : qp.entrySet())
        {
            List<String> val = entry.getValue();

            if (val.size() != 1)
                throw new IllegalStateException("Data URI expected to have a single value for key " + entry.getKey());

            qk.with(entry.getKey(), val.get(0));
        }

        return qk;
    }

    /**
     * The keys in the map MUST match what is declared to be the allowed/known set of query keys when
     * the method makeQueryKeys is called with an initialized instance of this class.
     */
    public static URI makeDataURI(String domain, Map<String,String> queryKeys)
    {
        StringBuilder sb  = new StringBuilder();
        boolean       amp = false;

        System.out.println("SFM makeDataURI(" + domain + ", " + queryKeys);

        for (Map.Entry<String,String> entry : queryKeys.entrySet())
        {
            if (amp)
                sb.append("&");
            else
                amp = true;

            sb.append(URLEncoder.encode(entry.getKey()));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue()));
        }

        try
        {
            return new URI(URI_BASE + domain + URI_PATH + sb.toString());
        }
        catch (URISyntaxException x)
        {
            throw new IllegalArgumentException("Bad URI construction for dataURI with map " + queryKeys, x);
        }
    }

    /**
     * Check that it's a URI that we understand
     */
    protected void validateScheme(URI dataURI)
    {
        if (!dataURI.getScheme().equals(URI_SCHEME))
            throw new IllegalArgumentException("ApxQuery DataURI works only with scheme " + URI_SCHEME + " in " + dataURI);
    }

    /**
     * Evaluate the list of args for the request
     */
    private List<Object> evalArgs(FxRequest req) throws Exception
    {
        List<Object> vals = argsEval.evaluate(req);

        if (vals.size() != allowedKeys.size())
            throw new IllegalStateException("Known key size != evaluated args size:  " + allowedKeys + " :: " + vals);

        return vals;
    }

    // from stackoverflow because i didn't want to pull in apache just for parsing
    public static Map<String, List<String>> decodeQueryParams(String query) throws UnsupportedEncodingException
    {
        final Map<String, List<String>> queryPairs = new LinkedHashMap<String, List<String>>();

        for (String pair : query.split("&"))
        {
            if (pair.length() > 0)
            {
                int    idx = pair.indexOf("=");
                String key = (idx > 0) ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;

                if (!queryPairs.containsKey(key))
                    queryPairs.put(key, new LinkedList<String>());

                queryPairs.get(key).add(
                    (idx > 0 && pair.length() > idx + 1) ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null);
            }
        }

        return queryPairs;
    }

}
