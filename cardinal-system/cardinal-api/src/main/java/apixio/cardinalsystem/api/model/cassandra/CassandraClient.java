package apixio.cardinalsystem.api.model.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class CassandraClient {
    private Cluster cluster;
    private Cluster.Builder clusterBuilder;
    private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
    private CassandraConfig cassandraConfig;

    public CassandraClient(CassandraConfig cassandraConfig) {
        this.cassandraConfig = cassandraConfig;
        clusterBuilder = Cluster.builder();
        for (String host : cassandraConfig.getHost()) {
            clusterBuilder.addContactPointsWithPorts(new InetSocketAddress(host, cassandraConfig.getPort()));
        }

        if (Objects.nonNull(cassandraConfig.getUsername())) {
            clusterBuilder.withCredentials(cassandraConfig.getUsername(), cassandraConfig.getPassword());
        }
        clusterBuilder.withSocketOptions(getSocketOptions());
        clusterBuilder.withLoadBalancingPolicy(getLoadBalancingPolicy());
        clusterBuilder.withPoolingOptions(getPoolingOptions());
        clusterBuilder.withRetryPolicy(getRetryPolicy());


        cluster = clusterBuilder.build();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Builder getClusterBuilder() {
        return clusterBuilder;
    }

    public List<String> createCassandraTables() {
        return createCassandraTables(cluster.newSession());
    }

    private List<CassandraPojoToCreate> getPojoToCreateObjs() {
        List<CassandraPojoToCreate> pojoToCreates = new ArrayList<>();
        for (String packageName : Arrays.asList("docinfo", "fileinfo", "processevent", "transferevent")) {
            new Reflections(String.format("apixio.cardinalsystem.api.model.cassandra.%s", packageName), Scanners.SubTypes.filterResultsBy(c -> true))
                    .getSubTypesOf(Object.class)
                    .stream()
                    .forEach(klass -> {
                        pojoToCreates.add(new CassandraPojoToCreate(klass));
                    });
        }
        return pojoToCreates;
    }

    public List<String> createCqls() {
        List<String> createCqls = new ArrayList<>();
        getPojoToCreateObjs().stream().forEach(create -> {
            create.parseFields();
            createCqls.add(create.createTableWithOptions().getQueryString());
        });
        return createCqls;
    }

    public List<String> createCassandraTables(Session cqlSession) {
        List<String> createdTables = new ArrayList<>();
        for (CassandraPojoToCreate pojoToCreateObj : getPojoToCreateObjs()) {
            pojoToCreateObj.parseFields();
            String createCQL = pojoToCreateObj.createTableWithOptions().getQueryString();
            logger.info(String.format("Running create table %s", createCQL));
            cqlSession.execute(String.format("%s;", createCQL));
            createdTables.add(pojoToCreateObj.getTableName());
        }
        return createdTables;
    }

    // below is copied from apixio-datasource. not pulling it in as the dependency increase is way too big and causes some issues

    public ExponentialReconnectionPolicy getReconnectionPolicy() {
        // if you can't connect the first time, change you connection policy to exponential
        // with min set to 50 milliseconds and max set to 250 milliseconds

        return new ExponentialReconnectionPolicy(cassandraConfig.getBaseDelayMs(), cassandraConfig.getMaxDelayMs());
    }

    public LoadBalancingPolicy getLoadBalancingPolicy() {
        DCAwareRoundRobinPolicy dcAwareRoundRobinPolicy = DCAwareRoundRobinPolicy.builder().withLocalDc(cassandraConfig.getDatacenter()).build();
        return new TokenAwarePolicy(dcAwareRoundRobinPolicy);
    }

    public RetryPolicy getRetryPolicy() {
        return new LoggingRetryPolicy(new CardinalRetryPolicy(cassandraConfig.getReadAttempts(), cassandraConfig.getWriteAttempts(), cassandraConfig.getUnavailableAttemps(), false));
    }

    public PoolingOptions getPoolingOptions() {
        PoolingOptions poolingOptions = new PoolingOptions();
        poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, cassandraConfig.getMaxConnections());
        poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, cassandraConfig.getMinConnections());
        return poolingOptions;
    }

    public SocketOptions getSocketOptions() {
        // keep alive socket options.
        // VK: TCP keep alives are notoriously useless - hope this is better!!!
        SocketOptions socketOptions = new SocketOptions();
        // VK: set those based on values on the server side. Should be always less or equal to server side
        // per Datastax Architect Sylvain Lebresne
        socketOptions.setReadTimeoutMillis(cassandraConfig.getTcpReadTimeout());
        socketOptions.setConnectTimeoutMillis(cassandraConfig.getTcpConnectTimeout());
        socketOptions.setKeepAlive(true);
        return socketOptions;
    }
}
