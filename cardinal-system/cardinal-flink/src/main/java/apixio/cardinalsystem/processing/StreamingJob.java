package apixio.cardinalsystem.processing;

import apixio.cardinalsystem.api.model.KafkaConfiguration;
import apixio.cardinalsystem.api.model.cassandra.CassandraConfig;
import apixio.cardinalsystem.processing.flink.FlinkConfiguration;
import apixio.cardinalsystem.processing.flink.PipeLineSetup;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.TimeUtils;

import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;

public class StreamingJob {

    public static Logger LOG = Logger.getLogger(StreamingJob.class.getName());

    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);

        String kafkaConfigFile = params.get("kafka-config");
        String cassandraConfigFile = params.get("cassandra-config");
        String flinkConfigFile = params.get("flink-config");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        KafkaConfiguration kafkaConfig = mapper.readValue(new File(kafkaConfigFile), KafkaConfiguration.class);
        CassandraConfig cassandraConfig = mapper.readValue(new File(cassandraConfigFile), CassandraConfig.class);

        // read flink configuration if argument was passed
        FlinkConfiguration flinkConfiguration;
        if (Objects.isNull(flinkConfigFile)) {
            flinkConfiguration = new FlinkConfiguration();
        } else {
            flinkConfiguration = mapper.readValue(new File(flinkConfigFile), FlinkConfiguration.class);
        }

        LOG.info(String.format("Kafka config: %s", mapper.writeValueAsString(kafkaConfig)));
        LOG.info(String.format("Cassandra config: %s", mapper.writeValueAsString(cassandraConfig)));
        LOG.info(String.format("Flink config: %s", mapper.writeValueAsString(cassandraConfig)));

        env.getCheckpointConfig().setCheckpointTimeout(
                TimeUtils.parseDuration(
                        flinkConfiguration.getCheckpointTimeout()
                ).toMillis()
        );
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(
                flinkConfiguration.getTolerableCheckpointFailures()
        );
        env.enableCheckpointing(
                TimeUtils.parseDuration(
                        flinkConfiguration.getCheckpointInterval()
                ).toMillis()
        );

        if (flinkConfiguration.getParalellism() != 0) {
            env.setParallelism(flinkConfiguration.getParalellism());
        }
        env.setMaxParallelism(1024);

        PipeLineSetup pipeLineSetup = new PipeLineSetup(kafkaConfig, cassandraConfig);
        KeyedStream<TrackingEventRecord, Integer> trackingEventRecordIntegerKeyedStream = pipeLineSetup.setupKafkaStream(env);
        pipeLineSetup.setupProcessing(trackingEventRecordIntegerKeyedStream);
        pipeLineSetup.setupCassandraSink(cassandraConfig);

        // execute program
        env.execute("Cardinal System Streaming");
    }
}
