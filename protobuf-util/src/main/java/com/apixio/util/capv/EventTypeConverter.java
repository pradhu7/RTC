package com.apixio.util.capv;

import com.apixio.model.event.*;
import com.apixio.model.owl.interfaces.donotimplement.Event;
import com.apixio.predictions.predictionbin.PredictionCommon;
import com.apixio.predictions.capv.CAPV;
import com.google.protobuf.util.Timestamps;
import com.apixio.predictions.predictionbin.PredictionContainer;
import com.apixio.predictions.predictionbin.PredictionContainer.*;
import com.apixio.predictions.predictionbin.PredictionContainer.Attribute.Builder;

import java.util.ArrayList;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.apixio.util.CodePredictionUtil;

public class EventTypeConverter
{
    static String evidenceSourceTypeStr = "evidenceSourceType";
    static String evidenceSourceUriStr = "evidenceSourceUri";

    static public CAPV.CAPVPrediction convert(final EventType e) {
        try
        {
            final Map<String, AttributeType> attributeTypeMap = CodePredictionUtil.getAttributeMap(e);
            final CAPV.CAPVPrediction capvPrediction = CAPV.CAPVPrediction.newBuilder()
                    .setSource(PredictionCommon.Source.newBuilder()
                            .setPatientId(PredictionCommon.PatientXUUID.newBuilder()
                                    .setXuuid(CodePredictionUtil.extractPatientId(e).toString()).build())
                            .setPredictionConfidence(CodePredictionUtil.extractScore(attributeTypeMap))
                            .build())
                    .setCode(CodePredictionUtil.buildCode(CodePredictionUtil.extractCode(e)))
                    .setDateOfService(PredictionCommon.DateOfService.newBuilder()
                            .setStartDate(Timestamps.fromMillis(e.getFact().getTime().getStartTime().getTime()))
                            .setEndDate(Timestamps.fromMillis(e.getFact().getTime().getStartTime().getTime()))
                            .build())
                    .setIsInferred(CodePredictionUtil.isInferredEvent(e))
                    .build();

            return capvPrediction;
        } catch (Exception ex) {
            throw new RuntimeException("Error: Could not convert EventType to CAPV Prediction", ex);
        }
    }

    static public EventType convertCAPVPredictionToEventType(CAPV.CAPVPrediction prediction) {
        try {
            EventType event = new EventType();
            ReferenceType patientRT = new ReferenceType();
            patientRT.setType("patient");
            patientRT.setUri(prediction.getSource().getPatientId().getXuuid().replace("PAT_", ""));
            event.setSubject(patientRT);

            CodeType codeType = CodePredictionUtil.buildEventCodeType(prediction.getCode());
            FactType ft = new FactType();
            ft.setCode(codeType);
            TimeRangeType trt = new TimeRangeType();
            trt.setStartTime(Date.from(Instant.ofEpochSecond(prediction.getDateOfService().getStartDate().getSeconds())
                    .atZone(ZoneId.of( "UTC" )).toInstant()));
            trt.setEndTime(Date.from(Instant.ofEpochSecond(prediction.getDateOfService().getEndDate().getSeconds())
                    .atZone(ZoneId.of( "UTC" )).toInstant()));
            ft.setTime(trt);
            event.setFact(ft);
            return event;
        } catch (Exception ex) {
            throw new RuntimeException("Error: Could not convert CAPV Prediction to EventType", ex);
        }
    }

    static public EventType convertPredictionBinToEventType(PredictionContainer.PredictionBin predictionBin) {
        EventType event = convertCAPVPredictionToEventType(predictionBin.getPrediction().getPrediction14());
        EvidenceType evidence = supportingDataAsAttributes(predictionBin);
        event.setEvidence(evidence);
        return event;
    }

    public static EvidenceType supportingDataAsAttributes(PredictionContainer.PredictionBin prediction) {
        List<SupportingData> supportingData = prediction.getSupportingDataList();
        EvidenceType evidence = new EvidenceType();
        List<Attribute> attributesList = new ArrayList<Attribute>();
        for (SupportingData sd : supportingData) {
            attributesList.addAll(sd.getAttributesList());
        }
        AttributesType attributesType = new AttributesType();
        ReferenceType source = new ReferenceType();
        for (Attribute attribute : attributesList) {
            if (attribute.getName().equals(evidenceSourceTypeStr)) {
                source.setType(attribute.getValue());
            } else if (attribute.getName().equals(evidenceSourceUriStr)) {
                source.setUri(attribute.getValue());
            } else {
                AttributeType att = new AttributeType();
                att.setName(attribute.getName());
                att.setValue(attribute.getValue());
                attributesType.getAttribute().add(att);
            }
        }
        evidence.setSource(source);
        evidence.setAttributes(attributesType);
        evidence.setInferred(prediction.getPrediction().getPrediction14().getIsInferred());
        return evidence;
    }

}