package com.apixio.metric;

import com.apixio.metric.AuditEventRecordOuterClass;
import com.apixio.metric.MetricEventWrapperOuterClass;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditEventRecordSerdeTest {

    private static MetricEventWrapperOuterClass.MetricEventWrapper metricWrapper;
    private static AuditEventRecordOuterClass.AuditEventRecord componentEvent;
    private static AuditEventRecordOuterClass.PatientSignalMeta patientSignalMeta;

    @BeforeAll
    public static void setupAll() throws Exception {
        patientSignalMeta = AuditEventRecordOuterClass.PatientSignalMeta.newBuilder()
                        .setMessageType("patientAssembly")
                        .setPersistedCategories("all")
                        .build();
        componentEvent = AuditEventRecordOuterClass.AuditEventRecord.newBuilder()
                .setIdentity(UUID.randomUUID().toString())
                .setWorkrequestId(UUID.randomUUID().toString())
                .setPdsId("1022")
                .setBatchName("Test Batch")
                .setPatientId(UUID.randomUUID().toString())
                .setSource("ETL")
                .setComponent("assembly")
                .setEvent("ASSEMBLY")
                .setStatus("Completed")
                .setWhenMeasured(System.currentTimeMillis())
//                .setMeta(toJson(patientSignalMeta))

                .setPatientSignalMeta(AuditEventRecordOuterClass.PatientSignalMeta.newBuilder()
                        .setMessageType("patientAssembly")
                        .setPersistedCategories("all")
                        .build())

                .build();

        metricWrapper = MetricEventWrapperOuterClass.MetricEventWrapper.newBuilder()
                .setVersion("1.0.0")
                .setMetricName("component_events")
                .setMessageType("metric")
                .setAuditEvent(componentEvent)
                .build();
    }

    @Test
    public void toJsonTest() throws Exception {
        try {
            String json = toJson(metricWrapper);
            assertThat(json).isNotEmpty();
            assertThat(json).contains("meta");
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid protocol buffer", e);
        }
    }

    private static String toJson(Message message) throws Exception {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid protocol buffer", e);
        }
    }



}
