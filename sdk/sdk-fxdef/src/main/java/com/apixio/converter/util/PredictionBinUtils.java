package com.apixio.converter.util;

import com.apixio.model.event.AttributeType;
import com.apixio.model.event.EventType;
import com.apixio.model.event.ReferenceType;
import com.apixio.util.capv.EventTypeConverter;
import com.apixio.predictions.predictionbin.PredictionContainer;
import com.apixio.predictions.predictionbin.PredictionContainer.Attribute;
import com.apixio.predictions.predictionbin.PredictionContainer.SupportingData;
import com.apixio.predictions.predictionbin.PredictionContainer.Attribute.Builder;
import com.apixio.predictions.predictionbin.PredictionContainer.AttributeTypes;
import com.apixio.predictions.predictionbin.PredictionContainer.Prediction;
import com.apixio.predictions.predictionbin.PredictionContainer.PredictionBin;
import com.apixio.predictions.predictionbin.PredictionContainer.Provenance;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class PredictionBinUtils {

    /**
     * Takes an EventType object and wraps it in a PredictionBin object.
     * @param eventType
     * @param request
     * @param generatorInfo
     * @return
     */
    public static PredictionContainer.PredictionBin convertEventTypeToPredictionBin(EventType eventType,
                                                                                    String algorithmID, String docsetID,
                                                                                    String generatorID, String mcID,
                                                                                    String modelVersion
        ) {
        return PredictionBin.newBuilder()
                .setPrediction(Prediction.newBuilder()
                        .setPrediction14(EventTypeConverter.convert(eventType))
                        .build())
                .setProvenance(Provenance.newBuilder()
                        .setName(eventType.getEvidence() == null ? "" : eventType.getEvidence().getSource() == null ? "" :
                                eventType.getEvidence().getSource().getType() == null ? "" : eventType.getEvidence().getSource().getType())
                        .setClassName(generatorID)
                        .setJarVersion(mcID)
                        .setModelVersion(modelVersion)
                        .build())
                .addSupportingData(attributesAsSupportingData(eventType))
                .addAuditInformation(Attribute.newBuilder()
                        .setName("TIMESTAMP")
                        .setType(AttributeTypes.DATE)
                        .setValue(String.valueOf(System.currentTimeMillis()))
                        .build())
                .addAuditInformation(Attribute.newBuilder()
                        .setName("ALGORITHM_ID")
                        .setType(PredictionContainer.AttributeTypes.STRING)
                        .setValue(algorithmID)
                        .build())
                .addAuditInformation(Attribute.newBuilder()
                        .setName("DOCSET_ID")
                        .setType(PredictionContainer.AttributeTypes.STRING)
                        .setValue(docsetID)
                        .build())
                .build();
    }

    public static SupportingData attributesAsSupportingData(EventType eventType)
    {
        SupportingData.Builder sdb = SupportingData.newBuilder();
        for(AttributeType at : eventType.getEvidence().getAttributes().getAttribute())
        {
            Builder ab = Attribute.newBuilder();
            ab.setName(at.getName());
            ab.setType(AttributeTypes.STRING);
            ab.setValue(at.getValue());
            sdb.addAttributes(ab.build());
        }
        ReferenceType evSource = eventType.getEvidence().getSource();
        if (evSource != null) {
            Builder ab = Attribute.newBuilder();
            ab.setName("evidenceSourceType");
            ab.setType(AttributeTypes.STRING);
            ab.setValue(evSource.getType());
            sdb.addAttributes(ab.build());
            Builder abu = Attribute.newBuilder();
            abu.setName("evidenceSourceUri");
            abu.setType(AttributeTypes.STRING);
            abu.setValue(evSource.getUri());
            sdb.addAttributes(abu.build());
        }
        return sdb.build();
    }

    public static void logDatesOfService(Logger logger, List<PredictionBin> output, String context) {
        Set<String> datesOfService = output.stream()
            .map(PredictionBinUtils::getPredictionDate)
            .filter(date -> date != null)
            .collect(Collectors.toSet());
        logger.info("==== Unique Dates of service for " + context);
        for (String dos : datesOfService.stream().sorted().collect(Collectors.toList())) {
            logger.info(dos);
        }
        logger.info("==== End of unique Dates of service for " + context);
    }

    @Nullable
    private static String getPredictionDate(PredictionBin bin) {
        try {
            Timestamp timestamp = bin
                .getPrediction().getPrediction11().getDateOfService().getStartDate();
            Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
            return instant.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
