package com.apixio.useracct.cmdline;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.UUID;

import com.apixio.dao.patient2.PatientDAO2;
import com.apixio.dao.utility.DaoServices;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

public class DumpOrgIdPatientIdLinkData extends CmdlineBase
{
    private static final String cUsage = ("Usage: DumpOrgIdPatientIdLinkData -c <conn-yamlfile> -f outputfile -m [test|change]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       file;
        boolean      testMode;

        Options(String connectionYaml, String file, boolean testMode)
        {
            this.connectionYamlFile = connectionYaml;
            this.file = file;
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
    private PatientDAO2         patientDAO;
    private boolean             testMode;
    private String              outputFile;

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
            (new DumpOrgIdPatientIdLinkData(options, config)).dumpToFile();
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

    private DumpOrgIdPatientIdLinkData(Options options, ConfigSet config) throws Exception
    {
        this.sysServices = setupServices(config);

        ConfigSet             psConfig  = config.getConfigSubtree(MicroserviceConfig.ConfigArea.PERSISTENCE.getYamlKey());
        ConfigSet             logConfig = config.getConfigSubtree(MicroserviceConfig.ConfigArea.LOGGING.getYamlKey());
        PersistenceServices ps        = ConfigUtil.createPersistenceServices(psConfig, logConfig);
        DaoBase             daoBase = new DaoBase(ps);

        DaoServices daoServices = new DaoServices(daoBase, config);

        this.pdsLogic    = sysServices.getPatientDataSetLogic();
        this.testMode    = options.testMode;
        this.patientDAO  = daoServices.getPatientDAO2();
        this.outputFile  = options.file;
    }

    private void dumpToFile() throws Exception
    {
        info("Beginning dump - test mode: " + this.testMode);

        Writer writer = null;
        try
        {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(outputFile), "utf-8"));

            for (PatientDataSet pds : pdsLogic.getAllPatientDataSets(false))
            {
                if(pds==null || pds.getID() == null) continue;

                try
                {
                    Iterator<UUID> patientKeysIterator = patientDAO.getPatientKeys(pds.getCOID());

                    while (patientKeysIterator.hasNext())
                    {
                        UUID patientUUID = patientKeysIterator.next();
                        writer.write(pds.getCOID() + "," + patientUUID.toString());
                        writer.write(System.getProperty("line.separator"));
                    }
                } catch (Exception ex) {
                    //ignore
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
            if(writer!=null)
            {
                writer.flush();
                writer.close();
            }
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

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");
        String  file       = ps.getOptionArg("-f");
        String  mode       = ps.getOptionArg("-m");

        mode = strEmpty(mode) ? "test" : mode;

        if (strEmpty(connection) || strEmpty(file)
                || !(mode.equals("test") || mode.equals("change")))
            return null;

        return new Options(connection, file, mode.equals("test"));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }
}
