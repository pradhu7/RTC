package com.apixio.v1sectest;

import java.io.*;
import org.apache.commons.io.IOUtils;
import com.apixio.v1security.*;
import com.apixio.Timer;

public class EncryptDataWithV1
{

    // commandline:
    // java com.apixio.v1sectest.EncryptDataWithV1 inputfile outputfile
    public static void main(String... args) throws Exception
    {
        if (args.length != 2)
            throw new IllegalArgumentException("Usage:  java com.apixio.v1sectest.EncryptDataWithV1 inputfile outputfile");

        EncryptDataWithV1 cvd = new EncryptDataWithV1();

        cvd.encryptFile(args[0], args[1]);
    }

    public void encryptFile(String inFile, String outFile) throws Exception
    {
        Security security = Security.getInstance();
        Timer    t        = new Timer("EncryptDataWithV1");

        // run twice to avoid timing of initialization stuff
        for (int c = 0; c < 2; c++)
        {
            try (InputStream  is = new BufferedInputStream(new FileInputStream(inFile));
                 OutputStream os = new FileOutputStream(outFile))
            {
                InputStream is2 = security.encryptInputStream(is);

                t.start();

                IOUtils.copy(is2, os);
                //            os.close();
            }
        }

        t.stop();
    }

}
