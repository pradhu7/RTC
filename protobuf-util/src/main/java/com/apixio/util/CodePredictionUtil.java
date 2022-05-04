package com.apixio.util;

import com.apixio.XUUID;
import com.apixio.model.event.AttributeType;
import com.apixio.model.event.CodeType;
import com.apixio.model.event.EventType;
import com.apixio.predictions.predictionbin.PredictionCommon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CodePredictionUtil {
    public static PredictionCommon.Code buildCode(CodeType codeType) {
        PredictionCommon.Code.Builder builder = PredictionCommon.Code.newBuilder()
                .setCode(codeType.getCode())
                .setSystem(codeType.getCodeSystem());

        if(codeType.getDisplayName()!=null) builder.setDisplayName(codeType.getDisplayName());
        if(codeType.getCodeSystemName()!=null) builder.setSystemName(codeType.getCodeSystemName());
        if(codeType.getCodeSystemVersion()!=null) builder.setSystemVersion(codeType.getCodeSystemVersion());

        return builder.build();
    }

    public static CodeType buildEventCodeType(PredictionCommon.Code code) {
        CodeType codeType  = new CodeType();
        codeType.setCode(code.getCode());
        codeType.setCodeSystem(code.getSystem());
        if (code.getDisplayName() != null) codeType.setDisplayName(code.getDisplayName());
        if (code.getSystemName() != null) codeType.setCodeSystemName(code.getSystemName());
        if (code.getSystemVersion() != null) codeType.setCodeSystemVersion(code.getSystemVersion());

        return codeType;
    }

    static public XUUID extractPatientId(final EventType e)
    {
        XUUID xuuid = null;
        if(e.getSubject().getType().equals("patient"))
        {
            xuuid = XUUID.fromString("PAT_" + e.getSubject().getUri());
        }

        return xuuid;
    }

    public static boolean isInferredEvent(final EventType e)
    {
        return e.getEvidence().isInferred();
    }

    static public XUUID extractDocumentId(final EventType e)
    {
        XUUID xuuid = null;
        if (e.getSource().getType().equals("document"))
        {
            xuuid = XUUID.fromString("DOC_" + e.getSource().getUri());
        }

        return xuuid;
    }

    static public Set<Integer> extractPages(final Map<String, AttributeType> attributeTypeMap) {
        Set<Integer> pageNumberSet= new HashSet<>();

        if(attributeTypeMap.containsKey("pageNumber"))
        {
            AttributeType pageNumberAttribute = attributeTypeMap.get("pageNumber");
            String [] pageNumbers = pageNumberAttribute.getValue().split(",");
            for(String pageNumber : pageNumbers)
            {
                pageNumberSet.add(Integer.valueOf(pageNumber));
            }
        }

        return pageNumberSet;
    }

    static public Double extractScore(final Map<String, AttributeType> attributeTypeMap) {
        Double score = null;

        if(attributeTypeMap.containsKey("predictionConfidence"))
        {
            AttributeType scoreAttr = attributeTypeMap.get("predictionConfidence");
            score = Double.valueOf(scoreAttr.getValue());
        }

        if(attributeTypeMap.containsKey("score"))
        {
            AttributeType scoreAttr = attributeTypeMap.get("score");
            score = Double.valueOf(scoreAttr.getValue());
        }
        return score;
    }

    static public CodeType extractCode(final EventType event) {
        return event.getFact().getCode();
    }

    public static Map<String, AttributeType> getAttributeMap(final EventType event)
    {
        Map<String, AttributeType> attributeTypeMap = new HashMap<>();
        for(AttributeType at : event.getEvidence().getAttributes().getAttribute())
        {
            attributeTypeMap.put(at.getName(), at);
        }
        return attributeTypeMap;
    }    
}
