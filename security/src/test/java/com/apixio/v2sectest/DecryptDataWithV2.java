package com.apixio.v2sectest;

import java.io.*;
import com.apixio.Timer;
import com.apixio.security.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

public class DecryptDataWithV2
{

    protected static Options options = new Options();
    static
    {
        Option req;

        req = new Option(null, "outdir",  true, "Directory path to put decrypted data files");
        options.addOption(req);
    }

    // commandline:
    // java com.apixio.v2sectest.DecryptDataWithV2 inputfile outputfile
    // java com.apixio.v2sectest.DecryptDataWithV2 -outdir dir infile ...

    public static void main(String... args) throws Exception
    {
        if (args.length > 0)
        {
            DecryptDataWithV2 cvd    = new DecryptDataWithV2();
            CommandLineParser parser = new PosixParser();
            CommandLine       line   = parser.parse(options, args);
            String            outdir = line.getOptionValue("outdir");

            if (outdir != null)
            {
                for (String file : line.getArgs())
                {
                    cvd.decryptFile(file, outdir + "/" + (new File(file)).getName());
                }
            }
            else if (args.length == 2)
            {
                cvd.decryptFile(args[0], args[1]);
            }
        }
    }

    public void decryptFile(String inFile, String outFile) throws Exception
    {
        Security security = Security.getInstance();
        Timer    t        = new Timer("DecryptDataWithV2");

        // run twice to avoid timing of initialization stuff
        for (int c = 0; c < 2; c++)
        {
            try (InputStream  is = new BufferedInputStream(new FileInputStream(inFile));
                 OutputStream os = new FileOutputStream(outFile))
            {
                InputStream is2 = new DecryptedInputStream(is, security);

                t.start();

                IOUtils.copy(is2, os);
            }
        }

        t.stop();
    }

}
