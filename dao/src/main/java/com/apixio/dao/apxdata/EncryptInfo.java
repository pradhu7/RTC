package com.apixio.dao.apxdata;

/**
 * Collects encryption-related info
 */
class EncryptInfo
{
    boolean encrypt;
    String  pdsID;

    EncryptInfo(boolean encrypt, String pdsID)
    {
        this.encrypt = encrypt;
        this.pdsID   = pdsID;
    }

}

