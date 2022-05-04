package com.apixio.restbase.config;

/**
 * DwRedisConfig holds the yaml-poked configuration regarding Redis.
 */
public class DwRedisConfig {
    private String host;
    private int    port;
    private String keyPrefix;

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getHost()
    {
        return host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return port;
    }

    public void setKeyPrefix(String prefix)
    {
        this.keyPrefix = prefix;
    }

    public String getKeyPrefix()
    {
        return keyPrefix;
    }

    public String toString()
    {
        return "Redis: " + host + ":" + port + " " + keyPrefix;
    }

}
