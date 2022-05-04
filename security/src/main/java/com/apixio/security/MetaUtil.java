package com.apixio.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class MetaUtil
{
    /**
     * Creates v2 metadata with the given details and returns it as a byte array
     * that can be reconstructed to a MetaV2 instance using readMetaV2.  The bytes
     * are the UTF8 encoding of the String that is the serialized form of the metadata
     * and MUST be prefixed (or associated with) the actual encrypted data as this
     * metadata must be used for decryption.
     *
     * Note that the big reason these bytes are UTF8 of a string is that it's possible
     * then to view metadata as ASCII text (to aid in debugging, recovery, etc.)
     */
    static byte[] createMetaV2(String encryptedKey, String scope, String algorithm, byte[] iv)
    {
        MetaV2 meta = new MetaV2(encryptedKey, algorithm, iv);

        // scope is the only optional item
        if (scope != null)
            meta.setScope(scope);

        return meta.asString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Reads the bytes returned from createMetaV2 and reconstructs the corresponding
     * MetaV2 object.
     */
    static MetaV2 readMetaV2(InputStream bytes) throws IOException
    {
        return new MetaV2(bytes);
    }

}
