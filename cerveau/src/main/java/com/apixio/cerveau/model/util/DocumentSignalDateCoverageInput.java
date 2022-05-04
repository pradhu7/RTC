package com.apixio.cerveau.model.util;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.Signal;

import java.util.List;
import java.util.Map;

public class DocumentSignalDateCoverageInput {
    public final static String AssumedDateSignal = "AssumedDate";
    public final static String InferredDateSignal = "InferredDate";
    public final static String DateIsAssumedSignal = "DateIsAssumed";

    private String docset;
    private String patientId;
    private List<XUUID> documentList;

    private Map<XUUID, List<Iterable<Signal>>> dateIsAssumedSignals;
    private Map<XUUID, List<Iterable<Signal>>> assumedDateSignals;
    private Map<XUUID, List<Iterable<Signal>>> inferredDateSignals;

    public DocumentSignalDateCoverageInput(String docset, String patientId, List<XUUID> documentList, Map<XUUID, List<Iterable<Signal>>> dateIsAssumedSignals, Map<XUUID, List<Iterable<Signal>>> assumedDateSignals, Map<XUUID, List<Iterable<Signal>>> inferredDateSignals) {
        this.docset = docset;
        this.patientId = patientId;
        this.documentList = documentList;
        this.dateIsAssumedSignals = dateIsAssumedSignals;
        this.assumedDateSignals = assumedDateSignals;
        this.inferredDateSignals = inferredDateSignals;
    }

    public String getDocset() {
        return docset;
    }

    public String getPatientId() {
        return patientId;
    }

    public List<XUUID> getDocumentList() {
        return documentList;
    }

    public Map<XUUID, List<Iterable<Signal>>> getDateIsAssumedSignals() {
        return dateIsAssumedSignals;
    }

    public Map<XUUID, List<Iterable<Signal>>> getAssumedDateSignals() {
        return assumedDateSignals;
    }

    public Map<XUUID, List<Iterable<Signal>>> getInferredDateSignals() {
        return inferredDateSignals;
    }
}
