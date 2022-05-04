package com.apixio.dw.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.datastax.driver.core.*;

import com.apixio.datasource.cassandra.CqlConnector;
import com.apixio.logger.EventLogger;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.PersistenceServices.CassandraCluster;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.MicroserviceConfig;


/**
 * Connects with casandra and pulls the version number.
 */
public class CassandraHealthCheck extends BaseHealthCheck
{

    private static final String CASSANDRA_HEALTH_CHECK = "CassandraHealthCheck";

    private Session   session;
    private Session   session1;
    private Session   session2;
    private Statement versionQuery;

    public CassandraHealthCheck(MicroserviceConfig config)
    {
        super(config);

        ConfigSet    cassConfig  = config.getPersistenceConfig();
        CqlConnector connector   = PersistenceServices.toCqlConnector(cassConfig, true, CassandraCluster.INTERNAL);
        CqlConnector connector1  = PersistenceServices.toCqlConnector(cassConfig, true, CassandraCluster.SCIENCE);
        CqlConnector connector2  = PersistenceServices.toCqlConnector(cassConfig, true, CassandraCluster.APPLICATION);

        session  = connector.getSession();
        session1 = connector1.getSession();
        session2 = connector2.getSession();

        versionQuery = new SimpleStatement("select release_version from system.local where key = 'local';");
    }

    @Override
    protected final String getLoggerName()
    {
        return CASSANDRA_HEALTH_CHECK;
    }

    @Override
    protected final HealthCheck.Result runHealthCheck(EventLogger logger) throws Exception
    {
        ResultSet rs    = session.execute(versionQuery);
        Row       vrow  = rs.one();

        ResultSet rs1   = session1.execute(versionQuery);
        Row       vrow1 = rs1.one();

        ResultSet rs2   = session2.execute(versionQuery);
        Row       vrow2 = rs2.one();

        // assume all versions are the same!!!
        if (vrow != null && vrow1 != null && vrow2 != null)
            return HealthCheck.Result.healthy(String.format("Version %s", vrow.getString("release_version")));
        else
            return HealthCheck.Result.unhealthy("Failed to find version number from cassandra.");
    }
}
