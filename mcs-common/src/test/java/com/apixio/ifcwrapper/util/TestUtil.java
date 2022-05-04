package com.apixio.ifcwrapper.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class TestUtil {

    public static String resourceToString(String resource) throws Exception {
        ClassLoader classLoader = TestUtil.class.getClassLoader();
        InputStream is = classLoader.getResource(resource).openStream();
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
}
