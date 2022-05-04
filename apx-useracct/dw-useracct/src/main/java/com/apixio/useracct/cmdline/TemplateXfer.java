package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.apixio.XUUID;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.config.EmailConfig;
import com.apixio.useracct.dao.*;
import com.apixio.useracct.entity.*;

/**
 *
 */
public class TemplateXfer extends CmdlineBase
{
    private static final String cUsage = ("Usage:  TemplateXfer -c <conn-yamlfile> id1 id2 ...\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;
        List<String> templates;

        Options(String connectionYaml, List<String> templates)
        {
            this.connectionYamlFile = connectionYaml;
            this.templates          = templates;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private static PrivSysServices   sysServices;
    private static TextBlobLogic     textBlobLogic;
    private static Transactions      transactions;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options              options   = parseOptions(new ParseState(args));
        ConfigSet            config    = null;

        if ((options == null) || ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        sysServices = setupServices(config);

        textBlobLogic = sysServices.getTextBlobLogic();
        transactions  = sysServices.getRedisTransactions();

        try
        {
            transferTemplates(config, options.templates);
        }
        catch (Exception x)
        {
            x.printStackTrace();
            System.exit(1);
        }

        System.exit(0);  // jedis or cql creates non-daemon thread.  boo
    }

    /**
     *
     */
    private static void transferTemplates(ConfigSet config, List<String> templates) throws IOException
    {
        try
        {
            transactions.begin();

            handleEmail(config, templates);

            transactions.commit();
        }
        catch (Exception x)
        {
            transactions.abort();
            x.printStackTrace();
            System.out.println("\n################# NO changes made to redis due to error\n");
        }
    }

    /**
     *
     */
    private static void handleEmail(ConfigSet config, List<String> templates) throws IOException
    {
        if (templates == null)
            return;

        String  dir = config.getString("emailConfig.templates");

        if (!dir.endsWith("/"))
            dir = dir + "/";

        for (String contentID : templates)
        {
            transfer(dir, contentID + ".subject");
            transfer(dir, contentID + ".html");
            transfer(dir, contentID + ".plain");
        }
    }

    private static void transfer(String templateDir, String id) throws IOException
    {
        InputStream is = null;

        try
        {
            is = TemplateXfer.class.getResourceAsStream(templateDir + id);

            if (is != null)
            {
                StringBuilder     sb  = new StringBuilder();
                InputStreamReader isr = new InputStreamReader(is);
                char[]            buf = new char[4096];
                TextBlobDTO       dto = new TextBlobDTO();
                int               n;

                while ((n = isr.read(buf, 0, buf.length)) != -1)
                    sb.append(buf, 0, n);

                dto.name     = id;
                dto.contents = sb.toString();

                textBlobLogic.putTextBlob(id, dto);

                System.out.println("Transferred [" + id + "]");
            }
            else
            {
                System.out.println("Unable to transfer [" + id + "]:  resource not found.");
            }
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException iox)
                {
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

        if (connection == null)
            return null;

        return new Options(connection, ps.whatsLeftList());
    }

    /**
     * Load configuration from .yaml file into generic Map.
     */
    private static Map<String, Object> loadYaml(String filepath) throws Exception
    {
        InputStream input = new FileInputStream(new File(filepath));
        Yaml        yaml  = new Yaml();
        Object      data = yaml.load(input);

        input.close();

        return (Map<String, Object>) data;
    }

    private static boolean isEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
