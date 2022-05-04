package com.apixio.v1sectest;

import java.io.*;
import com.apixio.v1security.*;
import com.apixio.Timer;

public class EncryptTextWithV1
{
    private Security security = Security.getInstance();

    // usage:  [-in file -out file] [-hex]
    //         [-hex] str str str ...
    public static void main(String... args) throws Exception
    {
        EncryptTextWithV1 cvd   = new EncryptTextWithV1();
        boolean           doAll = true;

        if (args.length >= 4)
        {
            if (args[0].equals("-in") && args[2].equals("-out"))
            {
                boolean asHex = (args.length > 4) && args[4].equals("-hex");
                Timer   t     = new Timer("EncryptTextWithV1");

                // run twice to avoid timing of initialization stuff
                for (int c = 0; c < 2; c++)
                {
                    try (InputStream  is = new FileInputStream(args[1]);
                         OutputStream os = new FileOutputStream(args[3]))
                    {
                        t.start();
                        os.write(cvd.encryptString(readFile(is), asHex).getBytes("UTF-8"));
                    }
                }

                t.stop();
                doAll = false;
            }
        }

        if (doAll && (args.length > 0))
        {
            boolean asHex = args[0].equals("-hex");
            int     start = (asHex) ? 1 : 0;

            for (int i = start; i < args.length; i++)
                System.out.println(args[i] + "\t" + cvd.encryptString(args[i], asHex));
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

    private String encryptString(String s, boolean asHex) throws Exception
    {
        return (asHex) ? security.encryptToHex(s) : security.encrypt(s);
    }

}
