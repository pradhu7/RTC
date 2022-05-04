package com.apixio.restbase.tool;

import java.io.File;
import java.util.Map;
import java.util.Set;

import com.apixio.restbase.config.ConfigSet;
import org.apache.commons.cli.*;

import com.apixio.datasource.consul.ConsulDS;
import com.apixio.datasource.consul.consultype.ConsulTypesUtils;

public class ConfigMigrate
{
    private String  actionType;
    private String  yamlPath;
    private String  consulRoot;
    private Options options;
    private int     exitValue = 0;

    public static void main(String[] args)
    {
        try
        {
            ConfigMigrate cm = new ConfigMigrate(args);
            if (cm.exitValue == 1)
            {
                System.exit(cm.exitValue);
            }

            cm.validateOptions();
            if (cm.exitValue == 1)
            {
                System.exit(cm.exitValue);
            }

            if (cm.actionType.equalsIgnoreCase("help"))
            {
                cm.printHelp();
            }

            else if (cm.actionType.equalsIgnoreCase("print"))
            {
                Set<ConsulDS.RootAndKV> rootAndKVs = ConsulDS.getKvs('.');
                System.out.println("Config Size = " + rootAndKVs.size());
                rootAndKVs.stream().sorted().forEach(rootAndKV -> System.out.println(rootAndKV));
            }

            else if (cm.actionType.equalsIgnoreCase("write"))
            {
                ConfigSet configSet = ConfigSet.fromYamlFile(new File(cm.yamlPath));
                Map<String, Object> configFromConfigSet = configSet.getProperties();

                System.out.println(configSet.toString());

                configFromConfigSet.forEach((k, v) -> ConsulDS.writeKV(k, v, cm.consulRoot, '.'));
            }

            // might not be too useful!!!
            else if (cm.actionType.equalsIgnoreCase("compare"))
            {
                ConfigSet configSet = ConfigSet.fromYamlFile(new File(cm.yamlPath));
                Map<String, Object> configFromConfigSet = configSet.getProperties();

                Map<String, Object> configFromConsul = ConsulDS.getConfig('.');

                configFromConsul.forEach( (k, v) ->
                {
                    Object obj = configFromConfigSet.get(k);
                    if (!ConsulTypesUtils.isSameObjectValue(obj, v))
                    {
                        cm.exitValue = 1;
                        System.out.println("Error 1: Consul different than Yaml: " + k);
                    }
                });

                configFromConfigSet.forEach( (k, v) ->
                {
                    Object obj = configFromConsul.get(k);
                    if (!ConsulTypesUtils.isSameObjectValue(obj, v))
                    {
                        cm.exitValue = 1;
                        System.out.println("Error 2: Yaml different than Consul: " + k);
                    }
                });
            }

            System.exit(cm.exitValue);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public ConfigMigrate(String[] args)
    {
        options = new Options();

        Option action = new Option("a", "action", true, "action can be print, write, or compare");
        action.setRequired(true);
        options.addOption(action);

        Option yaml = new Option("y", "yaml", true, "yaml file path. Used with action write or compare");
        yaml.setRequired(false);
        options.addOption(yaml);

        Option root = new Option("r", "root", true, "consul root. Used with action write");
        root.setRequired(false);
        options.addOption(root);

        Option help = new Option("h", "help", false, "help option");
        help.setRequired(false);
        options.addOption(help);

        try
        {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            actionType = cmd.getOptionValue("action");
            yamlPath = cmd.getOptionValue("yaml");
            consulRoot = cmd.getOptionValue("root");


            System.out.println("actionType = " + actionType);
            System.out.println("yamlPath = " + yamlPath);
            System.out.println("consulRoot =  " + consulRoot);
        }
        catch (ParseException e)
        {
            exitValue = 1;
            e.printStackTrace();
            printHelp();
        }
    }

    private void validateOptions()
    {
        if (actionType == null)
        {
            System.out.println("Null Action = " + actionType);
            exitValue = 1;
            return;
        }

        if ( !(actionType.equalsIgnoreCase("help") || actionType.equalsIgnoreCase("print") ||
                actionType.equalsIgnoreCase("write") || actionType.equalsIgnoreCase("compare")) )
        {
            System.out.println("Wrong Action = " + actionType);
            exitValue = 1;
            return;
        }

        if ( (actionType.equalsIgnoreCase("write") || actionType.equalsIgnoreCase("compare")) && yamlPath == null)
        {
            System.out.println("Action type of 'write' or 'compare' requires yamlPath");
            exitValue = 1;
            return;
        }

        if (actionType.equalsIgnoreCase("write") && consulRoot == null)
        {
            System.out.println("Action type of 'write' requires consulRoot");
            exitValue = 1;
            return;
        }
    }

    private void printHelp()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ConfigMigrate", options);
    }
}
