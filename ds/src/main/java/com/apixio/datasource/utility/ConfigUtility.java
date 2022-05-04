package com.apixio.datasource.utility;

import java.util.Map;

/**
 *
 */
public class ConfigUtility
{
    /**
     * Lookup the configName key in the map and return it, throwing an exception if it's not present
     */
    public static <T> T getRequiredConfig(Map<String, Object> config, Class<T> requiredType, String configName)
    {
        Object val = config.get(configName);

        if (val == null)
            throw new IllegalArgumentException("Required configuration '" + configName + "' not supplied");
        else if (!requiredType.isAssignableFrom(val.getClass()))
            throw new IllegalArgumentException("Required configuration '" + configName + "' value '" + val + "' has wrong type; required type is " + requiredType);

        return requiredType.cast(val);
    }

    /**
     * Lookup the configName key in the map and return it, returning the passed in default value if it's not present
     */
    public static <T> T getOptionalConfig(Map<String, Object> config, Class<T> requiredType, String configName, T defValue)
    {
        Object val = config.get(configName);

        if (val == null)
            return defValue;
        else if (!requiredType.isAssignableFrom(val.getClass()))
            throw new IllegalArgumentException("Required configuration '" + configName + "' value '" + val + "' has wrong type; required type is " + requiredType);

        return requiredType.cast(val);
    }

}
