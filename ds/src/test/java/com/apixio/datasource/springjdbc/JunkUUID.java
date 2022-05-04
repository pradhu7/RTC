package com.apixio.datasource.springjdbc;

import java.util.UUID;

public class JunkUUID
{

    public UUID id;
    public String name;

    public JunkUUID(UUID id, String name)
    {
        this.id   = id;
        this.name = name;
    }

    public String toString()
    {
        return "junk(" + id + ", " + name + ")";
    }

}
