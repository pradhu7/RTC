package com.apixio.metric;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

public class GenerateAuditEventRecordsTest {
    private static List<String> messageTypes = Arrays.asList("patientAssembly", "persist", "assembly", "ocr");
    private static List<String> pdsIds = Arrays.asList("1022", "1023", "1024");
    private static List<String> status = Arrays.asList("FINISH_SUCCEEDED", "FINISH_FAILED", "COMPLETED", "ERRORED");
    private static List<String> components = Arrays.asList("LOADER", "ETL_V2", "INTEGRATION_SVC");

    Map<String, Long> messageTypeMap = messageTypes.stream().collect(Collectors.toMap(messageType -> messageType, messageType -> 0L));
    Map<String, Long> pdsIdMap = pdsIds.stream().collect(Collectors.toMap(pdsId -> pdsId, pdsId -> 0L));
    Map<String, Long> statusMap = status.stream().collect(Collectors.toMap(status -> status, status -> 0L));
    Map<String, Long> componentMap = components.stream().collect(Collectors.toMap(component -> component, component -> 0L));

    @Test
    public void writeAuditRecords() throws Exception {
        List<AuditEventRecordOuterClass.AuditEventRecord> records = generateAuditEvents(10);
        String saami = "saami";
    }


    private List<AuditEventRecordOuterClass.AuditEventRecord> generateAuditEvents(long count) throws Exception {
        List<AuditEventRecordOuterClass.AuditEventRecord> componentEventRecords = new ArrayList<>();
        Random rand = new Random();

        for (int i=0; i<count; i++) {
                AuditEventRecordOuterClass.PatientSignalMeta patientSignalMeta = AuditEventRecordOuterClass.PatientSignalMeta.newBuilder()
                        .setMessageType(messageTypes.get(rand.nextInt(messageTypes.size())))
                        .setPersistedCategories("all")
                        .build();
                AuditEventRecordOuterClass.AuditEventRecord componentEvent = AuditEventRecordOuterClass.AuditEventRecord.newBuilder()
                        .setIdentity(UUID.randomUUID().toString())
                        .setWorkrequestId(UUID.randomUUID().toString())
                        .setPdsId(pdsIds.get(rand.nextInt(pdsIds.size())))
                        .setBatchName("Test Batch")
                        .setPatientId(UUID.randomUUID().toString())
                        .setSource("ETL")
                        .setComponent(components.get(rand.nextInt(components.size())))
                        .setEvent("PROCESSING")
                        .setStatus(status.get(rand.nextInt(status.size())))
                        .setWhenMeasured(System.currentTimeMillis())
//                        .setMeta(toJson(patientSignalMeta))
                        .setPatientSignalMeta(patientSignalMeta)
                        .build();

                updateDataCounts(patientSignalMeta.getMessageType(), componentEvent.getPdsId(), componentEvent.getStatus(), componentEvent.getComponent());
                componentEventRecords.add(componentEvent);
        }
        return componentEventRecords;
    }

    private void updateDataCounts(String messageType, String pdsId, String status, String component) {
        messageTypeMap.put(messageType, messageTypeMap.get(messageType) + 1);
        pdsIdMap.put(pdsId, pdsIdMap.get(pdsId) + 1);
        statusMap.put(status, statusMap.get(status) + 1);
        componentMap.put(component, componentMap.get(component) + 1);
    }


    private static String toJson(Message message) throws Exception {
        try {
            return JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid protocol buffer", e);
        }
    }
}
