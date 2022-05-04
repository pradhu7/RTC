package com.apixio.sdk.cmdline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.apixio.datasource.s3.S3Ops;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.logging.DefaultLogger;
import com.apixio.sdk.util.AllUriFetcher.BasicAuthInfo;
import com.apixio.sdk.util.AllUriFetcher;
import com.apixio.sdk.util.CacheUtil;
import com.apixio.sdk.util.MacroedYaml;
import com.apixio.sdk.util.McsUtil;
import com.apixio.sdk.util.PublishUtil;
import com.apixio.sdk.util.PublishYaml;
import com.apixio.sdk.util.S3Util;
import com.apixio.security.Security;
import com.apixio.util.UriFetcher;

/**
 * Java-based command-line publishing of f(x) implementations.  The details of publishing are
 * taken from the mc.publish (yaml) file specified while some behaviors are specified by
 * command line arguments:
 *
 *  config:    the local filepath to the yaml config that contains s3 access and mcs server info
 *  publish:   the local filepath to the mc.publish file that contains info re publishing
 *  cachedir:  optional override for the local filesystem directory to use for caching assets fetched
 *  mcs:       optional override for MCS server server
 *
 * The structure of the config.yaml is:
 *
 *  cache:
 *    directory:  /tmp/s3cache
 *  mcs:
 *    serverUrl:  https://server:port/   # required
 *    connectTimeoutMs:  nnn             # optional; timeout used when connection is being established; default is 10s
 *    connectionRequestTimeoutMs:  nnn   # optional; timeout used when getting connection from connection manager; default is 10s
 *    socketTimeoutMs:  nnn              # optional; timeout used when waiting for data on a socket; default is 5s
 *  s3:
 *    accessKeyUnencrypted:
 *    secretKeyUnencrypted:
 *    accessKey:
 *    secretKey:
 *
 * Due to the S3Connector code the usual VAULT_TOKEN environment variable must exist, etc.
 *
 * The intent of separating config.yaml from mc.publish (a yaml file) is that one should be able to
 * use the same config.yaml to publish many f(x) impls.
 */
public class PublishFxImpl extends Cmdbase
{
    private static class Args
    {
        String creator;       // email address of person who's publishing
        String configPath;
        String publishPath;
        String cacheDir;      // override
        String mcsServerUrl;  // override

        Args(String creator, String configPath, String publishPath, String cacheDir, String mcsServerUrl)
        {
            this.creator      = creator;
            this.configPath   = configPath;
            this.publishPath  = publishPath;
            this.cacheDir     = cacheDir;
            this.mcsServerUrl = mcsServerUrl;
        }

        @Override
        public String toString()
        {
            return ("args(" +
                    "creator=" + creator +
                    ", configPath=" + configPath +
                    ", publishPath=" + publishPath +
                    ", cachedir=" + cacheDir +
                    ", mcsServerUrl=" + mcsServerUrl +
                    "):");
        }
    }

    /**
     * Fields
     */
    private Security security;   // !! TODO ?? non-null only if we need to decrypt S3 keys

    /**
     *
     */
    public static void main(String... args) throws Exception
    {
        PublishFxImpl cmd = new PublishFxImpl();
        Args          pargs;

        if ((pargs = cmd.parseArgs(args)) == null)
        {
            usage();
        }
        else
        {
            ConfigSet   config = ConfigSet.fromMap(MacroedYaml.readYaml(pargs.configPath).getYaml());
            PublishYaml py     = PublishYaml.readYaml(pargs.publishPath);

            cmd.publish(pargs.creator, config, py, pargs.cacheDir, pargs.mcsServerUrl);

            System.out.println("Meta:  " + py.getMeta());
            System.out.println("Dependencies:  " + py.getDependencies());
            System.out.println("Assets:  " + py.getAssets());
            //            System.out.println("Core:  " + py.getCore());
            //            System.out.println("Search:  " + py.getSearch());
        }
    }

    private Args parseArgs(String[] args)
    {
        List<String> largs = toModifiableList(args);

        try
        {
            return new Args(
                require(largs, "creator"),
                require(largs, "config"),
                require(largs, "publish"),
                optional(largs, "cachedir"),
                optional(largs, "mcs"));
        }
        catch (IllegalArgumentException x)
        {
            System.err.println(x.getMessage());

            return null;
        }
    }

    /**
     *
     */
    private void publish(String creator, ConfigSet config, PublishYaml py,
                         String cacheDir, String mcsServerUrl                // cmdline overrides
        ) throws Exception
    {
        PublishUtil pu = new PublishUtil(new DefaultLogger(),
                                         CacheUtil.makeCacheManager(
                                             new CacheUtil.CacheInfo(config, cacheDir),
                                             makeFetcher(S3Util.makeS3Ops(S3Util.S3Info.fromConfig(config)), makeBasicAuths(config))),
                                         new McsUtil.McsServerInfo(config, mcsServerUrl));

        pu.publish(creator, py);
    }

    private UriFetcher makeFetcher(S3Ops s3Ops, List<BasicAuthInfo> basicAuths)
    {
        return new AllUriFetcher(s3Ops, basicAuths);
    }

    /**
     * BasicAuths list is used to fetch URIs that are accessible via HTTP BasicAuth and the use of them
     * is triggered by matching on the URI prefix given.
     *
     * The yaml structure is generalized to work with a list of domains (URL prefixes) but due to
     * limitations of ConfigSet and lists, the actual structure requires that the "basicauths"
     * key be a list of strings, where each string is the name of a top-level yaml key that has
     * underneath it a set of name:value pairs to declare uriPrefix, username, and password
     */
    private List<BasicAuthInfo> makeBasicAuths(ConfigSet config)
    {
        List<BasicAuthInfo> auths = new ArrayList<>();
        List<Object>        list;

        if ((list = config.getList("basicauths")) != null)   // null means no key
        {
            for (Object id : list)
            {
                if (!(id instanceof String))
                {
                    System.out.println("Skipping basicauth id '" + id + "' as it's not a string");
                }
                else
                {
                    ConfigSet auth = config.getConfigSubtree((String) id);

                    if (auth == null)
                        System.out.println("Skipping basicauth id '" + id + "' as there's no yaml key for it");
                    else
                        auths.add(makeBasicAuth(auth));
                }
            }
        }

        return auths;
    }

    private BasicAuthInfo makeBasicAuth(ConfigSet config)
    {
        return new BasicAuthInfo(config.getString("uriPrefix"),
                                 config.getString("username"),
                                 config.getString("password"));
    }

    private static String getConfigString(ConfigSet config, String key, String override)
    {
        if (override == null)
            override = config.getString(key);

        if ((override == null) || ((override = override.trim()).length() == 0))
            throw new IllegalArgumentException("Configuration value for key " + key + " is null/empty");

        return override;
    }

    private static void usage()
    {
        System.out.println("Usage:  PublishFxImpl \\\n" +
                           " creator={emailaddr} config={yaml} publish={filepath} [cachedir={dir}] [mcs={serverUrl}]");
    }

}
