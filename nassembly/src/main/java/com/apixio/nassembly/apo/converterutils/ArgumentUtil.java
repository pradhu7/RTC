package com.apixio.nassembly.apo.converterutils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Objects;

public class ArgumentUtil {

    public static boolean isNotNull(Object obj) {
        return Objects.nonNull(obj);
    }

    public static boolean isNull(Object obj) {
        return Objects.isNull(obj);
    }

    public static boolean isEmpty(String string) {
        return StringUtils.isEmpty(string);
    }

    public static boolean isEmpty(Collection coll) {
        return CollectionUtils.isEmpty(coll);
    }

    public static boolean isNotEmpty(Collection coll) {
        return CollectionUtils.isNotEmpty(coll);
    }

    public static boolean isNotEmpty(String string) {
        return StringUtils.isNotEmpty(string);
    }

    public static boolean toBoolean(String string) {
         return (string.toLowerCase().contains("true") || string.toLowerCase().contains("t")
                || string.toLowerCase().contains("yes") || string.toLowerCase().contains("y"));
    }


}
