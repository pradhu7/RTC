package com.apixio.useracct.cmdline;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.apixio.restbase.config.BootConfig;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;

/**
 * Cmdline prog to read in a .yaml config and boot config and dump out the
 * final flattened config.
 *
 * Usage:
 *
 *  java com.apixio.useracct.cmdline.ConfigDump -bootconfig someconfig.yaml [-bootprops csvpropfilelist] [--config name=value]  [cfg1 ...]
 */
public class ConfigDump
{

    private final static String BOOTCONFIG = BootConfig.CMDLINE_BOOTCONFIG;
    private final static String BOOTPROPS  = BootConfig.CMDLINE_BOOTPROPS;
    private final static String CONFIG     = BootConfig.CMDLINE_CONFIG;;

    private String[]      bootConfig;
    private String[]      bootProps;
    private String[]      configOverrides;
    private List<String>  configToList;
    private ConfigSet     fullConfig;

    protected static Options options = new Options();
    static
    {
        options.addOption(null,  BOOTCONFIG, true, "Comma-separated list of paths to config .yaml file(s); repeatable");
        options.addOption(null,  BOOTPROPS,  true, "Comma-separated list of .properties file paths to use as boot properties; repeatable");
        options.addOption(null,  CONFIG,     true, "Config override; format name=value; repeatable");
    }

    public static void main(String[] args) throws IOException
    {
        ConfigDump cd = new ConfigDump();

        try
        {
            if (!cd.parseCmdline(args))
            {
                usage();
            }
            else
            {
                cd.dumpConfig();
            }
        }
        catch (ParseException px)
        {
            usage();
        }
    }

    private boolean parseCmdline(String[] args) throws ParseException, IOException
    {
        //        if (true) { fullConfig = BootConfig.fromSystemProps(ConfigSet.emptyConfig()).getBootConfig(); return true; }

        boolean valid = false;

        if (args.length > 0)
        {
            CommandLineParser parser = new PosixParser();
            CommandLine       line   = parser.parse(options, args);

            bootConfig      = line.getOptionValues(BOOTCONFIG);
            bootProps       = line.getOptionValues(BOOTPROPS);
            configOverrides = line.getOptionValues(CONFIG);
            configToList    = line.getArgList();

            fullConfig = BootConfig.fromParams(bootProps, bootConfig, configOverrides).getBootConfig();

            valid = (fullConfig != null);
        }

        return valid;
    }

    private void dumpConfig() throws IOException
    {
        Map<String, Object> props = fullConfig.getProperties();

        System.out.println("\n>>>>>>>>>>>>>>>> Configuration:");

        if ((configToList != null) && (configToList.size() > 0))
        {
            for (String config : configToList)
                dumpOne(props, config);
        }
        else
        {
            List<String> inOrder = new ArrayList<>();

            for (String key : props.keySet())
                inOrder.add(key);

            Collections.sort(inOrder);

            for (String key : inOrder)
                dumpOne(props, key);
        }
    }

    private static void dumpOne(Map<String, Object> props, String key)
    {
        Object val = props.get(key);

        if (val != null)
        {
            System.out.print(key + "\t" + val);

            if (val instanceof String)
            {
                String sval = (String) val;

                if (sval.length() == 0)
                    System.out.print("\t\t[WARN:  empty value]");
                else if (sval.trim().length() == 0)
                    System.out.print("\t\t[WARN:  trimmed value is empty]");
            }

            System.out.println("");
        }
        else
        {
            System.out.println("Config key [" + key + "] not found");
        }
    }
    
    private static void usage()
    {
        (new HelpFormatter()).printHelp(ConfigDump.class.getCanonicalName() + ":  ", options);
    }

}
