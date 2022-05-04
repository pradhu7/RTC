package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.datasource.cassandra.*;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;

/**
 */
public class DeleteCassKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  DeleteCassKeys -c <conn-yamlfile> -colfam <columnFamily> [-l] [-autoconfirm <y/n>] -regex <key-regex>\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       columnFamily;
        String       regex;
        boolean      listOnly;
        boolean      autoconfirm;

        Options(String connectionYaml, String colfam, String regex, boolean listOnly, boolean autoconfirm)
        {
            this.connectionYamlFile = connectionYaml;
            this.columnFamily       = colfam;
            this.regex              = regex;
            this.listOnly           = listOnly;
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

            deleteKeys(options);
        }
        catch (Exception x)
        {
            x.printStackTrace();
            throw x;
        }
        finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }
    }

    /**
     *
     */
    private static void deleteKeys(Options options) throws Exception
    {
        CqlCrud     cqlCrud = sysServices.getCqlCrud();
        Set<String> keys    = getMatchingRowkeys(options);

        System.out.println(keys.size() + " keys matching " + options.regex);

        if (options.listOnly)
        {
            for (String key : keys)
                System.out.println(key);
        }
        else
        {
            boolean confirmed = false;
            Console cs        = System.console();

            if ((cs == null) && !options.autoconfirm)
            {
                System.out.println("Fatal error:  unable to get a console to prompt for username/password.\n");
                System.exit(1);
            }

            if (!options.autoconfirm)
            {
                cs.format("\nConfirm deletion of these keys with 'yes':  ");
                confirmed = cs.readLine().equals("yes");
            }

            if (options.autoconfirm || confirmed)
            {
                CqlTransactionCrud ctc = (CqlTransactionCrud) sysServices.getCqlCrud();

                ctc.setBatchSyncEnabled(true);

                ctc.beginTransaction();

                for (String key : keys)
                    ctc.deleteRow(options.columnFamily, key, false);

                ctc.commitTransaction();
            }
        }
    }

    /**
     *
     */
    private static Set<String> getMatchingRowkeys(Options options) throws Exception
    {
        Pattern     regex   = Pattern.compile(options.regex);
        Set<String> matches = new HashSet<>();

        System.out.println("pattern = [" + options.regex + "]");

        for (Iterator<String> iter = sysServices.getCqlCrud().getAllKeys(options.columnFamily); iter.hasNext(); )
        {
            String rowkey = iter.next();

            if (regex.matcher(rowkey).matches())
                matches.add(rowkey);
        }

        return matches;
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  colfam     = ps.getOptionArg("-colfam");
        String  autoc      = ps.getOptionArg("-autoconfirm");
        String  regex      = ps.getOptionArg("-regex");
        boolean listOnly   = ps.getOptionFlag("-l");

        if ((connection == null) ||
            (colfam == null) || (colfam.trim().length() == 0) ||
            (regex  == null) || (regex.trim().length()  == 0))
            return null;

        return new Options(connection, colfam, regex, listOnly, "y".equals(autoc));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
