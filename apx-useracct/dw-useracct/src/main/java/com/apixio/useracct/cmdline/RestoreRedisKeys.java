package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.RedisKeyDump;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;

/**
 */
public class RestoreRedisKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  RestoreRedisKeys -c <conn-yamlfile> -in <file> [-from <prefix> -to <prefix>] [-autoconfirm <y/n>]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       infile;        // required
        String       fromPrefix;    // optional
        String       toPrefix;      // optional
        boolean      autoconfirm;   // optional

        Options(String connectionYaml, String infile, String fromPrefix, String toPrefix, boolean autoconfirm)
        {
            this.connectionYamlFile = connectionYaml;
            this.infile             = infile;
            this.fromPrefix         = fromPrefix;
            this.toPrefix           = toPrefix;
            this.autoconfirm        = autoconfirm;
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

        restoreKeys(options);

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    /**
     *
     */
    private static void restoreKeys(Options options) throws Exception
    {
        RedisOps    redis     = sysServices.getRedisOps();
        Console     cs        = System.console();
        boolean     confirmed = false;
        Set<String> keys;

        if ((cs == null) && !options.autoconfirm)
        {
            System.out.println("Fatal error:  unable to get a console to confirm operation.\n");
            System.exit(1);
        }

        if (options.fromPrefix == null)
            options.fromPrefix = "";
        if (options.toPrefix == null)
            options.toPrefix = "";

        if (!options.autoconfirm)
        {
            cs.format("Confirm restore of keys in file %s, changing prefix from '%s' to '%s'.  Enter 'yes' to proceed:  ",
                      options.infile, options.fromPrefix, options.toPrefix);
            confirmed = cs.readLine().equals("yes");
        }

        if (options.autoconfirm || confirmed)
        {
            BufferedReader in = null;

            try
            {
                int    count = 0;
                String line;

                in = new BufferedReader(new FileReader(options.infile));

                while ((line = in.readLine()) != null)
                {
                    count++;
                    RedisKeyDump.restoreKey(redis, line, options.fromPrefix, options.toPrefix);
                }

                System.out.println("Processed " + count + " lines");
            }
            catch (Exception x)
            {
                x.printStackTrace();
                throw x;
            }
            finally
            {
                in.close();
            }
        }
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  infile     = ps.getOptionArg("-in");
        String  fromPrefix = ps.getOptionArg("-from");
        String  toPrefix   = ps.getOptionArg("-to");
        String  autoc      = ps.getOptionArg("-autoconfirm");

        if ((connection == null) || (infile == null) || ((infile = infile.trim()).length() == 0))
            return null;

        return new Options(connection, infile, fromPrefix, toPrefix, "y".equals(autoc));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
