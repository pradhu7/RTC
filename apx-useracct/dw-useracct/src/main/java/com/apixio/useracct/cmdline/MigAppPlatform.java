package com.apixio.useracct.cmdline;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

public class MigAppPlatform extends CmdlineBase
{
    private static final String cUsage = ("Usage:  MigAppPlatform -c <conn-yamlfile>  -m [test|change]\n\n");

    private static class Options {
        String       connectionYamlFile;
        boolean      testMode;

        Options(String connectionYaml, boolean testMode)
        {
            this.connectionYamlFile = connectionYaml;
            this.testMode           = testMode;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile + "]");
        }
    }

    // ################################################################

    private PatientDataSetLogic pdsLogic;
    private boolean             testMode;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        MigAppPlatform.Options options   = parseOptions(new ParseState(args));
        ConfigSet config    = null;

        if ((options == null) ||
                ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        try
        {
            (new MigAppPlatform(options, config)).migrate();
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
        finally
        {
            System.exit(0);
        }

    }

    private MigAppPlatform(MigAppPlatform.Options options, ConfigSet config) throws Exception
    {
        PrivSysServices sysServices = setupServices(config);
        this.pdsLogic    = sysServices.getPatientDataSetLogic();
        this.testMode    = options.testMode;
    }

    private void migrate() throws Exception
    {
        info("Beginning migration - test mode: " + this.testMode);
        activateApplicationPlatform();
    }

    private void activateApplicationPlatform() throws Exception
    {
        info("========== activating application platform for existing PDSes");

        try
        {
            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(true))
            {
                if(!testMode)
                {
                    pdsLogic.activateApplicationPlatformCluster(pds);
                }
                info("Creating new legacy data store for patient data set: " + pds.toString());
            }
        }
        catch (Exception x)
        {
            x.printStackTrace();
            throw x;
        }
    }

    /**
     * Project helper functions
     */

    private static boolean strEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     *
     */
    private void info(String fmt, Object... args)
    {
        System.out.println("INFO:   " + StringUtil.subargsPos(fmt, args));
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static MigAppPlatform.Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  mode       = ps.getOptionArg("-m");

        mode = strEmpty(mode) ? "test" : mode;

        if (strEmpty(connection) || !(mode.equals("test") || mode.equals("change")))
            return null;

        return new MigAppPlatform.Options(connection, mode.equals("test"));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }
}

