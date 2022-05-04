package com.apixio.dao.cmdline;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.apixio.restbase.config.*;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.apixio.model.utility.ApixioDateSerializer;
import com.apixio.model.utility.EitherStringOrNumberDeserializer;
import com.apixio.model.utility.EitherStringOrNumberSerializer;


public abstract class CmdLineBase
{
    protected static Options cOptions = new Options();
    static
    {
        cOptions.addOption(null,  "help",    false, "Print help information and exit");
        cOptions.addOption(null,  "input",   true,  "Input file - required");
        cOptions.addOption(null,  "output",  true,  "Output file - required");
        cOptions.addOption(null,  "yaml",    true,  "yaml config file - required");
    }

    protected boolean  runProgram;

    protected DaoServices  daoServices;
    protected ObjectMapper objectMapper;

    protected String inputFile;
    protected String outputFile;
    protected String yamlFile;

    protected void parseCommandLine(CommandLine line, String[] args)
    {
        if (line.hasOption("help"))
        {
            runProgram = false;
        }
        else
        {
            runProgram = true;

            if (line.hasOption("input"))
            {
                inputFile = line.getOptionValue("input");
            }

            if (line.hasOption("output"))
            {
                outputFile = line.getOptionValue("output");
            }

            if (line.hasOption("yaml"))
            {
                yamlFile = line.getOptionValue("yaml");
            }

        }
    }

    protected void printUsage(String name)
    {
        (new HelpFormatter()).printHelp(name + ": ", cOptions);
    }

    protected void setupObjectMapper()
    {
        objectMapper = new ObjectMapper();

        SimpleModule module1 = new SimpleModule("DateTimeDeserializerModule");
        module1.addDeserializer(DateTime.class, new ApixioDateDeserialzer());

        objectMapper.registerModule(module1);

        SimpleModule module2 = new SimpleModule("EitherStringOrNumberDeserializerModule");
        module2.addDeserializer(EitherStringOrNumber.class, new EitherStringOrNumberDeserializer());

        objectMapper.registerModule(module2);

        SimpleModule module3 = new SimpleModule("DateTimeSerializerModule");
        module3.addSerializer(DateTime.class, new ApixioDateSerializer());

        objectMapper.registerModule(module3);

        SimpleModule module4 = new SimpleModule("EitherStringOrNumberSerializerModule");
        module4.addSerializer(EitherStringOrNumber.class, new EitherStringOrNumberSerializer());

        objectMapper.registerModule(module4);
    }

    protected void setupDaoServices() throws Exception
    {
        ConfigSet configuration = ConfigSet.fromYamlFile((new File(yamlFile)));

        daoServices = DaoServicesSet.createDaoServices(configuration);
    }

    protected void setupDaoServices(boolean local) throws Exception
    {
        ConfigSet configuration = ConfigSet.fromYamlFile((new File(yamlFile)));

        Map<String, Object> override = new HashMap<>();
        override.put("customerPropertiesConfig.local", local);

        configuration.merge(override);

        daoServices = DaoServicesSet.createDaoServices(configuration);
    }

    protected abstract void setup() throws Exception;
    protected abstract void executeProgram() throws Exception;
    protected abstract void run() throws Exception;
    protected abstract void done() throws Exception;
}
