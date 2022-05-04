package apixio.cardinalsystem.processing.flink;

import apixio.cardinalsystem.api.model.KafkaConfiguration;
import apixio.cardinalsystem.api.model.cassandra.CassandraConfig;
import apixio.cardinalsystem.api.model.cassandra.converters.DocInfoConverter;
import apixio.cardinalsystem.api.model.cassandra.converters.FileInfoConverter;
import apixio.cardinalsystem.api.model.cassandra.converters.ProcessEventConverter;
import apixio.cardinalsystem.api.model.cassandra.converters.TransferEventConverter;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByChecksum;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoById;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrg;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrgPatientId;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrgPds;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoByChecksum;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoById;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoByOrg;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventById;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventByOrg;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventFromXuuid;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventToXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventByContentXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventById;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventByOrg;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventFromXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventToXuuid;
import apixio.cardinalsystem.processing.cassandra.CassandraSinkHelper;
import apixio.cardinalsystem.utils.kafka.TrackingEventDeserilaizer;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.Contents.DataCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.util.concurrent.ThreadLocalRandom;

/**
 * wrapper class to handle the logic of setting up the processing pipeline. mainly intended to make testing easier
 * order of method calls is important and variables are accessible publicly so objects can be pulled out and forked
 * for additional processing
 */
public class PipeLineSetup {
    // process event tables
    public DataStream<ProcessEventById> processEventsById;
    public DataStream<ProcessEventByOrg> processEventByOrg;
    public DataStream<ProcessEventFromXuuid> processEventFromXuuid;
    public DataStream<ProcessEventToXuuid> processEventToXuuid;

    // transfer event tables
    public DataStream<TransferEventById> transferEventsById;
    public DataStream<TransferEventByOrg> transferEventByOrg;
    public DataStream<TransferEventFromXuuid> transferEventFromXuuid;
    public DataStream<TransferEventToXuuid> transferEventToXuuid;
    public DataStream<TransferEventByContentXuuid> transferEventByContentXuuid;

    // docinfo tables
    public DataStream<DocInfoByChecksum> docInfoByChecksum;
    public DataStream<DocInfoById> docInfoById;
    public DataStream<DocInfoByOrg> docInfoByOrg;
    public DataStream<DocInfoByOrgPatientId> docInfoByOrgPatientId;
    public DataStream<DocInfoByOrgPds> docInfoByOrgPds;

    // fileinfo tables
    public DataStream<FileInfoByChecksum> fileInfoByChecksum;
    public DataStream<FileInfoById> fileInfoById;
    public DataStream<FileInfoByOrg> fileInfoByOrg;

    // converters
    public DataStream<ProcessEventConverter> processEventConverters;
    public DataStream<TransferEventConverter> transferEventConverters;
    public DataStream<DocInfoConverter> docInfoConverters;
    public DataStream<FileInfoConverter> fileInfoConverters;

    public DataStream<TrackingEventRecord> processEvents;
    public DataStream<TrackingEventRecord> transferEvents;
    public DataStream<TrackingEventRecord> docInfoEvents;
    public DataStream<TrackingEventRecord> fileInfoEvents;

    public CassandraConfig cassandraConfig;
    public KafkaConfiguration kafkaConfig;

    /**
     * not intended for use when the source and sink are supplied externally
     */
    public PipeLineSetup() {
    }

    /**
     * initalize method that makes config objects available to source and sink configurations
     *
     * @param kafkaConfig     a {@link KafkaConfiguration} object that contains connection and consumer information
     * @param cassandraConfig a {@link CassandraConfig} object that contains connection information for the cassandra cluster
     */
    public PipeLineSetup(KafkaConfiguration kafkaConfig, CassandraConfig cassandraConfig) {
        this.kafkaConfig = kafkaConfig;
        this.cassandraConfig = cassandraConfig;
    }

    /**
     * sets up the processing pipeline minus the source and sink
     *
     * @param keyedStream a keyed input stream for processing to run over
     */
    public void setupProcessing(
            KeyedStream<TrackingEventRecord, Integer> keyedStream
    ) {

        setupFilteredStreams(keyedStream);
        // create converter objects
        setupConverters();


        setupProcessEventPojos();
        setupTransferEventPojos();
        setupDocinfoPojos();
        setupFileinfoPojos();

    }

    /**
     * configures instance variables needed for later processing. creates a stream with events filtered for each type
     *
     * @param keyedStream a keyed input stream for processing to run over
     */
    public void setupFilteredStreams(KeyedStream<TrackingEventRecord, Integer> keyedStream) {
        processEvents = keyedStream.filter(
                (FilterFunction<TrackingEventRecord>) value -> value.getEventCase().equals(EventCase.PROCESS_EVENT)
        ).uid("filter-process-events").name("filter-process-events");
        transferEvents = keyedStream.filter(
                (FilterFunction<TrackingEventRecord>) value -> value.getEventCase().equals(EventCase.TRANSFER_EVENT)
        ).uid("filter-transfer-events").name("filter-transfer-events");
        docInfoEvents = keyedStream.filter(
                (FilterFunction<TrackingEventRecord>) value -> {
                    switch (value.getEventCase()) {
                        case TRANSFER_EVENT:
                            return value.getTransferEvent().getSubject().getDataCase().equals(DataCase.DOC_INFO);
                        case PROCESS_EVENT:
                            return value.getProcessEvent().getSubject().getDataCase().equals(DataCase.DOC_INFO);
                        default:
                            return false;
                    }
                }
        ).uid("filter-docinfo").name("filter-docinfo");

        fileInfoEvents = keyedStream.filter(
                (FilterFunction<TrackingEventRecord>) value -> {
                    switch (value.getEventCase()) {
                        case TRANSFER_EVENT:
                            return value.getTransferEvent().getSubject().getDataCase().equals(DataCase.FILE_INFO);
                        case PROCESS_EVENT:
                            return value.getProcessEvent().getSubject().getDataCase().equals(DataCase.FILE_INFO);
                        default:
                            return false;
                    }
                }
        ).uid("filter-fileinfo").name("filter-fileinfo");
    }

    public KeyedStream<TrackingEventRecord, Integer> setupKafkaStream(StreamExecutionEnvironment env) {
        return this.setupKafkaStream(env, kafkaConfig);
    }

    /**
     * a wrapper for the kafka source
     *
     * @param env         a stream execution environment (unbounded) to configure for processing
     * @param kafkaConfig configuration for the consumer and how to connect to the cluster
     * @return a keyed datastream to be passed to the setupProcessing method
     */
    public KeyedStream<TrackingEventRecord, Integer> setupKafkaStream(StreamExecutionEnvironment env, KafkaConfiguration kafkaConfig) {
        KafkaSource<TrackingEventRecord> source = KafkaSource.<TrackingEventRecord>builder()
                .setBootstrapServers(kafkaConfig.bootstrapServers)
                .setTopics(kafkaConfig.topicName)
                .setGroupId(kafkaConfig.groupId)
                .setClientIdPrefix(kafkaConfig.clientId)
                .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(TrackingEventDeserilaizer.class))
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setProperty("partition.discovery.interval.ms", "600000")
                .setProperties(kafkaConfig.toProperties())
                .build();
        DataStream<TrackingEventRecord> sourceStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka").uid("kafka-tracking-events");
        return setupKeyBy(sourceStream);
    }

    public KeyedStream<TrackingEventRecord, Integer> setupKeyBy(DataStream<TrackingEventRecord> source) {
        return source.keyBy(
                // use a random number up to 10000. and change range if needed
                // if random computation takes too many resources change to use current time in nanoseconds
                (KeySelector<TrackingEventRecord, Integer>) record -> ThreadLocalRandom.current().nextInt(0, 10000)
        );
    }

    public void setupCassandraSink() throws Exception {
        this.setupCassandraSink(cassandraConfig);
    }

    /**
     * a wrapper for the cassandra sink
     *
     * @param cassandraConfig configuration on how to connect to the cassandra cluster
     * @throws Exception if there is an issue configuring the cassandra sink (IE bad pojo)
     */
    public void setupCassandraSink(CassandraConfig cassandraConfig) throws Exception {
        CassandraSinkHelper.configureSink(processEventsById, cassandraConfig)
                .build().uid("csql-processEventsById").name("csql-processEventsById");
        CassandraSinkHelper.configureSink(processEventByOrg, cassandraConfig)
                .build().uid("csql-processEventByOrg").name("csql-processEventByOrg");
        CassandraSinkHelper.configureSink(processEventFromXuuid, cassandraConfig)
                .build().uid("csql-processEventFromXuuid").name("csql-processEventFromXuuid");
        CassandraSinkHelper.configureSink(processEventToXuuid, cassandraConfig)
                .build().uid("csql-processEventToXuuid").name("csql-processEventToXuuid");

        CassandraSinkHelper.configureSink(transferEventsById, cassandraConfig)
                .build().uid("csql-transferEventsById").name("csql-transferEventsById");
        CassandraSinkHelper.configureSink(transferEventByOrg, cassandraConfig)
                .build().uid("csql-transferEventByOrg").name("csql-transferEventByOrg");
        CassandraSinkHelper.configureSink(transferEventFromXuuid, cassandraConfig)
                .build().uid("csql-transferEventFromXuuid").name("csql-transferEventFromXuuid");
        CassandraSinkHelper.configureSink(transferEventToXuuid, cassandraConfig)
                .build().uid("csql-transferEventToXuuid").name("csql-transferEventToXuuid");
        CassandraSinkHelper.configureSink(transferEventByContentXuuid, cassandraConfig)
                .build().uid("cql-transferEventByContentXuuid").name("cql-transferEventByContentXuuid");

        CassandraSinkHelper.configureSink(docInfoByChecksum, cassandraConfig)
                .build().uid("csql-docInfoByChecksum").name("csql-docInfoByChecksum");
        CassandraSinkHelper.configureSink(docInfoById, cassandraConfig)
                .build().uid("csql-docInfoById").name("csql-docInfoById");
        CassandraSinkHelper.configureSink(docInfoByOrg, cassandraConfig)
                .build().uid("csql-docInfoByOrg").name("csql-docInfoByOrg");
        CassandraSinkHelper.configureSink(docInfoByOrgPatientId, cassandraConfig)
                .build().uid("csql-docInfoByOrgPatientId").name("csql-docInfoByOrgPatientId");
        CassandraSinkHelper.configureSink(docInfoByOrgPds, cassandraConfig)
                .build().uid("csql-docInfoByOrgPds").name("csql-docInfoByOrgPds");

        CassandraSinkHelper.configureSink(fileInfoByChecksum, cassandraConfig)
                .build().uid("csql-fileInfoByChecksum").name("csql-fileInfoByChecksum");
        CassandraSinkHelper.configureSink(fileInfoById, cassandraConfig)
                .build().uid("csql-fileInfoById").name("csql-fileInfoById");
        CassandraSinkHelper.configureSink(fileInfoByOrg, cassandraConfig)
                .build().uid("csql-fileInfoByOrg").name("csql-fileInfoByOrg");
    }

    /**
     * generates converters from filtered processing events
     */
    public void setupConverters() {
        processEventConverters = processEvents.map(
                (MapFunction<TrackingEventRecord, ProcessEventConverter>) record -> new ProcessEventConverter(record)
        ).uid("process-event-converters").name("process-event-converters");
        transferEventConverters = transferEvents.map(
                (MapFunction<TrackingEventRecord, TransferEventConverter>) record -> new TransferEventConverter(record)
        ).uid("transfer-events-converters").name("transfer-events-converters");
        docInfoConverters = docInfoEvents.map(
                (MapFunction<TrackingEventRecord, DocInfoConverter>) record -> new DocInfoConverter(record)
        ).uid("docinfo-content-converters").name("docinfo-content-converters");
        fileInfoConverters = fileInfoEvents.map(
                (MapFunction<TrackingEventRecord, FileInfoConverter>) record -> new FileInfoConverter(record)
        ).uid("fileinfo-content-converters").name("fileinfo-content-converters");
    }

    /**
     * configures the different pojo's needed to write to cassandra for processing events
     */
    public void setupProcessEventPojos() {
        // process event tables
        processEventsById = processEventConverters.map(
                (MapFunction<ProcessEventConverter, ProcessEventById>) event -> event.getProcessEventById()
        ).uid("processEventsById").name("processEventsById");
        processEventByOrg = processEventConverters.map(
                (MapFunction<ProcessEventConverter, ProcessEventByOrg>) event -> event.getProcessEventByOrg()
        ).uid("processEventsByOrg").name("processEventsByOrg");
        processEventFromXuuid = processEventConverters.map(
                (MapFunction<ProcessEventConverter, ProcessEventFromXuuid>) event -> event.getProcessEventFromXuuid()
        ).uid("processEventFromXuuid").name("processEventFromXuuid");
        processEventToXuuid = processEventConverters.map(
                (MapFunction<ProcessEventConverter, ProcessEventToXuuid>) event -> event.getProcessEventToXuuid()
        ).uid("processEventToXuuid").name("processEventToXuuid");
    }

    /**
     * configures the different pojo's needed to write to cassandra for transfer events
     */
    public void setupTransferEventPojos() {
        // transfer event tables
        transferEventsById = transferEventConverters.map(
                (MapFunction<TransferEventConverter, TransferEventById>) event -> event.getTransferEventById()
        ).uid("transferEventsById").name("transferEventsById");
        transferEventByOrg = transferEventConverters.map(
                (MapFunction<TransferEventConverter, TransferEventByOrg>) event -> event.getTransferEventByOrg()
        ).uid("transferEventByOrg").name("transferEventByOrg");
        transferEventFromXuuid = transferEventConverters.map(
                (MapFunction<TransferEventConverter, TransferEventFromXuuid>) event -> event.getTransferEventFromXuuid()
        ).uid("transferEventFromXuuid").name("transferEventFromXuuid");
        transferEventToXuuid = transferEventConverters.map(
                (MapFunction<TransferEventConverter, TransferEventToXuuid>) event -> event.getTransferEventToXuuid()
        ).uid("transferEventToXuuid").name("transferEventToXuuid");
        transferEventByContentXuuid = transferEventConverters.map(
                (MapFunction<TransferEventConverter, TransferEventByContentXuuid>) event -> event.getTransferEventByContentXuuid()
        ).uid("transferEventByContentXuuid").name("transferEventByContentXuuid");
    }

    /**
     * configures the different pojo's needed to write to cassandra for docinfo contents
     */
    public void setupDocinfoPojos() {
        docInfoByChecksum = docInfoConverters.map(
                (MapFunction<DocInfoConverter, DocInfoByChecksum>) event -> event.getDocInfoByChecksum()
        ).uid("docInfoByChecksum").name("docInfoByChecksum");
        docInfoById = docInfoConverters.map(
                (MapFunction<DocInfoConverter, DocInfoById>) event -> event.getDocInfoById()
        ).uid("docInfoById").name("docInfoById");
        docInfoByOrg = docInfoConverters.map(
                (MapFunction<DocInfoConverter, DocInfoByOrg>) event -> event.getDocInfoByOrg()
        ).uid("docInfoByOrg").name("docInfoByOrg");
        docInfoByOrgPatientId = docInfoConverters.map(
                (MapFunction<DocInfoConverter, DocInfoByOrgPatientId>) event -> event.getDocInfoByOrgPatientId()
        ).uid("docInfoByOrgPatientId").name("docInfoByOrgPatientId");
        docInfoByOrgPds = docInfoConverters.map(
                (MapFunction<DocInfoConverter, DocInfoByOrgPds>) event -> event.getDocInfoByOrgPds()
        ).uid("docInfoByOrgPds").name("docInfoByOrgPds");
    }

    /**
     * configures the different pojo's needed to write to cassandra for flieinfo contents
     */
    public void setupFileinfoPojos() {
        fileInfoByChecksum = fileInfoConverters.map(
                (MapFunction<FileInfoConverter, FileInfoByChecksum>) event -> event.getFileInfoByChecksum()
        ).uid("fileInfoByChecksum").name("fileInfoByChecksum");
        fileInfoById = fileInfoConverters.map(
                (MapFunction<FileInfoConverter, FileInfoById>) event -> event.getFileInfoById()
        ).uid("fileInfoById").name("fileInfoById");
        fileInfoByOrg = fileInfoConverters.map(
                (MapFunction<FileInfoConverter, FileInfoByOrg>) event -> event.getFileInfoByOrg()
        ).uid("fileInfoByOrg").name("fileInfoByOrg");
    }
}
