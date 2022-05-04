package com.apixio.dao.apxdata;

/**
 * LibInit is part of the convenience/centralization library code that is for the good
 * of the clients.  Its functionality is not meant to be pulled into ApxDataDao as that
 * class shouldn't know about or care about the specifics of various DataTypes.
 */
public class LibInit
{
    /**
     * Version numbers for generic ApxData
     */
    public final static int APXDATA_TAG_PROTOBUF =  0 << 3;  // ApxData -> protobuf.toBytes(); as value is 0, this is the default
    public final static int APXDATA_TAG_LAST     = 31 << 3;  // reminder that we can only have 32 types (0..31)

}
