package com.apixio.datasource.afs;

/**
 * 
 * Mount point for "Apixio File System"
 *
 */

public class Mount
{
    static public enum Type
    {
        FS,
        S3
    }

    public Mount(String mount, String prefix, Type type)
    {
        this.mount = mount;
        this.prefix = prefix;
        this.type = type;
    }

    String mount;
    String prefix;
    Type type;
 }
