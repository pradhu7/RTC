package com.apixio.dao.cmdline.eventtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.apixio.dao.Constants;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.utility.HadoopUtility;
import com.apixio.utility.PropertyHelper;

/**
 * EventConfigTool is the single configuration point for specifying science project dynamic properties
 *
 *  1.  saves a new java properties in a persistent store
 *  2.  updates the latest java properties in a persistent store
 *  3.  loads/prints any persistent java properties from a persistent store
 *  4.  deletes any persistent java properties
 *  5.  Interprets certain properties:
 *      - $modelFile means copy the file to hdfs to be used for distributed cache
 *
 */
public class EventConfigTool
{
    /**
     * Only one of these categories can be specified per command invocation.
     */
    private enum Action {SAVE, UPDATE, LOAD, DELETE, CONFIG};

    private static final String PREFIX    = "prefix";
    private static final String REDIS_HOST   = "redisHost";

    private static final String HDFSDIR       = "hdfsDir";
    private static final String PROPERTY_NAME = "propertyName";

    private static final String ORG_ID     = "orgId";

    private static final String HELP       = "help";

    private static final String SAVE       = "save";
    private static final String UPDATE     = "update";
    private static final String LOAD       = "load";
    private static final String DELETE     = "delete";
    private static final String CONFIG     = "config";

    private static final String VERSION    = "version";
    private static final String FILE       = "file";

    private Action action;

    private String prefix;
    private String host;

    private String     name;
    private String     orgId;
    private String     hdfsDir;

    private String     filename;
    private long       version;
    private boolean    runProgram = false;

    private PropertyHelper propertyHelper;

    private static Options cOptions = new Options();
    static
    {
        cOptions.addOption(null, PREFIX,      true, "Prefix for Redis keys");
        cOptions.addOption(null, REDIS_HOST,  true, "Redis host");

        cOptions.addOption(null, HDFSDIR,         true, "HDFS Directory");
        cOptions.addOption(null, PROPERTY_NAME,   true, "Property Name");

        cOptions.addOption(null, ORG_ID,          true, "org id");

        cOptions.addOption(null, HELP,     false, "Prints help information and exit");

        cOptions.addOption(null, SAVE,     false, "Saves a new event configuration properties");
        cOptions.addOption(null, UPDATE,   false, "Updates the last version of the event configuration properties");
        cOptions.addOption(null, LOAD,     false, "Loads an existing event configuration properties to a file");
        cOptions.addOption(null, DELETE,   false, "Deletes an existing event configuration properties");
        cOptions.addOption(null, CONFIG,   false, "Get the names of all existing event configuration files");

        cOptions.addOption(null, VERSION,  true,  "Version to load");
        cOptions.addOption(null, FILE,     true,  "Configuration location/file to read from or save to");
    }

    public static void main(String[] args)
    {
        try
        {
            EventConfigTool tool = new EventConfigTool(args);

            System.exit(tool.run());
        }
        catch (IllegalArgumentException a)
        {
            System.out.println("\nERROR:  " + a.getMessage());
            a.printStackTrace();
        }
        catch (Exception x)
        {
            x.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructor initializes Spring and gets required beans.
     */
    private EventConfigTool(String[] args) throws Exception
    {
        parseArgs(args);

        RedisOps redisOps = new RedisOps(host, 6379);
        Transactions redisTransactions = redisOps.getTransactions();

        propertyHelper = new PropertyHelper();
        propertyHelper.setPrefix(prefix);
        propertyHelper.setRedisOps(redisOps);
        propertyHelper.setTransactions(redisTransactions);
    }

    /**
     *
     */
    private void parseArgs(
        String[] args
        )
    {
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine       line   = parser.parse(cOptions, args);

            if (line.hasOption(HELP))
            {
                printUsage();
            }
            else
            {
                if (line.hasOption(PREFIX))
                {
                    prefix = line.getOptionValue(PREFIX);
                }

                if (line.hasOption(REDIS_HOST))
                {
                    host = line.getOptionValue(REDIS_HOST);
                }

                if (line.hasOption(HDFSDIR))
                {
                    hdfsDir = line.getOptionValue(HDFSDIR);
                }

                if (line.hasOption(PROPERTY_NAME))
                {
                    name = line.getOptionValue(PROPERTY_NAME);
                }

                if (line.hasOption(ORG_ID))
                {
                    orgId = line.getOptionValue(ORG_ID);
                }

                if (line.hasOption(SAVE))
                {
                    action = Action.SAVE;
                }
                else if (line.hasOption(UPDATE))
                {
                    action = Action.UPDATE;
                }
                else if (line.hasOption(LOAD))
                {
                    action = Action.LOAD;
                }
                else if (line.hasOption(DELETE))
                {
                    action = Action.DELETE;
                }
                else if (line.hasOption(CONFIG))
                {
                    action = Action.CONFIG;
                }

                if (line.hasOption(VERSION))
                {
                    version = Long.valueOf(line.getOptionValue(VERSION));
                }

                if (line.hasOption(FILE))
                {
                    filename = line.getOptionValue(FILE);
                }

                if (action == null)
                    printUsage();
                else
                    runProgram = true;
            }
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
    }

    private int run()
        throws Exception
    {
        if (runProgram)
        {
            switch (action)
            {
                case SAVE:
                    saveOrUpdateConfig(action);
                    break;
    
                case UPDATE:
                    saveOrUpdateConfig(action);
                    break;
    
                case LOAD:
                    loadConfig();
                    break;
    
                case DELETE:
                    deleteConfig();
                    break;
    
                case CONFIG:
                    getConfigNames();
                    break;
            }

            System.out.println("done");
        }

        return 0;
    }

    private void saveOrUpdateConfig(Action action) throws Exception
    {
        if (name == null || filename == null)
        {
            System.out.println("Save/Update requires config name and filename args");
            return;
        }

        Properties properties = getProperties();
        if (properties == null || properties.size() == 0)
        {
            System.out.println("Can't save/update null or empty properties");
            return;
        }

        String modelFile = properties.getProperty(Constants.Event.MODEL_FILE);
        if (modelFile != null)
        {
            File mFile = new File(modelFile);
            if (mFile != null && mFile.exists())
            {
                Long versionNo = createVersionNum();
                
                properties.setProperty(Constants.Event.PROPERTY_VERSION, Long.toString(versionNo));
                
                String modelVersion = makeModelVersion(mFile.getName(), Long.toString(versionNo));
                
                System.out.println("Copying local file:" + modelFile + " to hdfs:" + hdfsDir + "/" + modelVersion);
                HadoopUtility.copyModelToHDFS(modelFile, hdfsDir + "/" + modelVersion);
                
                properties.setProperty(Constants.Event.Model_NAME, modelVersion.substring(0, modelVersion.lastIndexOf(".")));

                switch (action)
                {
                    case SAVE:
                        propertyHelper.saveProperties(makeConfigName(), properties, versionNo);
                        break;

                    case UPDATE:
                        propertyHelper.updateProperties(makeConfigName(), properties, versionNo);
                        break;
                        
                    default:
                        break;
                }
            }
            else
                throw new Exception("File not found:" + modelFile);
        }
        else
            throw new Exception("No Model File Specified");
    }

    private void loadConfig()
    {
        if (name == null || filename == null)
        {
            System.out.println("Load requires config name and filename args");
            return;
        }

        Properties properties;

        if (version != 0L)
        {
            properties = propertyHelper.loadProperties(makeConfigName(), version);
        }
        else
        {
            properties = propertyHelper.loadProperties(makeConfigName());
        }

        if (properties == null || properties.size() == 0)
        {
            System.out.println("Can't load null or empty properties");
            return;
        }

        savePropertiesToFile(properties);
    }

    private void deleteConfig()
    {
        if (name == null)
        {
            System.out.println("Delete requires config name arg");
            return;
        }

        if (version != 0L)
        {
            propertyHelper.deleteProperties(makeConfigName(), version);
        }
        else
        {
            propertyHelper.deleteProperties(makeConfigName());
        }
    }

    private void getConfigNames()
    {
        if (name == null)
        {
            System.out.println("Config requires config name arg");
            return;
        }

        List<String> names = propertyHelper.getAllConfigNames(name);

        for (String name: names)
        {
            System.out.println(name);
        }
    }

    private String makeConfigName()
    {
        String configName = name;
        if (orgId != null)
            configName = name + orgId;

        return configName;
    }

    private Properties getProperties()
    {
        InputStream input = null;

        try
        {
            input = new FileInputStream(filename);

            Properties properties = new Properties();
            properties.load(input);

            return properties;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }

    private void savePropertiesToFile(Properties properties)
    {
        OutputStream output = null;

        try
        {
            output = new FileOutputStream(filename);

            properties.store(output, null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (output != null)
            {
                try
                {
                    output.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void printUsage()
    {
        System.out.println();

        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp("EventConfigTool: ", cOptions, true);
    }
    
    private String makeModelVersion(String modelFileName, String versionNumber)
    {
        String org = (orgId == null) ? "" : orgId;

        if (modelFileName != null && modelFileName.contains("."))
        {
            String filename = modelFileName.substring(0, modelFileName.lastIndexOf("."));
            return filename + org + versionNumber + modelFileName.substring(modelFileName.lastIndexOf("."));
        }

        return modelFileName + org + versionNumber;
    }
    
    private long createVersionNum()
    {
        return System.currentTimeMillis();
    }
}
