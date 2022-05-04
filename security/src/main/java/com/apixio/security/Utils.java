package com.apixio.security;

import java.util.Base64;

public class Utils
{

    private static Base64.Encoder b64Encoder = Base64.getEncoder();
    private static Base64.Decoder b64Decoder = Base64.getDecoder();

    private static char hexChar[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     *
     */
    public static String encodeBase64(byte b[])
    {
        return b64Encoder.encodeToString(b);
    }

    public static byte[] decodeBase64(String s)
    {
        if (s == null)
            return null;
        else
            return b64Decoder.decode(s);
    }

    protected static String getHexString(byte b[])
    {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for(int i = 0; i < b.length; i++)
        {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0xf]);
        }

        return sb.toString();
    }

    protected static byte[] getBinArray(String hexStr)
    {
        byte bArray[] = new byte[hexStr.length() / 2];
        for(int i = 0; i < hexStr.length() / 2; i++)
        {
            byte firstNibble = Byte.parseByte(hexStr.substring(2 * i, 2 * i + 1), 16);
            byte secondNibble = Byte.parseByte(hexStr.substring(2 * i + 1, 2 * i + 2), 16);
            int finalByte = secondNibble | firstNibble << 4;
            bArray[i] = (byte)finalByte;
        }
        return bArray;
    }

}
