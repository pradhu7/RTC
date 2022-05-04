package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.util.ConversionUtil;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;
import com.apixio.utility.StringList;
import com.apixio.utility.StringUtil;

/**
 */
public class RestoreCassKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  RestoreCassKeys -c <conn-yamlfile> -to colfam -in <file> [-autoconfirm <y/n>]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       columnFamily;
        String       infile;
        boolean      autoconfirm;

        Options(String connectionYaml, String columnFamily, String infile, boolean autoconfirm)
        {
            this.connectionYamlFile = connectionYaml;
            this.columnFamily       = columnFamily;
            this.infile             = infile;
            this.autoconfirm        = autoconfirm;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "; columnFamily=" + columnFamily +
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
            restoreColumnFamily(options);
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

    /**
     *
     */
    private static void restoreColumnFamily(Options options) throws Exception
    {
        Console     cs        = System.console();
        boolean     confirmed = false;

        if ((cs == null) && !options.autoconfirm)
        {
            System.out.println("Fatal error:  unable to get a console to confirm operation.\n");
            System.exit(1);
        }

        if (!options.autoconfirm)
        {
            cs.format("Confirm restore of all rowkeys from column family %s.  Enter 'yes' to proceed:  ", options.columnFamily);
            confirmed = cs.readLine().equals("yes");
        }

        if (options.autoconfirm || confirmed)
        {
            CqlCrud        cqlCrud = sysServices.getCqlCrud();
            BufferedReader in      = null;

            try
            {
                int    count = 0;
                String line;

                cqlCrud.setBatchSyncEnabled(true);

                in = new BufferedReader(new FileReader(options.infile));

                while ((line = in.readLine()) != null)
                {
                    List<String>          deser  = StringList.restoreList(line);
                    String                rowkey = deser.get(0);
                    Map<String, String>   nmap   = StringUtil.mapFromString(deser.get(1));
                    List<CqlRowData>      ncols  = new ArrayList<>();

                    for (Map.Entry<String, String> entry : nmap.entrySet())
                        ncols.add(new CqlRowData(options.columnFamily, rowkey, entry.getKey(),
                                                 ConversionUtil.hexStringToByteBuffer(entry.getValue())));

                    //                    System.out.println("Inserting rows " + ncols);
                    cqlCrud.insertRows(ncols);

                    count++;
                }

                System.out.println("Processed " + count + " lines");
            }
            finally
            {
                if (in != null)
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
        String  to         = ps.getOptionArg("-to");
        String  infile     = ps.getOptionArg("-in");
        String  autoc      = ps.getOptionArg("-autoconfirm");

        if ((connection == null) || (to == null) || ((to = to.trim()).length() == 0))
            return null;

        return new Options(connection, to, infile, "y".equals(autoc));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
