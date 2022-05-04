package com.apixio.model.raps.csv;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.joda.time.LocalDate;

import com.apixio.model.external.CodingSystem;
import com.apixio.model.external.CaretParser;
import com.apixio.model.csv.DataDefinition;
import com.apixio.model.raps.core.*;
import com.apixio.model.raps.core.Record;

import static com.apixio.model.raps.RapsConstants.*;

public class RapsToCsvParser {

    private final static String VERSION = "1";

    private final static LocalDate icd10StartDate = LocalDate.parse("2015-10-01");

    private final RapsParser lowLevelParser;
    private AaaRecord fileHeader;
    private BbbRecord batchHeader;
    private CccRecord details;
    private List<DiagnosisCluster> clusters;
    private int index = 0;

    public RapsToCsvParser(BufferedReader reader) throws IOException, RapsFormatException {
        lowLevelParser = new RapsParser(reader);
        nextFile();
    }

    public boolean hasNext() {
        return clusters.size() - index > 0;
    }

    public Iterable<String> getWarnings() {
        return lowLevelParser.getWarnings();
    }

    public String[] next() throws IOException, RapsFormatException {
        if (!hasNext()) {
            throw new IllegalStateException("no more RAPS data");
        }
        DiagnosisCluster cluster = clusters.get(index++);
        String[] row = new String[DataDefinition.RAPS_COLS.length];

        String patientId = details.getHicNumber() + "^^HIC_NUMBER";
        row[0] = patientId;

        String originalId = fileHeader.getSubmitterId()
            + "-" + fileHeader.getFileId()
            + "-" + batchHeader.getSequenceNumber()
            + "-" + details.getSequenceNumber()
            + "-" + index + "^^" + "APIXIO_RAPS_PARSER_V" + VERSION;
        row[1] = originalId;

        row[2] = cluster.getFromDate().toString(); // START_DATE
        row[3] = cluster.getThroughDate().toString(); // END_DATE

        DiagnosisType diagnosisType = fileHeader.getDiagnosisType();
        CodingSystem codingSystem = null;
        if (cluster.getThroughDate().isBefore(icd10StartDate))
        {
            codingSystem = CodingSystem.ICD_9CM_DIAGNOSIS_CODES;
        }
        else
        {
            codingSystem = CodingSystem.ICD_10CM_DIAGNOSIS_CODES;
        }

        String clinicalCode = "" // name
            + "^" + cluster.getDiagnosisCode() // code
            + "^" + codingSystem.name()
            + "^" + codingSystem.getOid()
            + "^" + ""; // version
        row[4] = clinicalCode; // CODE

        Map<String, String> metadata = new HashMap<>();
        metadata.put(META_KEY_TRANSACTION_DATE, fileHeader.getTransactionDate().toString());
        if (fileHeader.getProdTestIndicator() == ProdTestIndicator.OPMT) {
            metadata.put(META_KEY_OVERPAYMENT_ID, batchHeader.getOverpaymentId());
            metadata.put(META_KEY_PAYMENT_YEAR, String.valueOf(batchHeader.getPaymentYear()));
            putOptionalMetadata(metadata, META_KEY_OVERPAYMENT_ID_ERROR, batchHeader.getOverpaymentIdErrorCode());
            putOptionalMetadata(metadata, META_KEY_PAYMENT_YEAR_ERROR, batchHeader.getPaymentYearErrorCode());
        }
        metadata.put(META_KEY_PROVIDER_TYPE, cluster.getProviderType().getRapsCode());
        metadata.put(META_KEY_DELETE_INDICATOR, String.valueOf(cluster.hasDeleteIndicator()));

        putOptionalMetadata(metadata, META_KEY_PATIENT_DOB, details.getPatientDateOfBirth());
        putOptionalMetadata(metadata, META_KEY_DETAIL_NUMBER_ERROR, details.getSequenceNumberErrorCode());
        putOptionalMetadata(metadata, META_KEY_PATIENT_CONTROL_NUMBER, details.getPatientControlNumber());
        putOptionalMetadata(metadata, META_KEY_PATIENT_HIC_NUMBER_ERROR, details.getHicErrorCode());
        putOptionalMetadata(metadata, META_KEY_CORRECTED_HIC_NUMBER, details.getCorrectedHicNumber());
        putOptionalMetadata(metadata, META_KEY_PATIENT_DOB_ERROR, details.getDateOfBirthErrorCode());
        putOptionalMetadata(metadata, META_KEY_DIAGNOSIS_CLUSTER_ERROR1, cluster.getError1());
        putOptionalMetadata(metadata, META_KEY_DIAGNOSIS_CLUSTER_ERROR2, cluster.getError2());

        List<RiskAssessmentCodeCluster> riskAssessmentCodeClusters = details.getRiskAssessmentCodeClusters();
        if (!riskAssessmentCodeClusters.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (RiskAssessmentCodeCluster racCluster : riskAssessmentCodeClusters) {
                if (racCluster != null)
                {
                    if (racCluster.getCode() != null)
                    {
                        b.append(racCluster.getCode().toString());
                    }

                    if (racCluster.getError() != null) {
                        b.append('^').append(racCluster.getError().toString());
                    }
                }
                b.append(',');
            }
            b.deleteCharAt(b.length() - 1); // delete trailing comma
            metadata.put(META_KEY_RISK_ASSESSMENT_CODE_CLUSTERS, b.toString());
        }

        row[5] = CaretParser.flattenMap(metadata); // META_DATA
        //row[6] = "ACTIVE"; // EDIT_TYPE

        if (index == clusters.size()) {
            nextCluster();
        }
        return row;
    }

    private void putOptionalMetadata(Map<String, String> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value.toString());
        }
    }

    private void nextCluster() throws IOException, RapsFormatException {
        clusters = Collections.emptyList();
        index = 0; // reset cursor into diagnosis clusters

        if (!lowLevelParser.hasNext()) {
            return;
        }

        Record record = lowLevelParser.next();

        if (record instanceof CccRecord) {
            details = (CccRecord) record;
            clusters = details.getDiagnosisClusters();
        } else if (record instanceof YyyRecord) {

            if (!lowLevelParser.hasNext()) {
                return;
            }

            record = lowLevelParser.next();
            if (record instanceof BbbRecord) {
                batchHeader = (BbbRecord) record;

                if (!lowLevelParser.hasNext()) {
                    return;
                }

                details = (CccRecord) lowLevelParser.next();
                clusters = details.getDiagnosisClusters();
            } else if (record instanceof ZzzRecord) {
                if (lowLevelParser.hasNext()) {
                    nextFile();
                }
            } else {
                throw new IllegalStateException("problem in the low level parser");
            }
        } else {
            throw new IllegalStateException("problem in the low level parser");
        }
    }

    private void nextFile() throws IOException, RapsFormatException
    {
        fileHeader = (AaaRecord) lowLevelParser.next();
        batchHeader = (BbbRecord) lowLevelParser.next();
        details = (CccRecord) lowLevelParser.next();
        clusters = details.getDiagnosisClusters();
    }

}
