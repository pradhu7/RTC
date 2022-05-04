package com.apixio.sdk.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 */
public class ToolUtil
{

    public static void saveBytes(String filepath, byte[] bs) throws IOException
    {
        try (OutputStream fos = new FileOutputStream(filepath))
        {
            fos.write(bs);
        }
    }

    public static byte[] readBytes(File filepath) throws IOException
    {
        return Files.readAllBytes(filepath.toPath());
    }

}
