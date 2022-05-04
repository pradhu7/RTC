package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

import com.apixio.SysServices;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;

/**
 */
public class SetNextPdsID extends CmdlineBase
{

    private static final String cUsage = ("Usage:  SetNextPdsID -c <conn-yamlfile> [-next <nextID>] [-get]\n\n");

    enum Action { GET, SET }

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String   connectionYamlFile;
        Action   action;
        Long     nextID;

        Options(String connectionYaml, Action action, Long nextID)
        {
            this.connectionYamlFile = connectionYaml;
            this.action             = action;
            this.nextID             = nextID;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private static SysServices   sysServices;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options            options   = parseOptions(new ParseState(args));
        ConfigSet          config    = null;

        if ((options == null) || ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        try
        {
            sysServices = setupServices(config);

            if (options.action == Action.GET)
                printNextPdsID();
            else if (options.action == Action.SET)
                setNextPdsID(options.nextID);
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
    private static void printNextPdsID() throws Exception
    {
        System.out.println("Current value:  " + sysServices.getPatientDataSets().getNextPdsID(false));
    }

    private static void setNextPdsID(Long nextID) throws Exception
    {
        //confirmed = cs.readLine().equals("yes");
        sysServices.getPatientDataSets().setNextPdsID(nextID);
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        boolean get        = ps.getOptionFlag("-get");
        String  next       = ps.getOptionArg("-next");

        if ((connection == null) ||
            (get && (next != null)) ||
            (!get && (next == null)))
            return null;

        return new Options(connection, (get ? Action.GET : Action.SET), ((next != null) ? Long.valueOf(next) : null));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
