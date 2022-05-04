package com.apixio.util.ma;

import com.apixio.model.event.AttributeType;
import com.apixio.model.event.EventType;
import com.apixio.predictions.predictionbin.PredictionCommon;
import com.apixio.predictions.ma.MedicareAdvantage;
import com.google.protobuf.util.Timestamps;

import com.apixio.util.CodePredictionUtil;

import java.util.Map;

public class EventTypeConverter
{
    static public MedicareAdvantage.MedicareAdvantagePrediction convert(final EventType e) {
        try
        {
            final Map<String, AttributeType> attributeTypeMap = CodePredictionUtil.getAttributeMap(e);
            final MedicareAdvantage.MedicareAdvantagePrediction medicareAdvantagePrediction = MedicareAdvantage.MedicareAdvantagePrediction.newBuilder()
                    .setSource(PredictionCommon.Source.newBuilder()
                            .setPatientId(PredictionCommon.PatientXUUID.newBuilder()
                                    .setXuuid(CodePredictionUtil.extractPatientId(e).toString()).build())
                            .setDocumentId(PredictionCommon.DocumentXUUID.newBuilder()
                                    .setXuuid(CodePredictionUtil.extractDocumentId(e).toString()).build())
                            .setPage(CodePredictionUtil.extractPages(attributeTypeMap).iterator().next())
                            .setPredictionConfidence(CodePredictionUtil.extractScore(attributeTypeMap))
                            .build())
                    .setCode(CodePredictionUtil.buildCode(CodePredictionUtil.extractCode(e)))
                    .setDateOfService(PredictionCommon.DateOfService.newBuilder()
                            .setStartDate(Timestamps.fromMillis(e.getFact().getTime().getStartTime().getTime()))
                            .setEndDate(Timestamps.fromMillis(e.getFact().getTime().getStartTime().getTime()))
                            .build())
                    .setIsInferred(CodePredictionUtil.isInferredEvent(e))
                    .build();

            return medicareAdvantagePrediction;
        } catch (Exception ex) {
            throw new RuntimeException("Error: Could not convert EventType to Data Platform Prediction", ex);
        }
    }
}