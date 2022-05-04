package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.util.ConversionUtil;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;
import com.apixio.utility.StringList;
import com.apixio.utility.StringUtil;

/**
 */
public class DumpCassKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  DumpCassKeys -c <conn-yamlfile> -from colfam -out <file> [-autoconfirm <y/n>]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        String       columnFamily;
        String       outfile;
        boolean      autoconfirm;

        Options(String connectionYaml, String columnFamily, String outfile, boolean autoconfirm)
        {
            this.connectionYamlFile = connectionYaml;
            this.columnFamily       = columnFamily;
            this.outfile            = outfile;
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
            dumpColumnFamily(options);
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
    private static void dumpColumnFamily(Options options) throws Exception
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
            cs.format("Confirm dump of all rowkeys from column family %s.  Enter 'yes' to proceed:  ", options.columnFamily);
            confirmed = cs.readLine().equals("yes");
        }

        if (options.autoconfirm || confirmed)
        {
            CqlCrud cqlCrud = sysServices.getCqlCrud();
            int     count   = 0;
            FileWriter out = null;

            try
            {
                if (options.outfile != null)
                    out = new FileWriter(options.outfile);

                cqlCrud.setBatchSyncEnabled(true);

                for (Iterator<String> iter = cqlCrud.getAllKeys(options.columnFamily); iter.hasNext(); )
                {
                    String                        rowkey = iter.next();
                    SortedMap<String, ByteBuffer> cols   = cqlCrud.getColumnsMap(options.columnFamily, rowkey, null);
                    Map<String, String>           nmap   = new HashMap<>();

                    if (cols.size() > 0)
                    {
                        String line;

                        for (Map.Entry<String, ByteBuffer> entry : cols.entrySet())
                        {
                            String     k = entry.getKey();
                            ByteBuffer v = entry.getValue();

                            if (v == null)
                                System.out.println("ERROR?  value is null for rowkey " + rowkey + ", col " + k);
                            else
                                nmap.put(k, ConversionUtil.byteBufferToHexString(v));
                        }

                        line = StringList.flattenList(rowkey, StringUtil.mapToString(nmap));

                        if (out == null)
                        {
                            System.out.println(line);
                        }
                        else
                        {
                            out.write(line);
                            out.write("\n");
                        }

                        count++;
                    }
                }

                System.out.println("Dumped " + count + " non-empty rowkeys");
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
        String  from       = ps.getOptionArg("-from");
        String  outfile    = ps.getOptionArg("-out");
        String  autoc      = ps.getOptionArg("-autoconfirm");

        if ((connection == null) ||
            (from == null) || ((from = from.trim()).length() == 0))
            return null;

        return new Options(connection, from, outfile, "y".equals(autoc));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
