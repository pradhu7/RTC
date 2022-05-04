package com.apixio.restbase.config;

/**
 * DwJedisPool holds the yaml-poked configuration regarding Jedis.
 */
public class DwJedisPool {
    private int    timeout;
    private String password;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    public int getTimeout()
    {
        return timeout;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
    public String getPassword()
    {
        return password;
    }

    public String toString()
    {
        return "JedisPool: timeout=" + timeout;
    }

}
