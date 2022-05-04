package com.apixio.v2sectest;

import java.io.*;
import com.apixio.Timer;
import com.apixio.security.*;
import org.apache.commons.cli.*;

public class EncryptTextWithV2
{
    private Security security = Security.getInstance();

    protected static Options options = new Options();
    static
    {
        Option req;

        req = new Option(null, "in",  true, "File path to data to encrypt");
        req.setRequired(true);
        options.addOption(req);

        req = new Option(null, "out",true, "File path to output file");
        req.setRequired(true);
        options.addOption(req);

        options.addOption(null, "scope", true,  "Scope (PDS id) with datakey to use");
        options.addOption(null, "hex",   false, "Encrypt to hexadecimal");
    }

    // usage:  [-in file -out file] [-scope scope] [-hex]
    //         [-hex] str str str ...
    public static void main(String... args) throws Exception
    {
        if (args.length > 0)
        {
            EncryptTextWithV2 cvd    = new EncryptTextWithV2();
            CommandLineParser parser = new PosixParser();
            CommandLine       line   = parser.parse(options, args);
            boolean           asHex  = line.hasOption("hex");
            String            scope  = line.getOptionValue("scope");
            String            in     = line.getOptionValue("in");
            String            out    = line.getOptionValue("out");
            Timer             t      = new Timer("EncryptTextWithV2");

            // run twice to avoid timing of initialization stuff
            for (int c = 0; c < 2; c++)
            {
                try (InputStream  is = new FileInputStream(in);
                     OutputStream os = new FileOutputStream(out))
                    {
                        t.start();
                        os.write(cvd.encryptString(readFile(is), asHex, line.getOptionValue("scope")).getBytes("UTF-8"));
                    }
            }

                t.stop();
        }
    }

    private static String readFile(InputStream is) throws IOException
    {
        BufferedReader r  = new BufferedReader(new InputStreamReader(is));
        StringBuilder  sb = new StringBuilder();
        String         s;

        while ((s = r.readLine()) != null)
        {
            sb.append(s);
            sb.append("\n");
        }

        return sb.toString();
    }

    private String encryptString(String s, boolean asHex, String scope) throws Exception
    {
        if (asHex)
            return security.encryptToHex(s);
        else
            return security.encryptWithScope(s, scope);
    }

}
