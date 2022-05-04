package com.apixio.v2sectest;

import java.io.*;
import com.apixio.security.*;
import com.apixio.Timer;
import org.apache.commons.cli.*;

public class DecryptTextWithV2
{

    protected static Options options = new Options();
    static
    {
        Option req;

        req = new Option(null, "in",  true, "Input file");
        options.addOption(req);

        req = new Option(null, "out",  true, "Output file");
        options.addOption(req);

        req = new Option(null, "hex",  false, "Encrypt to hexadecimal string");
        options.addOption(req);

        req = new Option(null, "outdir",  true, "Directory path to put decrypted data files");
        options.addOption(req);
    }

    private Security security = Security.getInstance();

    // usage:  [-in file -out file] [-hex]
    //         [-hex] str str str ...
    //         -outdir dir infile ...
    public static void main(String... args) throws Exception
    {
        DecryptTextWithV2 cvd   = new DecryptTextWithV2();

        if (args.length > 0)
        {
            CommandLineParser parser  = new PosixParser();
            CommandLine       line    = parser.parse(options, args);
            boolean           asHex   = line.hasOption("hex");
            String            infile  = line.getOptionValue("in");
            String            outfile = line.getOptionValue("out");
            String            outdir  = line.getOptionValue("outdir");
            Timer             t       = new Timer("DecryptTextWithV2");

                // run twice to avoid timing of initialization stuff
            for (int c = 0; c < 2; c++)
            {
                if (outdir != null)
                {
                    for (String file : line.getArgs())
                        cvd.decryptTextFile(t, asHex, file, outdir + "/" + (new File(file)).getName());
                }
                else if ((infile != null) && (outfile != null))
                {
                    cvd.decryptTextFile(t, asHex, infile, outfile);
                }
                else
                {
                    for (String str : line.getArgs())
                        System.out.println(str + "\t" + cvd.decryptString(str, asHex));
                }
            }
        }
    }

    private void decryptTextFile(Timer t, boolean asHex, String infile, String outfile) throws Exception
    {
        try (InputStream  is = new FileInputStream(infile);
             OutputStream os = new FileOutputStream(outfile))
        {
            t.start();
            os.write(decryptString(readFile(is), asHex).getBytes("UTF-8"));
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

    private String decryptString(String s, boolean asHex) throws Exception
    {
        return (asHex) ? security.decryptFromHex(s) : security.decrypt(s);
    }

}
