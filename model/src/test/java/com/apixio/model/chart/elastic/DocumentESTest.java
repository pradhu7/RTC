package com.apixio.model.chart.elastic;

import com.apixio.model.patient.EncounterType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DocumentESTest {

    private static final UUID docUUID = UUID.randomUUID();
    private static final UUID patientUUID = UUID.randomUUID();
    private static final UUID providerUUID = UUID.randomUUID();

    @Test
    public void testDocumentSerialization() throws JsonProcessingException {
        DocumentES doc = createDocument();

        ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(doc);
        System.out.println(json);
        DocumentES deserializedDoc = mapper.readValue(json, DocumentES.class);
        Assert.assertEquals(doc, deserializedDoc);
    }

    @Ignore
    @Test
    public void testDocumentMapping() throws Exception {
        Map<String, Object> mapping = DocumentES.getElasticMapping();
        String json = new ObjectMapper().writeValueAsString(mapping);
        System.out.println(json);
    }

    private static DocumentES createDocument() {
        DocumentInfoES docInfo = DocumentInfoES.Builder.newBuilder()
                .externalIds(ImmutableList.of("externalId123"))
                .dateReceived(DateTime.now())
                .dateTransferred(DateTime.now())
                .build();
        return DocumentES.Builder.newBuilder()
                .documentUUID(docUUID)
                .documentInfo(docInfo)
                .encounterInfo(EncounterInfoES.Builder.newBuilder()
                        .encounterId("encounter1")
                        .date(DateTime.now())
                        .type(EncounterType.APPOINTMENT)
                        .build())
                .patientInfo(PatientInfoES.Builder.newBuilder()
                        .patientUUID(patientUUID)
                        .build())
                .providerInfo(ProviderInfoES.Builder.newBuilder()
                        .servicingProviderUUID(providerUUID)
                        .placeOfService("some_place")
                        .build())
                .projectInfo(ImmutableList.of(ProjectInfoES.Builder.newBuilder()
                        .projectId("project123")
                        .build()))
                .annotations(createAnnotations())
                .build();
    }

    private static List<AnnotationES> createAnnotations() {
        AnnotationES annotation1 = AnnotationES.Builder.newBuilder()
                .id("annotation1")
                .timestamp(DateTime.now())
                .finding(AnnotationES.FindingES.FindingBuilder.newBuilder()
                        .code("code1")
                        .attributes(ImmutableMap.of("findingAttribute1", "findingValue1"))
                        .build())
                .attributes(ImmutableMap.of("annotationAttribute1", "annotationValue1"))
                .evidence(ImmutableMap.of("evidence1", "evidenceValue1"))
                .build();
        AnnotationES annotation2 = AnnotationES.Builder.newBuilder()
                .id("annotation2")
                .timestamp(DateTime.now())
                .finding(AnnotationES.FindingES.FindingBuilder.newBuilder()
                        .code("code1")
                        .attributes(ImmutableMap.of("findingAttribute2", "findingValue2"))
                        .build())
                .attributes(ImmutableMap.of("annotationAttribute2", "annotationValue2"))
                .evidence(ImmutableMap.of("evidence2", "evidenceValue2"))
                .build();
        return ImmutableList.of(annotation1, annotation2);
    }
}
