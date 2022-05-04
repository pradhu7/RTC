package com.apixio.model.raps;

import java.io.BufferedReader;
import java.io.StringReader;

import static com.apixio.model.raps.RapsConstants.*;

/**
 * TODO: unchecked exceptions here should be converted to checked exceptions
 */
public abstract class RapsUtils {

    public static Integer checkCode(Integer code) {
        if (code != null && (code < 100 || code > 999)) {
            throw new IllegalArgumentException("code must be between 100 and 999");
        }
        return code;
    }

    public static int checkSequenceNumber(int sequenceNumber) {
        if (sequenceNumber < 1) {
            throw new IllegalArgumentException("sequence number must be positive");
        }
        return sequenceNumber;
    }

    public static String pad(String value, int length) {
        if (value == null) {
            value = "";
        }
        if (value.length() > length) {
            throw new IllegalArgumentException("can not squeeze " + value.length() + " chars string into " + length + " chars");
        }
        return String.format("%-" + length + "s", value);
    }

    public static String toRapsRecordLength(StringBuilder builder) {
        return toRapsRecordLength(builder.toString());
    }

    public static String toRapsRecordLength(String str) {
        return pad(str, RECORD_LENGTH);
    }

    public static String toDiagnosisClustersLength(String str) {
        return pad(str, MAX_DIAGNOSIS_CLUSTERS * DIAGNOSIS_CLUSTER_LENGTH);
    }

    public static BufferedReader toReader(String str) {
        return new BufferedReader(new StringReader(str));
    }

    public static String formatErrorCode(Integer errorCode) {
        return errorCode == null ? "   " : errorCode.toString();
    }

}
