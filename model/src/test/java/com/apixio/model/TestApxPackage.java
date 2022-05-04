package com.apixio.model;

import java.io.*;
import java.util.zip.*;

import com.apixio.model.file.ApxPackagedStream;
import com.apixio.model.file.ApxPackagedStream.ResettableApxPackagedStream;
import com.apixio.security.DecryptedInputStream;
import com.apixio.security.Security;

public class TestApxPackage
{

    public static void main(String ... args) throws Exception
    {
        Security          security = Security.getInstance();
        InputStream       input    = new DecryptedInputStream(new BufferedInputStream(new FileInputStream(args[0])), security);
        ApxPackagedStream apx      = new ApxPackagedStream(new ZipInputStream(input));
        String            src;
        String            out;

        if (args.length == 0)
            throw new IllegalArgumentException("TestApxPackage requires args[0] as input file path");

        src = args[0];
        out = src + ".apx.out";

        if (args.length > 1)
        {
            apx.serialize(new FileOutputStream(out));
            System.out.println("___ fileContentHash=" + apx.getFileContentHash());
        }
        else
        {
            ResettableApxPackagedStream raps = new ResettableApxPackagedStream(apx);
            InputStream                 is   = raps.getFileContent();

            if (true)
            {
                FileOutputStream   fos  = new FileOutputStream(src + ".file");
                byte[]             buf  = new byte[4096];
                int                c;

                System.out.println("... writing to " + src + ".file");
                while ((c = is.read(buf)) >= 0)
                    fos.write(buf, 0, c);

                fos.close();
                System.out.println("___ fileContentHash=" + raps.getFileContentHash());
            }

            System.out.println("...serializing to " + out);
            raps.serialize(new FileOutputStream(out));

            System.out.println("___ final fileContentHash=" + raps.getFileContentHash());
        }
    }
}

