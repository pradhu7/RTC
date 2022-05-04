package com.apixio.useracct.cmdline;

import java.util.Map;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

/**
 * WellcareSpecialPDSMigration migrates seq store to new seq store property.
 */
public class WellcareSpecialPDSMigration extends CmdlineBase
{
    private static final String cUsage = ("Usage:  WellcareSpecialPDSMigration -c <conn-yamlfile> -m [test|change]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        boolean      testMode;

        Options(String connectionYaml,  boolean testMode)
        {
            this.connectionYamlFile = connectionYaml;
            this.testMode           = testMode;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private PrivSysServices     sysServices;
    private PatientDataSetLogic pdsLogic;
    private boolean             testMode;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options   options   = parseOptions(new ParseState(args));
        ConfigSet config    = null;

        if ((options == null) ||
                ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        try
        {
            (new WellcareSpecialPDSMigration(options, config)).migrate();
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
        finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }

    }

    private WellcareSpecialPDSMigration(Options options, ConfigSet config) throws Exception
    {
        this.sysServices = setupServices(config);
        this.pdsLogic    = sysServices.getPatientDataSetLogic();
        this.testMode    = options.testMode;
    }

    private void beginTrans() throws Exception
    {
        sysServices.getRedisTransactions().begin();
    }
    private void commitTrans() throws Exception
    {
        sysServices.getRedisTransactions().commit();
    }
    private void abortTrans() throws Exception
    {
        sysServices.getRedisTransactions().abort();
    }

    private void migrate() throws Exception
    {
        info("Beginning migration - test mode: " + this.testMode);
        info("========== Migrating Seq Store Table properties");

        try
        {
            beginTrans();

            Map<String, Object> nameToValue = null;

            //
            // Get the Wellcare PDS properties
            //
            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(true))
            {
                String cOID                = pds.getCOID();

                if(cOID.equals("10000330"))
                {
                    nameToValue = pdsLogic.getPatientDataSetProperties(pds);

                    for(String key : nameToValue.keySet())
                    {
                        System.out.println("Original Wellcare key ==> " + nameToValue.get(key));
                    }
                }
            }

            if(nameToValue == null) return;

            //
            // Set the special PDS....
            //
            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(true))
            {
                String cOID = pds.getCOID();

                if (cOID.equals("10001037"))
                {
                    for (String key : nameToValue.keySet())
                    {
                        String value = (String)nameToValue.get(key);
                        System.out.println("Existing key ==> " + value);

                        if(key.equalsIgnoreCase(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY) ||
                                key.equalsIgnoreCase(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY) )
                        {
                            continue;
                        }

                        if (testMode)
                            continue;

                        pdsLogic.setPatientDataSetProperty(pds, key, value);

                    }
                }
            }

            commitTrans();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            abortTrans();

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
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  mode       = ps.getOptionArg("-m");

        mode = strEmpty(mode) ? "test" : mode;

        if (strEmpty(connection) || !(mode.equals("test") || mode.equals("change")))
            return null;

        return new Options(connection, mode.equals("test"));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }
}
