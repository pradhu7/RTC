package apixio.cardinalsystem.processing.cassandra;

import apixio.cardinalsystem.api.model.cassandra.CassandraClient;
import apixio.cardinalsystem.api.model.cassandra.CassandraConfig;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.cassandra.CassandraSink;
import org.apache.flink.streaming.connectors.cassandra.ClusterBuilder;
import org.apache.flink.util.TimeUtils;

public class CassandraSinkHelper {
    public static <IN> CassandraSink.CassandraSinkBuilder<IN> configureSink(DataStream<IN> data, CassandraConfig config) {

        return CassandraSink.addSink(data)
                .setClusterBuilder(new ClusterBuilder() {
                    @Override
                    protected Cluster buildCluster(Builder builder) {
                        return new CassandraClient(config).getCluster();
                    }
                })
                .enableIgnoreNullFields()
                .setMaxConcurrentRequests(config.getFlinkMaxConcurrentRequests(), TimeUtils.parseDuration(config.getFlinkConcurrentRequestTimeout()))
                .setDefaultKeyspace(config.getKeyspace());
    }

    public static <IN> CassandraSink.CassandraSinkBuilder<IN> configureSink(DataStream<IN> data, ClusterBuilder clusterBuilder) {
        return CassandraSink.addSink(data)
                .enableIgnoreNullFields()
                .setClusterBuilder(clusterBuilder);
    }
}
