package com.apixio.model.raps.core;

import org.joda.time.LocalDate;

import static com.apixio.model.raps.RapsUtils.*;
import static com.apixio.model.raps.RapsConstants.*;

public class DiagnosisCluster {
    private static final int DIAGNOSIS_CODE_FIELD_LENGTH = 7;

    private final ProviderType providerType;
    private final LocalDate fromDate;
    private final LocalDate throughDate;
    private final boolean hasDeleteIndicator;
    private final String diagnosisCode;
    private final Integer error1;
    private final Integer error2;

    public DiagnosisCluster(ProviderType providerType, LocalDate fromDate,
            LocalDate throughDate, boolean hasDeleteIndicator, String diagnosisCode,
            Integer error1, Integer error2) {
        this.providerType = providerType;
        this.fromDate = fromDate;
        this.throughDate = throughDate;
        this.hasDeleteIndicator = hasDeleteIndicator;
        this.diagnosisCode = diagnosisCode;

        this.error1 = checkCode(error1);
        this.error2 = checkCode(error2);
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getThroughDate() {
        return throughDate;
    }

    public boolean hasDeleteIndicator() {
        return hasDeleteIndicator;
    }

    public String getDiagnosisCode() {
        return diagnosisCode;
    }

    public Integer getError1() {
        return error1;
    }

    public Integer getError2() {
        return error2;
    }

    public boolean hasValidFromDate() {
        boolean error1IsAboutFromDate = error1 != null && (error1 == SERVICE_FROM_DATE_NOT_WITHIN_MEDICARE_ENTITLEMENT_ERROR
                || error1 == SERVICE_FROM_DATE_NOT_WITHIN_MA_ORG_ENROLLMENT_ERROR);

        boolean error2IsAboutFromDate = error2 != null && (error2 == SERVICE_FROM_DATE_NOT_WITHIN_MEDICARE_ENTITLEMENT_ERROR
                || error2 == SERVICE_FROM_DATE_NOT_WITHIN_MA_ORG_ENROLLMENT_ERROR);

        return !error1IsAboutFromDate && !error2IsAboutFromDate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
            .append(providerType.getRapsCode())
            .append(fromDate.toString(DATE_FORMAT))
            .append(throughDate.toString(DATE_FORMAT))
            .append(hasDeleteIndicator ? 'D' : ' ')
            .append(formatDiagnosisCode(diagnosisCode))
            .append(formatErrorCode(error1))
            .append(formatErrorCode(error2));
        return builder.toString();
    }

    private String formatDiagnosisCode(String code) {
        String codeString = code == null ? "" : code;
        return String.format("%-" + DIAGNOSIS_CODE_FIELD_LENGTH + "s", codeString);
    }
}
