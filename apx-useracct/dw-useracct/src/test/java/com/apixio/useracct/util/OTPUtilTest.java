package com.apixio.useracct.util;

import org.junit.Assert;
import org.junit.Test;

public class OTPUtilTest
{

    @Test
    public void testcreateOTP()
    {
        Assert.assertEquals(OTPUtil.createOTP(8).length(), 8);
    }
}