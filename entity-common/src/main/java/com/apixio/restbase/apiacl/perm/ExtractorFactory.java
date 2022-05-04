package com.apixio.restbase.apiacl.perm;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.perm.exts.ConstExtractor;
import com.apixio.restbase.apiacl.perm.exts.HttpMethodExtractor;
import com.apixio.restbase.apiacl.perm.exts.JsonPathExtractor;
import com.apixio.restbase.apiacl.perm.exts.NullExtractor;
import com.apixio.restbase.apiacl.perm.exts.QueryParamExtractor;
import com.apixio.restbase.apiacl.perm.exts.TokenExtractor;
import com.apixio.restbase.apiacl.perm.exts.UrlElementExtractor;

/**
 * ExtractorFactory is responsible for converting from a text-based declaration of type
 * and per-type configuration to the correct Java class that implements the extraction
 * at runtime.
 *
 * This declaration can be either a single item or a list of semicolon-separated list of
 * items.  The list will begin with "[" and must end with "]".
 *
 * Some examples:
 *
 *    class:com.apixio.stuff.SomeExtractor
 *    [url-element:{docUUID}; class:com.apixio.utility.OrgFromDocument]
 *    query-param:patientUUID
 *    json-path:document.summary.someField
 *
 * Each predefined extractor could use just the "class:" form, as follows:
 *
 *    class:com.apixio.restbase.apiacl.perm.exts.UrlElementExtractor:{docUUID}
 *
 * This is allowed as everything after the second ":" is used by the extractor class.
 *
 * A list of items still will produce a single extracted value; the operation with the
 * list is to chain extraction calls together so that the output of one is used as the
 * input to the next.  This is needed to support the ability to translate a documentUUID
 * (for example) to a CustomerID (which can then be used when testing if the current
 * user has rights to that customer).
 */
class ExtractorFactory {

    public final static String EXTR_CLASS      = "class";           // syntax:  class:<class>[:<extraConfig>]
    public final static String EXTR_CONST      = "const";           // syntax:  const:<value>
    public final static String EXTR_HTTPMETHOD = "http-method";     // syntax:  http-method:<METHOD>=<string>,...
    public final static String EXTR_JSONPATH   = "json-path";       // syntax:  json-path:<dotted-path>
    public final static String EXTR_NULL       = "null";            // syntax:  <>
    public final static String EXTR_QUERYPARAM = "query-param";     // syntax:  query-param:<pname>
    public final static String EXTR_TOKEN      = "token";           // syntax:  token:
    public final static String EXTR_URLELEMENT = "url-element";     // syntax:  url-element:<pattern>  (pattern is like: {id1}...{id2}...)

    /**
     * Maps from simple name of extractor to actual Class (for newInstance()).
     */
    private static Map<String, Class<? extends Extractor>> factories = new HashMap<String, Class<? extends Extractor>>();

    static
    {
        factories.put(EXTR_QUERYPARAM, QueryParamExtractor.class);
        factories.put(EXTR_CONST,      ConstExtractor.class);
        factories.put(EXTR_HTTPMETHOD, HttpMethodExtractor.class);
        factories.put(EXTR_JSONPATH,   JsonPathExtractor.class);
        factories.put(EXTR_NULL,       NullExtractor.class);
        factories.put(EXTR_TOKEN,      TokenExtractor.class);
        factories.put(EXTR_URLELEMENT, UrlElementExtractor.class);
    }

    /**
     * Given extractor configuration, create the runtime representation of it.  This representation
     * will always be a list of Extractors since a single extraction could require a sequence of
     * calls to the primitive extractors (e.g., the JSON object has a patientUUID that needs to
     * be translated into an OrgID because the ACL is applied to the OrgID).
     */
    static List<Extractor> makeExtractor(InitInfo initInfo, ApiDef api, String typeConfig)
    {
        List<Extractor> extractors = new ArrayList<Extractor>();
        List<String>    configs    = new ArrayList<String>();

        typeConfig = typeConfig.trim();

        if (isList(typeConfig))
            separateConfigs(typeConfig, configs);
        else
            configs.add(typeConfig);

        for (String element : configs)
        {
            int       co     = element.indexOf(':');
            String    type   = "";
            String    config = "";
            Extractor ex;

            // split element into stuff before the ":" (the type) and stuff after the ":" (the config).
            // note that the config itself (after splitting) can have another ":" in it, as needed
            // by the type-specific config syntax.

            if (co != -1)
            {
                type = element.substring(0, co);

                if (co + 1 < element.length())
                    config = element.substring(co + 1);
            }
            else
            {
                type   = EXTR_CONST;
                config = element;
            }

            //            System.out.println("  element=" + element + "; type=" + type + ", config=" + config);

            if ((ex = createExtractor(initInfo, api, type, config)) != null)
                extractors.add(ex);
        }

        return extractors;
    }

    /**
     * Create an extractor given the type name and its configuration.  If the type is a generic
     * class, then we split on a second colon so that the arbitrary class itself can receive
     * configuration.  Note that this arbitrary class must extend Extractor).
     */
    private static Extractor createExtractor(InitInfo initInfo, ApiDef api, String type, String config)
    {
        Extractor extractor = null;

        try
        {
            Class<? extends Extractor> factory;

            if (type.equals(EXTR_CLASS))
            {
                int    co   = config.indexOf(':');
                String extra = "";

                if (co != -1)
                {
                    if (co + 1 < config.length())
                        extra = config.substring(co + 1);
                    config = config.substring(0, co);
                }

                factory = (Class<? extends Extractor>) Class.forName(config);    // this must be recursive...
                config = extra;
            }
            else
            {
                factory = factories.get(type);
            }

            if (factory != null)
            {
                Extractor ex = factory.newInstance();

                ex.init(initInfo, api, config);

                return ex;
            }
            else
            {
                System.out.println("APIACL config error:  unknown extractor [" + type + "]");
            }
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }

        return extractor;
    }

    /**
     * Returns true if the master config string has the syntax of a list of configs.
     */
    private static boolean isList(String configs)
    {
        return configs.startsWith("[");
    }

    /**
     * Splits the master list of configs into n separate configs, each of which has
     * the syntax of type:config.
     */
    private static void separateConfigs(String configs, List<String> asList)
    {
        if (!configs.endsWith("]"))
            throw new IllegalArgumentException("Extractor list MUST end with ']':  " + configs);

        int           pos = 0;
        StringBuilder sb  = new StringBuilder();
        boolean       esc;       // was last char a backslash?
        int           end;

        configs = configs.substring(1).trim();  // get rid of "["

        end = configs.length();
        esc = false;

        while (pos < end)
        {
            char c = configs.charAt(pos);

            if (pos + 1 == end)  // this allows embedded "]" characters
            {
                asList.add(sb.toString().trim());
                break;
            }
            else if ((c == ';') && !esc)
            {
                // end of element
                asList.add(sb.toString().trim());
                sb = new StringBuilder();
            }
            else
            {
                if (!(esc = (c == '\\')))
                    sb.append(c);
            }

            pos++;
        }
    }


    /**
     * Testing only
     */
    public static void main(String[] args)
    {
        ApiDef api = new ApiDef("myid", "myname", "get:/abc");

        for (String a : args)
        {
            List<Extractor> es = makeExtractor(null, api, a);
            System.out.println(a + " => " + es);
        }
    }

}
