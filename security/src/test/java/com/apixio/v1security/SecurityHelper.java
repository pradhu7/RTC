package com.apixio.v1security;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityHelper
{
    private static SecureRandom random = null;

    private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();

    static String algo = null;

    private static char hexChar[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'A', 'B', 'C', 'D', 'E', 'F'
    };

    public SecurityHelper()
    {
        try {
            // this is good and should be in every Java
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            algo = SecurityHelper.getAlgorithm(256, 256, "CBC", "BC");
        } catch (GeneralSecurityException e) {
            random = new SecureRandom();
        }
    }


    public BufferedBlockCipher getCipher(String key, String rand, byte[] ivData, boolean encryptMode) throws Exception
    {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(key.toCharArray(), rand.getBytes("UTF-8"), 50, 256);

        SecretKeyFactory keyFactory;
        try
        {
            keyFactory = SecretKeyFactory.getInstance(algo, BOUNCY_CASTLE_PROVIDER);
        } catch (NoSuchAlgorithmException e)
        {
            keyFactory = SecretKeyFactory.getInstance(algo);
        }

        SecretKeySpec secretKey = new SecretKeySpec(keyFactory.generateSecret(pbeKeySpec).getEncoded(), "AES");
        byte[] keyEncoded = secretKey.getEncoded();

        KeyParameter keyParam = new KeyParameter(keyEncoded);
        CipherParameters params = new ParametersWithIV(keyParam, ivData);

        BlockCipherPadding padding = new PKCS7Padding();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
        cipher.reset();
        cipher.init(encryptMode, params);

        return cipher;
    }

    protected String getRandomStr()
    {
        String randomString = null;
        randomString = (new StringBuilder(String.valueOf(random.nextInt(0x35a4e900) + 0x5f5e100))).append(random.nextInt(0x895440) + 0xf4240).toString();
        return randomString;
    }

    protected String obfuscateRandomNumber(String random, String keyVersion)
    {
        String obfuscatedRand = null;
        Long randomLong = Long.valueOf(Long.parseLong(random));
        randomLong = Long.valueOf(Long.rotateLeft(randomLong.longValue(), 4));
        String randSize = "";
        int randomNumberLength = (new StringBuilder()).append(randomLong).toString().length();
        if(randomNumberLength < 10)
        {
            randSize = (new StringBuilder("0")).append(randomNumberLength).toString();
        } else
        {
            randSize = (new StringBuilder()).append(randomNumberLength).toString();
        }
        String totalLength = "";
        int totalLengthInt = randomNumberLength + 2 + keyVersion.length() + 2;
        if(totalLengthInt < 10)
        {
            totalLength = (new StringBuilder("0")).append(totalLengthInt).toString();
        } else
        {
            totalLength = (new StringBuilder()).append(totalLengthInt).toString();
        }
        obfuscatedRand = (new StringBuilder(String.valueOf(totalLength))).append(randSize).append(randomLong).append(keyVersion).append('x').toString();
        return obfuscatedRand;
    }

    protected Map getKeyMetaData(String text)
    {
        Map keyMeta = new HashMap();
        int metaDataLength = Integer.parseInt(text.substring(0, 2));
        keyMeta.put("metaLength", (new StringBuilder(String.valueOf(metaDataLength))).toString());
        int randomNumberSize = Integer.parseInt(text.substring(2, 4));
        String metaString = text.substring(0, metaDataLength);
        String randomString = metaString.substring(4, randomNumberSize + 4);
        long actualRandomNumber = Long.rotateRight(Long.parseLong(randomString), 4);
        keyMeta.put("randomNumber", (new StringBuilder(String.valueOf(actualRandomNumber))).toString());
        String versionString = metaString.substring(randomNumberSize + 4);
        keyMeta.put("keyVersion", versionString);
        return keyMeta;
    }

    protected static String getAlgorithm(int val1, int val2, String mod1, String mod2)
    {
        String part1 = "PBE";
        String part2 = "WITHSHA";
        String part3 = (new StringBuilder()).append(val1).append("AND").append(val2).append("BITAES-").append(mod1).append("-").append(mod2).toString();
        return (new StringBuilder(String.valueOf(part1))).append(part2).append(part3).toString();
    }

    protected static String getHexString(byte b[])
    {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for(int i = 0; i < b.length; i++)
        {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0xf]);
        }

        return sb.toString();
    }

    protected static byte[] getBinArray(String hexStr)
    {
        byte bArray[] = new byte[hexStr.length() / 2];
        for(int i = 0; i < hexStr.length() / 2; i++)
        {
            byte firstNibble = Byte.parseByte(hexStr.substring(2 * i, 2 * i + 1), 16);
            byte secondNibble = Byte.parseByte(hexStr.substring(2 * i + 1, 2 * i + 2), 16);
            int finalByte = secondNibble | firstNibble << 4;
            bArray[i] = (byte)finalByte;
        }
        return bArray;
    }
}
