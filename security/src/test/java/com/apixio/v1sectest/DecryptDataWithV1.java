package com.apixio.v1sectest;

import java.io.*;
import org.apache.commons.io.IOUtils;
import com.apixio.v1security.*;
import com.apixio.Timer;

public class DecryptDataWithV1
{

    // commandline:
    // java com.apixio.v1sectest.DecryptDataWithV1 inputfile outputfile
    public static void main(String... args) throws Exception
    {
        if (args.length != 2)
            throw new IllegalArgumentException("Usage:  java com.apixio.v1sectest.DecryptDataWithV1 inputfile outputfile");

        DecryptDataWithV1 cvd = new DecryptDataWithV1();

        cvd.decryptFile(args[0], args[1]);
    }

    public void decryptFile(String inFile, String outFile) throws Exception
    {
        Security security = Security.getInstance();
        Timer    t        = new Timer("DecryptDataWithV1");

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
