package com.apixio.restbase.config;

import java.io.IOException;

import com.apixio.datasource.cassandra.CqlConnector;

/**
 * CassandraConfig holds the yaml-poked configuration regarding Cassandra.
 */
public class CassandraConfig {

    private String     keyspaceName;
    private String     hostsCsv;
    private int        binaryPort;
    private String     readConsistencyLevel;
    private String     writeConsistencyLevel;
    private int        minConnections;
    private int        maxConnections;
    private long       baseDelayMs;
    private long       maxDelayMs;
    private boolean    cqlDebug;
    private String     localDC;
    private String     username;
    private String     password;

    public String getHosts()
    {
        return hostsCsv;
    }
    public void setHosts(String hostsCsv)
    {
        this.hostsCsv = hostsCsv;
    }

    public String getKeyspaceName()
    {
        return keyspaceName;
    }
    public void setKeyspaceName(String keyspaceName)
    {
        this.keyspaceName = keyspaceName;
    }

    public int getBinaryPort()
    {
        return binaryPort;
    }
    public void setBinaryPort(int binaryPort)
    {
        this.binaryPort = binaryPort;
    }

    public String getReadConsistencyLevel()
    {
        return readConsistencyLevel;
    }
    public void setReadConsistencyLevel(String readConsistencyLevel)
    {
        this.readConsistencyLevel = readConsistencyLevel;
    }

    public String getWriteConsistencyLevel()
    {
        return writeConsistencyLevel;
    }
    public void setWriteConsistencyLevel(String writeConsistencyLevel)
    {
        this.writeConsistencyLevel = writeConsistencyLevel;
    }

    public int getMinConnections()
    {
        return minConnections;
    }
    public void setMinConnections(int minConnections)
    {
        this.minConnections = minConnections;
    }

    public int getMaxConnections()
    {
        return maxConnections;
    }
    public void setMaxConnections(int maxConnections)
    {
        this.maxConnections = maxConnections;
    }

    public long getBaseDelayMs()
    {
        return baseDelayMs;
    }
    public void setBaseDelayMs(long baseDelayMs)
    {
        this.baseDelayMs = baseDelayMs;
    }

    public long getMaxDelayMs()
    {
        return maxDelayMs;
    }
    public void setMaxDelayMs(long maxDelayMs)
    {
        this.maxDelayMs = maxDelayMs;
    }

    public boolean getCqlDebug()
    {
        return cqlDebug;
    }
    public void setCqlDebug(boolean cqlDebug)
    {
        this.cqlDebug = cqlDebug;
    }

    public String getLocalDC()
    {
        return localDC;
    }

    public void setLocalDC(String localDC)
    {
        this.localDC = localDC;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public CqlConnector toConnector(boolean init) throws IOException
    {
        CqlConnector conn = new CqlConnector();

        conn.setHosts(hostsCsv);
        conn.setBinaryPort(binaryPort);
        conn.setReadConsistencyLevel(readConsistencyLevel);
        conn.setWriteConsistencyLevel(writeConsistencyLevel);
        conn.setKeyspaceName(keyspaceName);
        conn.setMinConnections(minConnections);
        conn.setMaxConnections(maxConnections);
        conn.setLocalDC(localDC);
        conn.setUsername(username);
        conn.setPassword(password);

        if (init)
            conn.init();

        return conn;
    }

    public String toString()
    {
        return ("[Cass: " +
                "; keyspaceName=" + keyspaceName +
                "; hosts=" + hostsCsv +
                "; binaryPort=" + binaryPort +
                "; readConsistencyLevel=" + readConsistencyLevel +
                "; writeConsistencyLevel=" + writeConsistencyLevel +
                "; minConnections=" + minConnections +
                "; maxConnections=" + maxConnections +
                "; baseDelayMs=" + baseDelayMs +
                "; maxDelayMs=" + maxDelayMs +
                "; cqlDebug=" + cqlDebug +
                "; localDC=" + localDC +
                "]");
    }
}
