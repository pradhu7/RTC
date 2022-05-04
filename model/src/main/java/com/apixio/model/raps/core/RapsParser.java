package com.apixio.model.raps.core;

import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.apixio.model.raps.RapsConstants.*;
import static com.apixio.model.raps.RapsUtils.*;

/**
 * Warning: this parser is over-engineered to handle all possible scenarios (syntactic and semantic) for parsing RAPS files.
 * For Apixio purposes we most probably need just simple 'value-extraction' semantics per CCC record and basic validations for 'required' fields.
 *
 * A streaming iterative pull parser for Risk Adjustment Processing System (RAPS) files.
 *
 * Not thread-safe. Confine to a single thread in multi-threaded environments.
 *
 * Objects of this class are very lightweight to initialize and use.
 * Don't reuse a single parser object to parse multiple RAPS files.
 *
 * Usage example:
 *
 *      try (BufferedReader br = Files.newBufferedReader(Paths.get("/path/to/file.raps"))) {
 *          RapsParser parser = new RapsParser(br);
 *          while (parser.hasNext()) {
 *              Record record = parser.next();
 *              if (record instanceof AaaRecord) {
 *                  // do AAA record processing
 *              } else if (record instanceof BbbRecord) {
 *                  // do BBB record processing
 *              } else if (record instanceof CccRecord) {
 *                  // do CCC record processing
 *              } else if (record instanceof YyyRecord) {
 *                  // do YYY record processing
 *              } else if (record instanceof ZzzRecord) {
 *                  // to ZZZ record processing
 *              } else {
 *                  // handle error
 *              }
 *          }
 *      } catch (RapsFormatException e) {
 *          // process RAPS format error
 *      } catch (IOException e) {
 *          // process IO error
 *      }
 *
 */
public final class RapsParser {

    private final List<String> warnings = new ArrayList<>();

    private final BufferedReader rapsFile;

    public static final String VERSION = "0.1";

    /**
     * Risk assessment code clusters are a later addition to the RAPS Specification and only required for diagnosis clusters with a start date on or after Jan 1, 2014.
     */
    private static final LocalDate RISK_ASSESSMENT_INCLUSION_DATE =  new LocalDate(2014, 1, 1);

    static final DateTimeFormatter RAPS_DATE_FORMAT = ISODateTimeFormat.basicDate();
    static final Pattern SUBMITTER_ID_PATTERN = Pattern.compile("\\A\\p{Alnum}{6}\\z");
    static final Pattern FILE_ID_PATTERN = Pattern.compile("\\A\\p{Alnum}{10}\\z");
    static final Pattern PLAN_NUMBER_PATTERN = Pattern.compile("\\A\\p{Alnum}{5}\\z");
    static final Pattern HIC_PATTERN = Pattern.compile("\\A\\p{Alnum}*\\z");
    static final Pattern DIAGNOSIS_CODE_PATTERN = HIC_PATTERN;

    private RecordId state;

    private String line;
    private long lineNo = 0;
    private int batchSequenceNumber = 0;
    private int detailSequenceNumber = 0;
    private String fileId; // assigned once after reading AAA record and must be immutable afterwards
    private String submitterId; // assigned once after reading AAA record and must be immutable afterwards
    private ProdTestIndicator prodTestIndicator; // assigned once after reading AAA record and must be immutable afterwards
    private String batchPlanNumber; // updated every time BBB record is parsed

    public RapsParser(BufferedReader rapsFile) throws IOException {
        this.rapsFile = rapsFile;
        line = moveCursor();
    }

    public Record next() throws IOException, RapsFormatException {
        if (line == null)
        {
            throw new IllegalStateException("RAPS parser ran out of data");
        }

        // `parseRecord` may return `null` for CCC records which should be just skipped.
        Record record = null;
        while (record == null)
        {
            record = parseRecord(line);
        }

        return record;
    }

    public boolean hasNext() {
        return line != null;
    }

    Record parseRecord(String line) throws IOException, RapsFormatException {
        if (line.length() > RECORD_LENGTH)
        {
            throw new RapsFormatException(lineNo, "record length (chars)", RECORD_LENGTH + " or less");
        }
        else if (line.length() < RECORD_LENGTH)
        {
            line = toRapsRecordLength(line);
        }

        RecordId recordId = null;
        try
        {
            recordId = RecordId.valueOf(line.substring(0, 3));
        } catch (IllegalArgumentException e) {
            throw new RapsFormatException(lineNo, "record id", "one of " + EnumSet.allOf(RecordId.class));
        }

        Record record = null;
        switch (recordId) {
            case AAA:
                if (state != null && state != RecordId.ZZZ) {
                    throw new RapsFormatException(lineNo, RecordId.AAA + " position", "the first record in a RAPS file or come after ZZZ record if we have concatenated RAPS files, but was after '" + state + "'");
                }
                state = RecordId.AAA;
                record = parseAaaRecord(line);
                break;
            case BBB:
                if (state != RecordId.AAA && state != RecordId.YYY) {
                    throw new RapsFormatException(lineNo, RecordId.BBB + " position", "after either " + RecordId.AAA + " or " + RecordId.YYY);
                }
                state = RecordId.BBB;
                record = parseBbbRecord(line);
                break;
            case CCC:
                if (state != RecordId.CCC && state != RecordId.BBB) {
                    throw new RapsFormatException(lineNo, RecordId.CCC + " position", "after either " + RecordId.BBB + " or " + RecordId.CCC);
                }
                state = RecordId.CCC;
                record = parseCccRecord(line);
                break;
            case YYY:
                if (state != RecordId.CCC) {
                    throw new RapsFormatException(lineNo, RecordId.YYY + " position", "after " + RecordId.CCC);
                }
                state = RecordId.YYY;
                record = parseYyyRecord(line);
                break;
            case ZZZ:
                if (state != RecordId.YYY) {
                    throw new RapsFormatException(lineNo, RecordId.ZZZ + " position", "after " + RecordId.YYY);
                }
                state = RecordId.ZZZ;
                record = parseZzzRecord(line);
        }

        String nextLine = this.line = moveCursor();
        if (nextLine == null && state != RecordId.ZZZ) {
            warnings.add(new RapsFormatException(lineNo, "RAPS file", "end in ZZZ record").toString());
        }
        return record;
    }

    private AaaRecord parseAaaRecord(String line) throws RapsFormatException {
        String submitterId = this.submitterId = parseSubmitterId(line, "AAA");
        String fileId = this.fileId = parseFileId(line, "AAA");
        LocalDate transactionDate = parseDate(line.substring(19, 27), "AAA transaction date");

        ProdTestIndicator prodTestIndicator = null;
        try {
            prodTestIndicator = this.prodTestIndicator = ProdTestIndicator.valueOf(line.substring(27, 31));
        } catch (IllegalArgumentException e) {
            throw new RapsFormatException(lineNo, 27, "AAA prod/test indicator", "one of " + EnumSet.allOf(ProdTestIndicator.class));
        }

        DiagnosisType diagnosisType = null;
        try {
            diagnosisType = DiagnosisType.fromString(line.substring(31, 36));
        } catch (IllegalArgumentException e) {
            throw new RapsFormatException(lineNo, 31, "AAA diagnosis type", "one of " + EnumSet.allOf(DiagnosisType.class));
        }

        String filler = line.substring(36, 512);
        return new AaaRecord(submitterId, fileId, transactionDate, prodTestIndicator, diagnosisType, filler);
    }

    private BbbRecord parseBbbRecord(String line) throws RapsFormatException {
        checkSequenceNumber(line, ++batchSequenceNumber, "BBB");
        String planNumber = this.batchPlanNumber = parsePlanNumber(line, "BBB");

        String overpaymentId = null;
        Integer overpaymentIdErrorCode = null;
        Integer paymentYear = null;
        Integer paymentYearErrorCode = null;
        String filler = null;
        if (prodTestIndicator == ProdTestIndicator.OPMT) {
            overpaymentId = line.substring(15, 27).trim();
            if (overpaymentId.isEmpty()) {
                throw new RapsFormatException(lineNo, 15, "BBB overpayment id", "not empty when Prod/Test indicator is OPTM (overpayment)");
            }
            overpaymentIdErrorCode = parseErrorCode(line.substring(27, 30), "BBB overpayment id", 27);
            paymentYear = parsePositiveInteger(line.substring(30, 34), "BBB payment year", 30);
            paymentYearErrorCode = parseErrorCode(line.substring(34, 37), "BBB payment year", 34);
            filler = line.substring(37, 512);
        } else {
            filler = line.substring(15, 512);
        }

        return new BbbRecord(batchSequenceNumber, planNumber, overpaymentId, overpaymentIdErrorCode,
                paymentYear, paymentYearErrorCode, filler);
    }

    private CccRecord parseCccRecord(String line) throws RapsFormatException {
        Integer sequenceNumberErrorCode = parseErrorCode(line.substring(10, 13), "CCC sequence number", 10);
        detailSequenceNumber++;
        if (sequenceNumberErrorCode == null || sequenceNumberErrorCode != CCC_SEQ_NUM_IS_OUT_OF_SEQUENCE) {
            checkSequenceNumber(line, detailSequenceNumber, "CCC");
        }

        String patientControlNumber = line.substring(13, 53).trim();
        if (patientControlNumber.isEmpty()) {
            patientControlNumber = null;
        }

        Integer hicErrorCode = parseErrorCode(line.substring(78, 81), "CCC HIC number", 78);

        // Sometimes customer may submit invalid HIC number and RAPS return file will have it as submitted but followed by the error code.
        // In that case we want to accept invalid HIC number and a corresponding error code.
        // In other cases when HIC number is invalid and there is no a corresponding error code we treat this as a syntax error.
        //
        // If we have valid crosswalk for them we may refer the claim to a real patient and use it as a valuable signal data.
        // We would know due to 353 error code not to "subtract" that data in an HCC application.

        if (hicErrorCode != null && hicErrorCode == MISSING_OR_INVALID_HIC_NUMBER)
        {
            return null; // skip this CCC record
        }

        String hic = line.substring(53, 78).trim();
        if (hic.isEmpty()) {
            throw new RapsFormatException(lineNo, 53, "CCC HIC number", "not empty");
        }

        LocalDate dob = null;
        String dobString = line.substring(81, 89).trim();
        if (!dobString.isEmpty()) {
            dob = parseDate(dobString, "CCC patient date of birth");
        }

        Integer dobErrorCode = parseErrorCode(line.substring(89, 92), "CCC patient date of birth", 89);

        List<DiagnosisCluster> diagnosisClusters = parseDiagnosisClusters(line.substring(92, 412));

        if (diagnosisClusters.isEmpty()) {
            return null; // skip this CCC record
        }

        String correctedHic = line.substring(412, 437).trim();
        if (!correctedHic.isEmpty() && hicErrorCode == null) {
            throw new RapsFormatException(lineNo, 412, "CCC corrected HIC number", "empty when HIC error code is empty");
        }
        if (!correctedHic.isEmpty()) {
            if (!HIC_PATTERN.matcher(correctedHic).matches()) {
                throw new RapsFormatException(lineNo, 412, "CCC corrected HIC number", HIC_PATTERN.pattern());
            }
        }
        if (correctedHic.isEmpty()) {
            correctedHic = null;
        }

        // may be empty
        List<RiskAssessmentCodeCluster> riskAssessmentCodeClusters = parseRiskAssessmentCodeClusters(line.substring(437, 477));

        String filler = line.substring(477, 512).trim();
        return new CccRecord(detailSequenceNumber, sequenceNumberErrorCode, patientControlNumber,
                hic, hicErrorCode, dob, dobErrorCode, diagnosisClusters, correctedHic, riskAssessmentCodeClusters, filler);
    }

    private YyyRecord parseYyyRecord(String line) throws RapsFormatException {
        checkSequenceNumber(line, batchSequenceNumber, "YYY");
        String planNumber = parsePlanNumber(line, "YYY");
        if (!planNumber.equals(this.batchPlanNumber)) {
            throw new RapsFormatException(lineNo, 10, "YYY plan number", " same as in corresponding BBB record");
        }
        checkTotalRecords(line.substring(15, 22), 15, detailSequenceNumber, "CCC");
        int cccRecordsTotal = detailSequenceNumber;
        detailSequenceNumber = 0;
        String filler = line.substring(22, 512);
        return new YyyRecord(batchSequenceNumber, planNumber, cccRecordsTotal, filler);
    }

    private ZzzRecord parseZzzRecord(String line) throws RapsFormatException {
        String submitterId = parseSubmitterId(line, "ZZZ");
        if (!submitterId.equals(this.submitterId)) {
            throw new RapsFormatException(lineNo, 3, "ZZZ submitter id", " same as in AAA record");
        }
        String fileId = parseFileId(line, "ZZZ");
        if (!fileId.equals(this.fileId)) {
            throw new RapsFormatException(lineNo, 9, "ZZZ file id", " same as in AAA record");
        }

        checkTotalRecords(line.substring(19, 26), 19, batchSequenceNumber, "BBB");

        int yyyRecordsTotal = batchSequenceNumber;
        batchSequenceNumber = 0;
        String filler = line.substring(26, 512);
        return new ZzzRecord(submitterId, fileId, yyyRecordsTotal, filler);
    }

    private String parseSubmitterId(String line, String recordId) throws RapsFormatException {
        String submitterId = line.substring(3, 9);
        if (!SUBMITTER_ID_PATTERN.matcher(submitterId).matches()) {
            throw new RapsFormatException(lineNo, 3, recordId + " submitter id", SUBMITTER_ID_PATTERN.pattern());
        }
        return submitterId;
    }

    private String parseFileId(String line, String recordId) throws RapsFormatException {
        String fileId = line.substring(9, 19);
        if (!FILE_ID_PATTERN.matcher(fileId).matches()) {
            throw new RapsFormatException(lineNo, 9, recordId + " file id", FILE_ID_PATTERN.pattern());
        }
        return fileId;
    }

    private String parsePlanNumber(String line, String recordId) throws RapsFormatException {
        String planNumber = line.substring(10, 15);
        if (!PLAN_NUMBER_PATTERN.matcher(planNumber).matches()) {
            throw new RapsFormatException(lineNo, 10, recordId + " plan number", PLAN_NUMBER_PATTERN.pattern());
        }
        return planNumber;
    }

    private List<DiagnosisCluster> parseDiagnosisClusters(String data) throws RapsFormatException {
        List<DiagnosisCluster> clusters = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int startIndex = DIAGNOSIS_CLUSTER_LENGTH * i;
            int endIndex = DIAGNOSIS_CLUSTER_LENGTH * (i + 1);
            String detail = data.substring(startIndex, endIndex);
            if (!detail.trim().isEmpty()) {
                DiagnosisCluster cluster = parseDiagnosisCluster(detail, 92 + startIndex);
                if (cluster != null)
                {
                    clusters.add(cluster);
                }
            }
        }
        return clusters;
    }

    private DiagnosisCluster parseDiagnosisCluster(String detail, int index) throws RapsFormatException {
        Integer error1 = parseErrorCode(detail.substring(26, 29), "(1) CCC diagnosis cluster", index + 26);
        if (error1 != null && error1 == AT_LEAST_ONE_DIAGNOSIS_CLUSTER_REQUIRED_ON_TRANSACTION_ERROR)
        {
            return null;
        }

        Integer error2 = parseErrorCode(detail.substring(29, 32), "(2) CCC diagnosis cluster", index + 29);

        ProviderType providerType = null;
        try {
            providerType = ProviderType.fromRapsCode(detail.substring(0, 2));
        } catch (IllegalArgumentException e) {
            throw new RapsFormatException(lineNo, index, "CCC diagnosis cluster provider type", "one of 01, 02, 10, 20");
        }

        // FROM DATE may be later than THROUGH DATE if there is an accompanying 404 diagnosis cluster error code.
        LocalDate fromDate = parseDate(detail.substring(2, 10), "CCC diagnosis cluster from date");
        LocalDate throughDate = parseDate(detail.substring(10, 18), "CCC diagnosis cluster through date");

        boolean hasDeleteIndicator = false;
        if (detail.charAt(18) == 'D') {
            hasDeleteIndicator = true;
        } else if (detail.charAt(18) != ' ') {
            throw new RapsFormatException(lineNo, index + 18, "CCC diagnosis cluster delete indicator", "'D' or ' '");
        }

        String diagnosisCode = detail.substring(19, 26).trim();

        // throw exception only if there is no corresponding error code
        if (diagnosisCode.length() < 3 && error1 == null) {
            throw new RapsFormatException(lineNo, index + 19, "CCC diagnosis cluster code", "3 to 7 chars");
        }
        if (!DIAGNOSIS_CODE_PATTERN.matcher(diagnosisCode).matches() && error1 == null) {
            throw new RapsFormatException(lineNo, index + 19, "CCC diagnosis cluster code", DIAGNOSIS_CODE_PATTERN.pattern());
        }

        return new DiagnosisCluster(providerType, fromDate, throughDate, hasDeleteIndicator,
                diagnosisCode, error1, error2);
    }

    private List<RiskAssessmentCodeCluster> parseRiskAssessmentCodeClusters(String clustersString) throws RapsFormatException {
        List<RiskAssessmentCodeCluster> clusters = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int startIndex = RISK_ASSESSMENT_CODE_CLUSTER_LENGTH * i;
            int endIndex = RISK_ASSESSMENT_CODE_CLUSTER_LENGTH * (i + 1);
            String clusterString = clustersString.substring(startIndex, endIndex);

            if (!clusterString.trim().isEmpty()) {
                clusters.add(parseRiskAssessmentCodeCluster(clusterString, 437 + startIndex));
            } else {
                clusters.add(null);
            }
        }
        return clusters.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private RiskAssessmentCodeCluster parseRiskAssessmentCodeCluster(String detail, int index) throws RapsFormatException {
        Integer error = parseErrorCode(detail.substring(1), "CCC risk assessment cluster error", index + 1);

        String codeString = detail.substring(0, 1);
        RiskAssessmentCode code = null;

        try
        {
            code = RiskAssessmentCode.valueOf(codeString);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }

        return new RiskAssessmentCodeCluster(code, error);
    }

    private LocalDate parseDate(String value, String desc) throws RapsFormatException {
        try {
            return LocalDate.parse(value, RAPS_DATE_FORMAT);
        } catch (IllegalArgumentException e) {
            throw new RapsFormatException(lineNo, desc, "CCYYMMDD");
        }
    }

    /**
     * Return integer error code if there is one or null otherwise.
     */
    private Integer parseErrorCode(String value, String desc, int index) throws RapsFormatException {
        if (value.trim().isEmpty()) {
            return null;
        }
        return parsePositiveInteger(value, desc + " error code", index);
    }

    private void checkTotalRecords(String value, int index, int expectedTotalRecords, String recordType) throws RapsFormatException {
        String desc = recordType + " total records";
        int actualTotalRecords = parsePositiveInteger(value, desc, index);
        if (actualTotalRecords != expectedTotalRecords) {
            throw new RapsFormatException(lineNo, index, desc, String.valueOf(expectedTotalRecords));
        }
    }

    private void checkSequenceNumber(String line, int expectedSequenceNumber, String recordId) throws RapsFormatException {
        String desc = recordId + " sequence number";
        int sequenceNumber = parsePositiveInteger(line.substring(3, 10), desc, 3);
        if (sequenceNumber != expectedSequenceNumber) {
            throw new RapsFormatException(lineNo, 3, desc, String.valueOf(expectedSequenceNumber));
        }
    }

    private int parsePositiveInteger(String value, String desc, int index) throws RapsFormatException {
        try {
            int number = Integer.valueOf(value);
            if (number < 1) {
                throw new RapsFormatException(lineNo, index, desc, "a positive integer");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new RapsFormatException(lineNo, index, desc, "a valid number");
        }
    }

    private String moveCursor() throws IOException {
        lineNo++;
        String line = rapsFile.readLine();

        // skipping custom RAPS EOF indicator
        if (line != null && line.trim().equals("***** E N D    O F    T R A N S M I S S I O N *****")) {
            line = rapsFile.readLine();
        }
        return line;
    }

    public Iterable<String> getWarnings() {
        return warnings;
    }
}
