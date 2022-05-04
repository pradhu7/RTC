package com.apixio.model.raps.core;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Collections;

import static com.apixio.model.raps.RapsConstants.*;
import static com.apixio.model.raps.RapsUtils.*;

public class CccRecord extends Record {
    private static final int FILLER_MAX_LENGTH = 35;
    private static final int PATIENT_CONTROL_NUMBER_FIELD_LENGTH = 40;
    private static final int HIC_NUMBER_FIELD_LENGTH = 25;

    private final int sequenceNumber;
    private final String patientControlNumber; // dynamic length, optional
    private final String hicNumber; // dynamic length
    private final LocalDate patientDateOfBirth; // optional
    private final List<DiagnosisCluster> diagnosisClusters; // 10 occurrences max
    private final String correctedHicNumber; // optional, RAPS RETURN
    private final List<RiskAssessmentCodeCluster> riskAssessmentCodeClusters; // 10 occurrences max

    // RAPS RETURN ERROR CODES
    private final Integer sequenceNumberErrorCode;
    private final Integer hicErrorCode;
    private final Integer dateOfBirthErrorCode;

    public CccRecord(int sequenceNumber, Integer sequenceNumberErrorCode, String patientControlNumber,
            String hicNumber, Integer hicErrorCode, LocalDate patientDateOfBirth, Integer dateOfBirthErrorCode,
            List<DiagnosisCluster> diagnosisClusters, String correctedHicNumber,
            List<RiskAssessmentCodeCluster> riskAssessmentCodeClusters, String filler) {
        super(filler);

        this.sequenceNumber = sequenceNumber;
        this.patientControlNumber = patientControlNumber;
        this.hicNumber = hicNumber;
        this.patientDateOfBirth = patientDateOfBirth;

        if (diagnosisClusters.isEmpty()) {
            throw new IllegalArgumentException("No diagnosis clusters");
        } else if (diagnosisClusters.size() > 10) {
            throw new IllegalArgumentException("More than 10 diagnosis clusters");
        }
        this.diagnosisClusters = diagnosisClusters;

        this.correctedHicNumber = correctedHicNumber;

        if (riskAssessmentCodeClusters.size() > 10) {
            throw new IllegalArgumentException("More than 10 risk assessment code clusters");
        }
        this.riskAssessmentCodeClusters = riskAssessmentCodeClusters;

        // RAPS RETURN ERROR CODES
        this.sequenceNumberErrorCode = checkCode(sequenceNumberErrorCode);
        this.hicErrorCode = checkCode(hicErrorCode);
        this.dateOfBirthErrorCode = checkCode(dateOfBirthErrorCode);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getPatientControlNumber() {
        return patientControlNumber;
    }

    public String getHicNumber() {
        return hicNumber;
    }

    public LocalDate getPatientDateOfBirth() {
        return patientDateOfBirth;
    }

    public List<DiagnosisCluster> getDiagnosisClusters() {
        return diagnosisClusters;
    }

    public String getCorrectedHicNumber() {
        return correctedHicNumber;
    }

    public List<RiskAssessmentCodeCluster> getRiskAssessmentCodeClusters() {
        return riskAssessmentCodeClusters;
    }

    /**
     * Returns a RAPS RETURN error code or null if no error code was encountered.
     */
    public Integer getSequenceNumberErrorCode() {
        return sequenceNumberErrorCode;
    }

    /**
     * Returns a RAPS RETURN error code or null if no error code was encountered.
     */
    public Integer getHicErrorCode() {
        return hicErrorCode;
    }

    /**
     * Returns a RAPS RETURN error code or null if no error code was encountered.
     */
    public Integer getDateOfBirthErrorCode() {
        return dateOfBirthErrorCode;
    }

    @Override
    public int getFillerMaxLength() {
        return FILLER_MAX_LENGTH;
    }

    @Override
    public String toString() {
        StringBuilder record = new StringBuilder(RecordId.CCC.toString())
            .append(formatSequenceNumber(sequenceNumber))
            .append(formatErrorCode(sequenceNumberErrorCode))
            .append(pad(patientControlNumber, PATIENT_CONTROL_NUMBER_FIELD_LENGTH))
            .append(pad(hicNumber, HIC_NUMBER_FIELD_LENGTH))
            .append(formatErrorCode(hicErrorCode))
            .append(patientDateOfBirth.toString(DATE_FORMAT))
            .append(formatErrorCode(dateOfBirthErrorCode))
            .append(serializeDiagnosisClusters())
            .append(pad(correctedHicNumber, HIC_NUMBER_FIELD_LENGTH))
            .append(serializeRiskAssessmentCodeClusters())
            .append(filler);
        return toRapsRecordLength(record);
    }

    private String serializeDiagnosisClusters() {
        StringBuilder builder = new StringBuilder();
        for (DiagnosisCluster cluster : diagnosisClusters) {
            builder.append(cluster.toString());
        }
        return pad(builder.toString(), MAX_DIAGNOSIS_CLUSTERS * DIAGNOSIS_CLUSTER_LENGTH);
    }

    private String serializeRiskAssessmentCodeClusters() {
        StringBuilder builder = new StringBuilder();
        for (RiskAssessmentCodeCluster cluster : riskAssessmentCodeClusters) {
            builder.append(String.valueOf(cluster));
        }
        return pad(builder.toString(), MAX_RISK_ASSESSMENT_CODE_CLUSTERS * RISK_ASSESSMENT_CODE_CLUSTER_LENGTH);
    }
}
