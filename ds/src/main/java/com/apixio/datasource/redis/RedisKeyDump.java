package com.apixio.datasource.redis;

import java.util.List;
import redis.clients.jedis.Pipeline;

import com.apixio.utility.StringList;

/**
 * RedisKeyDump provides key dump and restore functions to duplicating key+value
 * in redis, along with optionally changing the key prefix.
 */
public class RedisKeyDump {

    final private static char[] HEXCHARS = "0123456789ABCDEF".toCharArray();

    /**
     * Dump the key to a string that can be passed into restoreKey.  If the key
     * doesn't exist an exception is thrown.
     */
    public static String dumpKey(RedisOps ops, String key)
    {
        Long pttl = ops.pttl(key);

        if ((pttl == null) || (pttl == -2L))
            throw new IllegalArgumentException("Key " + key + " doesn't exist.");
        else if (pttl == -1L)
            pttl = 0L;

        return StringList.flattenList(
            key, pttl.toString(), toHex(ops.dump(key))
            );
    }

    /**
     * Restores the serialized redis key+value, optionally changing the prefix of the
     * key.
     */
    public static boolean restoreKey(RedisOps ops, String serialized, String fromPrefix, String toPrefix)
    {
        List<String> unser = StringList.restoreList(serialized);
        String       key   = unser.get(0);

        if ((fromPrefix != null) && (toPrefix != null))
        {
            if (key.startsWith(fromPrefix))
                key = toPrefix + key.substring(fromPrefix.length());
        }

        return "OK".equals(ops.restore(key,
                                       Integer.valueOf(unser.get(1)),
                                       fromHex(unser.get(2))));
    }

    /**
     * Stolen shameless from stackoverflow because I didn't want to pull in another
     * dependency.
     */
    private static String toHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];

        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xff;

            hexChars[j * 2]     = HEXCHARS[v >>> 4];
            hexChars[j * 2 + 1] = HEXCHARS[v & 0x0f];
        }

        return new String(hexChars);
    }

    private static byte[] fromHex(String s)
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

}
