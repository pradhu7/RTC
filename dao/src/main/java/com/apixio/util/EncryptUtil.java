package com.apixio.util;

import com.apixio.security.Security;
import com.apixio.security.exception.ApixioSecurityException;

/**
 * This layer centralizes and simplifies security library
 */
public class EncryptUtil
{
    /**
     * 
     */
    private static Security security = Security.getInstance();


    /**
     * Utility methods to perform encryption and decryption on byte arrays
     */

    public static byte[] encryptBytes(String pdsID, byte[] data) throws ApixioSecurityException
    {
        return security.encryptBytesToBytesWithScope(data, pdsID);
    }

    public static byte[] decryptBytes(byte[] data) throws ApixioSecurityException
    {
        return security.decryptBytesToBytes(data);
    }

}
