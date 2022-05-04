package com.apixio.model.raps;

import com.apixio.model.raps.core.*;
import com.apixio.model.raps.core.Record;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import static com.apixio.model.raps.RapsConstants.*;

/**
 * Useful in tests, can be used from command-line.
 *
 * TODO: Implement parameterized generic RAPS builder as different unit tests require different RAPS data.
 *       It will help to keep unit tests isolated.
 *       Ex.
 *
 *       RapsBuilder builder = new RapsBuilder();
 *       String rapsText = builder.startFile("subm01", "file123456", new LocalDate(2014, 3, 1), ProdTestIndicator.PROD, DiagnosisType.ICD9)
 *              .startBatch("plan1")
 *              .startDetails("hicn123", new LocalDate(1932, 1, 1)
 *              .addDiagnosis(ProviderType.HMO, new LocalDate(2014, 1, 1), new LocalDate(2014, 1, 1), "v.1234", RiskAssessmentCode.A)
 *              .addDiagnosis(ProviderType.PPO, new LocalDate(2014, 1, 1), new LocalDate(2014, 1, 1), "v.1235", RiskAssessmentCode.B)
 *              .closeDetails()
 *              .closeBatch()
 *              .closeFile().toString();
 */
public class RapsGenerator {

    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java -cp <model.jar> <file>");
            System.exit(1);
        }
        Path file = Paths.get(args[0]);
        Files.write(file, generateRapsLines(ProdTestIndicator.PROD, 2, 3), StandardCharsets.UTF_8);
    }

    public static List<String> generateRapsLines(ProdTestIndicator fileMode, int batches, int claimsPerBatch) {
        List<String> rapsLines = new ArrayList<>();
        List<Record> file = generateFile(batches, claimsPerBatch, fileMode);
        for (Record r : file) {
            rapsLines.add(r.toString());
        }
        return rapsLines;
    }

    public static List<String> generateRapsLines(ProdTestIndicator fileMode) {
        return generateRapsLines(fileMode, 1, 1);
    }

    private static List<Record> generateFile(int batchCount, int claimsPerBatch, ProdTestIndicator fileMode) {
        String submitterId = "subm01";
        String fileId = "file123456";

        List<Record> file = new ArrayList<>(2 + batchCount * (claimsPerBatch + 2));
        AaaRecord aaaRecord = new AaaRecord(submitterId, fileId, LocalDate.parse("2014-03-01"),
            fileMode, DiagnosisType.ICD9, "");
        file.add(aaaRecord);
        for (int i = 1; i <= batchCount; i++) {
            List<Record> batch = generateBatch(i, claimsPerBatch, fileMode);
            file.addAll(batch);
        }
        ZzzRecord zzzRecord = new ZzzRecord(submitterId, fileId, batchCount, "");
        file.add(zzzRecord);
        return file;
    }

    private static List<Record> generateBatch(int sequenceNumber, int claimCount, ProdTestIndicator fileMode) {
        String planNumber = "plan1";
        String overpaymentId = null;
        Integer overpaymentIdErrorCode = null;
        Integer paymentYear = null;
        Integer paymentYearErrorCode = null;
        if (fileMode == ProdTestIndicator.OPMT) {
            overpaymentId = "overpmt_id01";
            overpaymentIdErrorCode = 100;
            paymentYear = 2014;
            paymentYearErrorCode = 100;
        }

        List<Record> batch = new ArrayList<>(claimCount + 2);
        BbbRecord bbbRecord = new BbbRecord(sequenceNumber, planNumber, overpaymentId, overpaymentIdErrorCode, paymentYear, paymentYearErrorCode, "");
        batch.add(bbbRecord);
        for (int i = 1; i <= claimCount; i++) {
            batch.add(generateClaim(i, sequenceNumber));
        }
        YyyRecord yyyRecord = new YyyRecord(sequenceNumber, planNumber, claimCount, "");
        batch.add(yyyRecord);
        return batch;
    }

    private static CccRecord generateClaim(int sequenceNumber, int batchNumber) {
        List<DiagnosisCluster> diagnosisClusters = new ArrayList<>();
        DiagnosisCluster diagnosisCluster = new DiagnosisCluster(ProviderType.PHYSICIAN, LocalDate.parse("2014-09-01"),
            LocalDate.parse("2014-09-02"), false, "V2345", 100, 100);
        diagnosisClusters.add(diagnosisCluster);

        List<RiskAssessmentCodeCluster> riskAssessmentCodeClusters = new ArrayList<>();
        RiskAssessmentCodeCluster riskAssessmentCodeCluster = new RiskAssessmentCodeCluster(RiskAssessmentCode.A, null);
        riskAssessmentCodeClusters.add(riskAssessmentCodeCluster);

        CccRecord cccRecord = new CccRecord(sequenceNumber, 100, "patient ctrl no w spaces", "hic" + batchNumber + sequenceNumber, 100, LocalDate.parse("1930-04-05"), 100,
            diagnosisClusters, null, riskAssessmentCodeClusters, "");
        return cccRecord;
    }

}
