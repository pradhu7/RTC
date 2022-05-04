package com.apixio.model.raps.core;

import static com.apixio.model.raps.RapsConstants.RECORD_LENGTH;

public abstract class Record {

    /**
     * Trimmed filler
     */
    protected final String filler;

    protected Record(String filler) {
        int fillerMaxLength = getFillerMaxLength();
        if (filler.length() > fillerMaxLength) {
            throw new IllegalArgumentException("filler must be at most " + fillerMaxLength + " characters long");
        }
        this.filler = filler.trim();
    }

    public boolean isFillerBlank() {
        return filler.isEmpty();
    }

    public String getFiller() {
        return filler;
    }

    protected static String formatSequenceNumber(int i) {
        return String.format("%07d", i);
    }

    protected abstract int getFillerMaxLength();

}
