package com.apixio.aclsys.dao;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;

/**
 */
public class DaoUtil
{
    /**
     * Simple wrappers for converting String to/from ByteBuffer.
     */
    public static ByteBuffer toByteBuffer(String s)
    {
        try
        {
            return ByteBuffer.wrap(s.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException x)
        {
            x.printStackTrace();

            return null;
        }
    }

    public static String stringFromByteBuffer(ByteBuffer bb)
    {
        try
        {
            int pos = bb.position();

            return new String(bb.array(), pos, bb.limit() - pos, "UTF-8");
        }
        catch (UnsupportedEncodingException x)
        {
            x.printStackTrace();

            return null;
        }
    }

}
