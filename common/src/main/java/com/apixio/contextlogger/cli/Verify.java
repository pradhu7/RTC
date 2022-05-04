package com.apixio.contextlogger.cli;

import com.apixio.contextlogger.*;

import static com.apixio.contextlogger.CtxLogLevel.LOG_DEBUG;

/**
 * Simple command-line validator program.
 * It sends a few messages to the local logging infrastructure.
 *
 * It also provides sample code for new developers starting using the new
 * logging library.
 *
 * Can use this class to verify that the fluentd connection is working in a local dev setup.
 * Not for use in production or staging.
 *
 */
public class Verify {
    ICtxLoggerAdapter logService;

    Verify() {
        this.logService = CtxLogAdapterFactory.buildStdAdapter(
                "localhost", "dev", "verification", LOG_DEBUG);
    }

    /**
     * Populates the log model object with sample data.
     * @param lg    The object that implements the IApxLogBase interface.
     */
    void addPayloadProperties(ICtxLogBase lg) {
        // Few different numeric types.
        short testShort = 2;
        float testFloat = 1.2444F;
        double testDouble = 2.12222;
        int testInt = 28;
        lg.setAppName("hcc");
        lg.pushAppMetric("session.number", 9999);
        lg.pushAppMetric("event_data.docID", "c965ba9e-bcbe-4a36-ba45-29f0b39251d2");
        lg.pushAppMetric("event_data.finding.documentUuid", "c965ba9e-bcbe-4a36-ba45-29f0b39251d2");
        lg.pushAppMetric("event_data.finding.patientUuid", "50c58bb5-a4ba-46f6-b492-07245059a79d");
        lg.pushAppMetric("event_data.finding.type.short", testShort);
        lg.pushAppMetric("event_data.finding.type.float", testFloat);
        lg.pushAppMetric("event_data.finding.type.double", testDouble);
        lg.pushAppMetric("event_data.finding.type.int", testInt);
        lg.pushAppMetric("event_data.message", "New Page Loaded");
        lg.pushAppMetric("event_name", "New Page Loaded");
        lg.pushAppMetric("event_name", "New Page Loaded");
        lg.pushUserInfo("coding_org_id", "UO_4bf27159-5348-4302-a186-71944c022450");
        lg.pushUserInfo("coding_org_name", "Clarus");
        lg.pushUserInfo("type.string", "A String");
        lg.pushUserInfo("type.short", testShort);
        lg.pushUserInfo("type.float", testFloat);
        lg.pushUserInfo("type.double", testDouble);
        lg.pushUserInfo("type.int", testInt);
    }

    /**
     * Simple use of the Log library directly with no errors/exceptions.
     */
    void generateEventSimpleLog() {
        ICtxLogEvent lg = CtxLogEvent.get(this.logService);
        lg.pushKeyValue("a_message_id", "Simple log");
        addPayloadProperties(lg);
        lg.info();
    }

    /**
     * Simple use of the Log library directly if an exception is raised.
     */
    void generateEventSimpleLogWithError() {
        ICtxLogEvent lg = CtxLogEvent.get(this.logService);
        try {
            lg.pushKeyValue("a_message_id", "Simple Log With Error");
            addPayloadProperties(lg);
            lg.info();
            throw new Exception("This is a test");
        } catch (Exception e) {
            lg.setException(e);
        }
    }

    /**
     * Using the Log library to represent 'Context' logging if an exception occurred.
     */
    void generateEventContextLog() {
        try ( CtxLogEventContext lg = CtxLogEventContext.get(this.logService, CtxLogLevel.LOG_INFO) ) {
            try {
                lg.pushKeyValue("a_message_id", "Context Log");
                addPayloadProperties(lg);
            } catch (Exception e) {
                lg.setException(e);
            }
        }
    }

    /**
     * Using the Log library to represent 'Context' logging if an exception occurred.
     */
    void generateEventContextLogWithError() {
        try ( CtxLogEventContext lg = CtxLogEventContext.get(this.logService, CtxLogLevel.LOG_INFO) ) {
            try {
                lg.pushKeyValue("a_message_id", "Context Log With Error");
                addPayloadProperties(lg);
                throw new Exception("This is a test");
            } catch (Exception e) {
                lg.setException(e);
            }
        }
    }

    /**
     * Verification entry point.
     * @param args  Command line arguments. Not used.
     */
    public static void main(String[] args) {
        Verify gen = new Verify();
        gen.generateEventSimpleLog();
        gen.generateEventSimpleLogWithError();
        gen.generateEventContextLog();
        gen.generateEventContextLogWithError();
    }
}
