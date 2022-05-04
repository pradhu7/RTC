package com.apixio.dao.seqstore.utility;

/**
 * Created by dyee on 12/2/16.
 */
public class Range {

    static public final Range DEFAULT_RANGE = new Range(0, Long.MAX_VALUE, true, true);

    private long start;
    private long end;
    private boolean includeLower;
    private boolean includeUpper;

    private Range(long start, long end, boolean includeLower, boolean includeUpper) {
        this.start = start;
        this.end = end;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public boolean isIncludeLower() {
        return includeLower;
    }

    public boolean isIncludeUpper() {
        return includeUpper;
    }

    public Range clone() {
        return new Range(start, end, includeLower, includeUpper);
    }

    public static class RangeBuilder {
        //Default to epoch
        private long start = 0;

        //Defaults to the max date
        private long end = Long.MAX_VALUE;

        private boolean includeLower = true;
        private boolean includeUpper = true;

        public Range build() {
            return new Range(start, end, includeLower, includeUpper);
        }

        public RangeBuilder setStart(long start) {
            this.start = start;
            return this;
        }

        public RangeBuilder setEnd(long end) {
            this.end = end;
            return this;
        }

        public RangeBuilder setIncludeLower(boolean includeLower) {
            this.includeLower = includeLower;
            return this;
        }

        public RangeBuilder setIncludeUpper(boolean includeUpper) {
            this.includeUpper = includeUpper;
            return this;
        }
    }

    @Override
    public String toString() {
        return "Range{" +
                "start=" + start +
                ", end=" + end +
                ", includeLower=" + includeLower +
                ", includeUpper=" + includeUpper +
                '}';
    }
}