package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;

/**
 */
public class DeleteRedisKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  DeleteRedisKeys -c <conn-yamlfile> [-l] <key-regex>\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       regex;
        boolean      listOnly;

        Options(String connectionYaml, String regex, boolean listOnly)
        {
            this.connectionYamlFile = connectionYaml;
            this.regex              = regex;
            this.listOnly           = listOnly;
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

        deleteKeys(options);

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    /**
     *
     */
    private static void deleteKeys(Options options)
    {
        RedisOps    redis = sysServices.getRedisOps();
        Set<String> keys  = redis.keys(options.regex);

        System.out.println(keys.size() + " keys matching " + options.regex);

        if (options.listOnly)
        {
            for (String key : keys)
                System.out.println(key);
        }
        else
        {
            Console cs = System.console();

            if (cs == null)
            {
                System.out.println("Fatal error:  unable to get a console to prompt for username/password.\n");
                System.exit(1);
            }

            cs.format("\nConfirm deletion of these keys with 'yes':  ");

            if (cs.readLine().equals("yes"))
            {
                Transactions trans = redis.getTransactions();

                trans.begin();

                try
                {
                    for (String key : keys)
                        redis.del(key);

                    trans.commit();
                }
                catch (Exception x)
                {
                    trans.abort();
                }
            }
        }
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        boolean listOnly   = ps.getOptionFlag("-l");
        String  regex      = ps.whatsLeft();         // must be last

        if ((connection == null) || (regex == null) || (regex.trim().length() == 0))
            return null;

        return new Options(connection, regex, listOnly);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
