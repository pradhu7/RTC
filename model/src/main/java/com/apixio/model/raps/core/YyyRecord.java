package com.apixio.model.raps.core;

import static com.apixio.model.raps.RapsUtils.*;

public class YyyRecord extends BatchRecordBase {
    private static final int FILLER_MAX_LENGTH = 490;

    private final int cccRecordsTotal;

    public YyyRecord(int sequenceNumber, String planNumber, int cccRecordsTotal, String filler) {
        super(sequenceNumber, planNumber, filler);
        this.cccRecordsTotal = checkSequenceNumber(cccRecordsTotal);
    }

    public int getCccRecordsTotal() {
        return cccRecordsTotal;
    }

    @Override
    public int getFillerMaxLength() {
        return FILLER_MAX_LENGTH;
    }

    @Override
    public String toString() {
        StringBuilder record = new StringBuilder(RecordId.YYY.toString())
            .append(formatSequenceNumber(sequenceNumber))
            .append(planNumber)
            .append(formatSequenceNumber(cccRecordsTotal))
            .append(filler);
        return toRapsRecordLength(record);
    }
}
