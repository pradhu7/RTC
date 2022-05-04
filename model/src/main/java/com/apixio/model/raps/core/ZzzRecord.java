package com.apixio.model.raps.core;

import static com.apixio.model.raps.RapsUtils.*;

public class ZzzRecord extends HeaderTrailerBase {
    private static int FILLER_MAX_LENGTH = 486;

    private final int bbbRecordsTotal;

    public ZzzRecord(String submitterId, String fileId,
            int bbbRecordsTotal, String filler) {
        super(submitterId, fileId, filler);

        this.bbbRecordsTotal = checkSequenceNumber(bbbRecordsTotal);
    }

    public int getBbbRecordsTotal() {
        return bbbRecordsTotal;
    }

    @Override
    public int getFillerMaxLength() {
        return FILLER_MAX_LENGTH;
    }

    @Override
    public String toString() {
        StringBuilder record = new StringBuilder(RecordId.ZZZ.toString())
            .append(submitterId)
            .append(fileId)
            .append(formatSequenceNumber(bbbRecordsTotal))
            .append(filler);
        return toRapsRecordLength(record);
    }
}
