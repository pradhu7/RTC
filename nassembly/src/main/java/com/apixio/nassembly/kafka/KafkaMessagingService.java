package com.apixio.nassembly.kafka;

import com.apixio.messages.DataMessages;
import com.apixio.messages.DataMessages.DataRequest;
import com.apixio.messages.MessageMetadata;
import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.messages.OcrMessages;
import com.apixio.security.Security;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.JsonFormat;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import com.apixio.messages.OcrMessages.OcrRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.apixio.util.Protobuf;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KafkaMessagingService {

    private final KafkaProducer<String, String> kafkaProducer;

    private final String ocrTopic;
    private final String patientTopic;

    private final Security security;

    private static final String DOC_UUID_TYPE = "DOC";
    private static final String KEY_DELIMITER = "_";

    private static final String SERVERS = "servers";
    private static final String KEY_SERIALIZER = "keySerializer";
    private static final String VALUE_SERIALIZER = "valueSerializer";
    private static final String OCR_TOPIC = "ocrTopic";
    private static final String PATIENT_TOPIC = "patientTopic";

    private static final List<OcrMessages.Operation> OPERATIONS = ImmutableList.of(OcrMessages.Operation.OCR,
            OcrMessages.Operation.PAGER);

    public KafkaMessagingService(Map<String, String> configMap) {
        if (!configMap.containsKey(SERVERS) || !configMap.containsKey(KEY_SERIALIZER) || !configMap.containsKey(VALUE_SERIALIZER)
                || !configMap.containsKey(OCR_TOPIC) || !configMap.containsKey(PATIENT_TOPIC)) {
            throw new IllegalArgumentException("Config map is missing one or more required parameters: " +
                    "[servers, keySerializer, valueSerializer, ocrTopic, patientTopic]");
        }
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configMap.get(SERVERS));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, configMap.get(KEY_SERIALIZER));
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, configMap.get(VALUE_SERIALIZER));
        kafkaProducer = new KafkaProducer<>(properties);
        security = Security.getInstance();
        this.ocrTopic = configMap.get(OCR_TOPIC);
        this.patientTopic = configMap.get(PATIENT_TOPIC);
    }

    @VisibleForTesting
    public void sendOcrMessage(final String docId, final String pdsId, final String batchId)
            throws Exception {
        final String recordKey = docId + KEY_DELIMITER + pdsId;

        final com.apixio.XUUID convertedPdsId = PatientDataSetLogic.patientDataSetIDFromLong(Long.parseLong(pdsId));

        OcrRequest.Builder messageBuilder = OcrRequest.newBuilder();
        messageBuilder.setHeader(Protobuf.makeMessageHeader(MessageMetadata.MessageType.OCR_REQUEST))
                .setDocumentID(XUUID.newBuilder().setUuid(docId).setType(DOC_UUID_TYPE))
                .setPdsID(XUUID.newBuilder().setUuid(convertedPdsId.getUUID().toString()).setType(convertedPdsId.getType()))
                .setBatchName(batchId)
                .addAllOperations(OPERATIONS);

        //convert proto to json string
        final String jsonString = JsonFormat.printer()
                .includingDefaultValueFields()
                .omittingInsignificantWhitespace()
                .print(messageBuilder);

        //send kafka message
        ProducerRecord<String, String> record = new ProducerRecord<>(ocrTopic, recordKey, jsonString);
        kafkaProducer.send(record).get();
    }

    public void sendPatientMessage(final String docId, final String pdsId, final String batchId) throws Exception {
        final String recordKey = docId + KEY_DELIMITER + pdsId;

        final com.apixio.XUUID convertedPdsId = PatientDataSetLogic.patientDataSetIDFromLong(Long.parseLong(pdsId));

        //build proto message
        DataRequest.Builder messageBuilder = DataRequest.newBuilder();
        messageBuilder.setHeader(Protobuf.makeMessageHeader(MessageMetadata.MessageType.DATA_REQUEST))
                .setDocId(XUUID.newBuilder().setUuid(docId).setType(DOC_UUID_TYPE))
                .setPdsId(XUUID.newBuilder().setUuid(convertedPdsId.getUUID().toString()).setType(convertedPdsId.getType()))
                .setDataType(DataMessages.DataType.apo)
                .setBatchId(batchId)
                .setFileType(DataMessages.FileType.ARCHIVE_DOCUMENT)
                .setFileLocation(DataMessages.FileLocation.APIXIO_FILE_SYSTEM);

        //convert proto to json string
        final String jsonString = JsonFormat.printer()
                .includingDefaultValueFields()
                .omittingInsignificantWhitespace()
                .print(messageBuilder);

        //send kafka message
        ProducerRecord<String, String> record = new ProducerRecord<>(patientTopic, recordKey, jsonString);
        kafkaProducer.send(record).get();
    }

    //it is the responsibility of the initializer to close resources at end
    public void close() {
        kafkaProducer.close();
    }
}
