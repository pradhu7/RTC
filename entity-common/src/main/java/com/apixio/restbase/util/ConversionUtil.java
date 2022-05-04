package com.apixio.restbase.util;

import java.nio.ByteBuffer;

public class ConversionUtil {

    final private static char[] HEXCHARS = "0123456789ABCDEF".toCharArray();

    /**
     * Stolen shameless from stackoverflow because I didn't want to pull in another
     * dependency.
     */
    public static String toHex(byte[] bytes)
    {
        return toHex(bytes, 0, bytes.length);
    }

    public static String toHex(byte[] bytes, int from, int to)  // exclusive of 'to'
    {
        if ((from >= to) || (from < 0) || (to > bytes.length))
            throw new IllegalArgumentException("'from' (" + from + ") or 'to' (" + to + ") is out of range of [0.." + bytes.length + ")");

        char[] hexChars = new char[(to - from) * 2];
        int    pos      = 0;

        for ( int j = from; j < to; j++ )
        {
            int v = bytes[j] & 0xff;

            hexChars[pos++] = HEXCHARS[v >>> 4];
            hexChars[pos++] = HEXCHARS[v & 0x0f];
        }

        return new String(hexChars);
    }

    public static byte[] fromHex(String s)
    {
        int    len  = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) (
                (Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16)
                );
        }

        return data;
    }

    public static String byteBufferToHexString(ByteBuffer bb)
    {
        return toHex(bb.array(), bb.position(), bb.limit());
    }

    public static ByteBuffer hexStringToByteBuffer(String hex)
    {
        return ByteBuffer.wrap(fromHex(hex));
    }

    // test
    /*
    public static void main(String[] args)
    {
        byte[] b = new byte[] { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };

        System.out.println(toHex(b));
        System.out.println(toHex(b, 0, 1));
        System.out.println(toHex(b, 1, b.length));
        System.out.println(toHex(b, 1, 2));
        System.out.println(toHex(b, 2, b.length));

    }
    */

}
