package com.apixio.dao.utility;

import com.apixio.model.patient.Document;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Patient;
import com.apixio.protobuf.Doccacheelements.DocCacheElementProto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by mramanna on 6/19/17.
 */
public class DocCacheElemUtilityTest {

    private Document d;
    private Patient  p;
    private Random r = new Random(new Date().getTime());
    private static final String content = "abcedf";


    @Before
    public void setUp() throws IOException
    {
        p = new Patient();
        p.setPatientId(UUID.randomUUID());
        List<Document> docs = new ArrayList<>();
        docs.add(d);
        p.setDocuments(docs);

        d = new Document();
        d.setDocumentTitle("Test Title");
        d.setInternalUUID(UUID.randomUUID());

        ExternalID externalID = new ExternalID();
        externalID.setAssignAuthority("TEST_AA");
        externalID.setId(Long.toString(r.nextLong()));
        d.setOriginalId(externalID);

        d.setStringContent(content);
    }

    @After
    public void tearDown() {
        d = null;
        p = null;
    }

    @Test
    public void testUpdateAPO() {
        List<DocCacheElementProto> protos = new ArrayList<>();
        for (int i = 0;  i < Math.abs(r.nextInt(20)) + 2 ; i++) {
            protos.add(getRandomDocCacheElementProto(p.getPatientId().toString(), d.getInternalUUID().toString()));
        }

        String data = DocCacheElemUtility.updateDocumentWithDocCacheElementProtoList(d, protos);

        Map<String, String> md = d.getMetadata();
        assertThat(md.containsKey(DocCacheElemUtility.MD_KEY_DCE_FORMAT), is(true));
        assertThat(md.containsKey(DocCacheElemUtility.MD_KEY_DCE_TS), is(true));
        assertThat(md.containsKey(DocCacheElemUtility.MD_KEY_DEC_DATA), is(true));

        // assert that we didn't destroy string content
        assertThat(d.getStringContent(), notNullValue());
        assertThat(d.getStringContent().length(), is(content.length()));

        String data2 = DocCacheElemUtility.getSerializedDocCacheElemData(d);
        assertThat(data2, is(data));
    }

    @Test
    public void testExtractAPO() {
        List<DocCacheElementProto> protos = new ArrayList<>();
        for (int i = 0;  i < Math.abs(r.nextInt(20)) + 2; i++) {
            protos.add(getRandomDocCacheElementProto(p.getPatientId().toString(), d.getInternalUUID().toString()));
        }

        DocCacheElemUtility.updateDocumentWithDocCacheElementProtoList(d, protos);
        List<DocCacheElementProto> protos2 = DocCacheElemUtility.extractDocCacheElementProtoList(d);

        assertEquals(protos, protos2);
    }


    @Test
    public void testSerDeser() {

        List<DocCacheElementProto> protos = new ArrayList<>();
        for (int i = 0;  i < r.nextInt(20) + 2; i++) {
            protos.add(getRandomDocCacheElementProto(p.getPatientId().toString(), d.getInternalUUID().toString()));
        }

        String ser = DocCacheElemUtility.serializeDocCacheElementProtoList(protos);
        assertThat(ser, notNullValue());

        List<DocCacheElementProto> protos2;
        protos2 = DocCacheElemUtility.deserializeToDocCacheElementProtoList(ser);

        assertEquals(protos, protos2);
    }


    private void assertEquals(List<DocCacheElementProto> protos, List<DocCacheElementProto> protos2) {
        assertThat(protos, notNullValue());
        assertThat(protos2, notNullValue());
        assertThat(protos2.size(), is(protos.size()));
        for (int i = 0; i < protos2.size(); i++) {
            DocCacheElementProto proto = protos.get(i);
            DocCacheElementProto proto2 = protos2.get(i);
            assertThat(proto.getPatientUUID(), is(proto2.getPatientUUID()));
            assertThat(proto.getDocumentUUID(), is(proto2.getDocumentUUID()));
            assertThat(proto.getContent(), is(equalTo(proto2.getContent())));
        }
    }

    private DocCacheElementProto getRandomDocCacheElementProto(String patientUUID, String documentUUID) {
        DocCacheElementProto.Builder builder = DocCacheElementProto.newBuilder();
        builder.setPatientUUID(patientUUID);
        builder.setDocumentUUID(documentUUID);
        builder.setContent("content" + Integer.toString(r.nextInt()));
        return builder.build();
    }

}