package com.apixio.useracct.cmdline;

import com.apixio.XUUID;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

import java.util.Map;

/**
 * Created by dyee on 4/19/17.
 */
public class MigAssemblyProperty extends CmdlineBase
{
    private static final String cUsage = ("Usage:  MigAssemblyProperty -c <conn-yamlfile> -m [test|change]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
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
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private PrivSysServices sysServices;
    private CqlCrud cqlCrud;
    private CqlCrud internalCrud;

    private PatientDataSetLogic pdsLogic;
    private boolean             testMode;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        MigAssemblyProperty.Options options   = parseOptions(new ParseState(args));
        ConfigSet config    = null;

        if ((options == null) ||
                ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        try
        {
            (new MigAssemblyProperty(options, config)).migrate();
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

    private MigAssemblyProperty(MigAssemblyProperty.Options options, ConfigSet config) throws Exception
    {
        this.sysServices = setupServices(config);
        this.cqlCrud     = sysServices.getCqlCrud();
        this.internalCrud = sysServices.getCqlCrud1();
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

        migrateAssemblyStore();
    }

    private void createAssemblyProperty(PatientDataSet pds) {
        XUUID pdsId    = pds.getID();
        String  pdsIdStr = pdsLogic.normalizePdsIdToLongFormat(pdsId.getID());

        String assemblyTableName = "patient" + pdsIdStr;

        info("creating: [" + assemblyTableName + "]");
        if (testMode)
            return;

        pdsLogic.setPdsProperty(PatientDataSetConstants.PATIENT_DATASRC_KEY2, assemblyTableName, pdsId);
        internalCrud.createTableLeveled(assemblyTableName);
    }

    private void migrateAssemblyStore() throws Exception
    {
        info("========== Migrating Assembly Store Table properties");

        try
        {
            beginTrans();

            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(true))
            {
                String existingAssemblyProperty = getExistingAssemblyProperty(pds);

                if (existingAssemblyProperty == null)
                {
                    if (pds.getActive() && (pds.getID() != null))
                    {
                        info("Creating new assembly data store for patient data set: " + pds.toString());

                        //HACK: we delete a bunch of cf without marking them inactive, deal with this here.
                        if(cqlCrud.tableExists("cf" + pds.getCOID()))
                        {
                            createAssemblyProperty(pds);
                        }
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

    private String getExistingAssemblyProperty(PatientDataSet pds) throws Exception
    {
        Map<String, Object> nameToValue = pdsLogic.getPatientDataSetProperties(pds);

        System.out.println("PDSID=" + pds.getID() + ": nameToValue: " + nameToValue);

        return nameToValue != null ? (String) nameToValue.get(PatientDataSetConstants.PATIENT_DATASRC_KEY2) : null;
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
    private void warning(String fmt, Object... args)
    {
        System.out.println("WARN:   " + StringUtil.subargsPos(fmt, args));
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static MigAssemblyProperty.Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  mode       = ps.getOptionArg("-m");

        mode = strEmpty(mode) ? "test" : mode;

        if (strEmpty(connection) || !(mode.equals("test") || mode.equals("change")))
            return null;

        return new MigAssemblyProperty.Options(connection, mode.equals("test"));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }
}

