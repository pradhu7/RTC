package com.apixio.bizlogic.patient.assembly.coverage;

import com.apixio.bizlogic.patient.assembly.all.AllAssembler;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Coverage;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.bizlogic.patient.assembly.SummaryTestUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Created by dyee on 4/28/17.
 */
public class CoverageAssemblerTest
{
    private static final String HEALTH_PLAN_META_KEY = "HEALTH_PLAN";

    AllAssembler allAssembler;
    CoverageAssembler coverageAssembler;
    PatientJSONParser parser;

    @Before
    public void init()
    {
        coverageAssembler = new CoverageAssembler();
        allAssembler = new AllAssembler();
        parser = new PatientJSONParser();
    }

    protected Patient generateTestPatient()
    {
        Patient p = new Patient();
        return p;
    }

/*    String samplePatient = "{\n" +
            "   \"procedures\":[\n" +
            "      {\n" +
            "         \"metadata\":{\n" +
            "            \"TRANSACTION_DATE\":\"2016-12-10\",\n" +
            "            \"CLAIM_TYPE\":\"FEE_FOR_SERVICE_CLAIM\"\n" +
            "         },\n" +
            "         \"sourceId\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"689655f8-2762-4c59-9eea-08370ef14a4a\",\n" +
            "         \"originalId\":{\n" +
            "            \"id\":\"PRQ\",\n" +
            "            \"assignAuthority\":\"CLAIM_ID\"\n" +
            "         },\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.030-07:00\",\n" +
            "         \"primaryClinicalActorId\":\"4aca5b76-ea71-46b7-b90a-8fb67b9c5534\",\n" +
            "         \"code\":{\n" +
            "            \"code\":\"64722\",\n" +
            "            \"codingSystem\":\"CPT_4\",\n" +
            "            \"codingSystemOID\":\"2.16.840.1.113883.6.12\",\n" +
            "            \"displayName\":\"64722\"\n" +
            "         },\n" +
            "         \"procedureName\":\"F2F Claim\",\n" +
            "         \"performedOn\":\"2016-03-05T16:00:00.000-08:00\",\n" +
            "         \"endDate\":\"2016-03-05T16:00:00.000-08:00\",\n" +
            "         \"supportingDiagnosis\":[\n" +
            "            {\n" +
            "               \"code\":\"S99291A\",\n" +
            "               \"codingSystem\":\"ICD_10CM_DIAGNOSIS_CODES\",\n" +
            "               \"codingSystemOID\":\"2.16.840.1.113883.6.90\",\n" +
            "               \"displayName\":\"S99291A\"\n" +
            "            }\n" +
            "         ]\n" +
            "      }\n" +
            "   ],\n" +
            "   \"patientId\":\"8c3d8672-a448-4aa3-9d82-22051839c697\",\n" +
            "   \"primaryExternalID\":{\n" +
            "      \"id\":\"888-808-888\",\n" +
            "      \"assignAuthority\":\"RANDOM_UUID\"\n" +
            "   },\n" +
            "   \"externalIDs\":[\n" +
            "      {\n" +
            "         \"id\":\"888-808-888\",\n" +
            "         \"assignAuthority\":\"RANDOM_UUID\"\n" +
            "      }\n" +
            "   ],\n" +
            "   \"primaryDemographics\":{\n" +
            "      \"sourceId\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "      \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "      \"internalUUID\":\"47ee02a4-6078-46c1-8379-da91d89757d9\",\n" +
            "      \"editType\":\"ACTIVE\",\n" +
            "      \"lastEditDateTime\":\"2017-05-17T10:56:04.027-07:00\",\n" +
            "      \"name\":{\n" +
            "         \"internalUUID\":\"70edba6b-b429-491a-bb3f-101022edaafc\",\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.027-07:00\",\n" +
            "         \"givenNames\":[\n" +
            "            \"Anthony\"\n" +
            "         ],\n" +
            "         \"familyNames\":[\n" +
            "            \"LaRocca\"\n" +
            "         ],\n" +
            "         \"nameType\":\"CURRENT_NAME\"\n" +
            "      },\n" +
            "      \"dateOfBirth\":\"1982-09-03T17:00:00.000-07:00\",\n" +
            "      \"gender\":\"MALE\"\n" +
            "   },\n" +
            "   \"coverage\":[\n" +
            "      {\n" +
            "         \"sourceId\":\"c64cea29-1219-4215-b5fd-f9ff01cc4bf3\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"62e5bb9a-5a4b-44df-bff5-88ca6fce5e42\",\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.029-07:00\",\n" +
            "         \"sequenceNumber\":1,\n" +
            "         \"startDate\":[\n" +
            "            2015,\n" +
            "            1,\n" +
            "            1\n" +
            "         ],\n" +
            "         \"endDate\":[\n" +
            "            2019,\n" +
            "            1,\n" +
            "            1\n" +
            "         ],\n" +
            "         \"type\":\"CMS\",\n" +
            "         \"healthPlanName\":\"Apixio\",\n" +
            "         \"subscriberID\":{\n" +
            "            \"id\":\"XYZ\",\n" +
            "            \"assignAuthority\":\"HP_ID\"\n" +
            "         }\n" +
            "      }\n" +
            "   ],\n" +
            "   \"encounters\":[\n" +
            "      {\n" +
            "         \"sourceId\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"ca578deb-27b0-4429-9aa2-17bac648eda9\",\n" +
            "         \"originalId\":{\n" +
            "            \"assignAuthority\":\"12345678\"\n" +
            "         },\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.028-07:00\",\n" +
            "         \"primaryClinicalActorId\":\"e821f7e7-ce98-46eb-8efe-7ca87ba19f35\",\n" +
            "         \"encounterStartDate\":\"2016-03-05T16:00:00.000-08:00\",\n" +
            "         \"encounterEndDate\":\"2016-03-05T16:00:00.000-08:00\",\n" +
            "         \"encType\":\"FACE_TO_FACE\",\n" +
            "         \"siteOfService\":{\n" +
            "            \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "            \"internalUUID\":\"5de8e2d4-e114-4efb-8590-ebe8b294a6f1\",\n" +
            "            \"editType\":\"ACTIVE\",\n" +
            "            \"lastEditDateTime\":\"2017-05-17T10:56:04.028-07:00\",\n" +
            "            \"careSiteId\":\"5de8e2d4-e114-4efb-8590-ebe8b294a6f1\"\n" +
            "         },\n" +
            "         \"encounterId\":\"ca578deb-27b0-4429-9aa2-17bac648eda9\",\n" +
            "         \"encounterDate\":\"2016-03-05T16:00:00.000-08:00\"\n" +
            "      }\n" +
            "   ],\n" +
            "   \"sources\":[\n" +
            "      {\n" +
            "         \"metadata\":{\n" +
            "            \"HEALTH_PLAN\":\"Apixio\"\n" +
            "         },\n" +
            "         \"sourceId\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.027-07:00\",\n" +
            "         \"sourceSystem\":\"EHR\",\n" +
            "         \"sourceType\":\"EHR\",\n" +
            "         \"creationDate\":\"2017-04-30T17:00:00.000-07:00\",\n" +
            "         \"dciStart\":[\n" +
            "            2016,\n" +
            "            5,\n" +
            "            1\n" +
            "         ],\n" +
            "         \"dciEnd\":[\n" +
            "            2017,\n" +
            "            5,\n" +
            "            1\n" +
            "         ]\n" +
            "      },\n" +
            "      {\n" +
            "         \"metadata\":{\n" +
            "            \"HEALTH_PLAN\":\"Apixio\"\n" +
            "         },\n" +
            "         \"sourceId\":\"c64cea29-1219-4215-b5fd-f9ff01cc4bf3\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"c64cea29-1219-4215-b5fd-f9ff01cc4bf3\",\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.029-07:00\",\n" +
            "         \"sourceSystem\":\"EHR\",\n" +
            "         \"sourceType\":\"EHR\",\n" +
            "         \"creationDate\":\"2017-04-30T17:00:00.000-07:00\",\n" +
            "         \"dciStart\":[\n" +
            "            2016,\n" +
            "            5,\n" +
            "            1\n" +
            "         ],\n" +
            "         \"dciEnd\":[\n" +
            "            2017,\n" +
            "            5,\n" +
            "            1\n" +
            "         ]\n" +
            "      }\n" +
            "   ],\n" +
            "   \"parsingDetails\":[\n" +
            "      {\n" +
            "         \"metadata\":{\n" +
            "            \"sourceUploadBatch\":\"370_assembly_full_sample_set8a\",\n" +
            "            \"sourceFileArchiveUUID\":\"11c8ac5f-2104-496b-b51e-9213331cc3e5\",\n" +
            "            \"sourceFileHash\":\"b40f5527b837c1133381d63a5bca29a7e7d3749b\"\n" +
            "         },\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"parsingDateTime\":\"2017-05-17T10:56:04.024-07:00\",\n" +
            "         \"contentSourceDocumentType\":\"STRUCTURED_DOCUMENT\",\n" +
            "         \"parser\":\"AXM\",\n" +
            "         \"parserVersion\":\"0.1\"\n" +
            "      }\n" +
            "   ],\n" +
            "   \"clinicalActors\":[\n" +
            "      {\n" +
            "         \"sourceId\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"e821f7e7-ce98-46eb-8efe-7ca87ba19f35\",\n" +
            "         \"originalId\":{\n" +
            "            \"id\":\"1234567890\",\n" +
            "            \"assignAuthority\":\"NPI\"\n" +
            "         },\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.029-07:00\",\n" +
            "         \"primaryId\":{\n" +
            "            \"id\":\"1234567890\",\n" +
            "            \"assignAuthority\":\"NPI\"\n" +
            "         },\n" +
            "         \"role\":\"ATTENDING_PHYSICIAN\",\n" +
            "         \"clinicalActorId\":\"e821f7e7-ce98-46eb-8efe-7ca87ba19f35\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"sourceId\":\"85a1fa0f-a95e-4da9-a7c5-ba246b3429ad\",\n" +
            "         \"parsingDetailsId\":\"b0f00f22-5532-45ce-98cd-824de10775aa\",\n" +
            "         \"internalUUID\":\"4aca5b76-ea71-46b7-b90a-8fb67b9c5534\",\n" +
            "         \"originalId\":{\n" +
            "            \"id\":\"1234567890\",\n" +
            "            \"assignAuthority\":\"NPI\"\n" +
            "         },\n" +
            "         \"editType\":\"ACTIVE\",\n" +
            "         \"lastEditDateTime\":\"2017-05-17T10:56:04.030-07:00\",\n" +
            "         \"primaryId\":{\n" +
            "            \"id\":\"1234567890\",\n" +
            "            \"assignAuthority\":\"NPI\"\n" +
            "         },\n" +
            "         \"role\":\"ATTENDING_PHYSICIAN\",\n" +
            "         \"clinicalActorId\":\"4aca5b76-ea71-46b7-b90a-8fb67b9c5534\"\n" +
            "      }\n" +
            "   ]\n" +
            "}";

    String samplePatient2 = "{\n" +
            "\t\"procedures\": [{\n" +
            "\t\t\"metadata\": {\n" +
            "\t\t\t\"TRANSACTION_DATE\": \"2016-12-10\",\n" +
            "\t\t\t\"CLAIM_TYPE\": \"FEE_FOR_SERVICE_CLAIM\"\n" +
            "\t\t},\n" +
            "\t\t\"sourceId\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"15347359-04d4-411a-a851-785d90f7a155\",\n" +
            "\t\t\"originalId\": {\n" +
            "\t\t\t\"id\": \"PRQ\",\n" +
            "\t\t\t\"assignAuthority\": \"CLAIM_ID\"\n" +
            "\t\t},\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.218-07:00\",\n" +
            "\t\t\"primaryClinicalActorId\": \"89d6d556-120d-4132-b01a-f42c2f583d4b\",\n" +
            "\t\t\"code\": {\n" +
            "\t\t\t\"code\": \"64722\",\n" +
            "\t\t\t\"codingSystem\": \"CPT_4\",\n" +
            "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.12\",\n" +
            "\t\t\t\"displayName\": \"64722\"\n" +
            "\t\t},\n" +
            "\t\t\"procedureName\": \"F2F Claim\",\n" +
            "\t\t\"performedOn\": \"2016-03-05T16:00:00.000-08:00\",\n" +
            "\t\t\"endDate\": \"2016-03-05T16:00:00.000-08:00\",\n" +
            "\t\t\"supportingDiagnosis\": [{\n" +
            "\t\t\t\"code\": \"S99291A\",\n" +
            "\t\t\t\"codingSystem\": \"ICD_10CM_DIAGNOSIS_CODES\",\n" +
            "\t\t\t\"codingSystemOID\": \"2.16.840.1.113883.6.90\",\n" +
            "\t\t\t\"displayName\": \"S99291A\"\n" +
            "\t\t}]\n" +
            "\t}],\n" +
            "\t\"patientId\": \"8c3d8672-a448-4aa3-9d82-22051839c697\",\n" +
            "\t\"primaryExternalID\": {\n" +
            "\t\t\"id\": \"888-808-888\",\n" +
            "\t\t\"assignAuthority\": \"RANDOM_UUID\"\n" +
            "\t},\n" +
            "\t\"externalIDs\": [{\n" +
            "\t\t\"id\": \"888-808-888\",\n" +
            "\t\t\"assignAuthority\": \"RANDOM_UUID\"\n" +
            "\t}],\n" +
            "\t\"primaryDemographics\": {\n" +
            "\t\t\"sourceId\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"32dde04a-7fc1-4116-8db4-c66fa9f49fdc\",\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.215-07:00\",\n" +
            "\t\t\"name\": {\n" +
            "\t\t\t\"internalUUID\": \"350f63d3-2d18-4479-9019-9e1f0a2e27f3\",\n" +
            "\t\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.215-07:00\",\n" +
            "\t\t\t\"givenNames\": [\"Anthony\"],\n" +
            "\t\t\t\"familyNames\": [\"LaRocca\"],\n" +
            "\t\t\t\"nameType\": \"CURRENT_NAME\"\n" +
            "\t\t},\n" +
            "\t\t\"dateOfBirth\": \"1982-09-03T17:00:00.000-07:00\",\n" +
            "\t\t\"gender\": \"MALE\"\n" +
            "\t},\n" +
            "\t\"coverage\": [{\n" +
            "\t\t\"sourceId\": \"3131b7c9-37a1-40ae-ab3c-5214d4fc5569\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"b89a111b-4afb-4e61-b6ef-7e817ba46f66\",\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.217-07:00\",\n" +
            "\t\t\"sequenceNumber\": 1,\n" +
            "\t\t\"startDate\": [2015, 1, 1],\n" +
            "\t\t\"endDate\": [2019, 1, 1],\n" +
            "\t\t\"type\": \"CMS\",\n" +
            "\t\t\"healthPlanName\": \"Apixio\",\n" +
            "\t\t\"subscriberID\": {\n" +
            "\t\t\t\"id\": \"XYZ\",\n" +
            "\t\t\t\"assignAuthority\": \"HP_ID\"\n" +
            "\t\t}\n" +
            "\t}],\n" +
            "\t\"encounters\": [{\n" +
            "\t\t\"sourceId\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"e7855a19-8d57-4413-ab78-84815e404a25\",\n" +
            "\t\t\"originalId\": {\n" +
            "\t\t\t\"assignAuthority\": \"12345678\"\n" +
            "\t\t},\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.216-07:00\",\n" +
            "\t\t\"primaryClinicalActorId\": \"d425175f-fd0d-48f1-803a-3c5c763fc18f\",\n" +
            "\t\t\"encounterStartDate\": \"2016-03-05T16:00:00.000-08:00\",\n" +
            "\t\t\"encounterEndDate\": \"2016-03-05T16:00:00.000-08:00\",\n" +
            "\t\t\"encType\": \"FACE_TO_FACE\",\n" +
            "\t\t\"siteOfService\": {\n" +
            "\t\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\t\"internalUUID\": \"e9274328-1aac-4535-9a21-4caf700ba9b3\",\n" +
            "\t\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.217-07:00\",\n" +
            "\t\t\t\"careSiteId\": \"e9274328-1aac-4535-9a21-4caf700ba9b3\"\n" +
            "\t\t},\n" +
            "\t\t\"encounterId\": \"e7855a19-8d57-4413-ab78-84815e404a25\",\n" +
            "\t\t\"encounterDate\": \"2016-03-05T16:00:00.000-08:00\"\n" +
            "\t}],\n" +
            "\t\"sources\": [{\n" +
            "\t\t\"sourceId\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.215-07:00\",\n" +
            "\t\t\"sourceSystem\": \"EHR\",\n" +
            "\t\t\"sourceType\": \"EHR\",\n" +
            "\t\t\"creationDate\": \"2017-04-30T17:00:00.000-07:00\"\n" +
            "\t}, {\n" +
            "\t\t\"sourceId\": \"3131b7c9-37a1-40ae-ab3c-5214d4fc5569\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"3131b7c9-37a1-40ae-ab3c-5214d4fc5569\",\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.217-07:00\",\n" +
            "\t\t\"sourceSystem\": \"EHR\",\n" +
            "\t\t\"sourceType\": \"EHR\",\n" +
            "\t\t\"creationDate\": \"2017-04-30T17:00:00.000-07:00\",\n" +
            "\t\t\"dciStart\": [2016, 5, 1],\n" +
            "\t\t\"dciEnd\": [2017, 5, 1]\n" +
            "\t}],\n" +
            "\t\"parsingDetails\": [{\n" +
            "\t\t\"metadata\": {\n" +
            "\t\t\t\"sourceUploadBatch\": \"370_assembly_full_sample_set8a\",\n" +
            "\t\t\t\"sourceFileArchiveUUID\": \"11c8ac5f-2104-496b-b51e-9213331cc3e5\",\n" +
            "\t\t\t\"sourceFileHash\": \"b40f5527b837c1133381d63a5bca29a7e7d3749b\"\n" +
            "\t\t},\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"parsingDateTime\": \"2017-05-17T22:52:40.212-07:00\",\n" +
            "\t\t\"contentSourceDocumentType\": \"STRUCTURED_DOCUMENT\",\n" +
            "\t\t\"parser\": \"AXM\",\n" +
            "\t\t\"parserVersion\": \"0.1\"\n" +
            "\t}],\n" +
            "\t\"clinicalActors\": [{\n" +
            "\t\t\"sourceId\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"d425175f-fd0d-48f1-803a-3c5c763fc18f\",\n" +
            "\t\t\"originalId\": {\n" +
            "\t\t\t\"id\": \"1234567890\",\n" +
            "\t\t\t\"assignAuthority\": \"NPI\"\n" +
            "\t\t},\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.217-07:00\",\n" +
            "\t\t\"primaryId\": {\n" +
            "\t\t\t\"id\": \"1234567890\",\n" +
            "\t\t\t\"assignAuthority\": \"NPI\"\n" +
            "\t\t},\n" +
            "\t\t\"role\": \"ATTENDING_PHYSICIAN\",\n" +
            "\t\t\"clinicalActorId\": \"d425175f-fd0d-48f1-803a-3c5c763fc18f\"\n" +
            "\t}, {\n" +
            "\t\t\"sourceId\": \"05c4ef9d-ec4e-46fa-8270-1ddfda09a28d\",\n" +
            "\t\t\"parsingDetailsId\": \"c86b771c-fc68-4b85-a1f3-e9386692d33f\",\n" +
            "\t\t\"internalUUID\": \"89d6d556-120d-4132-b01a-f42c2f583d4b\",\n" +
            "\t\t\"originalId\": {\n" +
            "\t\t\t\"id\": \"1234567890\",\n" +
            "\t\t\t\"assignAuthority\": \"NPI\"\n" +
            "\t\t},\n" +
            "\t\t\"editType\": \"ACTIVE\",\n" +
            "\t\t\"lastEditDateTime\": \"2017-05-17T22:52:40.218-07:00\",\n" +
            "\t\t\"primaryId\": {\n" +
            "\t\t\t\"id\": \"1234567890\",\n" +
            "\t\t\t\"assignAuthority\": \"NPI\"\n" +
            "\t\t},\n" +
            "\t\t\"role\": \"ATTENDING_PHYSICIAN\",\n" +
            "\t\t\"clinicalActorId\": \"89d6d556-120d-4132-b01a-f42c2f583d4b\"\n" +
            "\t}]\n" +
            "}";*/


    @Test
    public void testSeparate() {
        Patient patient1 = generateTestPatient();

        patient1.setPrimaryExternalID(createExternalId("002-coverage-summary", "PAT_AA"));

        Source s1 = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        patient1.addSource(s1);
        Coverage patientCoverage1 = createCoverage(s1, "WellCare HMO", "001^^PAT_AA", "2015-01-01", "2015-05-31");
        patient1.addCoverage(patientCoverage1);

        Source s2 = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        s2.setMetaTag(HEALTH_PLAN_META_KEY, "CMS");
        patient1.addSource(s2);
        Coverage patientCoverage2 = createCoverage(s2, "CMS", "001^^PAT_AA", "2015-06-01", "2015-12-31");
        patient1.addCoverage(patientCoverage2);

        List<Part<Patient>> partList = coverageAssembler.separate(patient1);

        int i = 1;
        for(Part<Patient> patientPart : partList)
        {
            assertEquals("coverage", patientPart.getCategory());

            Iterator<Coverage> coverageIterator = patientPart.getPart().getCoverage().iterator();

            Coverage coverage = coverageIterator.next();
            if(i==1)
                assertEquals(patientCoverage2, coverage);
            else
                assertEquals(patientCoverage1, coverage);

            assertFalse(coverageIterator.hasNext());
            i++;
        }
    }

    @Test
    public void testSeparateDuplicate() {
        Patient patient1 = generateTestPatient();

        patient1.setPrimaryExternalID(createExternalId("002-coverage-summary", "PAT_AA"));

        Source s1 = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        patient1.addSource(s1);
        Coverage patientCoverage1 = createCoverage(s1, "WellCare HMO", "001^^PAT_AA", "2015-01-01", "2015-05-31");
        patient1.addCoverage(patientCoverage1);

        Source s2 = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        patient1.addSource(s2);
        Coverage patientCoverage2 = createCoverage(s2, "WellCare HMO", "001^^PAT_AA", "2015-06-01", "2015-12-31");
        patient1.addCoverage(patientCoverage2);

        List<Part<Patient>> partList = coverageAssembler.separate(patient1);

        for(Part<Patient> patientPart : partList)
        {
            assertEquals("coverage", patientPart.getCategory());

            Iterator<Coverage> coverageIterator = patientPart.getPart().getCoverage().iterator();

            Coverage coverage = coverageIterator.next();
            assertTrue(patientCoverage1.equals(coverage) || patientCoverage2.equals(coverage));

            Coverage coverage2 = coverageIterator.next();
            assertTrue(patientCoverage1.equals(coverage2) || patientCoverage2.equals(coverage2));

            assertNotSame(coverage, coverage2);
            assertFalse(coverageIterator.hasNext());
        }
    }


    @Test
    public void testMerge() {
        //
        // Patient #1
        //
        Patient patient1 = generateTestPatient();
        patient1.setPrimaryExternalID(createExternalId("002-coverage-summary", "PAT_AA"));
        Source s1 = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        patient1.addSource(s1);
        Coverage patientCoverage1 = createCoverage(s1,
                "WellCare HMO", "001^^PAT_AA", "2015-01-01", "2015-05-31");
        patient1.addCoverage(patientCoverage1);

        Source s2 = createSource("2016-01-01", "2015-01-01", "2015-12-31");
        patient1.addSource(s2);
        Coverage patientCoverage2 = createCoverage(s2, "WellCare HMO", "001^^PAT_AA", "2015-06-01", "2015-12-31");
        patient1.addCoverage(patientCoverage2);

        //
        // Patient #2
        //
        Patient patient2 = generateTestPatient();
        patient2.setPrimaryExternalID(createExternalId("003-coverage-summary", "PAT_AA"));
        Source p2s2 = createSource("2017-01-01", "2016-01-01", "2016-12-31");
        patient2.addSource(p2s2);
        Coverage patient2_Coverage1 = createCoverage(p2s2,
                "WellCare HMO", "002^^PAT_AA", "2016-01-01", "2016-12-31");
        patient2.addCoverage(patient2_Coverage1);

        //
        // Merge
        //
        Patient mergedPatient = coverageAssembler.merge(null, new PartEnvelope<Patient>(patient1, 0L), new PartEnvelope<Patient>(patient2, 0L)).part;

        assertNotNull(mergedPatient);

        int mergedCoverageCount = 0;
        Iterator<Coverage> mergedPatientCoverageList = mergedPatient.getCoverage().iterator();
        while(mergedPatientCoverageList.hasNext()) {
            mergedPatientCoverageList.next();
            mergedCoverageCount++;
        }

        assertTrue(isCoverageInMergedObject(patientCoverage1, mergedPatient));
        assertTrue(isCoverageInMergedObject(patientCoverage2, mergedPatient));
        assertTrue(isCoverageInMergedObject(patient2_Coverage1, mergedPatient));

        assertEquals(3, mergedCoverageCount);

    }

    @Test
    public void testMerge2() {
        // Patient 1, more recent that p2
        Patient p1 = new Patient();
        p1.addSource(SummaryTestUtils.createSource(DateTime.now().minusYears(20)));
        Demographics d1 = SummaryTestUtils.createDemographics("First", "", "Last", "01/01/1980", "Female");
        p1.setPrimaryDemographics(d1);
        ExternalID e1 = SummaryTestUtils.createExternalId("ID1", "PatientID1");
        p1.setPrimaryExternalID(e1);
        ContactDetails cd1 = SummaryTestUtils.createContactDetails("111 Test St","San Mateo", "CA", "11111", "test@apixio.com", "111-111-1111");
        p1.setPrimaryContactDetails(cd1);
    }

    protected boolean isCoverageInMergedObject(Coverage testCoverage, Patient mergedPatient) {
        Iterator<Coverage> mergedPatientCoverageList = mergedPatient.getCoverage().iterator();
        while(mergedPatientCoverageList.hasNext()) {
            Coverage coverage = mergedPatientCoverageList.next();
//            if(coverage.equals(testCoverage)) return true;
            if (coverage.getSequenceNumber() == testCoverage.getSequenceNumber()
                    && coverage.getSubscriberID().equals(testCoverage.getSubscriberID())
                    && coverage.getHealthPlanName().equals(testCoverage.getHealthPlanName())
                    && coverage.getStartDate().equals(testCoverage.getStartDate())
                    && coverage.getEndDate().equals(testCoverage.getEndDate()))
                return true;
        }
        return false;
    }

    /**
     * Creates a health plan coverage span for a subscriber originating from a source.
     *
     * Test data factory.
     */
    public static Coverage createCoverage(Source source, String healthPlan, String subscriberId, String start, String end)
    {
        Coverage coverage = createCoverage(healthPlan, subscriberId, start, end);
        coverage.setSourceId(source.getInternalUUID());
        return coverage;
    }


    /**
     * Creates a coverage span for a subscriber originating from a source.
     *
     * Test data factory.
     */
    private Coverage createCoverage(Source source, String subscriberId, String start, String end)
    {
        Coverage coverage = createCoverage(subscriberId, start, end);
        coverage.setSourceId(source.getInternalUUID());
        return coverage;
    }

    /**
     * Creates a health plan coverage span for a subscriber.
     *
     * Test data factory.
     */
    private static Coverage createCoverage(String healthPlan, String subscriberId, String start, String end)
    {
        Coverage coverage = createCoverage(subscriberId, start, end);
        coverage.setHealthPlanName(healthPlan);
        return coverage;
    }

    /**
     * Creates a coverage span for a subscriber.
     *
     * Test data factory.
     */
    private static Coverage createCoverage(String subscriberId, String start, String end)
    {
        Coverage coverage = new Coverage();
        String[] parts = subscriberId.split("\\^\\^");
        coverage.setSubscriberID(createExternalId(parts[0], parts[1]));
        coverage.setStartDate(LocalDate.parse(start));
        coverage.setEndDate(LocalDate.parse(end));
        return coverage;
    }

    //not used
//    private Coverage createCoverageOpenEnded(String subscriberId, String start)
//    {
//        Coverage coverage = new Coverage();
//        String[] parts = subscriberId.split("\\^\\^");
//        coverage.setSubscriberID(createExternalId(parts[0], parts[1]));
//        coverage.setStartDate(LocalDate.parse(start));
//        return coverage;
//    }

    /**
     * Creates a source with a freashness date and DCI.
     *
     * Test data factory.
     */
    public static Source createSource(String createdAt, String dciStart, String dciEnd)
    {
        Source source = new Source();
        source.setSourceId(source.getInternalUUID());
        source.setCreationDate(DateTime.parse(createdAt));
        source.setDciStart(LocalDate.parse(dciStart));
        source.setDciEnd(LocalDate.parse(dciEnd));

        source.setMetaTag(HEALTH_PLAN_META_KEY, "WellCare HMO");
        return source;
    }

    private ExternalID createExternalId(String idWithAuthority)
    {
        String[] parts = idWithAuthority.split("\\^\\^");
        return createExternalId(parts[0], parts[1]);
    }

    public static ExternalID createExternalId(String id, String assigningAuthority)
    {
        ExternalID externalId = new ExternalID();
        externalId.setId(id);
        externalId.setAssignAuthority(assigningAuthority);
        return externalId;
    }

    private boolean hasCoverage(Iterable<Coverage> spans, String healthPlan, String subscriberId, String startDate, String endDate)
    {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        for (Coverage span : spans)
        {
            if (span.getStartDate().equals(start)
                    && span.getEndDate().equals(end)
                    && span.getHealthPlanName().equals(healthPlan)
                    && span.getSubscriberID().equals(createExternalId(subscriberId)))
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasCoverage(Iterable<Coverage> spans, UUID sourceId, String healthPlan, String subscriberId, String startDate, String endDate)
    {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        for (Coverage span : spans)
        {
            if (span.getStartDate().equals(start)
                    && span.getEndDate().equals(end)
                    && span.getSourceId().equals(sourceId)
                    && span.getHealthPlanName().equals(healthPlan)
                    && span.getSubscriberID().equals(createExternalId(subscriberId)))
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasCoverage(Patient patient, String healthPlan, String subscriberId, String startDate, String endDate)
    {
        return hasCoverage(getCoverage(patient), healthPlan, subscriberId, startDate, endDate);
    }

    private boolean hasCoverage(Patient patient, UUID sourceId, String healthPlan, String subscriberId, String startDate, String endDate)
    {
        return hasCoverage(getCoverage(patient), sourceId, healthPlan, subscriberId, startDate, endDate);
    }


    private List<Coverage> getCoverage(Patient patient)
    {
        List<Coverage> spans = new ArrayList<>();
        for (Coverage span : patient.getCoverage())
        {
            spans.add(span);
        }
        return spans;
    }

    private List<Source> getSources(Patient patient)
    {
        List<Source> sources = new ArrayList<>();
        for (Source source : patient.getSources())
        {
            sources.add(source);
        }
        return sources;
    }

    private Source getSource(Iterable<Source> sources, String healthPlan, String creationDate, String dciStartDate, String dciEndDate)
    {
        DateTime createdAt = DateTime.parse(creationDate);
        LocalDate dciStart = LocalDate.parse(dciStartDate);
        LocalDate dciEnd = LocalDate.parse(dciEndDate);
        for (Source source : sources)
        {
            if(source.getMetaTag(HEALTH_PLAN_META_KEY) == null) continue;

            if (source.getCreationDate().equals(createdAt)
                    && source.getDciStart().equals(dciStart)
                    && source.getDciEnd().equals(dciEnd)
                    && source.getMetaTag(HEALTH_PLAN_META_KEY).equals(healthPlan))

            {
                return source;
            }
        }
        return null;
    }

//    private Source getSource(Iterable<Source> sources, String creationDate, String dciStartDate, String dciEndDate)
//    {
//        DateTime createdAt = DateTime.parse(creationDate);
//        LocalDate dciStart = LocalDate.parse(dciStartDate);
//        LocalDate dciEnd = LocalDate.parse(dciEndDate);
//        for (Source source : sources)
//        {
//            if (source.getCreationDate().equals(createdAt)
//                    && source.getDciStart().equals(dciStart)
//                    && source.getDciEnd().equals(dciEnd))
//
//            {
//                return source;
//            }
//        }
//        return null;
//    }

    private Source getSource(Patient patient, String healthPlan, String creationDate, String dciStart, String dciEnd)
    {
        return getSource(getSources(patient), healthPlan, creationDate, dciStart, dciEnd);
    }

    private boolean hasSource(Patient patient, String healthPlan, String creationDate, String dciStart, String dciEnd)
    {
        return getSource(patient, healthPlan, creationDate, dciStart, dciEnd) != null;
    }

//    /**
//     * Health plan name should not be relevant for unit tests relying on this method.
//     */
//    private boolean hasSource(Set<Source> sources, String creationDate, String dciStartDate, String dciEndDate)
//    {
//        return getSource(sources, creationDate, dciStartDate, dciEndDate) != null;
//    }

    /**
     * Clones a patient.
     */
    private Patient clone(Patient original) throws Exception
    {
        return parser.parsePatientData(parser.toJSON(original));
    }
}
