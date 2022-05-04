package com.apixio.nassembly;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.apixio.security.Config;
import com.apixio.security.Security;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.utility.DataCompression;

import static com.apixio.security.Config.SYSPROP_PREFIX;

public class Marshalling
{
    /**
     * Due to streaming needs we need to use Security module directly
     */

    private static  Security crypt;

    Marshalling()
    {
        try
        {
            // to force v2 in new assembly
            System.setProperty(SYSPROP_PREFIX + Config.ConfigKey.CONFIG_V1_MODE, "false");

            crypt = Security.getInstance();

            // test encryption (no other way to validate that things work...)
            crypt.encrypt("test");
        }
        catch (Exception e)
        {
            throw new RuntimeException("can't instantiate encryption", e);
        }
    }

    byte[] encrypt(byte[] data, String scope)
    {
        try
        {
            DataCompression compression = new DataCompression();
            byte[] result = compression.compressData(data, true);
            return crypt.encryptBytesToBytesWithScope(result, PatientDataSetLogic.computeScope(scope));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Can't encrypt data", e);
        }
    }

    InputStream decrypt(byte[] data)
    {
        try
        {
            DataCompression compressor = new DataCompression();
            InputStream decryptedBytes = crypt.decryptBytesToInputStream(data, false);

            return (decryptedBytes != null) ? compressor.decompressData(decryptedBytes, true) : null;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Can't decrypt data", e);
        }
    }
}
