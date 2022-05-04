package com.apixio.useracct.cmdline;

import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * MigSeqStoreProperty migrates seq store to new seq store property.
 */
public class MigSeqStoreGlobalProperty extends CmdlineBase
{
    private static final String cUsage = ("Usage:  MigSeqStoreGlobalProperty -c <conn-yamlfile> -g <globalSeqStoreCF> -m [test|change]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       globalCF;
        boolean      testMode;

        Options(String connectionYaml, String globalCF, boolean testMode)
        {
            this.connectionYamlFile = connectionYaml;
            this.globalCF           = globalCF;
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
    private CqlCrud             cqlCrud;
    private PatientDataSetLogic pdsLogic;
    private String              globalCF;
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
            (new MigSeqStoreGlobalProperty(options, config)).migrate();
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

    private MigSeqStoreGlobalProperty(Options options, ConfigSet config) throws Exception
    {
        this.sysServices = setupServices(config);
        this.cqlCrud     = sysServices.getCqlCrud();
        this.pdsLogic    = sysServices.getPatientDataSetLogic();
        this.globalCF    = options.globalCF;
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

        migrateSeqStore();
    }

    private void migrateSeqStore() throws Exception
    {
        info("========== Migrating Seq Store Table properties");

        try
        {
            beginTrans();

            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(false))
            {
                String cOID                = pds.getCOID();

                Map<String, String> seqStoreMap = getNewSeqStoreProperty(pds);

                for (Map.Entry<String, String> entry : seqStoreMap.entrySet())
                {
                    info("Seq store map entry: " + entry.getKey() + "; " + entry.getValue() + "; Org: " + cOID);
                }

                //String newMapSt = updateSeqStoreGlobalProperty(seqStoreMap);

                //info("New seq stores: " + newMapSt + "; Org: " + cOID);

//                persistNewSeqStoreProperty(pds, newMapSt);
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

    private Map<String, String> getNewSeqStoreProperty(PatientDataSet pds) throws Exception
    {
        Map<String, Object> nameToValue = pdsLogic.getPatientDataSetProperties(pds);
        String              mapSt       = nameToValue != null ? (String) nameToValue.get(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY) : null;

        return (mapSt != null) ? StringUtil.mapFromString(mapSt) : new HashMap<String, String>();
    }

    // what a hack!!!
    private String updateSeqStoreGlobalProperty(Map<String, String> seqStoreMap)
    {
        String property = seqStoreMap.get("global");

        int index = property.indexOf("::", (globalCF + "::").length());
        if (index != -1)
            property = property.substring(0, index);

        seqStoreMap.put("global", property + "::true::true");

        return StringUtil.mapToString(seqStoreMap);
    }

    private void persistNewSeqStoreProperty(PatientDataSet pds, String propertyValue) throws Exception
    {
        if (testMode)
            return;

        try
        {
            pdsLogic.addPropertyDef(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, PropertyType.STRING);
        }
        catch (Exception e) {}

        pdsLogic.setPatientDataSetProperty(pds, PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, propertyValue);
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
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  globalCF   = ps.getOptionArg("-g");
        String  mode       = ps.getOptionArg("-m");

        mode = strEmpty(mode) ? "test" : mode;

        if (strEmpty(connection) || strEmpty(globalCF) || !(mode.equals("test") || mode.equals("change")))
            return null;

        return new Options(connection, globalCF, mode.equals("test"));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }
}
