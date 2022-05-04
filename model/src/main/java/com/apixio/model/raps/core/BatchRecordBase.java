package com.apixio.model.raps.core;

import static com.apixio.model.raps.RapsUtils.*;

abstract class BatchRecordBase extends Record {

    protected final int sequenceNumber;
    protected final String planNumber;

    public BatchRecordBase(int sequenceNumber, String planNumber, String filler) {
        super(filler);
        this.sequenceNumber = checkSequenceNumber(sequenceNumber);

        if (planNumber.length() != 5) {
            throw new IllegalArgumentException("plan number must be 5 characters long");
        }
        this.planNumber = planNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getPlanNumber() {
        return planNumber;
    }

}
