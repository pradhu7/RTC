package com.apixio.contextlogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CtxLogModelTest {

    CtxLogModel model;

    @BeforeEach
    public void beforeEach() {
        model = CtxLogModel.build(true);
    }

    @Test
    public void testSmallMessage() {
        model.pushAppMetric("session.id", 101);
        String current = model.emit();
        String expected = "{\"app\":{\"app_data\":{\"session\":{\"id\":\"101\"}}}}";
        assertEquals(current, expected);
    }

    @Test
    public void testAnotherMessage() {
        String expected =
                "{\"app\":{\"app_data\":{\"event_data\":{\"docID\":\"c965ba9e-bcbe-4a36-ba45-29f0b39251d2\"" +
                        ",\"finding\":{\"documentUuid\":\"c965ba9e-bcbe-4a36-ba45-29f0b39251d2\""+
                        ",\"patientUuid\":\"50c58bb5-a4ba-46f6-b492-07245059a79d\",\"type\":"+
                        "{\"double\":\"2.12222\",\"float\":\"1.2444\",\"int\":\"28\",\"short\":\"2\"}},"+
                        "\"message\":\"New Page Loaded\"},"+
                        "\"event_name\":\"New Page Loaded\",\"session\":{\"number\":\"3104466\"}}," +
                        "\"app_name\":\"hcc\",\"app_user_info\":{\"coding_org_id\":" +
                        "\"UO_4bf27159-5348-4302-a186-71944c022450\",\"coding_org_name\":\"Clarus\"," +
                        "\"type\":{\"double\":\"2.12222\",\"float\":\"1.2444\",\"int\":\"28\",\"short\":\"2\""+
                        ",\"string\":\"A String\"}}}}";

        short testShort = 2;
        float testFloat = 1.2444F;
        double testDouble = 2.12222;
        int testInt = 28;
        model.setAppName("hcc");
        model.pushAppMetric("session.number", "3104466");
        model.pushAppMetric("event_data.docID", "c965ba9e-bcbe-4a36-ba45-29f0b39251d2");
        model.pushAppMetric("event_data.finding.documentUuid", "c965ba9e-bcbe-4a36-ba45-29f0b39251d2");
        model.pushAppMetric("event_data.finding.patientUuid", "50c58bb5-a4ba-46f6-b492-07245059a79d");
        model.pushAppMetric("event_data.finding.type.short", testShort);
        model.pushAppMetric("event_data.finding.type.float", testFloat);
        model.pushAppMetric("event_data.finding.type.double", testDouble);
        model.pushAppMetric("event_data.finding.type.int", testInt);
        model.pushAppMetric("event_data.message", "New Page Loaded");
        model.pushAppMetric("event_name", "New Page Loaded");
        model.pushAppMetric("event_name", "New Page Loaded");
        model.pushUserInfo("coding_org_id", "UO_4bf27159-5348-4302-a186-71944c022450");
        model.pushUserInfo("coding_org_name", "Clarus");
        model.pushUserInfo("type.string", "A String");
        model.pushUserInfo("type.short", testShort);
        model.pushUserInfo("type.float", testFloat);
        model.pushUserInfo("type.double", testDouble);
        model.pushUserInfo("type.int", testInt);
        String current = model.emit();
        assertEquals(current, expected);
    }
}
