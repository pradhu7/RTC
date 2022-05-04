package com.apixio.dao.annos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

class IndexUtil  // package visibility
{
    static int keybits = 10; // how many bits of the patient id should be used for 'patient groups'; one index file per group

    // convert a UUID to 2 longs
    static long[] asLongs(UUID uuid)
    {
        return new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()};
    }

    // The group key is the last {keybits} bits of the UUID, represetned as a short
    // This key eventually ends up in an index file name, e.g., 479.index.gz
    static short getGroupKey(UUID id)
    {
        return (short) (id.getLeastSignificantBits() & ((1 << keybits) -1 ));
    }
    
    // The inverse of filepath2long
    static String long2filepath(String template, long filelong)
    {
        long dateint = (long) filelong >> 32;
        int filenum = (int) filelong & 0xffffffff;
        String datestr = String.valueOf(dateint);

        //        String template = S3_FOLDER_TEMPLATE + "/annotationscience_prd+%s+%010d.json.gz";
        String othernumber = datestr.substring(8, 10);
        if (othernumber.startsWith("0"))
            othernumber = othernumber.substring(1);
        String daystr = datestr.substring(6, 8);
        String mostr = datestr.substring(4, 6);
        String yearstr = datestr.substring(0, 4);
        String filepath = String.format(template, yearstr, mostr, daystr, othernumber, filenum);
        return filepath;
    }

    // Compress an annotation file path into a long for efficient storage.
    static long filepath2long(String filepath)
    {
        // e.g. prod/topics/annotationscience_prd/year=2020/month=02/day=16/annotationscience_prd+0+0010092424.json.gz
        //annotationscience_prd+0+0004911956.json.gz
        // first int (dateint) is YYYYMMDD plus the one or two digit "0" number in the path above
        // second int (filenum) is the 0010092424 part of the path above
        // first int concat second int is the long representation of the file
        String[] parts = filepath.split("/");
        String[] last = parts[parts.length-1].split("\\+");

        String daystr = parts[parts.length-2].split("=")[1];
        String mostr = parts[parts.length-3].split("=")[1];
        String yearstr = parts[parts.length-4].split("=")[1];
        int filenum = Integer.parseInt(last[last.length-1].substring(0, 10));
        String other = "0" + last[last.length-2];
        other = other.substring(other.length()-2);
        long dateint = Long.parseLong(yearstr + mostr +  daystr + other);
        return (dateint << 32) + filenum;
    }

    static List<String> readLinesFromGzStream(String filePath) throws FileNotFoundException, IOException
    {
        try (InputStream fileStream = new FileInputStream(filePath))
        {
            return readLinesFromGzStream(fileStream);
        }
    }

    static List<String> readLinesFromGzStream(InputStream stream)
    {
        List<String> alllines = new ArrayList<String>();

        try (InputStream    gzipStream = new GZIPInputStream(stream);
             Reader         decoder    = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
             BufferedReader br         = new BufferedReader(decoder))
        {
            String thisLine;

            while ((thisLine = br.readLine()) != null)
                alllines.add(thisLine);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return alllines;
    }

    /**
     *
     */
    public static void main(String... args) throws Exception
    {
        String templatePath = args[0];

        for (int idx = 1; idx < args.length; idx++)
        {
            UUIDToAnnotationFileIndexer.dumpIndexFile(templatePath, new java.io.File(args[idx]));
        }
    }

}
