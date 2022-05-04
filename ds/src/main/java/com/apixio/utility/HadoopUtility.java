package com.apixio.utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HadoopUtility
{
    public static void copyModelToHDFS(String localFile, String hdfsDir)
        throws IOException
    {
        Configuration mainConfig = new Configuration();
        
        FileSystem fileSystem = FileSystem.get(mainConfig);

        File          file     = new File(localFile);
        String        absPath  = file.getAbsolutePath();
        
        Path dst = new Path(hdfsDir);

        fileSystem.copyFromLocalFile(new Path(absPath), dst);   
    }

    public static void checkForCoreSite()
    {
        InputStream coreDefault = HadoopUtility.class.getResourceAsStream("/core-site.xml");

        // ya know, this isn't necessary if everything is set up fine, but it's a
        // nice way to alert the user as to what's wrong.  if we don't do this kind
        // of checking, then a far more generic error is thrown from the guts of the
        // hadoop setup (something like "EOFException"...)
        if (coreDefault == null)
            throw new IllegalStateException("Unable to find core-default.xml in CLASSPATH!  " +
                                            "CLASSPATH must include hadoop's \"conf\" directory (to be able to load core-site.xml)");

        try
        {
            coreDefault.close();
        }
        catch (IOException iox)
        {
        }
    }
}
