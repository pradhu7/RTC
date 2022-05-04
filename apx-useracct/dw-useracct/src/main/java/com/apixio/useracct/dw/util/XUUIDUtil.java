package com.apixio.useracct.dw.util;

import com.apixio.XUUID;

import static com.apixio.restbase.web.BaseException.badRequest;

/**
 * This class is a shared class used by resources to getting their xuuid
 * also handling some bad requests
 */
public class XUUIDUtil {

    /**
     * Method to catch invalid xuuid strings throwing IllegalArgumentExceptions
     * @param xuuidStr
     * @param type
     * @return
     */
    public static XUUID getXuuid(String xuuidStr, String type)
    {
        XUUID xuuid = null;

        try
        {
            xuuid = XUUID.fromString(xuuidStr, type);
        }
        catch (IllegalArgumentException iae)
        {
            throw badRequest(iae.getMessage());
        }

        return xuuid;
    }
}
