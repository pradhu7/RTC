package com.apixio.v1security;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;

import com.apixio.v1security.exception.ApixioSecurityException;
import com.apixio.v1security.utility.SecurityUtil;

public class Security
implements IApixioSecurity
{
    
    private static volatile Security instance = null;

    private Configuration config = null;
    private KeyManager keyManager = null;

    private Security() {
        try {
            config = new PropertiesConfiguration("apixio-security.properties");
            keyManager = new KeyManager(config);
        } catch (Exception e) {
            System.out.println("Properties file not found. \n" + e.getMessage());
        }
    }

    private Security(String filePath) {
        try {
            config = new PropertiesConfiguration(filePath);
            keyManager = new KeyManager(config);
        } catch (Exception e) {
            System.out.println("Properties file not found. \n" + e.getMessage());
        }
    }

    private Security(String dns, String token) {
        try
        {
            config = new BaseConfiguration();
            config.setProperty("primaryDNS", dns);
            config.setProperty("secondaryDNS", dns);
            config.setProperty("token", token);
            keyManager = new KeyManager(config);
        }
        catch (Exception x)
        {
            System.out.println("Failure during security initialization" + x.getMessage());
        }
    }
    
    public static Security getInstance(String filePath)
    {
        if (instance == null)
        {
            synchronized (Security.class)
            {
                if (instance == null)
                    instance = new Security(filePath);
            }
        }
        return instance;
    }
    
    public static Security getInstance(String dns, String token)
    {
        if (instance == null)
        {
            synchronized (Security.class)
            {
                if (instance == null)
                    instance = new Security(dns, token);
            }
        }
        return instance;
    }
    
    public static Security getInstance()
    {
        if (instance == null)
        {
            synchronized (Security.class)
            {
                if (instance == null)
                    instance = new Security();
            }
        }
        return instance;
    }

    // NOTE: Char encryption isn't real encryption and doesn't use key service
    public String encrypt(String text, boolean isCharEncryption)
            throws ApixioSecurityException
    {
        if (isCharEncryption)
            return encryptChar(text);
        else
            return encrypt(text);
    }

    private String encryptChar(String plainText)
    {
        String encryptedString = "";
        for (int i = 0; i < plainText.length(); i++) {
            encryptedString = encryptedString + SecurityUtil.getOfC(plainText.charAt(i));
        }

        return encryptedString;
    }

    public String encryptToHex(String text) throws ApixioSecurityException
    {
        String encryptedStr = encrypt(text, null);
        try {
            return SecurityHelper.getHexString(encryptedStr.getBytes("UTF-8"));
        }
        catch (Exception ex) {
            throw new ApixioSecurityException(ex.getMessage());
        }
    }

    public String encrypt(String text)
            throws ApixioSecurityException
    {
        return encrypt(text,null);
    }

    public String encrypt(String text, String salt) throws ApixioSecurityException
    {
        if (text == null) {
            return null;
        }
        String response = null;
        InputStream encryptedInputStream = null;
        try (ByteArrayInputStream inputStream  = new ByteArrayInputStream(text.getBytes("UTF-8"));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            encryptedInputStream = new EncryptedInputStream(
                    new ApixioBase64InputStream(inputStream, true), this, salt);

            IOUtils.copy(encryptedInputStream,outputStream);
            encryptedInputStream.close();
            response = outputStream.toString("UTF-8");
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage(), ex);
        }
        finally
        {
            IOUtils.closeQuietly(encryptedInputStream);
        }
        return response;
    }

    public byte[] encryptBytesToBytes(byte[] bytes) throws ApixioSecurityException {
        if (bytes == null) {
            return null;
        }
        byte[] response = null;
        EncryptedInputStream encryptedInputStream = null;
        try (ByteArrayInputStream inputStream  = new ByteArrayInputStream(bytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            encryptedInputStream = new EncryptedInputStream(inputStream, this);
            IOUtils.copy(encryptedInputStream,outputStream);
            response = outputStream.toByteArray();
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage());
        }
        finally
        {
            IOUtils.closeQuietly(encryptedInputStream);
        }
        return response;
    }

    public OutputStream encryptOutputStream(OutputStream outputStream) {
        return new EncryptedOutputStream(outputStream, this);
    }

    public InputStream encryptInputStream(InputStream inputStream) {
        return new EncryptedInputStream(inputStream, this);
    }


    public String decrypt(String text, boolean isCharEncryption) throws ApixioSecurityException
    {
        String decryptedString = "";
        if (isCharEncryption) {
            for (int i = 0; i < text.length(); i++)
                decryptedString = decryptedString + SecurityUtil.getDOfC(text.charAt(i));
        }
        else {
            decryptedString = decrypt(text);
        }
        return decryptedString;
    }

    public byte[] decryptBytesToBytes(byte[] bytes) throws ApixioSecurityException
    {
        return decryptBytesToBytes(bytes,false);
    }

    public InputStream decryptBytesToInputStream(byte[] bytes, boolean decompress) throws ApixioSecurityException
    {
        if (bytes == null) {
            return null;
        }

        InputStream decryptedInputStream;
        try (ByteArrayInputStream inputStream  = new ByteArrayInputStream(bytes))
        {
            decryptedInputStream =
                    new DecryptedInputStream(inputStream, this);

            if(decompress) {
                decryptedInputStream = new DecompressedInputStream(decryptedInputStream, Charset.forName("UTF8"));
            }
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage());
        }
        return decryptedInputStream;
    }

    public byte[] decryptBytesToBytes(byte[] bytes, boolean decompress) throws ApixioSecurityException
    {
        if (bytes == null) {
            return null;
        }
        byte[] response = null;
        try (ByteArrayInputStream inputStream  = new ByteArrayInputStream(bytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            InputStream decryptedInputStream =
                    new DecryptedInputStream(inputStream, this);

            if(decompress) {
                decryptedInputStream = new DecompressedInputStream(decryptedInputStream, Charset.forName("UTF8"));
            }

            IOUtils.copy(decryptedInputStream,outputStream);
            decryptedInputStream.close();
            response = outputStream.toByteArray();
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage());
        }
        return response;
    }

    public String decryptFromHex(String hexText) throws ApixioSecurityException
    {
        byte[] bytes = SecurityHelper.getBinArray(hexText);
        String decrypted = null;
        try {
            String rawText = new String(bytes, "UTF-8");
            decrypted = decrypt(rawText);
        }
        catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        return decrypted;
    }

    public String decrypt(String text) throws ApixioSecurityException
    {
        return decrypt(text,null);
    }

    public String decrypt(String text, String salt) throws ApixioSecurityException {

        if (text == null) {
            return null;
        }
        String response = null;
        try (ByteArrayInputStream inputStream  = new ByteArrayInputStream(text.getBytes("UTF-8"));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            InputStream decryptedInputStream = new DecryptedInputStream(new ApixioBase64InputStream(inputStream), this);

            IOUtils.copy(decryptedInputStream,outputStream);
            decryptedInputStream.close();
            response = outputStream.toString("UTF-8");
        }
        catch (Exception ex)
        {
            throw new ApixioSecurityException(ex.getMessage());
        }
        return response;
    }

    protected Configuration getConfig()
    {
        return config;
    }

    protected KeyManager getKeyManager()
    {
        return keyManager;
    }
}
