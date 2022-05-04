package com.apixio.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;

import com.apixio.security.Security.EncryptingData;

public class SecurityTest
{
    private Security security;

    /**
     *
     */
    public static void main(String... args) throws Exception
    {
        SecurityTest st = new SecurityTest();

        st.security = Security.getInstance();

        st.testAll(args);
    }

    private void testAll(String[] args) throws Exception
    {
        // v2 only tests:
        testTextCharMangling();
        testTextEncyption();
        testTextHexEncyption();
        testTextWithSaltEncyption();
        testByteEncryption();
        testEncryptingInputStream();

        // test string->string encryption
        if (true)
        {
            for (String a : args)
            {
                String encrypted = security.encrypt(a, null);

                System.out.println("text encrypt(" + a + ") => " + encrypted);
                System.out.println("text encrypt(" + a + ") => " + security.decrypt(encrypted, null));
            }
        }
        
        // test bytes->bytes encryption
        if (true)
        {
            for (String a : args)
            {
                byte[] encrypted = security.encryptBytesToBytes(a.getBytes(StandardCharsets.UTF_8));

                System.out.println("bytes encrypt(" + a + ") => " + Utils.encodeBase64(encrypted));
                System.out.println("decrypt(" + a + ") => " + new String(security.decryptBytesToBytes(encrypted), StandardCharsets.UTF_8));
            }
        }
        

        if (true)
            return;

        for (String a : args)
        {
            long           st = System.currentTimeMillis();
            EncryptingData ed;
            byte[]         enc;
            Cipher         dec;

            System.out.println("\n################ for cleartext [" + a + "]");

            ed = security.prepareEncryption("noscope");

            System.out.println("Metadata:  " + new String(ed.metadata));
            enc = ed.encrypter.doFinal(a.getBytes(StandardCharsets.UTF_8));
            System.out.println("Encrypted: " + Utils.encodeBase64(enc));

            System.out.println("trick    version:  " + security.getEncryptionVersion(new java.io.ByteArrayInputStream("012x".getBytes())));
            System.out.println("metadata version:  " + security.getEncryptionVersion(new java.io.ByteArrayInputStream(ed.metadata)));

            dec = security.getDecryptionCipher(new java.io.ByteArrayInputStream(ed.metadata), null, Security.EncryptionVersion.UNKNOWN);
            System.out.println("Decrypted: " + new String(dec.doFinal(enc), StandardCharsets.UTF_8));

            System.out.println("Took " + (System.currentTimeMillis() - st) + "ms");
        }

        try
        {
            Thread.sleep(1001L);
        }
        catch (InterruptedException x)
        {
        }
    }

    private void testTextCharMangling()
    {
        String plaintext = "abcdefghijklmnopqrstuvwxyz";
        String ciphertext = security.encrypt(plaintext, true);   // true:  "char encryption" (wtf?!)

        if (!plaintext.equals(security.decrypt(ciphertext, true)))
            throw new IllegalStateException("'char encryption' failed");

        System.out.println("testTextCharMangling passed");
    }

    private void testTextEncyption()
    {
        String plaintext = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String ciphertext = security.encrypt(plaintext, false);   // false:  "non-char" (wtf?) encryption

        if (!plaintext.equals(security.decrypt(ciphertext, false)))
            throw new IllegalStateException("Simple text encryption failed");

        System.out.println("testTextEncyption passed");
    }

    private void testTextHexEncyption()
    {
        String plaintext = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String ciphertext = security.encryptToHex(plaintext);

        if (!plaintext.equals(security.decryptFromHex(ciphertext)))
            throw new IllegalStateException("Text hex encryption failed");

        System.out.println("testTextHexEncyption passed");
    }

    private void testTextWithSaltEncyption()
    {
        String plaintext = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String ciphertext = security.encrypt(plaintext, "salt");

        if (!plaintext.equals(security.decrypt(ciphertext, "salt")))
            throw new IllegalStateException("text with salt encryption failed");

        System.out.println("testTextWithSaltEncyption passed");
    }

    private void testByteEncryption()
    {
        byte[] plaintext  = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] ciphertext = security.encryptBytesToBytes(plaintext);

        if (!equalBytes(plaintext, security.decryptBytesToBytes(ciphertext)))
            throw new IllegalStateException("bytes encryption failed");

        System.out.println("testByteEncryption passed");
    }

    private void testEncryptingInputStream() throws IOException
    {
        byte[]       plaintext  = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        InputStream  inBytes    = new ByteArrayInputStream(plaintext);
        byte[]       ciphertext = encryptInputStream(new ByteArrayInputStream(plaintext));
        byte[]       decrypted  = decryptInputStream(new ByteArrayInputStream(ciphertext));

        if (!equalBytes(plaintext, decrypted))
            throw new IllegalStateException("input stream encryption failed");

        System.out.println("testEncryptingInputStream passed");
    }

    private byte[] encryptInputStream(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EncryptedInputStream  eis  = new EncryptedInputStream(is, security);
        
        IOUtils.copy(eis, baos);

        return baos.toByteArray();
    }

    private byte[] decryptInputStream(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DecryptedInputStream  dis  = new DecryptedInputStream(is, security);
        
        IOUtils.copy(dis, baos);

        return baos.toByteArray();
    }

    /**
     *
     */
    private boolean equalBytes(byte[] b1, byte[] b2)
    {
        if ((b1 == null) && (b2 == null))
            return true;
        else if ((b1 == null) && (b2 != null))
            return false;
        else if ((b1 != null) && (b2 == null))
            return false;
        else if (b1.length != b2.length)
            return false;

        for (int i = 0; i < b1.length; i++)
        {
            if (b1[i] != b2[i])
                return false;
        }

        return true;
    }

}
