package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.RedisKeyDump;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;

/**
 * Produces output that can be fed into RestoreRedisKeys.  Typical usage is to redirect
 * stdout to a file.  Each key is on its own line.
 */
public class DumpRedisKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  DumpRedisKeys -c <conn-yamlfile> -regex <regex> [-autoconfirm <y/n>] [-out <file>]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       regex;
        String       outfile;
        boolean      autoconfirm;

        Options(String connectionYaml, String regex, String outfile, boolean autoconfirm)
        {
            this.connectionYamlFile = connectionYaml;
            this.regex              = regex;
            this.outfile            = outfile;
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

        try
        {
            sysServices = setupServices(config);

            dumpKeys(options);
        }
        finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }
    }

    /**
     *
     */
    private static void dumpKeys(Options options) throws Exception
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

        keys = redis.keys(options.regex);

        System.out.println("SFM:  # of keys = " + keys.size());

        if (!options.autoconfirm)
        {
            cs.format("Confirm dump of %d keys with regex %s.  Enter 'yes' to proceed:  ",
                      keys.size(), options.regex);
            confirmed = cs.readLine().equals("yes");
        }

        if (options.autoconfirm || confirmed)
        {
            FileWriter out = null;

            try
            {
                if (options.outfile != null)
                    out = new FileWriter(options.outfile);

                for (String key : keys)
                {
                    try
                    {
                        String line = RedisKeyDump.dumpKey(redis, key);

                        if (out == null)
                        {
                            System.out.println(line);
                        }
                        else
                        {
                            out.write(line);
                            out.write("\n");
                        }
                    }
                    catch (Exception x)
                    {
                        // do NOT abort dump as we expect short-lived tokens (e.g.) to not be found
                    }
                }
            }
            catch (Exception x)
            {
                x.printStackTrace();
                throw x;
            }
            finally
            {
                if (out != null)
                {
                    out.flush();
                    out.close();
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
        String  regex      = ps.getOptionArg("-regex");
        String  outfile    = ps.getOptionArg("-out");
        String  autoc      = ps.getOptionArg("-autoconfirm");

        if ((connection == null) || (regex == null) || ((regex = regex.trim()).length() == 0))
            return null;

        return new Options(connection, regex, outfile, "y".equals(autoc));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
