package com.apixio.useracct.util;

import com.apixio.restbase.web.BaseException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneUtil
{
    private static String US_PHONE_NUMBER_FORMAT = "(...) ...-..%d";
    private static String INDIAN_PHONE_NUMBER_FORMAT = ".....-...%d";

    private static int US_COUNTRY_CODE = 1;
    private static int INDIA_COUNTRY_CODE = 91;

    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    // TO DO Need to add phone verification and validation here later

    /**
     *  Need to provide last 2 digits
     */
    public static String getLast2DigitsPhNo(String cellPhone)
    {
        String retString = null;
        Phonenumber.PhoneNumber phoneNumber = null;

        try {
            phoneNumber = phoneNumberUtil.parse(cellPhone, "US");
        } catch (NumberParseException e) {
            // Phone number is already verified we should not be getting any exceptions here
            throw BaseException.badRequest(e.getMessage());
        }

        if (phoneNumber.getCountryCode() == US_COUNTRY_CODE) {
            retString = getFormattedNumber(phoneNumber, US_PHONE_NUMBER_FORMAT);
        } else if (phoneNumber.getCountryCode() == INDIA_COUNTRY_CODE) {
            retString = getFormattedNumber(phoneNumber, INDIAN_PHONE_NUMBER_FORMAT);
        } else {
            // default format as US
            retString = getFormattedNumber(phoneNumber, US_PHONE_NUMBER_FORMAT);
        }
        return retString;
    }

    private static String getFormattedNumber(Phonenumber.PhoneNumber phoneNumber, String numberFormat)
    {
        // need only the last two digits
        return String.format(numberFormat, phoneNumber.getNationalNumber() % 100);
    }
}
