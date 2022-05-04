package com.apixio.datasource.cassandra;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.jmx.JmxReporter;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.SocketOptions;

/**
 * CqlConnector holds the configured Cassandra connection information.
 */
public class CqlConnector
{
	private static Logger logger = LoggerFactory.getLogger(CqlConnector.class);

    /**
     * As configured in external .properties files
     */
    private String              id;
    private Session             session;
    private Cluster             cluster;
    private String              localDC;
    private String[]            hosts;
    private int                 binaryPort;
    private String              username;
    private String              password;
    private String              readCLStr;
    private String              writeCLStr;
    private ConsistencyLevel    readCL;
    private ConsistencyLevel    writeCL;
    private String              keyspaceName;
    private int                 minConnections;
    private int                 maxConnections;
    private long                baseDelayMs;
    private long                maxDelayMs;

    // Config for Apixio retry policy
    private int                 readAttempts;
    private int                 writeAttempts;
    private int                 unavailableAttempts;
    private boolean             downgrade;

    /**
     * Setters for the configured values.
     */
    public void setHosts(String hosts)
    {
        this.hosts = hosts.split(",");
    }
    public void setLocalDC(String localDC)
    {
        this.localDC = localDC;
    }
    public void setBinaryPort(int binaryPort)
    {
        this.binaryPort = binaryPort;
    }
    public void setPassword(String password)
    {
        this.password = password;
    }
    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setReadConsistencyLevel(String rcl)
    {
        readCLStr = rcl;
        readCL = ConsistencyLevel.valueOf(rcl);
    }
    public void setWriteConsistencyLevel(String wcl)
    {
    	writeCLStr = wcl;
        writeCL = ConsistencyLevel.valueOf(wcl);
    }
    public void setKeyspaceName(String name)
    {
        keyspaceName = name;
    }
    public void setMinConnections(int min)
    {
        minConnections = min;
    }
    public void setMaxConnections(int max)
    {
        maxConnections = max;
    }
    public String getID()
    {
        return id;
    }
    public Session getSession()
    {
        return session;
    }
    public void setBaseDelayMs(long baseDelay)
    {
        baseDelayMs = baseDelay;
    }
    public void setMaxDelayMs(long maxDelay)
    {
        maxDelayMs = maxDelay;
    }
    public void setWriteAttempts(int writeAttempts)
    {
        this.writeAttempts = writeAttempts;
    }
    public void setReadAttempts(int readAttempts)
    {
        this.readAttempts = readAttempts;
    }
    public void setUnavailableAttempts(int unavailableAttempts)
    {
        this.unavailableAttempts = unavailableAttempts;
    }
    public void setDowngrade(boolean downgrade)
    {
        this.downgrade = downgrade;
    }
    public String getReadConsistencyLevel()
    {
        return readCLStr;
    }
    public String getWriteConsistencyLevel()
    {
        return writeCLStr;
    }

    public ConsistencyLevel getReadCL()
    {
        return readCL;
    }
    public ConsistencyLevel getWriteCL()
    {
        return writeCL;
    }

    public CqlConnector()
    {
    }

    /**
     * init() sets up the actual CQL connection information.
     * @throws IOException
     *
     * Defaults used:
     * - LoadBalancingPolicy
     * - ExponentialReconnectionPolicy
     * - Retry queries in only two cases:
     * -- On a read timeout, if enough replica replied but data was not retrieved.
     */
    public void init()
    {
        id = UUID.randomUUID().toString();

    	logger.info("Setting up session to cassandra...");
        logger.info("...read consistency level: " + readCLStr + "; write consistency level: " + writeCLStr);

        checkConfig();

        Builder builder = Cluster.builder();

        for (int i = 0; i < hosts.length; i++)
        {
            builder.addContactPoint(hosts[i].trim());
        }

        builder.withPort(binaryPort);

        if (username != null && password != null)
            builder.withCredentials(username, password);

        // keep alive socket options.
        // VK: TCP keep alives are notoriously useless - hope this is better!!!
        SocketOptions socketOptions = new SocketOptions();
        // VK: set those based on values on the server side. Should be always less or equal to server side
        // per Datastax Architect Sylvain Lebresne
        socketOptions.setReadTimeoutMillis(5000);
        socketOptions.setConnectTimeoutMillis(10000);
        socketOptions.setKeepAlive(true);

        builder.withSocketOptions(socketOptions);

        // The load balancing policy we use is the DC Aware round robin policy wrapped by token aware policy
        if(StringUtils.isNotBlank(localDC))
        {
            DCAwareRoundRobinPolicy dcAwareRoundRobinPolicy = DCAwareRoundRobinPolicy.builder().withLocalDc(localDC).build();
            TokenAwarePolicy tokenAwarePolicy = new TokenAwarePolicy(dcAwareRoundRobinPolicy);
            builder.withLoadBalancingPolicy(tokenAwarePolicy);
        }

        // VERY IMPORTANT
        // DOWNGRADE MIGHT BE USED ONLY BT FRONTEND APPS
        // DOWNGRADE SHOULD BE NEVER USED BY PIPELINE

        builder.withRetryPolicy(new ApixioRetryPolicy(readAttempts, writeAttempts, unavailableAttempts, downgrade));

        // if you can't connect the first time, change you connection policy to exponential
        // with min set to 50 milliseconds and max set to 250 milliseconds

        // for now until everybody upgrades
        setBaseDelayMs(baseDelayMs != 0L ? baseDelayMs : 50L);
        setMaxDelayMs(maxDelayMs != 0L ? maxDelayMs : 250L);

        ExponentialReconnectionPolicy exponentialReconnectionPolicy = new ExponentialReconnectionPolicy(baseDelayMs, maxDelayMs);
        builder.withReconnectionPolicy(exponentialReconnectionPolicy);

        // set the min and max connections
        // always set max connection before mnx connection
        if (minConnections != 0 && maxConnections != 0)
        {
            PoolingOptions poolingOptions = new PoolingOptions();
            if (maxConnections != 0)
                poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, maxConnections);
            if (minConnections != 0)
                poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, minConnections);
            builder.withPoolingOptions(poolingOptions);
        }

        // Overwrite the default value of 10s - use 10 mns for schema agreement!!!
        // We don't want anybody to change this value through configuration. So, we will hardcode it!!!
        builder.withMaxSchemaAgreementWaitSeconds(60 * 10);

        // disable jmx reporting and initiate separately
        builder.withoutJMXReporting();

        cluster = builder.build()
            .init();


        JmxReporter reporter =
            JmxReporter.forRegistry(cluster.getMetrics().getRegistry())
                .inDomain(cluster.getClusterName() + "-metrics")
                .build();

        reporter.start();

        session = cluster.connect(keyspaceName);
    }

    /**
     * Check the health of the cql connection.
     * Intentionally throws exceptions to indicate an unhealthy session.
     */
    public String version()
    {
        Statement versionQuery = new SimpleStatement("select release_version from system.local where key = 'local';");
        return session.execute(versionQuery).one().getString("release_version");
    }
    /**
     * checkConfig makes sure that all required configuration information has been set.
     */
    private void checkConfig()
    {
        if (hosts == null)
            throw new IllegalStateException("cassandra.hosts not set; check system.properties");
        else if (binaryPort == 0)
            throw new IllegalStateException("cassandra.port not set; check system.properties");
        else if (keyspaceName == null)
            throw new IllegalStateException("cassandra.keyspacename not set; check system.properties");
    }

    public void close()
    {
        if (session != null)
            session.close();

        // closing cluster like this requires that Tez/YARN not be configured to reuse containers
        // as the static fields DaoServices daoServices in ApixioDaoProcessor will cause problems!
        if(cluster != null) {
            cluster.close();
        }
    }

}
