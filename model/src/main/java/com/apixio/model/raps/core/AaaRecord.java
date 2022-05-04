package com.apixio.model.raps.core;

import com.apixio.model.raps.RapsConstants;

import org.joda.time.LocalDate;

import static com.apixio.model.raps.RapsUtils.*;

public class AaaRecord extends HeaderTrailerBase {
    private static final int FILLER_MAX_LENGTH = 477;

    private final LocalDate transactionDate;
    private final ProdTestIndicator prodTestIndicator;
    private final DiagnosisType diagnosisType;

    public AaaRecord(String submitterId, String fileId, LocalDate transactionDate,
            ProdTestIndicator prodTestIndicator, DiagnosisType diagnosisType, String filler) {
        super(submitterId, fileId, filler);
        this.transactionDate = transactionDate;
        this.prodTestIndicator = prodTestIndicator;
        this.diagnosisType = diagnosisType;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public ProdTestIndicator getProdTestIndicator() {
        return prodTestIndicator;
    }

    public DiagnosisType getDiagnosisType() {
        return diagnosisType;
    }

    @Override
    public int getFillerMaxLength() {
        return FILLER_MAX_LENGTH;
    }

    @Override
    public String toString() {
        StringBuilder record = new StringBuilder(RecordId.AAA.toString())
            .append(submitterId)
            .append(fileId)
            .append(transactionDate.toString(RapsConstants.DATE_FORMAT))
            .append(prodTestIndicator.toString())
            .append(diagnosisType == DiagnosisType.ICD10 ? "ICD10" : "ICD9 ")
            .append(filler);
        return toRapsRecordLength(record);
    }
}
