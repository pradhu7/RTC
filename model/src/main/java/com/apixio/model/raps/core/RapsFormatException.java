package com.apixio.model.raps.core;

public class RapsFormatException extends Exception {
    private final long lineNo;
    private final int colNo;
    private final String what;
    private final String expected;

    public RapsFormatException(long lineNo, int colNo, String what, int expected) {
        this(lineNo, colNo, what, String.valueOf(expected));
    }

    public RapsFormatException(long lineNo, String what, int expected) {
        this(lineNo, 0, what, String.valueOf(expected));
    }

    public RapsFormatException(long lineNo, String what, String expected) {
        this(lineNo, 0, what, expected);
    }

    public RapsFormatException(long lineNo, int colNo, String what, String expected) {
        this.lineNo = lineNo;
        this.colNo = colNo;
        this.what = what;
        this.expected = expected;
    }

    @Override
    public String getMessage() {
        return String.format("Invalid %s at line %d col %d. Must be %s", what, lineNo, colNo, expected);
    }
}
