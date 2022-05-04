package com.apixio.model.patientdataset;

public enum PredictorType {
    DEFAULT("DEFAULT"),
    MODEL_GAP("MODEL_GAP");

    private String code;

    PredictorType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PredictorType fromCode(String code) {
        switch (code)
        {
            case "DEFAULT":
                return PredictorType.DEFAULT;
            case "MODEL_GAP":
                return PredictorType.MODEL_GAP;
            default:
                return null;
        }
    }
}
