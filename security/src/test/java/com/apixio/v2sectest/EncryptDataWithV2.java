package com.apixio.v2sectest;

import java.io.*;
import com.apixio.Timer;
import com.apixio.security.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

public class EncryptDataWithV2
{

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
    }

    // commandline:
    // java com.apixio.v2sectest.EncryptDataWithV2 [-scope abc] inputfile outputfile
    public static void main(String... args) throws Exception
    {
        if (args.length > 0)
        {
            EncryptDataWithV2 cvd    = new EncryptDataWithV2();
            CommandLineParser parser = new PosixParser();
            CommandLine       line   = parser.parse(options, args);
            String            scope  = line.getOptionValue("scope");
            String            in     = line.getOptionValue("in");
            String            out    = line.getOptionValue("out");

            cvd.encryptFile(scope, in, out);
        }
    }

    private void encryptFile(String scope, String inFile, String outFile) throws Exception
    {
        Security security = Security.getInstance();
        Timer    t        = new Timer("EncryptDataWithV2");

        // run twice to avoid timing of initialization stuff
        for (int c = 0; c < 2; c++)
        {
            try (InputStream  is = new BufferedInputStream(new FileInputStream(inFile));
                 OutputStream os = new FileOutputStream(outFile))
            {
                InputStream is2 = EncryptedInputStream.encryptInputStreamWithScope(security, is, scope);

                t.start();

                IOUtils.copy(is2, os);
                //            os.close();
            }
        }

        t.stop();
    }

}
