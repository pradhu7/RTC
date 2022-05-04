package com.apixio.restbase.config;

/**
 * DwJedisConfig holds the yaml-poked configuration regarding Jedis.
 */
public class DwJedisConfig {
    private boolean testWhileIdle;
    private boolean testOnBorrow;
    private int maxTotal;
    private long maxWaitMillis;

    public void setTestWhileIdle(boolean testWhileIdle)
    {
        this.testWhileIdle = testWhileIdle;
    }
    public boolean getTestWhileIdle()
    {
        return testWhileIdle;
    }

    public void setTestOnBorrow(boolean testOnBorrow)
    {
        this.testOnBorrow = testOnBorrow;
    }
    public boolean getTestOnBorrow()
    {
        return testOnBorrow;
    }

    public void setMaxTotal(int maxTotal)
    {
        this.maxTotal = maxTotal;
    }
    public int getMaxTotal()
    {
        return maxTotal;
    }

    public void setMaxWaitMillis(long maxWaitMillis) { this.maxWaitMillis = maxWaitMillis; }
    public long getMaxWaitMillis() { return maxWaitMillis; }

    public String toString()
    {
        return ("JedisConfig: testWhileIdle=" + testWhileIdle + "; testOnBorrow=" + testOnBorrow +
                "; maxTotal=" + maxTotal + "; maxWaitMillis=" + maxWaitMillis);
    }

}
