package com.apixio.model.cdi;

public enum BatchPriorityType {
    HIGHEST (1),
    HIGH (2),
    NORMAL (3),
    LOW (4);


    private final int code;


    BatchPriorityType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static BatchPriorityType getBatchPriorityTypeFromCode(int code) {
        switch (code)
        {
            case 1:
                return HIGHEST;
            case 2:
                return HIGH;
            case 3:
                return NORMAL;
            case 4:
                return LOW;
            default:
                return NORMAL;
        }
    }


}