package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.apixio.XUUID;
import com.apixio.dao.patient2.PatientDAO2;
import com.apixio.dao.utility.DaoServices;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

public class ImportOrgIdPatientIdLinkData extends CmdlineBase
{
    private static final String cUsage = ("Usage: ImportOrgIdPatientIdLinkData -c <conn-yamlfile> -g <globalSeqStoreCF> -f input-file -localCassandra [true] -m [test|change]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options
    {
        String connectionYamlFile;
        String file;
        boolean testMode;
        String       globalCF;


        Options(String connectionYaml, String file, boolean testMode, String globalCF)
        {
            this.connectionYamlFile = connectionYaml;
            this.testMode = testMode;
            this.file = file;
            this.globalCF = globalCF;

            if (!connectionYaml.contains("local"))
            {
                throw new RuntimeException("must point to a local cassandra!!");
            }
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private PrivSysServices     sysServices;
    private CqlCrud cqlCrud;
    private PatientDataSetLogic pdsLogic;
    private String              globalCF;
    private PatientDAO2         patientDAO;
    private boolean testMode;
    private String inputFile;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options options = parseOptions(new ParseState(args));
        ConfigSet config = null;

        if ((options == null) ||
                ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        try
        {
            (new ImportOrgIdPatientIdLinkData(options, config)).importFile();
        } catch (Exception x)
        {
            x.printStackTrace();
        } finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }

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


    private ImportOrgIdPatientIdLinkData(Options options, ConfigSet config) throws Exception
    {
        this.sysServices = setupServices(config);
        this.cqlCrud     = sysServices.getCqlCrud();
        this.pdsLogic    = sysServices.getPatientDataSetLogic();
        this.globalCF    = options.globalCF;

        ConfigSet psConfig = config.getConfigSubtree(MicroserviceConfig.ConfigArea.PERSISTENCE.getYamlKey());
        ConfigSet logConfig = config.getConfigSubtree(MicroserviceConfig.ConfigArea.LOGGING.getYamlKey());
        PersistenceServices ps = ConfigUtil.createPersistenceServices(psConfig, logConfig);
        DaoBase daoBase = new DaoBase(ps);

        DaoServices daoServices = new DaoServices(daoBase, config);

        this.inputFile = options.file;
        this.testMode = options.testMode;
        this.patientDAO = daoServices.getPatientDAO2();
    }

    private String createOldSeqStoreProperty(PatientDataSet pds) {
        XUUID pdsId    = pds.getID();
        String  pdsIdStr = pdsLogic.normalizePdsIdToLongFormat(pdsId.getID());

        String sequenceStoreTableName = "store" + pdsIdStr;

        info("creating: [" + sequenceStoreTableName + "]");

        if (testMode)
            return sequenceStoreTableName;

        pdsLogic.setPdsProperty(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY, sequenceStoreTableName, pdsId);
        pdsLogic.setPatientDataSetProperty(pds, PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY, sequenceStoreTableName);

//        cqlCrud.createTable(sequenceStoreTableName);
        return sequenceStoreTableName;
    }

    private void importFile() throws Exception
    {
        info("Beginning Import - test mode: " + this.testMode);

        Map<String, String> seenOrgMap = new HashMap<>();

        try
        {
            beginTrans();

            try (BufferedReader br = new BufferedReader(new FileReader(inputFile)))
            {
                int count = 0;

                for (String line; (line = br.readLine()) != null; )
                {
                    if (line.isEmpty())
                        continue;

                    String[] parsedLine = line.split(",");
                    String org = parsedLine[0];
                    String patient = parsedLine[1];

                    count++;
                    try
                    {
                        //
                        // Create the patient to org link table entry.
                        //
                        // TODO: If this is important, this needs to be fixed. Doesn't work with patientDAO2.
//                        if (testMode)
//                        {
//                            System.out.println(UUID.fromString(patient) + "::" + org);
//                        } else
//                        {
//                            patientDAO.saveOrgIdForPatient(UUID.fromString(patient), org);
//                        }

                        //
                        // Now for each unique org, we need to make sure there's a CustomerProperty
                        // entry for seqStore...
                        //
                        if (seenOrgMap.get(org) == null)
                        {
                            PatientDataSet customer = null;

                            try
                            {
                                customer = this.pdsLogic.getPatientDataSetByID(PatientDataSetLogic.patientDataSetIDFromLong(Long.parseLong(org)));
                            }
                            catch(Exception ex)
                            {
                                customer = this.pdsLogic.createPatientDataSet("test", "test", Long.parseLong(org));
                            }

                            if(customer == null)
                            {
                                customer = this.pdsLogic.createPatientDataSet("test", "test", Long.parseLong(org));
                            }

                            PatientDataSet pds = customer;
                            String cOID = pds.getCOID();
                            String oldSeqStoreProperty = getOldSeqStoreProperty(pds);

                            if (oldSeqStoreProperty == null)
                            {
                                warning("No seq store for org: [cOID=" + cOID + "], [ID=" + pds.getID() + "], [Active=" + pds.getActive() + "]");
                                warning("No seq store for patient data set: " + pds.toString());

                                if (pds.getActive() && (pds.getID() != null))
                                {
                                    info("Creating new legacy data store for patient data set: " + pds.toString());
                                    oldSeqStoreProperty = createOldSeqStoreProperty(pds);
                                }
                            }

                            info("Existing seq store: " + oldSeqStoreProperty + "; Org: " + cOID);

                            Map<String, String> seqStoreMap = getNewSeqStoreProperty(pds);

                            for (Map.Entry<String, String> entry : seqStoreMap.entrySet())
                            {
                                info("Seq store map entry: " + entry.getKey() + "; " + entry.getValue() + "; Org: " + cOID);
                            }

                            String newMapSt = addSeqStores(seqStoreMap, oldSeqStoreProperty, cOID);

                            info("New seq stores: " + newMapSt + "; Org: " + cOID);

                            persistNewSeqStoreProperty(pds, newMapSt);

                            //we've processed, this so mark it as done.
                            seenOrgMap.put(org, org);
                        }
                    } catch(Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }

                info("Imported " + count + " entries");
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

    private void warning(String fmt, Object... args)
    {
        System.out.println("WARN:   " + StringUtil.subargsPos(fmt, args));
    }

    private String getOldSeqStoreProperty(PatientDataSet pds) throws Exception
    {
        Map<String, Object> nameToValue = pdsLogic.getPatientDataSetProperties(pds);

        System.out.println("PDSID=" + pds.getID() + ": nameToValue: " + nameToValue);

        return nameToValue != null ? (String) nameToValue.get(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY) : null;
    }

    private Map<String, String> getNewSeqStoreProperty(PatientDataSet pds) throws Exception
    {
        Map<String, Object> nameToValue = pdsLogic.getPatientDataSetProperties(pds);
        String              mapSt       = nameToValue != null ? (String) nameToValue.get(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY) : null;

        return (mapSt != null) ? StringUtil.mapFromString(mapSt) : new HashMap<String, String>();
    }


    private String addSeqStores(Map<String, String> seqStoreMap, String oldSeqStoreProperty, String pdsIdStr)
    {
        seqStoreMap.remove("migrated");
        seqStoreMap.put("historical", oldSeqStoreProperty + "::" + System.currentTimeMillis() + "::" + "true");

        seqStoreMap.put("non_inferred", "store" + "non_inferred" + pdsIdStr + "::" + System.currentTimeMillis() + "::" + "false");

        seqStoreMap.put("global", globalCF + "::" + System.currentTimeMillis() + "::" + "false");

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
        catch (Exception e) {
//            e.printStackTrace();
        }

        cqlCrud.createTableLeveled("store" + "non_inferred" + pds.getCOID());

        pdsLogic.setPatientDataSetProperty(pds, PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, propertyValue);
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String connection = ps.getOptionArg("-c");
        String file = ps.getOptionArg("-f");
        String mode = ps.getOptionArg("-m");
        String localCassandra = ps.getOptionArg("-localCassandra");
        String  globalCF   = ps.getOptionArg("-g");


        mode = strEmpty(mode) ? "test" : mode;

        if (strEmpty(connection) || strEmpty(file)
                || strEmpty(globalCF)
                || !(localCassandra.equals("true"))
                || !(mode.equals("test") || mode.equals("change")))
            return null;

        return new Options(connection, file, mode.equals("test"), globalCF);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }
}

