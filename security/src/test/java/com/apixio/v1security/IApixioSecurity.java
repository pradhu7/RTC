package com.apixio.v1security;

import com.apixio.v1security.exception.ApixioSecurityException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

interface IApixioSecurity
{
    String encrypt(String s)
        throws ApixioSecurityException;

    String encrypt(String s, String s1)
        throws ApixioSecurityException;

    String encrypt(String s, boolean flag)
        throws ApixioSecurityException;

    byte[] encryptBytesToBytes(byte abyte0[])
        throws ApixioSecurityException;

    String encryptToHex(String s)
        throws ApixioSecurityException;

    String decrypt(String s)
        throws ApixioSecurityException;

    String decrypt(String s, String s1)
        throws ApixioSecurityException;

    String decrypt(String s, boolean flag)
        throws ApixioSecurityException;

    byte[] decryptBytesToBytes(byte abyte0[])
        throws ApixioSecurityException;

    String decryptFromHex(String s)
        throws ApixioSecurityException;

}
