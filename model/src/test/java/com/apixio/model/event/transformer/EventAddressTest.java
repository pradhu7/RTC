package com.apixio.model.event.transformer;

import com.apixio.model.event.EventAddress;
import org.junit.Test;
import static junit.framework.Assert.*;

/**
 * Created by vvyas on 3/11/2015.
 */
public class EventAddressTest {

    EventTypeJSONParser ejp = new EventTypeJSONParser();

    @Test
    public final void testNonEqualEventsShouldHaveDifferentEventAddress() {
        String event1 =
                "{\"subject\":{\"uri\":\"02b8a15f-37e1-4ff6-93c7-e09840edbc52\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"V12_108\",\"codeSystem\":\"APXCAT\",\"codeSystemVersion\":\"2.0\",\"displayName\":\"XXX\"},\"time\":{\"startTime\":\"2011-08-10T00:00:00+0000\",\"endTime\":\"2011-08-10T00:00:00+0000\"}},\"source\":{\"uri\":\"21011c21-4583-475f-903f-13d2f0eb7656\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"apxcat_dict_20140918b_BR.txt\",\"type\":\"dictionary\"},\"attributes\":{\"title\":\"XXX\",\"pageNumber\":\"9\",\"associatedEncounter\":\"Apixio:UNKNONW_ENCOUNTER\",\"snippet\":\"XXX\",\"patternsFile\":\"negation_triggers_20140211.txt\",\"extractionType\":\"ocrText\",\"face2face\":\"true\",\"f2fConfidence\":\"0.9715961493210464\",\"predictionConfidence\":\"1\",\"hasMention\":\"YES\",\"mentions\":\"XXX\",\"junkiness\":\"0.9507042253521126\",\"appliedDateExtraction\":\"false\"}},\"attributes\":{\"editType\":\"ACTIVE\",\"editTimestamp\":\"2015-02-24T11:52:54.597Z\",\"bucketType\":\"dictionary.v0.1\",\"sourceType\":\"NARRATIVE\",\"encHash\":\"ezh+r1y0xoz4+HDoBCiFCg==\",\"bucketName\":\"dictionary.v0.1.ex6d_big_idf_BRDREC20141028_RETRAIN_2014_10\",\"totalPages\":\"18\",\"$workId\":\"31655\",\"$jobId\":\"31710\",\"$orgId\":\"407\",\"$batchId\":\"407_eventSubmission_031115224036\",\"$propertyVersion\":\"1415655641557\",\"$documentId\":\"raps67030642_f845005d-e\",\"$documentUUID\":\"21011c21-4583-475f-903f-13d2f0eb7656\",\"$patientUUID\":\"02b8a15f-37e1-4ff6-93c7-e09840edbc52\"}}";

        String event2 =
                "{\"subject\":{\"uri\":\"02b8a15f-37e1-4ff6-93c7-e09840edbc52\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"V12_108\",\"codeSystem\":\"APXCAT\",\"codeSystemVersion\":\"2.0\",\"displayName\":\"XXX\"},\"time\":{\"startTime\":\"2011-08-10T00:00:00+0000\",\"endTime\":\"2011-08-10T00:00:00+0000\"}},\"source\":{\"uri\":\"21011c21-4583-475f-903f-13d2f0eb7656\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"apxcat_dict_20140918b_BR.txt\",\"type\":\"dictionary\"},\"attributes\":{\"title\":\"XXX\",\"pageNumber\":\"3\",\"associatedEncounter\":\"Apixio:UNKNONW_ENCOUNTER\",\"snippet\":\"XXX\",\"patternsFile\":\"negation_triggers_20140211.txt\",\"extractionType\":\"ocrText\",\"face2face\":\"false\",\"f2fConfidence\":\"0.9999825806494451\",\"predictionConfidence\":\"1\",\"hasMention\":\"YES\",\"mentions\":\"XXX\",\"junkiness\":\"0.9753086419753086\",\"appliedDateExtraction\":\"false\"}},\"attributes\":{\"editType\":\"ACTIVE\",\"editTimestamp\":\"2015-02-24T11:52:54.597Z\",\"bucketType\":\"dictionary.v0.1\",\"sourceType\":\"NARRATIVE\",\"encHash\":\"ezh+r1y0xoz4+HDoBCiFCg==\",\"bucketName\":\"dictionary.v0.1.ex6d_big_idf_BRDREC20141028_RETRAIN_2014_10\",\"totalPages\":\"18\",\"$workId\":\"31655\",\"$jobId\":\"31710\",\"$orgId\":\"407\",\"$batchId\":\"407_eventSubmission_031115224036\",\"$propertyVersion\":\"1415655641557\",\"$documentId\":\"raps67030642_f845005d-e\",\"$documentUUID\":\"21011c21-4583-475f-903f-13d2f0eb7656\",\"$patientUUID\":\"02b8a15f-37e1-4ff6-93c7-e09840edbc52\"}}";



        try {
            String eventAddress1 = EventAddress.getEventAddress(ejp.parseEventTypeData(event1));
            String eventAddress2 = EventAddress.getEventAddress(ejp.parseEventTypeData(event2));
            assertNotSame(eventAddress1,eventAddress2);
        } catch(Exception e) {
            fail("Failed with " + e.getMessage());
        }


    }

    @Test
    public final void testSimilarEventsShouldHaveDifferentEventAddress() {
        String event1 = "{\"subject\":{\"uri\":\"01d624b5-1f70-46d7-968f-570a022697c0\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"V12_96\",\"codeSystem\":\"APXCAT\",\"codeSystemVersion\":\"2.0\",\"displayName\":\"stroke\"},\"time\":{\"startTime\":\"2011-05-03T00:00:00+0000\",\"endTime\":\"2011-05-03T00:00:00+0000\"}},\"source\":{\"uri\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"apxcat_dict_20140918b_BR.txt\",\"type\":\"dictionary\"},\"attributes\":{\"title\":\"XXX\",\"pageNumber\":\"4\",\"associatedEncounter\":\"Apixio:UNKNONW_ENCOUNTER\",\"snippet\":\"stroke|stroke|stroke|stroke\",\"patternsFile\":\"negation_triggers_20140211.txt\",\"extractionType\":\"ocrText\",\"face2face\":\"false\",\"f2fConfidence\":\"0.9998950190782012\",\"predictionConfidence\":\"1\",\"hasMention\":\"YES\",\"mentions\":\"stroke|stroke|stroke|stroke\",\"junkiness\":\"0.8972972972972973\",\"appliedDateExtraction\":\"false\"}},\"attributes\":{\"editType\":\"ACTIVE\",\"editTimestamp\":\"2015-02-24T12:01:06.584Z\",\"bucketType\":\"dictionary.v0.1\",\"sourceType\":\"NARRATIVE\",\"encHash\":\"ezh+r1y0xoz4+HDoBCiFCg==\",\"bucketName\":\"dictionary.v0.1.ex6d_big_idf_BRDREC20141028_RETRAIN_2014_10\",\"totalPages\":\"4\",\"$workId\":\"31983\",\"$jobId\":\"32020\",\"$orgId\":\"407\",\"$batchId\":\"407_eventSubmission_031215004951\",\"$propertyVersion\":\"1415655641557\",\"$documentId\":\"raps64934772_40ca37d9-a\",\"$documentUUID\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"$patientUUID\":\"01d624b5-1f70-46d7-968f-570a022697c0\"}}";
        String event2 = "{\"subject\":{\"uri\":\"01d624b5-1f70-46d7-968f-570a022697c0\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"V12_96\",\"codeSystem\":\"APXCAT\",\"codeSystemVersion\":\"2.0\",\"displayName\":\"stroke\"},\"time\":{\"startTime\":\"2011-05-03T00:00:00+0000\",\"endTime\":\"2011-05-03T00:00:00+0000\"}},\"source\":{\"uri\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"apxcat_dict_20140918b_BR.txt\",\"type\":\"dictionary\"},\"attributes\":{\"title\":\"XXX\",\"pageNumber\":\"2\",\"associatedEncounter\":\"Apixio:UNKNONW_ENCOUNTER\",\"snippet\":\"stroke|stroke|stroke|stroke|stroke\",\"patternsFile\":\"negation_triggers_20140211.txt\",\"extractionType\":\"ocrText\",\"face2face\":\"false\",\"f2fConfidence\":\"0.9998805172088041\",\"predictionConfidence\":\"1\",\"hasMention\":\"YES\",\"mentions\":\"stroke|stroke|stroke|stroke|stroke\",\"junkiness\":\"0.9129554655870445\",\"appliedDateExtraction\":\"false\"}},\"attributes\":{\"editType\":\"ACTIVE\",\"editTimestamp\":\"2015-02-24T12:01:06.584Z\",\"bucketType\":\"dictionary.v0.1\",\"sourceType\":\"NARRATIVE\",\"encHash\":\"ezh+r1y0xoz4+HDoBCiFCg==\",\"bucketName\":\"dictionary.v0.1.ex6d_big_idf_BRDREC20141028_RETRAIN_2014_10\",\"totalPages\":\"4\",\"$workId\":\"31983\",\"$jobId\":\"32020\",\"$orgId\":\"407\",\"$batchId\":\"407_eventSubmission_031215004951\",\"$propertyVersion\":\"1415655641557\",\"$documentId\":\"raps64934772_40ca37d9-a\",\"$documentUUID\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"$patientUUID\":\"01d624b5-1f70-46d7-968f-570a022697c0\"}}";

        try {
            String eventAddress1 = EventAddress.getEventAddress(ejp.parseEventTypeData(event1));
            String eventAddress2 = EventAddress.getEventAddress(ejp.parseEventTypeData(event2));
            assertFalse(eventAddress1.equals(eventAddress2));
        } catch(Exception e) {
            fail("Failed with " + e.getMessage());
        }
    }

    @Test
    public final void testSimilarEventsShouldHaveDifferentEventAddress2() {
        String event1 = "{\"subject\":{\"uri\":\"01d624b5-1f70-46d7-968f-570a022697c0\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"V12_96\",\"codeSystem\":\"APXCAT\",\"codeSystemVersion\":\"2.0\",\"displayName\":\"stroke\"},\"time\":{\"startTime\":\"2011-05-03T00:00:00+0000\",\"endTime\":\"2011-05-03T00:00:00+0000\"}},\"source\":{\"uri\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"apxcat_dict_20140918b_BR.txt\",\"type\":\"dictionary\"},\"attributes\":{\"title\":\"XXX\",\"pageNumber\":\"4\",\"associatedEncounter\":\"Apixio:UNKNONW_ENCOUNTER\",\"snippet\":\"stroke|stroke|stroke|stroke\",\"patternsFile\":\"negation_triggers_20140211.txt\",\"extractionType\":\"ocrText\",\"face2face\":\"false\",\"f2fConfidence\":\"0.9998950190782012\",\"predictionConfidence\":\"1\",\"hasMention\":\"YES\",\"mentions\":\"stroke|stroke|stroke|stroke\",\"junkiness\":\"0.8972972972972973\",\"appliedDateExtraction\":\"false\"}},\"attributes\":{\"editType\":\"ACTIVE\",\"editTimestamp\":\"2015-02-24T12:01:06.584Z\",\"bucketType\":\"dictionary.v0.1\",\"sourceType\":\"NARRATIVE\",\"encHash\":\"ezh+r1y0xoz4+HDoBCiFCg==\",\"bucketName\":\"dictionary.v0.1.ex6d_big_idf_BRDREC20141028_RETRAIN_2014_10\",\"totalPages\":\"4\",\"$workId\":\"31983\",\"$jobId\":\"32020\",\"$orgId\":\"407\",\"$batchId\":\"407_eventSubmission_031215004951\",\"$propertyVersion\":\"1415655641557\",\"$documentId\":\"raps64934772_40ca37d9-a\",\"$documentUUID\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"$patientUUID\":\"01d624b5-1f70-46d7-968f-570a022697c0\"}}";
        String event2 = "{\"subject\":{\"uri\":\"01d624b5-1f70-46d7-968f-570a022697c0\",\"type\":\"patient\"},\"fact\":{\"code\":{\"code\":\"V12_96\",\"codeSystem\":\"APXCAT\",\"codeSystemVersion\":\"2.0\",\"displayName\":\"stroke\"},\"time\":{\"startTime\":\"2011-05-03T00:00:00+0000\",\"endTime\":\"2011-05-03T00:00:00+0000\"}},\"source\":{\"uri\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"type\":\"document\"},\"evidence\":{\"inferred\":true,\"source\":{\"uri\":\"apxcat_dict_20140918b_BR.txt\",\"type\":\"dictionary\"},\"attributes\":{\"title\":\"XXX\",\"pageNumber\":\"2\",\"associatedEncounter\":\"Apixio:UNKNONW_ENCOUNTER\",\"snippet\":\"stroke|stroke|stroke|stroke|stroke\",\"patternsFile\":\"negation_triggers_20140211.txt\",\"extractionType\":\"ocrText\",\"face2face\":\"false\",\"f2fConfidence\":\"0.9998805172088041\",\"predictionConfidence\":\"1\",\"hasMention\":\"YES\",\"mentions\":\"stroke|stroke|stroke|stroke|stroke\",\"junkiness\":\"0.9129554655870445\",\"appliedDateExtraction\":\"false\"}},\"attributes\":{\"editType\":\"ACTIVE\",\"editTimestamp\":\"2015-02-24T12:01:06.584Z\",\"bucketType\":\"dictionary.v0.1\",\"sourceType\":\"NARRATIVE\",\"encHash\":\"ezh+r1y0xoz4+HDoBCiFCg==\",\"bucketName\":\"dictionary.v0.1.ex6d_big_idf_BRDREC20141028_RETRAIN_2014_10\",\"totalPages\":\"4\",\"$workId\":\"31983\",\"$jobId\":\"32020\",\"$orgId\":\"407\",\"$batchId\":\"407_eventSubmission_031215004951\",\"$propertyVersion\":\"1415655641557\",\"$documentId\":\"raps64934772_40ca37d9-a\",\"$documentUUID\":\"6caf5996-b30e-4a2f-84ef-d41e9009ac69\",\"$patientUUID\":\"01d624b5-1f70-46d7-968f-570a022697c0\"}}";

        try {
            String eventAddress1 = EventAddress.getEventAddress(ejp.parseEventTypeData(event1), EventAddress.ADDRESSING_VERSION_2_0);
            String eventAddress2 = EventAddress.getEventAddress(ejp.parseEventTypeData(event2), EventAddress.ADDRESSING_VERSION_2_0);
            assertFalse(eventAddress1.equals(eventAddress2));
        } catch(Exception e) {
            fail("Failed with " + e.getMessage());
        }
    }
}
