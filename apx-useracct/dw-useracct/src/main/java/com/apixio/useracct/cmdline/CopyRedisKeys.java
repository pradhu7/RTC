package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;

/**
 */
public class CopyRedisKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  CopyRedisKeys -c <conn-yamlfile> -from <prefix> -to <prefix>\n\n");

    /**
     * Lua 5.1 script to perform copy.  From https://gist.github.com/itamarhaber/d30b3c40a72a07f23c70 (modified to deal with TTL)
     */
    private final static String LUA_COPYKEYS = (
        "local s = KEYS[1]\n" + 
        "local d = KEYS[2]\n" +
        "local ttl = redis.call(\"PTTL\", s)\n" +
        "\n" +
        "if redis.call(\"EXISTS\", d) == 1 then\n" +
        "  redis.call(\"DEL\", d)\n" +
        "end\n" +
        "\n" +
        "if ttl < 0 then ttl = 0 end\n" +
        "\n" +
        "redis.call(\"RESTORE\", d, ttl, redis.call(\"DUMP\", s))\n"
        );

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       fromPrefix;
        String       toPrefix;

        Options(String connectionYaml, String fromPrefix, String toPrefix)
        {
            this.connectionYamlFile = connectionYaml;
            this.fromPrefix         = fromPrefix;
            this.toPrefix           = toPrefix;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private static PrivSysServices   sysServices;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options            options   = parseOptions(new ParseState(args));
        ConfigSet          config    = null;

        if ((options == null) ||
            ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        sysServices = setupServices(config);

        copyKeys(options);

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    /**
     *
     */
    private static void copyKeys(Options options)
    {
        RedisOps    redis = sysServices.getRedisOps();
        Console     cs    = System.console();
        int         frLen = options.fromPrefix.length();
        Set<String> keys;

        if (cs == null)
        {
            System.out.println("Fatal error:  unable to get a console to confirm operation.\n");
            System.exit(1);
        }

        keys = redis.keys(options.fromPrefix + "*");

        cs.format("Confirm copy of %d keys with prefix %s to prefix %s.  Enter 'yes' to proceed:  ",
                  keys.size(), options.fromPrefix, options.toPrefix);

        if (cs.readLine().equals("yes"))
        {
            for (String key : keys)
            {
                String dkey = options.toPrefix + key.substring(frLen);

                redis.eval(LUA_COPYKEYS, Arrays.asList(new String[] {key, dkey}), null);
            }
        }
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  from       = ps.getOptionArg("-from");
        String  to         = ps.getOptionArg("-to");

        if ((connection == null) || (from == null) || ((from = from.trim()).length() == 0) ||
            (to == null) || ((to = to.trim()).length() == 0))
            return null;

        return new Options(connection, from, to);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
