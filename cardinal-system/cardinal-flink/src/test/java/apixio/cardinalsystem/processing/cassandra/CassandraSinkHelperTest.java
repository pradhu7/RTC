package apixio.cardinalsystem.processing.cassandra;

import apixio.cardinalsystem.api.model.cassandra.CassandraConfig;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;

public class CassandraSinkHelperTest {

    @Test
    public void testConfigSerialize() throws Exception {
        CassandraConfig cassandraConfig = new CassandraConfig();
        cassandraConfig.setHost(Arrays.asList("127.0.0.1"));
        cassandraConfig.setUsername("test");
        cassandraConfig.setPassword("test");
        assert (Objects.nonNull(InstantiationUtil.serializeObject(cassandraConfig)));
    }

}