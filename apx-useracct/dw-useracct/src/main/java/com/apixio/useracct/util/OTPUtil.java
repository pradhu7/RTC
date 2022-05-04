package com.apixio.useracct.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

public class OTPUtil
{
    private static final SecureRandom secureRandomGen = new SecureRandom();

    /**
     * Using SecureRandom to select char values over randomly generated alpha numeric strings using
     * Apache Commons library
     *
     * @param OTPlength
     * @return OTP string
     */
    public static String createOTP(int OTPlength)
    {
        StringBuilder stringBuilder = new StringBuilder();

        // increasing the count 100 will increase the time for generating the OTP
        String randomString = RandomStringUtils.randomNumeric(100);

        for (int i = 0; i < OTPlength; i++)
        {
            int randInt = secureRandomGen.nextInt(randomString.length());
            stringBuilder.append(randomString.charAt(randInt));
        }

        return stringBuilder.toString();
    }

}
