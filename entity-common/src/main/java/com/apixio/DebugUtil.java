package com.apixio;

import com.apixio.utility.StringUtil;

public class DebugUtil {

    /**
     */
    public static String subargs(
        String     format,
        Object...  args
        )
    {
        return StringUtil.subargsPos(format, args);
    }

    public static void LOG(String fmt, Object... args)
    {
        System.out.println("SFM:  " + subargs(fmt, args));
    }

}
