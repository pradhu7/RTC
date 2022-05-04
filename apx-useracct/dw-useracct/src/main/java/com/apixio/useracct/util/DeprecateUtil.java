package com.apixio.useracct.util;

import com.apixio.SysServices;

public class DeprecateUtil {
    public static String getRedisValue(SysServices sys, String key)
    {
        String keyWithEnv = sys.getProjects().makeKey(key);
        return sys.getRedisOps().get(keyWithEnv);
    }
}
