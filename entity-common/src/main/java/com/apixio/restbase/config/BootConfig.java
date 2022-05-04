package com.apixio.restbase.config;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The config bootup model for both dropwizard-based apps and command line programs includes the
 * idea of "boot properties" (simple name=value pairs specified in one or more Java .properties
 * files), "boot config" (YAML configuration specified in one or more .yaml files), and config
 * overrides (a list of simple name=value overrides that are applied after boot config is read in
 * and before boot properties are applied).  All referenced files must be on the local file system.
 *
 * Because dropwizard processes the command line arguments, these things are passed via System
 * properties (using the "-Dname=value" syntax).  For each of these System properties there is
 * a corresponding explicit command line option (e..g, "--bootprops").  Both boot properties
 * and boot config values can have more than a single file specified in the string; files are
 * separated with commas.
 */
public class BootConfig
{
    /**
     * Strings used in System properties and command line
     */
    private static final String SYSPROP_BOOTPROPS  = "_bootprops";       // e.g., -D_bootprops=a.properties,b.properties
    private static final String SYSPROP_BOOTCONFIG = "_bootconfig";      // e..g, -D_bootconfig=a.yaml,b.yaml
    // public so outside cmdline code can use them:
    public  static final String CMDLINE_BOOTPROPS  = "bootprops";        // e.g., --bootprops a.properties,b.properties --bootprops c.properties
    public  static final String CMDLINE_BOOTCONFIG = "bootconfig";       // e.g., --bootconfig a.yaml,b.yaml --bootconfi c.yaml
    public  static final String CMDLINE_CONFIG     = "config";           // e.g., --config name1=value1 --config name2=value2

    /**
     * Fields
     */
    private ConfigSet           bootConfig;  // contains fully processed config
    private Map<String, String> bootProps  = new HashMap<>();

    /**
     * Force factory use
     */
    private BootConfig(ConfigSet base)
    {
        this.bootConfig = base;
    }

    private BootConfig()
    {
        this.bootConfig = ConfigSet.emptyConfig();
    }

    /**
     * Reads the relevant System.getProperties() and processes them according to the
     * general model, returning the final merged, overridden, and replaced ConfigSet.
     *
     * This method should be called in dropwizard-based code and the main ConfigSet
     * that's created in MicroserviceConfig.setMicroserviceConfig() should be passed
     * in as the base config.
     */
    public static BootConfig fromSystemProps(ConfigSet base) throws IOException
    {
        if (base == null)
            throw new IllegalArgumentException("createConfigFromSystemProps requires a non-null base ConfigSet");

        Properties  props  = System.getProperties();
        BootConfig  boot   = new BootConfig(base);

        // bootConfig handling is more complex only because we have to support ConfigSet coming from dropwizard bootup

        boot.readBootProps(props.getProperty(SYSPROP_BOOTPROPS));
        boot.readBootConfig(props.getProperty(SYSPROP_BOOTCONFIG));

        // note that the 'true' at the end makes it so that we don't add unrelated System properties (good)
        // BUT it also makes it so that -Dname=value can only replace config specified by .yaml files (bad)
        boot.mergeOverrides(makeOverridesFromSystemProperties(props), true);
        boot.applyBootProperties();        // always do this even if no bootProps (makes it so that all {}s are replaced)

        return boot;
    }

    /**
     * Uses all the given info to create the final ConfigSet that represents the
     * combined yaml config (from reading the bootConfigCsv in defined order)
     * and the config overrides, with substituting the bootprops into the config
     */
    public static BootConfig fromParams(String[] bootPropCsv, String[] bootConfigCsv, String[] configs) throws IOException
    {
        BootConfig boot  = new BootConfig();

        // the way this is coded allows for *all* config to be passed via configs[]...

        // props/macros
        if (bootPropCsv != null)
        {
            for (String file : bootPropCsv)
                boot.readBootProps(file);
        }

        // yaml config
        if (bootConfigCsv != null)
        {
            for (String file : bootConfigCsv)
                boot.readBootConfig(file);
        }

        // overrides
        boot.mergeOverrides(makeOverridesFromParams(configs), false);
        boot.applyBootProperties();        // always do this even if no bootProps (makes it so that all {}s are replaced)

        return boot;
    }

    /**
     * Getters for the useful results of all the reading/construction of things.
     */
    public ConfigSet getBootConfig()
    {
        return bootConfig;
    }
    public Map<String, String> getBootProps()
    {
        return bootProps;
    }

    /**
     * Methods for consistency of factory method code (that code shouldn't dip into the fields of the object).
     */
    private void mergeOverrides(Map<String, Object> overrides, boolean replaceOnly)
    {
        bootConfig.merge(overrides, replaceOnly);
    }

    private void applyBootProperties()
    {
        bootConfig.applyBootProperties(bootProps);
    }

    /**
     * Apply config overrides that are specified via System.getProperties (i.e., that
     * are given on the cmdline via -Dname=value).
     */
    private static Map<String, Object> makeOverridesFromSystemProperties(Properties props)
    {
        Map<String, Object> overrides = new HashMap<>();

        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet())
        {
            // we except weird things in this list since JVM predefines a lot, hence the
            // try/catch
            try
            {
                overrides.put(((String) entry.getKey()), ConfigSetUtil.untagValue(((String) entry.getValue()), false));
            }
            catch (Exception x)
            {
                // intentionally igore
            }
        }

        return overrides;
    }

    /**
     * Apply config overrides that are specified via a list of name=value strings.
     */
    private static Map<String, Object> makeOverridesFromParams(String[] configs)
    {
        Map<String, Object> overrides = new HashMap<>();

        if (configs != null)
        {
            for (String over : configs)
            {
                int eq;

                over = over.trim();

                if (((eq = over.indexOf('=')) > 0) && (eq + 1 < over.length()))
                {
                    String name  = over.substring(0, eq);
                    String value = over.substring(eq + 1);

                    System.out.println("INFO:  using config override " + name + "=" + value);

                    overrides.put(name, ConfigSetUtil.untagValue(value, false));
                }
                else
                {
                    System.out.println("WARN:  ignoring ill-formed config override '" + over + "'.  Syntax is name=value");
                }
            }
        }

        return overrides;
    }

    /**
     * Reads from the given YAML file(s) and returns the (merged) result.  If a non-empty or
     * non-null base ConfigSet is passed, then config is merged into it.
     */
    private void readBootConfig(String csv) throws IOException
    {
        if (csv != null)
        {
            for (String yamlFile : csv.split(","))
            {
                File  yaml = new File(yamlFile);

                if (yaml.canRead())
                {
                    System.out.println("INFO:  processing boot config file '" + yamlFile + "'");

                    bootConfig.merge(ConfigSet.fromYamlFile(yaml), false);
                }
                else
                {
                    System.out.println("WARN:  can't read boot config file '" + yamlFile + "'");
                }
            }
        }
    }

    /**
     * Reads from the given Java .properties file(s) and returns the (merged) result.  If a non-empty or
     * non-null "props" param is passed, then properties are merged into it, overriding any
     * existing values.
     */
    private void readBootProps(String csv) throws IOException
    {
        if (csv != null)
        {
            for (String propFile : csv.split(","))
            {
                if ((new File(propFile)).canRead())
                {
                    FileReader reader = null;

                    try
                    {
                        Properties jProps = new Properties();

                        reader = new FileReader(propFile);

                        System.out.println("INFO:  processing boot properties file '" + propFile + "'");

                        jProps.load(reader);

                        for (Map.Entry<Object, Object> entry : jProps.entrySet())
                            bootProps.put(((String) entry.getKey()), ((String) entry.getValue()));
                    }
                    finally
                    {
                        safeClose(reader);
                    }
                }
                else
                {
                    System.out.println("WARN:  can't read boot properties file '" + propFile + "'");
                }
            }
        }
    }

    /**
     * Close a stream, ignoring null streams and IOExceptions.
     */
    public static void safeClose(Closeable stream)
    {
        if (stream != null)
        {
            try
            {
                stream.close();
            }
            catch (IOException iox)
            {
            }
        }
    }

}
