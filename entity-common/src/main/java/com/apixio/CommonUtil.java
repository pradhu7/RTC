package com.apixio;

import com.apixio.restbase.util.DateUtil;
import com.apixio.restbase.web.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class CommonUtil {
    public static ObjectMapper oMapper = new ObjectMapper();

    public static String getCurrentUTCTime()
    {
        Date curTime = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(curTime);
    }

    /*
        This method will transfer input Map to a jsonable string.
     */
    public static String mapObjToString(Map<String, Object> theMap)
    {
        try {
            String res = oMapper.writeValueAsString(theMap);
            return res;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("unable to use jackson trans map structure to string based on the input map: %s. Error: %s", theMap, e.getMessage()), e);
        }
    }

    public static Date iso8601(String parameter, String dateString)
    {
        Date date = null;
        if(dateString!=null) {
            date = DateUtil.validateIso8601(dateString);
            if (date == null) {
                throw BaseException.badRequest(String.format("Invalid date format [%s] provided for property [%s]", dateString, parameter));
            }
        }
        return date;
    }

    /**
     * Util to help you check if all fields in your class (include the fields in super class) are all null
     */
    public static boolean isAllFieldsNull(Object source) throws IllegalAccessException {
        for (Field f : source.getClass().getFields()) {
            if (f.get(source) != null) {
                return false;
            }
        }
        return true;
    }
}
