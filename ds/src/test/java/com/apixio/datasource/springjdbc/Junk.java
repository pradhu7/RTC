package com.apixio.datasource.springjdbc;

public class Junk
{

    public int id;
    public String name;

    public Junk(int id, String name)
    {
        this.id   = id;
        this.name = name;
    }

    public String toString()
    {
        return "junk(" + id + ", " + name + ")";
    }

}
