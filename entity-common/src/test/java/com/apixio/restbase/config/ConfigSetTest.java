package com.apixio.restbase.config;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by mramanna on 8/29/16.
 */
public class ConfigSetTest {

    Map<String, Object> testMap;
    ConfigSet configSet;


    @Before
    public void setUp() throws IOException {

        testMap = new HashMap<>();

        testMap.put("doubleKeyString", "11");
        testMap.put("doubleKey", new Double(11));
        configSet = ConfigSet.fromMap(testMap);
    }


    @Test
    public void testGetDouble() {

        Double dbl = configSet.getDouble("doubleKey");
        assertThat(dbl, is(new Double(11)));
    }

    @Test(expected = Exception.class)
    public void getGetDoubleException() {

        Double dbl = configSet.getDouble("doubleKeyString");

    }


}
