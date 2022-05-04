package com.apixio.sdk.util;

import java.net.URI;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;

/**
 * Proceses the yaml publishing-info file for use during publishing.
 *
 * The structure of a "publish" yaml file is:
 *
 *    meta:                              # this is translated into ModelCombination metadata
 *      name: "string"
 *      functionId: "xuuid"              # functionId OR functionIdl must be specified; pref is given to functionId
 *      functionDef:                     # functionId OR functionDef must be specified; pref is given to functionId
 *        idl: "idl..."
 *        creator: "emailaddr"
 *        name: "fn-name"
 *        description: "fn-desc"         # optional within functionDef
 *      entry:  "java classname"
 *      logicalId:  "string"
 *      # not usually specified:
 *      executor: "string"               # defaults to ""
 *      outputType: "string"             # defaults to ""
 *      product: "string"                # defaults to ""
 *
 *    assets:                           # this is translated into "parts" on the MC
 *      implementation.jar:
 *        uri: "uri"
 *
 *    dependencies:                     # this is translated into logical dependencies on the MC
 *      - "string"
 *        ...
 *
 *    core:
 *      ...arbitraryStructure...
 *
 *    search:
 *      ...arbitraryStructure...
 *
 * and the methods below reflect this structure
 */
public class PublishYaml
{

    /**
     * Top level YAML keys for the supported sections
     */
    private final static String TLK_META   = "meta";
    private final static String TLK_ASSETS = "assets";
    private final static String TLK_DEPS   = "dependencies";
    private final static String TLK_CORE   = "core";
    private final static String TLK_SEARCH = "search";

    /**
     *
     */
    public static class FunctionDef
    {
        public String idl;
        public String name;
        public String description;
        public String creator;

        public FunctionDef(String idl, String name, String description, String creator)
        {
            this.idl         = idl;
            this.name        = name;
            this.description = description;
            this.creator     = creator;
        }
    }

    /**
     * Formalization of what's under the TLK_META key
     */
    public static class Meta
    {
        /**
         * A number of these map directly to MC metadata, and some are specially handled
         */
        public String      name;
        public XUUID       fxid;
        public FunctionDef fnDef;
        public String      entry;
        public String      logicalID;
        public String      executor;
        public String      outputType;
        public String      product;

        public Meta(String name, XUUID fxid, FunctionDef fnDef, String entry, String logicalID,
                    String executor, String outputType, String product)
        {
            this.name       = name;
            this.fxid       = fxid;
            this.fnDef      = fnDef;
            this.entry      = entry;
            this.logicalID  = logicalID;
            this.executor   = executor;
            this.outputType = outputType;
            this.product    = product;
        }

        @Override
        public String toString()
        {
            return ("meta(" +
                    "name=" + name +
                    ", fxid=" + fxid +
                    ", fxdef=" + fnDef +
                    ", entry=" + entry +
                    ", logicalID=" + logicalID +
                    ", executor=" + executor +
                    ", outputType=" + outputType +
                    ", product=" + product +
                    ")");
        }
    }

    /**
     * Formalization of what's under the TLK_ASSETS key
     */
    public static class Asset
    {
        public String name;
        public URI    uri;
        public String mimeType;

        public Asset(String name, URI uri, String mimeType)
        {
            this.name     = name;
            this.uri      = uri;
            this.mimeType = mimeType;
        }

        @Override
        public String toString()
        {
            return ("asset(" +
                    "name=" + name +
                    ", uri=" + uri +
                    ", mimeType=" + mimeType +
                    ")");
        }
    }

    private MacroedYaml yaml;

    /**
     * Create a new PublishYaml from the contents of the given local file
     */
    public static PublishYaml readYaml(String filepath) throws Exception
    {
        PublishYaml py = new PublishYaml();

        py.yaml = MacroedYaml.readYaml(filepath);

        return py;
    }

    /**
     *
     */
    public Meta getMeta()
    {
        Object val = yaml.findValue(TLK_META);

        if (val == null)
            throw new IllegalStateException("Missing required YAML key '" + TLK_META + "'");
        else if (!(val instanceof Map))
            throw new IllegalStateException("YAML key '" + TLK_META + "' must be a map");

        Map<String,Object> map   = (Map<String,Object>) val;
        String             fxid  = optionalString(map, "functionId", null);
        FunctionDef        fnDef = getFunctionDef(map);

        if ((fxid == null) && (fnDef == null))
            throw new IllegalStateException("One of functionId or functionDef: must be specified");

        return new Meta(requireString(map,  "name"),
                        XUUID.fromString(fxid),
                        fnDef,
                        requireString(map,  "entry"),
                        requireString(map,  "logicalId"),
                        optionalString(map, "executor", ""),
                        optionalString(map, "outputType", ""),
                        optionalString(map, "product", ""));
    }

    /**
     * Gets the optional functionDef: section of the yaml, converting it to an instance of FunctionDef
     * if the top key exists
     */
    private FunctionDef getFunctionDef(Map<String,Object> yaml)
    {
        Map<String,Object> fnDef = (Map<String,Object>) MacroedYaml.fromMap(yaml, false).findValue("functionDef");

        if (fnDef != null)
        {
            return new FunctionDef(requireString(fnDef, "idl"),         requireString(fnDef, "name"),
                                   requireString(fnDef, "description"), requireString(fnDef, "creator"));
        }
        else
        {
            return null;
        }
    }

    /**
     *
     */
    public List<Asset> getAssets() throws Exception
    {
        Object val = yaml.findValue(TLK_ASSETS);

        if (val == null)
            throw new IllegalStateException("Missing required YAML key '" + TLK_ASSETS + "'");
        else if (!(val instanceof Map))
            throw new IllegalStateException("YAML key '" + TLK_ASSETS + "' must be a map");

        List<Asset> assets = new ArrayList<>();

        for (Map.Entry<String,Object> entry : ((Map<String,Object>) val).entrySet())
        {
            String              key = entry.getKey();
            Map<String,Object>  asset;

            val = entry.getValue();

            if (!(val instanceof Map))
                throw new IllegalArgumentException("YAML value for key '" + TLK_ASSETS + "." + key + "' must be a map");

            asset = (Map<String,Object>) val;

            assets.add(new Asset(key, new URI(requireString(asset, "uri")), optionalString(asset, "mimeType", "application/octet-stream")));
        }

        return assets;
    }

    /**
     *
     */
    public List<String> getDependencies()
    {
        Object val = yaml.findValue(TLK_DEPS);

        if ((val != null) && (!(val instanceof List)))
            throw new IllegalStateException("YAML key '" + TLK_DEPS + "' must be a list of strings but is type " + val.getClass());

        return (val != null) ? ((List<String>) val) : Collections.emptyList();
    }

    /**
     * Return value must somehow be convertible to JSON
     */
    public Map<String,Object> getCore()
    {
        return (Map<String,Object>) yaml.findValue(TLK_CORE);
    }

    /**
     * Return value must somehow be convertible to JSON
     */
    public Map<String,Object> getSearch()
    {
        return (Map<String,Object>) yaml.findValue(TLK_SEARCH);
    }

    /**
     * throws exception on invalid structure
     */
    private void verifyStructure()
    {
        // just check top level keys:
        //  meta, assets (or blobs), core, search
    }

    private String requireString(Map<String,Object> map, String key)
    {
        Object val = map.get(key);

        if ((val == null) || !(val instanceof String))
            throw new IllegalArgumentException("Expecting a non-empty subkey of '" + key + "' but got " + val);

        return ((String) val).trim();
    }

    private String optionalString(Map<String,Object> map, String key, String defValue)
    {
        Object val = map.get(key);

        if (val != null)
        {
            if (!(val instanceof String))
                throw new IllegalArgumentException("Expecting a string subkey of '" + key + "' but got " + val);
            else
                return ((String) val).trim();
        }
        else
        {
            return defValue;
        }
    }

}
