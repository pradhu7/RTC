package com.apixio.dao;


import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.security.Security;
import com.apixio.utility.DataCompression;
import com.google.protobuf.Descriptors;
import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Ignore("Broken")
public class ApoTest
{
//    static Security encryptor = Security.getInstance();
//
//    public static Test.apo.CLINICALACTORS generateClinicalActor()
//    {
//        return Test.apo.CLINICALACTORS.newBuilder()
//                .setClinicalActorId(UUID.randomUUID().toString())
//                .setEditType("EditType")
//                .setInternalUUID(UUID.randomUUID().toString())
//                .setLastEditDateTime("2017-12-12")
//                .setOriginalId(Test.apo.CLINICALACTORS.ORIGINALID.newBuilder()
//                        .setAssignAuthority("assign_auth")
//                        .setId(UUID.randomUUID().toString()).build())
//                .setPrimaryId(Test.apo.CLINICALACTORS.PRIMARYID.newBuilder()
//                         .setAssignAuthority(UUID.randomUUID().toString())
//                        .setId(UUID.randomUUID().toString()).build())
//                .build();
//    }
//
//    public static Test.apo.SOURCES generateSource()
//    {
//        return Test.apo.SOURCES.newBuilder()
//                .setCreationDate("2017-01-01")
//                .setEditType("EditType")
//                .setInternalUUID(UUID.randomUUID().toString())
//                .setParsingDetailsId(UUID.randomUUID().toString())
//                .setSourceSystem("source system")
//                .setLastEditDateTime("2017-09-23")
//                .setSourceId(UUID.randomUUID().toString()).build();
//    }
//
//    public static Test.apo.PARSINGDETAILS generateParsingDetail()
//    {
//        Test.apo.PARSINGDETAILS.METADATA metadata = Test.apo.PARSINGDETAILS.METADATA.newBuilder()
//            .setSourceFileArchiveUUID(UUID.randomUUID().toString())
//            .setSourceFileHash("sdkfjklsadjfsadlkfjasdkljf").build();
//
//        Test.apo.PARSINGDETAILS.Builder builder =  Test.apo.PARSINGDETAILS.newBuilder();
//
//        builder.setContentSourceDocumentType("contentSourceDocumentType");
//        builder.setParser("parser");
//        builder.setParserVersion("parser version");
//        builder.setParsingDateTime("2017-3-2");
//        builder.setMetadata(metadata);
//        return builder.build();
//    }
//
//    static  byte[] makePatientBytes(byte [] p, boolean compress)
//            throws Exception
//    {
//        if (p != null)
//        {
//            DataCompression compression = new DataCompression();
//
//            byte[] result = compression.compressData(p, compress);
//
//            return encryptor.encryptBytesToBytes(result);
//        }
//
//        return null;
//    }
//
//    static InputStream getDecryptedDecompressedBytes(byte[] patientData, boolean isCompressed) throws Exception
//    {
//        DataCompression compressor = new DataCompression();
//        if (patientData != null)
//        {
//            InputStream decryptedBytes = null;
//            try
//            {
//                /* If it's compressed, we decrypt bytes to bytes. else since there is no compression, we will use normal decryption. */
//                if (isCompressed)
//                    decryptedBytes = encryptor.decryptBytesToInputStream(patientData, false);
//                else
//                    decryptedBytes = new ByteArrayInputStream(encryptor.decrypt(new String(patientData, "UTF-8")).getBytes("UTF-8"));
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//
//            if (decryptedBytes != null)
//                return compressor.decompressData(decryptedBytes, isCompressed);
//        }
//
//        return null;
//    }
//
//    public static void main(String [] args) throws Exception
//    {
//        Test.apo.Builder builder = Test.apo.newBuilder();
//
//        for(int i=0; i<11000; i++)
//        {
//            builder.addClinicalActors(generateClinicalActor());
//        }
//
//        List<Test.apo.SOURCES> sources = new ArrayList<>();
//        for(int i=0; i<11000; i++)
//        {
//            sources.add(generateSource());
//        }
//        builder.addAllSources(sources);
//
//        for(int i=0; i<11000; i++)
//        {
//            builder.addParsingDetails(generateParsingDetail());
//        }
//
//        Test.apo apo = builder.build();
//        System.out.println(apo.toString());
//        System.out.println(apo.toString().length());
//
//        byte[] patient = makePatientBytes(apo.toByteArray(), true);
//
//        for(int i=0; i<20; i++)
//        {
//            long start = System.currentTimeMillis();
//            Test.apo apoRestored = Test.apo.parseFrom(getDecryptedDecompressedBytes(patient, true));
//            System.out.println("restore took: [" + (System.currentTimeMillis() - start) + "] ms");
//        }
//
//        // =========== now test APO
//        String json = generateTestJson();
//
//        PatientDataUtility patientDataUtility = new PatientDataUtility();
//
//        PatientJSONParser parser = new PatientJSONParser();
//        Patient jsonPatient = parser.parsePatientData(json);
//
//        byte [] patientBytes = patientDataUtility.makePatientBytes(jsonPatient, true);
//
//        for(int i=0; i<20; i++)
//        {
//            long start = System.currentTimeMillis();
//            Patient apoRestored = patientDataUtility.getPatientObj(patientBytes, true);
//
//            System.out.println(parser.toJSON(apoRestored).length());
//            System.out.println("restore took: [" + (System.currentTimeMillis() - start) + "] ms");
//        }
//    }
//
//    public static String generateTestJson() {
//        StringBuffer buffer = new StringBuffer("{\n" +
//                "\t\"procedures\": [{\n" +
//                "\t\t\"metadata\": {\n" +
//                "\t\t\t\"TRANSACTION_DATE\": \"2016-12-10\"\n" +
//                "\t\t},\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"523bd944-ee3e-438b-b5f9-1452efc8a21b\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"assignAuthority\": \"onefish\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.763-08:00\",\n" +
//                "\t\t\"primaryClinicalActorId\": \"96ea2470-c9e2-417b-9127-79c201e6a30e\",\n" +
//                "\t\t\"code\": {\n" +
//                "\t\t\t\"code\": \"64722\",\n" +
//                "\t\t\t\"codingSystem\": \"CPT_4\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.12\",\n" +
//                "\t\t\t\"displayName\": \"64722\"\n" +
//                "\t\t},\n" +
//                "\t\t\"procedureName\": \"NOT PART OF UNIQUENESS^\",\n" +
//                "\t\t\"performedOn\": \"2016-12-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"endDate\": \"2016-12-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"interpretation\": \"INTERPRETATION1\",\n" +
//                "\t\t\"supportingDiagnosis\": [{\n" +
//                "\t\t\t\"code\": \"S99291A\",\n" +
//                "\t\t\t\"codingSystem\": \"ICD_10CM_DIAGNOSIS_CODES\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.90\",\n" +
//                "\t\t\t\"displayName\": \"S99291A\"\n" +
//                "\t\t}]\n" +
//                "\t}, {\n" +
//                "\t\t\"metadata\": {\n" +
//                "\t\t\t\"TRANSACTION_DATE\": \"2016-12-11\"\n" +
//                "\t\t},\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"cb4c6fc7-217e-4cd4-a0c0-f81928c05462\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"assignAuthority\": \"twofish\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                "\t\t\"primaryClinicalActorId\": \"f27eab82-687c-449e-8f31-f7d6cd470c67\",\n" +
//                "\t\t\"code\": {\n" +
//                "\t\t\t\"code\": \"64722\",\n" +
//                "\t\t\t\"codingSystem\": \"CPT_4\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.12\",\n" +
//                "\t\t\t\"displayName\": \"64722\"\n" +
//                "\t\t},\n" +
//                "\t\t\"procedureName\": \"NOT PART OF UNIQUENESS^\",\n" +
//                "\t\t\"performedOn\": \"2016-12-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"endDate\": \"2016-12-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"supportingDiagnosis\": [{\n" +
//                "\t\t\t\"code\": \"M4141\",\n" +
//                "\t\t\t\"codingSystem\": \"ICD_10CM_DIAGNOSIS_CODES\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.90\",\n" +
//                "\t\t\t\"displayName\": \"M4141\"\n" +
//                "\t\t}]\n" +
//                "\t}, {\n" +
//                "\t\t\"metadata\": {\n" +
//                "\t\t\t\"TRANSACTION_DATE\": \"2017-02-11\"\n" +
//                "\t\t},\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"e4769c15-c66e-49df-bf64-10539f83d44a\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"assignAuthority\": \"redfish\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                "\t\t\"primaryClinicalActorId\": \"31f9860a-a210-44ab-8f7e-8517ecfbee47\",\n" +
//                "\t\t\"code\": {\n" +
//                "\t\t\t\"code\": \"64722\",\n" +
//                "\t\t\t\"codingSystem\": \"CPT_4\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.12\",\n" +
//                "\t\t\t\"displayName\": \"64722\"\n" +
//                "\t\t},\n" +
//                "\t\t\"procedureName\": \"NOT PART OF UNIQUENESS^\",\n" +
//                "\t\t\"performedOn\": \"2017-02-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"endDate\": \"2017-02-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"supportingDiagnosis\": [{\n" +
//                "\t\t\t\"code\": \"M4141\",\n" +
//                "\t\t\t\"codingSystem\": \"ICD_10CM_DIAGNOSIS_CODES\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.90\",\n" +
//                "\t\t\t\"displayName\": \"M4141\"\n" +
//                "\t\t}]\n" +
//                "\t}, {\n" +
//                "\t\t\"metadata\": {\n" +
//                "\t\t\t\"TRANSACTION_DATE\": \"2016-12-11\"\n" +
//                "\t\t},\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"ad5b1822-a8b5-44b7-a30c-0378f11d4284\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"assignAuthority\": \"bluefish\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                "\t\t\"primaryClinicalActorId\": \"1b7fbdbd-51cf-4d18-a614-167c77b24759\",\n" +
//                "\t\t\"code\": {\n" +
//                "\t\t\t\"code\": \"64722\",\n" +
//                "\t\t\t\"codingSystem\": \"CPT_4\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.12\",\n" +
//                "\t\t\t\"displayName\": \"64722\"\n" +
//                "\t\t},\n" +
//                "\t\t\"procedureName\": \"NOT PART OF UNIQUENESS^\",\n" +
//                "\t\t\"performedOn\": \"2016-12-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"endDate\": \"2016-12-09T16:00:00.000-08:00\",\n" +
//                "\t\t\"supportingDiagnosis\": [{\n" +
//                "\t\t\t\"code\": \"M4141\",\n" +
//                "\t\t\t\"codingSystem\": \"ICD_10CM_DIAGNOSIS_CODES\",\n" +
//                "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.90\",\n" +
//                "\t\t\t\"displayName\": \"M4141\"\n" +
//                "\t\t}]\n" +
//                "\t}],\n" +
//                "\t\"patientId\": \"31b67fcb-9567-426a-8cb9-d009b2b09771\",\n" +
//                "\t\"primaryExternalID\": {\n" +
//                "\t\t\"id\": \"proceduresWithSameNPI2\",\n" +
//                "\t\t\"assignAuthority\": \"PAT_ID\"\n" +
//                "\t},\n" +
//                "\t\"externalIDs\": [{\n" +
//                "\t\t\"id\": \"proceduresWithSameNPI2\",\n" +
//                "\t\t\"assignAuthority\": \"PAT_ID\"\n" +
//                "\t}],\n" +
//                "\t\"sources\": [{\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.763-08:00\",\n" +
//                "\t\t\"sourceSystem\": \"Apixio\",\n" +
//                "\t\t\"sourceType\": \"Test\",\n" +
//                "\t\t\"creationDate\": \"2018-01-29T16:00:00.000-08:00\"\n" +
//                "\t}],\n" +
//                "\t\"parsingDetails\": [{\n" +
//                "\t\t\"metadata\": {\n" +
//                "\t\t\t\"sourceUploadBatch\": \"1109_procedures_with_same_npi_2\",\n" +
//                "\t\t\t\"sourceFileArchiveUUID\": \"a176b7b0-ae8a-4f1c-848a-8c79bb0daeee\",\n" +
//                "\t\t\t\"sourceFileHash\": \"c7ac55ffb06becafd001cec2b01d4e9f09c19fac\"\n" +
//                "\t\t},\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"parsingDateTime\": \"2018-02-13T14:46:13.760-08:00\",\n" +
//                "\t\t\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\",\n" +
//                "\t\t\"parser\": \"AXM\",\n" +
//                "\t\t\"parserVersion\": \"0.1\"\n" +
//                "\t}],\n" +
//                "\t\"clinicalActors\": [{\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"96ea2470-c9e2-417b-9127-79c201e6a30e\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.764-08:00\",\n" +
//                "\t\t\"primaryId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"role\": \"ADMINISTERING_PHYSICIAN\",\n" +
//                "\t\t\"clinicalActorId\": \"96ea2470-c9e2-417b-9127-79c201e6a30e\"\n" +
//                "\t}, {\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"f27eab82-687c-449e-8f31-f7d6cd470c67\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                "\t\t\"primaryId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"role\": \"ADMINISTERING_PHYSICIAN\",\n" +
//                "\t\t\"clinicalActorId\": \"f27eab82-687c-449e-8f31-f7d6cd470c67\"\n" +
//                "\t}, {\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"31f9860a-a210-44ab-8f7e-8517ecfbee47\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                "\t\t\"primaryId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"role\": \"ADMINISTERING_PHYSICIAN\",\n" +
//                "\t\t\"clinicalActorId\": \"31f9860a-a210-44ab-8f7e-8517ecfbee47\"\n" +
//                "\t}, {\n" +
//                "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                "\t\t\"internalUUID\": \"1b7fbdbd-51cf-4d18-a614-167c77b24759\",\n" +
//                "\t\t\"originalId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"editType\": \"ACTIVE\",\n" +
//                "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                "\t\t\"primaryId\": {\n" +
//                "\t\t\t\"id\": \"0123456789\",\n" +
//                "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                "\t\t},\n" +
//                "\t\t\"role\": \"ADMINISTERING_PHYSICIAN\",\n" +
//                "\t\t\"clinicalActorId\": \"1b7fbdbd-51cf-4d18-a614-167c77b24759\"\n" +
//                "\t}");
//
//        for(int i = 0; i<22000; i++)
//        {
//            buffer.append(", {\n" +
//                    "\t\t\"sourceId\": \"673e6c02-21d5-4935-aa71-b0c404556ffb\",\n" +
//                    "\t\t\"parsingDetailsId\": \"091f2fd2-9f31-4d5f-a9b5-c087ff474c53\",\n" +
//                    "\t\t\"internalUUID\": \"1b7fbdbd-51cf-4d18-a614-167c77b24759\",\n" +
//                    "\t\t\"originalId\": {\n" +
//                    "\t\t\t\"id\": \"0123456789\",\n" +
//                    "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                    "\t\t},\n" +
//                    "\t\t\"editType\": \"ACTIVE\",\n" +
//                    "\t\t\"lastEditDateTime\": \"2018-02-13T14:46:13.765-08:00\",\n" +
//                    "\t\t\"primaryId\": {\n" +
//                    "\t\t\t\"id\": \"0123456789\",\n" +
//                    "\t\t\t\"assignAuthority\": \"NPI\"\n" +
//                    "\t\t},\n" +
//                    "\t\t\"role\": \"ADMINISTERING_PHYSICIAN\",\n" +
//                    "\t\t\"clinicalActorId\": \"1b7fbdbd-51cf-4d18-a614-167c77b24759\"\n" +
//                    "\t}");
//        }
//
//        buffer.append("]\n" +
//                "}");
//
//        return buffer.toString();
//    }

}
