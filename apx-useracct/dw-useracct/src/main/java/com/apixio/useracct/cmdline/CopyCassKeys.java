package com.apixio.useracct.cmdline;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

import org.yaml.snakeyaml.Yaml;

import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlCrud.ColumnMapResult;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices.CassandraCluster;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;

/**
 */
public class CopyCassKeys extends CmdlineBase
{
    private static final String cUsage = ("Usage:  CopyCassKeys -conf {yamlfile} -cluster {name} [-rowkey {rowkey}] [-filter {prefix}] -from {colfam} -to {colfam} [-compare] [-compts] [-compval]\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String           connectionYamlFile;
        boolean          compare;
        boolean          compareTs;
        boolean          compareVals;
        String           rowkey;
        String           filterPrefix;
        String           fromColumnFamily;
        String           toColumnFamily;
        CassandraCluster cluster;

        Options(String connectionYaml, boolean compare, boolean compareTs, boolean compareVals,
                String rowkey, String filterPrefix, String fromColumnFamily, String toColumnFamily, CassandraCluster cluster)
        {
            this.connectionYamlFile = connectionYaml;
            this.compare            = compare;
            this.compareTs          = compareTs;
            this.compareVals        = compareVals;
            this.rowkey             = rowkey;
            this.filterPrefix       = filterPrefix;
            this.fromColumnFamily   = fromColumnFamily;
            this.toColumnFamily     = toColumnFamily;
            this.cluster            = cluster;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "; rowkey=" + rowkey +
                    "; compare=" + compare +
                    "; compareTs=" + compareTs +
                    "; compareVals=" + compareVals +
                    "; filterPrefix=" + filterPrefix +
                    "; fromColumnFamily=" + fromColumnFamily +
                    "; toColumnFamily=" + toColumnFamily +
                    "; cluster=" + cluster +
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
        Options         options   = parseOptions(new ParseState(args));
        ConfigSet       config    = null;

        if ((options == null) ||
            ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        sysServices = setupServices(config);

        if (!options.compare)
            copyColumnFamily(options);
        else
            compareColumnFamilies(options);

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    /**
     *
     */
    private static void copyColumnFamily(Options options) throws Exception
    {
        Console     cs    = System.console();
        int         frLen = options.fromColumnFamily.length();

        if (cs == null)
        {
            System.out.println("Fatal error:  unable to get a console to confirm operation.\n");
            System.exit(1);
        }

        if (options.rowkey != null)
            cs.format("Confirm copy of rowkey %s from column family %s to column family %s in cluster %s.  Enter 'yes' to proceed:  ",
                      options.rowkey, options.fromColumnFamily, options.toColumnFamily, options.cluster.toString());
        else
            cs.format("Confirm copy of all rowkeys from column family %s to column family %s in cluster %s.  Enter 'yes' to proceed:  ",
                      options.fromColumnFamily, options.toColumnFamily, options.cluster.toString());

        if (cs.readLine().equals("yes"))
        {
            CqlCrud cqlSrc = sysServices.getCqlCrud(options.cluster);
            CqlCrud cqlDst = sysServices.getCqlCrud(options.cluster);  // for now src/dst clusters are the same
            int     count  = 0;

            cqlDst.setBatchSyncEnabled(true);

            if (options.rowkey != null)
            {
                copyRowkey(options, cqlSrc, cqlDst, options.rowkey);
                count++;
            }
            else
            {
                for (Iterator<String> iter = cqlSrc.getAllKeys(options.fromColumnFamily); iter.hasNext(); )
                {
                    String rowkey = iter.next();

                    if ((options.filterPrefix == null) || rowkey.startsWith(options.filterPrefix))
                    {
                        copyRowkey(options, cqlSrc, cqlDst, rowkey);
                        count++;
                    }
                }
            }

            System.out.println("Copied " + count + " non-empty rowkeys");
        }
    }

    /**
     *
     */
    private static void compareColumnFamilies(Options options) throws Exception
    {
        CqlCrud cqlOne = sysServices.getCqlCrud(options.cluster);
        CqlCrud cqlTwo = sysServices.getCqlCrud(options.cluster);  // for now src/dst clusters are the same

        if (options.rowkey != null)
        {
//            compareRowkey(options, cqlSrc, cqlDst, options.rowkey);
        }
        else
        {
            Set<String>  oneKeys = fillSet(cqlOne.getAllKeys(options.fromColumnFamily));
            Set<String>  twoKeys = fillSet(cqlTwo.getAllKeys(options.toColumnFamily));

            if (options.compareTs)
                System.out.println("Diffing will include timestamps");
            if (options.compareVals)
                System.out.println("Diffing will include colvalues");

            System.out.println("Rowkey diff (" + options.fromColumnFamily + " - " + options.toColumnFamily   + "): " + setSub(oneKeys, twoKeys));
            System.out.println("Rowkey diff (" + options.toColumnFamily   + " - " + options.fromColumnFamily + "): " + setSub(twoKeys, oneKeys));

            compareRowkeys(options, cqlOne, cqlTwo, oneKeys, twoKeys, options.fromColumnFamily, options.toColumnFamily);
            compareRowkeys(options, cqlTwo, cqlOne, twoKeys, oneKeys, options.toColumnFamily,   options.fromColumnFamily);
        }
    }

    private static Set<String> fillSet(Iterator<String> iter)
    {
        Set<String> set = new HashSet<>();

        while (iter.hasNext())
            set.add(iter.next());

        return set;
    }

    private static Set<String> setSub(Set<String> one, Set<String> two)
    {
        Set<String> diff = new HashSet<>(one);

        for (String ele : two)
            diff.remove(ele);

        return diff;
    }

    private static void compareRowkeys(Options options, CqlCrud one, CqlCrud two, Set<String> oneKeys, Set<String> twoKeys, String oneColFam, String twoColFam)
    {
        for (String oneKey : oneKeys)
            compareRowkey(options, one, two, oneKey, oneColFam, twoColFam);
    }

    private static void compareRowkey(Options options, CqlCrud one, CqlCrud two, String rowkey, String oneColFam, String twoColFam)
    {
        ColumnMapResult colsOne   = one.getColumnsMapWithWriteTime(oneColFam, rowkey);   // .value is type SortedMap<String, ByteBuffer>, .time is SortedMap<String, Long>
        ColumnMapResult colsTwo   = two.getColumnsMapWithWriteTime(twoColFam, rowkey);
        Set<String>     oneExtra  = setSub(colsOne.value.keySet(), colsTwo.value.keySet());

        if (oneExtra.size() == 0)
            ;  // identical so don't print
        else
            System.out.println("Rowkey diff for " + rowkey + ", colname diff (" + oneColFam + " :: " + twoColFam + "):  " + oneExtra);

        if (options.compareTs)
        {
            for (String colname : colsOne.value.keySet())
            {
                Long ts1 = colsOne.time.get(colname);
                Long ts2 = colsTwo.time.get(colname);

                if (ts1 == null)
                    System.out.println("ERROR:  colname " + colname + " exists in " + oneColFam + "." + rowkey + " .value but doesn't exist in .time");
                else if (ts2 == null)
                    System.out.println("ERROR:  colname " + colname + " exists in " + oneColFam + "." + rowkey + " but doesn't exist in " + twoColFam + "." + rowkey + " .time");
                else if (ts1.longValue() != ts2.longValue())
                    System.out.println("Rowkey diff for " + rowkey + " in timestamp:  ts1=" + ts1 + ", ts2=" + ts2);
            }
        }

        if (options.compareVals)
        {
            boolean diffs = false;

            for (String colname : colsOne.value.keySet())
            {
                ByteBuffer bb1 = colsOne.value.get(colname);
                ByteBuffer bb2 = colsTwo.value.get(colname);

                if (bb2 == null)
                {
                    System.out.println("ERROR:  colname " + colname + " exists in " + oneColFam + "." + rowkey + " but doesn't exist in " + twoColFam + "." + rowkey + " .value");
                    diffs = true;
                }
                else if (bb1.compareTo(bb2) != 0)
                {
                    System.out.println("Rowkey diff for " + rowkey + " col " + colname + ":  values are different");
                    diffs = true;
                }
            }
        }
    }

    private static void copyRowkey(Options options, CqlCrud src, CqlCrud dst, String rowkey)
    {
        SortedMap<String, ByteBuffer> cols   = src.getColumnsMap(options.fromColumnFamily, rowkey, null);
        List<CqlRowData>              ncols  = new ArrayList<>();

        for (Map.Entry<String, ByteBuffer> entry : cols.entrySet())
            ncols.add(new CqlRowData(options.toColumnFamily, rowkey, entry.getKey(), entry.getValue()));

        if (ncols.size() > 0)  // in case no columns...
            dst.insertRows(ncols);
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-conf");
        String  cluster    = ps.getOptionArg("-cluster");
        String  rowkey     = ps.getOptionArg("-rowkey");
        String  filter     = ps.getOptionArg("-filter");
        String  from       = ps.getOptionArg("-from");
        String  to         = ps.getOptionArg("-to");

        if ((connection == null) || (cluster == null) ||
            (from == null) || ((from = from.trim()).length() == 0) ||
            (to   == null) || ((to   = to.trim()).length()   == 0))
            return null;

        return new Options(connection,
                           ps.getOptionFlag("-compare"),
                           ps.getOptionFlag("-compts"),
                           ps.getOptionFlag("-compval"),
                           rowkey, filter, from, to, CassandraCluster.valueOf(cluster.toUpperCase()));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
