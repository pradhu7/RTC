package com.apixio.model.event.transformer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.io.IOException;
import java.util.UUID;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.junit.Test;

import com.apixio.model.event.AttributeType;
import com.apixio.model.event.EventType;
import com.apixio.model.utility.ClinicalCodeBuilder;

/**
 * Created by vvyas on 1/23/14.
 */
public class EventTypeJSONParserTest {

    @Test
    public final void testParseNarrativeEvent() {
       String narrativeEvent =
               //"{\"subject\":{\"uri\":\"4ae240b4-84c2-44c3-9788-64cfee215001\",\"type\":\"patient\"},\"source\":{\"uri\":\"4ae240b4-84c2-44c3-9788-64cfee215001\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"1\",\"codeSystem\":\"APXCAT\"},\"values\":{\"result\":\"REJECTED\"},\"timeRange\":{\"startTime\":\"2017-01-01\",\"endTime\":\"2017-12-31\"}},\"evidence\":{\"inferred\":\"true\",\"source\":{\"type\":\"DocCoverageCombiner\",\"key\":\"1.0.0\"}},\"attributes\":{\"sourceType\":\"AUDITOR\",\"SOURCE_TYPE\":\"AUDITOR\"}}";

               "{\"subject\":{\"uri\":\"7d1ee538-7cb0-4c83-bd26-6406d4b6b9c9\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"83\",\"codeSystem\":\"HCC2013PYFinal\",\"codeSystemVersion\":\"2013Final\",\"displayName\":\"83\"},\"time\":{\"startTime\":\"2012-12-18T12:45:40-0800\",\"endTime\":\"2012-12-18T12:45:40-0800\"},\"values\":{\"face2face\":\"YES\",\"face2faceConfidence\":\"0.9709066893408104\",\"predictionEvidence\":\"cardiac angina\"}},\"source\":{\"uri\":\"a4302eda-3304-46f1-b679-0e3407faf9de\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"hccdict_bl20140106a.txt\",\"type\":\"File\"},\"attributes\":{\"predictionTs\":\"1389949359463\",\"classifierClass\":\"com.apixio.advdev.eventgen.extractors.tagging.TaggingService\",\"title\":\"Office Visit\",\"pageNumber\":\"1\",\"extractionType\":\"plainText\",\"snippet\":\"cardiac angina\"}},\"attributes\":{\"sweep\":\"Dec2013\",\"uploadingOrganizaiton\":\"Monarch\",\"cohort\":\"Monarch\",\"bucketType\":\"dictionaryModel.v0.0.1\",\"bucketName\":\"dictionaryModel.v0.0.1.83\"}}";

               //"{\"attributes\":{\"bucketType\":\"Signal2EventDecisionTree\",\"$jobId\":\"workNum1357862\",\"$patientUUID\":\"fb61b2a0-6067-48d2-8da0-0510cc2d40b7\",\"$orgId\":\"10001051\",\"sourceType\":\"NARRATIVE\",\"tags\":\"chronicDict\",\"$documentId\":\"unknownc50125\",\"$documentUUID\":\"eef1b96c-1395-4749-b19a-087e66b62ca4\",\"$sourceArchiveDocumentUUID\":\"eef1b96c-1395-4749-b19a-087e66b62ca4\",\"$workId\":\"WR_e042f920-21ed-438d-8c89-5e71db00a034\",\"bucketName\":\"Signal2EventDecisionTree-a843417c0e4b4e3d8893d44b647afd25de6b7347\",\"$modelVersion\":\"1542765783888+8882661167584\",\"$batchId\":\"10001051_cr2018id_microproject\",\"$propertyVersion\":\"1542765783938\",\"$extractBatchId\":\"10001051_cr2018id_microproject^^EHR^^1542765783938+1542661167584^^5.1.10\"},\"source\":{\"type\":\"document\",\"uri\":\"eef1b96c-1395-4749-b19a-087e66b62ca4\"},\"evidence\":{\"source\":{\"type\":\"Signal2EventDecisionTree\",\"uri\":\"1.0.0\"},\"inferred\":\"True\",\"attributes\":{\"tags\":\"chronicDict\",\"pageNumber\":\"9\",\"predictionConfidence\":\"1.0\"}},\"fact\":{\"code\":{\"codeSystemVersion\":\"2.0\",\"codeSystemName\":\"APXCAT\",\"code\":\"CRV2_3\",\"codeSystem\":\"APXCAT\"},\"time\":{\"endTime\":\"2018-03-01T00:00:00+0000\",\"startTime\":\"2018-03-01T00:00:00+0000\"}},\"subject\":{\"type\":\"patient\",\"uri\":\"fb61b2a0-6067-48d2-8da0-0510cc2d40b7\"}}";
        String testEvent = "{'attributes': {'bucketType': 'XGBoostEnsembleCombiner', '$jobId': 'workNum1357737', '$patientUUID': '00027a76-b1d1-482e-a0c6-8de9b50275dc', '$orgId': '10001060', 'sourceType': 'NARRATIVE', '$documentId': 'unknowne36099', '$documentUUID': 'c090916c-26a8-40c1-8f24-d4b33bf17691', '$sourceArchiveDocumentUUID': 'c090916c-26a8-40c1-8f24-d4b33bf17691', '$workId': 'WR_c4c3634c-99c7-4667-85aa-50217dcbdff5', 'bucketName': 'XGBoostEnsembleCombiner-1.0.0', '$modelVersion': '1542765783938+1542661167584', '$batchId': '10001060_Anthem_ensemble_extract_20181121', '$propertyVersion': '1542765783938', '$extractBatchId': '10001060_Anthem_ensemble_extract_20181121^^EHR^^1542765783938+1542661167584^^5.1.10'}, 'source': {'type': 'document', 'uri': 'c090916c-26a8-40c1-8f24-d4b33bf17691'}, 'evidence': {'source': {'type': 'XGBoostEnsembleCombiner', 'uri': '1.0.0'}, 'inferred': True, 'attributes': {'pageNumber': '25', 'predictionConfidence': '0.7818449'}}, 'fact': {'code': {'codeSystemVersion': '2.0', 'codeSystemName': 'APXCAT', 'code': 'V23_82', 'codeSystem': 'APXCAT'}, 'time': {'endTime': '2018-03-15T00:00:00+0000', 'startTime': '2018-03-15T00:00:00+0000'}}, 'subject': {'type': 'patient', 'uri': '00027a76-b1d1-482e-a0c6-8de9b50275dc'}}\n" +
                "{'attributes': {'bucketType': 'XGBoostEnsembleCombiner', '$jobId': 'workNum1357737', '$patientUUID': '00027a76-b1d1-482e-a0c6-8de9b50275dc', '$orgId': '10001060', 'sourceType': 'NARRATIVE', '$documentId': 'unknowne36099', '$documentUUID': 'c090916c-26a8-40c1-8f24-d4b33bf17691', '$sourceArchiveDocumentUUID': 'c090916c-26a8-40c1-8f24-d4b33bf17691', '$workId': 'WR_c4c3634c-99c7-4667-85aa-50217dcbdff5', 'bucketName': 'XGBoostEnsembleCombiner-1.0.0', '$modelVersion': '1542765783938+1542661167584', '$batchId': '10001060_Anthem_ensemble_extract_20181121', '$propertyVersion': '1542765783938', '$extractBatchId': '10001060_Anthem_ensemble_extract_20181121^^EHR^^1542765783938+1542661167584^^5.1.10'}, 'source': {'type': 'document', 'uri': 'c090916c-26a8-40c1-8f24-d4b33bf17691'}, 'evidence': {'source': {'type': 'XGBoostEnsembleCombiner', 'uri': '1.0.0'}, 'inferred': True, 'attributes': {'pageNumber': '9', 'predictionConfidence': '0.7868545'}}, 'fact': {'code': {'codeSystemVersion': '2.0', 'codeSystemName': 'APXCAT', 'code': 'CRV2_125', 'codeSystem': 'APXCAT'per doc;/tmp/smuggle/g/g_ee016d8f-e0de-4e14-b100-3bda8b981989}, \n" +
                "'time': {'endTime': '2018-05-15T00:00:00+0000', 'startTime': '2018-05-15T00:00:00+0000'}}, 'subject': {'type': 'patient', 'uri': '00027a76-b1d1-482e-a0c6-8de9b50275dc'}}";
        EventTypeJSONParser jsonParser = new EventTypeJSONParser();
        try {
            EventType e = jsonParser.parseEventTypeData(narrativeEvent);

            // make some important assertions - and see if they hold
            assertEquals("Patient UUID should be imported correctly",
                    "7d1ee538-7cb0-4c83-bd26-6406d4b6b9c9",e.getSubject().getUri());
            assertEquals("Subject type should be patient",
                    "patient",e.getSubject().getType());

            // make assertions about dates
            assertNotNull("Date should not be empty", e.getFact().getTime().getStartTime());
            assertEquals("Date should match json",
                    DateTime.parse("2012-12-18T12:45:40-0800").toDate(),
                    e.getFact().getTime().getStartTime());

            assertEquals("Attribute should not be empty", 5, e.getAttributes().getAttribute().size());

            AttributeType sweepAttr = null;
            for(AttributeType attr: e.getAttributes().getAttribute()) {
                if("sweep".equals(attr.getName())) {
                    sweepAttr = attr;
                    break;
                }
            }
            assertNotNull("Sweep attribute should not be null",sweepAttr);
            assertEquals("Sweep attribute should have Dec2013 as value", "Dec2013", sweepAttr.getValue());


        } catch (IOException e1) {
            fail(e1.getMessage());
        }
    }


    @Test
    public final void testGenerateEvent() {
        String serializedObject = "{\"subject\":{\"uri\":\"7d1ee538-7cb0-4c83-bd26-6406d4b6b9c9\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"100\",\"codeSystemName\":\"ICD-9\"},\"time\":{\"startTime\":\"2012-10-10T00:00:00+0000\",\"endTime\":\"2012-10-10T00:00:00+0000\"},\"values\":{\"sweep\":\"TestSweep\",\"uploadingOrganization\":\"Test Organization\"}},\"source\":{\"uri\":\"a4302eda-3304-46f1-b679-0e3407faf9de\",\"type\":\"document\"}}";

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        EventType e = new EventType();
        e.setSubject(EventTransformerUtils.patient(UUID.fromString("7d1ee538-7cb0-4c83-bd26-6406d4b6b9c9")));
        e.setSource(EventTransformerUtils.document(UUID.fromString("a4302eda-3304-46f1-b679-0e3407faf9de")));
        e.setFact(EventTransformerUtils.createFact(
                new ClinicalCodeBuilder().code("100").codingSystem("ICD-9").build(),
                DateTime.parse("2012-10-10T00:00:00+0000"),
                DateTime.parse("2012-10-10T00:00:00+0000"),
                new EventTypeAttributesBuilder()
                        .add("sweep", "TestSweep")
                        .add("uploadingOrganization", "Test Organization")
                        .build()));

        EventTypeJSONParser parser = new EventTypeJSONParser();

        try {
            assertEquals("Events should serialize into valid json objects",
                    serializedObject,
                    parser.toJSON(e));
        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

}
