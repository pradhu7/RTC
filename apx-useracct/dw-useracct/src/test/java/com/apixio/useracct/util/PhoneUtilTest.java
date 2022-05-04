package com.apixio.useracct.util;

import org.junit.Assert;
import org.junit.Test;

public class PhoneUtilTest {

    @Test
    public void testForUSPhoneNumber()
    {
        Assert.assertEquals("(...) ...-..25", PhoneUtil.getLast2DigitsPhNo("+12132349925"));
    }

    @Test
    public void testForIndianPhoneNumber()
    {
        Assert.assertEquals(".....-...95", PhoneUtil.getLast2DigitsPhNo("+919245653795"));
    }

    @Test
    public void testSomeNationNumber()
    {
        Assert.assertEquals("(...) ...-..23", PhoneUtil.getLast2DigitsPhNo("+9323"));
    }
}
