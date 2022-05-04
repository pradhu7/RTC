package com.apixio.model.raps.core;

import com.apixio.model.raps.RapsGenerator;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Before;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import static com.apixio.model.raps.RapsConstants.*;
import static com.apixio.model.raps.RapsUtils.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.apache.commons.lang3.StringUtils.*;

public class RapsParserTest {

    private List<String> rapsLines;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        rapsLines = RapsGenerator.generateRapsLines(ProdTestIndicator.PROD);
    }

    @Test
    @Ignore("The latest requirement is to automatically extends invalid lines")
    public void throws_on_invalid_raps_record_length() throws IOException, RapsFormatException {
        String rapsData = "record with a wrong size";
        assertFail(rapsData, "record length (chars)", 1);
    }

    @Test
    public void throws_on_invalid_raps_record_id() throws IOException, RapsFormatException {
        String rapsData = toRapsRecordLength("abc");
        assertFail(rapsData, "record id", 1);
    }

    @Test
    public void throws_on_invalid_aaa_submitter_id() throws IOException, RapsFormatException {
        String rapsData = toRapsRecordLength("AAA######");
        assertFail(rapsData, "AAA submitter id", 1);
    }

    @Test
    public void throws_on_invalid_aaa_file_id() throws IOException, RapsFormatException {
        String rapsData = toRapsRecordLength("AAAsubm01##########");
        assertFail(rapsData, "AAA file id", 1);
    }

    @Test
    public void throws_on_invalid_aaa_transaction_date() throws IOException, RapsFormatException {
        String rapsData = toRapsRecordLength("AAAsubm01file123456########");
        assertFail(rapsData, "AAA transaction date", 1);
    }

    @Test
    public void throws_on_invalid_aaa_prod_test_indicator() throws IOException, RapsFormatException {
        String rapsData = toRapsRecordLength("AAAsubm01file12345620130401####");
        assertFail(rapsData, "AAA prod/test indicator", 1);
    }

    @Test
    public void blank_filler_in_aaa_record_is_reported() throws IOException, RapsFormatException {
        rapsLines.remove(0);
        rapsLines.add(0, toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 "));
        Record record = parseFirst(join(rapsLines, '\n'));
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void non_blank_filler_in_aaa_record_is_reported() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9    some additional data in the filler ");
        rapsLines.remove(0);
        rapsLines.add(0, aaaRecord);
        Record record = parseFirst(join(rapsLines, '\n'));
        assertFalse(record.isFillerBlank());
    }

    @Test
    public void throws_when_bbb_sequence_number_is_not_a_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB000x001plan1"); // letter 'x' is invalid
        assertFail(aaaRecord + "\n" + bbbRecord, "BBB sequence number", 2);
    }

    @Test
    public void throws_when_sequence_number_of_the_first_bbb_record_is_not_0000001() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000002plan1"); // first bbb record has 0000002 sequence number
        assertFail(aaaRecord + "\n" + bbbRecord, "BBB sequence number", 2);
    }

    @Test
    public void throws_when_bbb_sequence_numbers_do_not_increase_incrementally_by_1() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord1 = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "012013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String bbbRecord2 = toRapsRecordLength("BBB0000003plan1"); // second bbb record has 0000003 sequence number (must be 0000002)
        assertFail(aaaRecord + "\n" + bbbRecord1 + "\n" + cccRecord + "\n" + yyyRecord + "\n" + bbbRecord2, "BBB sequence number", 5);
    }

    @Test
    public void throws_on_invalid_bbb_plan_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001#####");
        assertFail(aaaRecord + "\n" + bbbRecord, "BBB plan number", 2);
    }

    @Test
    public void blank_filler_in_bbb_record_is_reported() throws IOException, RapsFormatException {
        Record record = parseAt(join(rapsLines, '\n'), 2);
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void non_blank_filler_in_bbb_record_is_reported() throws IOException, RapsFormatException {
        String bbbRecord = toRapsRecordLength("BBB0000001plan1  some additional data in the filler");
        rapsLines.remove(1);
        rapsLines.add(1, bbbRecord);
        Record record = parseAt(join(rapsLines, '\n'), 2);
        assertFalse(record.isFillerBlank());
    }

    @Test
    public void blank_filler_in_ccc_record_is_reported() throws IOException, RapsFormatException {
        String diagnosisCluster = "012013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "corhicn123               ");

        rapsLines.remove(2);
        rapsLines.add(2, cccRecord);

        Record record = parseAt(join(rapsLines, '\n'), 3);
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void non_blank_filler_in_ccc_record_is_reported() throws IOException, RapsFormatException {
        String diagnosisCluster = "012014010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020140303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "corhicn123               A100                                    some staff in the filler place");
        rapsLines.remove(2);
        rapsLines.add(2, cccRecord);
        Record record = parseAt(join(rapsLines, '\n'), 3);
        assertFalse(record.isFillerBlank());
    }

    @Test
    public void aaa_record_is_parsed_correctly() throws IOException, RapsFormatException {
        AaaRecord record = (AaaRecord) parseFirst(join(rapsLines, '\n'));
        assertEquals("subm01", record.getSubmitterId());
        assertEquals("file123456", record.getFileId());
        assertEquals("2014-03-01", record.getTransactionDate().toString());
        assertSame(ProdTestIndicator.PROD, record.getProdTestIndicator());
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void bbb_record_is_parsed_correctly() throws IOException, RapsFormatException {
        BbbRecord record = (BbbRecord) parseAt(join(rapsLines, '\n'), 2);
        assertEquals(1, record.getSequenceNumber());
        assertEquals("plan1", record.getPlanNumber());
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void throws_when_ccc_sequence_number_is_not_a_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC#######");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC sequence number", 3);
    }

    @Test
    public void throws_when_ccc_sequence_number_of_the_first_ccc_record_is_not_0000001() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000002"); // must be 0000001
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC sequence number", 3);
    }

    @Test
    public void throws_when_ccc_sequence_numbers_do_not_increase_incrementally_by_1() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "012013010120170101D42732  100100";
        String cccRecord1 = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "corhicn123");
        String cccRecord2 = toRapsRecordLength("CCC0000003100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "corhicn123"); // sequence number is 0000003 but must be 0000002
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord1 + "\n" + cccRecord2, "CCC sequence number", 4);
    }

    @Test
    public void throws_when_ccc_sequence_number_error_code_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001###");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC sequence number error code", 3);
    }

    @Test
    @Ignore("Analysis on real data showed that patient control number may contain spaces, underscores and probably other non-standard chars")
    public void throws_when_ccc_patient_control_number_is_not_alphanumeric() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100#########################                 ");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC patient control number", 3);
    }

    @Test
    @Ignore("Allow bad staff in hic number as we can have them in the RAPS files followed by error codes")
    public void throws_when_ccc_hic_number_is_not_alphanumeric() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               #########################");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC HIC number", 3);
    }

    @Test
    public void throws_when_ccc_hic_error_code_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         ###");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC HIC number error code", 3);
    }

    @Test
    public void throws_when_ccc_patient_date_of_birth_is_not_a_valid_date() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         100########");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC patient date of birth", 3);
    }

    @Test
    public void throws_when_ccc_patient_date_of_birth_error_code_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303###");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC patient date of birth error code", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_provider_type_is_not_in_01_02_10_20() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         1002013030310044"); // 44 at the end is wrong
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster provider type", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_from_date_is_not_a_valid_date() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         1002013030310001########");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster from date", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_through_date_is_not_a_valid_date() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         100201303031000120130101########");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster through date", 3);
    }

    @Test
    @Ignore("This is a valid case when there is a 404 error code")
    public void throws_when_ccc_diagnosis_cluster_through_date_is_less_than_from_date() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120120101");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster through date", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_delete_indicator_is_not_space_or_D() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120170101#");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster delete indicator", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_code_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120170101D#######");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster code", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_code_is_invalid_and_there_is_no_error() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120170101D#######");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC diagnosis cluster code", 3);
    }

    @Test
    public void accepts_invalid_ccc_diagnosis_cluster_code_when_there_is_an_error() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120170101D#######100");
        assertParseable(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_error_code1_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120170101D42732  ###");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "(1) CCC diagnosis cluster error code", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_error_code2_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100012013010120170101D42732  100###");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "(2) CCC diagnosis cluster error code", 3);
    }

    @Test
    public void throws_when_ccc_diagnosis_cluster_corrected_hicn_is_not_alphanumeric() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "012013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "##########               ");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC corrected HIC number", 3);
    }

    @Test
    public void throws_when_corrected_hic_number_and_no_hic_error_code() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "012013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412            20130303100"
                + toDiagnosisClustersLength(diagnosisCluster) + "corhicn123               ");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord, "CCC corrected HIC number", 3);
    }

    @Test
    public void ccc_record_is_parsed_correctly() throws IOException, RapsFormatException {
        CccRecord record = (CccRecord) parseAt(join(rapsLines, '\n'), 3);
        assertEquals(1, record.getSequenceNumber());
        assertEquals((Integer) 100, record.getSequenceNumberErrorCode());
        assertEquals("patient ctrl no w spaces", record.getPatientControlNumber());
        assertEquals("hic11", record.getHicNumber());
        assertEquals((Integer) 100, record.getHicErrorCode());
        assertEquals("1930-04-05", record.getPatientDateOfBirth().toString());
        assertEquals((Integer) 100, record.getDateOfBirthErrorCode());
        assertEquals(null, record.getCorrectedHicNumber());
        assertTrue(record.isFillerBlank());

        List<DiagnosisCluster> clusters = record.getDiagnosisClusters();
        assertNotNull(clusters);
        assertFalse("Diagnosis clusters list must be not empty", clusters.isEmpty());
        DiagnosisCluster cluster = clusters.get(0);
        assertSame(ProviderType.PHYSICIAN, cluster.getProviderType());
        assertEquals("2014-09-01", cluster.getFromDate().toString());
        assertEquals("2014-09-02", cluster.getThroughDate().toString());
        assertFalse("Diagnosis cluster must not have a delete indicator", cluster.hasDeleteIndicator());
        assertEquals("V2345", cluster.getDiagnosisCode());
        assertEquals((Integer) 100, cluster.getError1());
        assertEquals((Integer) 100, cluster.getError2());

        List<RiskAssessmentCodeCluster> riskAssessmentCodeClusters = record.getRiskAssessmentCodeClusters();
        assertEquals(1, riskAssessmentCodeClusters.size());

        RiskAssessmentCodeCluster riskAssessmentCodeCluster1 = riskAssessmentCodeClusters.get(0);
        assertEquals(RiskAssessmentCode.A, riskAssessmentCodeCluster1.getCode());
        assertEquals((Integer) null, riskAssessmentCodeCluster1.getError());
    }

    @Test
    public void throws_when_yyy_sequence_number_is_not_an_integer() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY000x001plan1"); // letter 'x' is invalid
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, "YYY sequence number", 4);
    }

    @Test
    public void throws_when_yyy_sequence_number_does_not_match_with_bbb_sequence_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000002plan1");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, "YYY sequence number", 4);
    }

    @Test
    public void throws_on_invalid_yyy_plan_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001#####");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, "YYY plan number", 4);
    }

    @Test
    public void throws_when_yyy_plan_number_does_not_match_with_bbb_plan_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan2");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, "YYY plan number", 4);
    }

    @Test
    public void throws_on_invalid_yyy_total_ccc_records_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan1#######");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, "CCC total records", 4);
    }

    @Test
    public void throws_when_yyy_total_ccc_records_does_not_match_actual() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000002"); // must be 0000001
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, "CCC total records", 4);
    }

    @Test
    public void blank_filler_in_yyy_record_is_reported() throws IOException, RapsFormatException {
        YyyRecord record = (YyyRecord) parseAt(join(rapsLines, '\n'), 4);
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void non_blank_filler_in_yyy_record_is_reported() throws IOException, RapsFormatException {
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001  some staff in the filler");
        rapsLines.remove(3);
        rapsLines.add(3, yyyRecord);
        YyyRecord record = (YyyRecord) parseAt(join(rapsLines, '\n'), 4);
        assertFalse(record.isFillerBlank());
    }

    @Test
    public void throws_on_invalid_zzz_submitter_id() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZ######");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "ZZZ submitter id", 5);
    }

    @Test
    public void throws_when_zzz_submitter_id_does_not_match_aaa_record() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm02"); // must be 'subm01' as in AAA record
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "ZZZ submitter id", 5);
    }

    @Test
    public void throws_on_invalid_zzz_file_id() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01##########");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "ZZZ file id", 5);
    }

    @Test
    public void throws_when_zzz_file_id_does_not_match_aaa_record() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file654321"); // must be 'file123456' as in AAA record
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "ZZZ file id", 5);
    }

    @Test
    public void throws_on_invalid_zzz_total_bbb_records_number() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file123456#######");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "BBB total records", 5);
    }

    @Test
    public void throws_when_zzz_total_bbb_records_does_not_match_actual() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000002"); // must be 0000001
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "BBB total records", 5);
    }

    @Test
    public void blank_filler_in_zzz_record_is_reported() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        ZzzRecord record = (ZzzRecord) parseAt(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, 5);
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void non_blank_filler_in_zzz_record_is_reported() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001   some random staff in the filler");
        ZzzRecord record = (ZzzRecord) parseAt(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, 5);
        assertFalse(record.isFillerBlank());
    }

    @Test
    public void zzz_record_is_parsed_correctly() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        ZzzRecord record = (ZzzRecord) parseAt(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, 5);
        assertEquals("subm01", record.getSubmitterId());
        assertEquals("file123456", record.getFileId());
        assertEquals(1, record.getBbbRecordsTotal());
        assertTrue(record.isFillerBlank());
    }

    @Test
    public void end_of_transmission_trailer_with_trailing_spaces_is_ignored() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertParseable(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord + "\n***** E N D    O F    T R A N S M I S S I O N *****     \n");
    }

    /**
     * This test relies on rest of the data to generate no warnings so it's fragile.
     */
    @Test
    public void warns_when_zzz_is_not_the_last_record_in_the_file() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        RapsParser parser = assertParseable(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord, 5);
        assertTrue(parser.getWarnings().iterator().next() != null);
    }

    @Test
    public void yyy_record_is_parsed_correctly() throws IOException, RapsFormatException {
        YyyRecord record = (YyyRecord) parseAt(join(rapsLines, '\n'), 4);
        assertEquals(1, record.getSequenceNumber());
        assertEquals("plan1", record.getPlanNumber());
        assertEquals(1, record.getCccRecordsTotal());
        assertTrue(record.isFillerBlank());
    }

    // Record Sequencing

    @Test
    public void throws_when_bbb_record_does_not_follow_aaa_or_yyy_record() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord1 = toRapsRecordLength("BBB0000001plan1");
        String bbbRecord2 = toRapsRecordLength("BBB0000002plan2");
        assertFail(aaaRecord + "\n" + bbbRecord1 + "\n" + bbbRecord2, RecordId.BBB.toString(), 3);
    }

    @Test
    public void throws_when_ccc_record_does_not_follow_ccc_or_bbb_record() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String diagnosisCluster = "012013010120170101D42732  100100";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster));
        assertFail(aaaRecord + "\n" + cccRecord, RecordId.CCC.toString(), 2);
    }

    @Test
    public void throws_when_yyy_record_does_not_follow_ccc_record() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        assertFail(aaaRecord + "\n" + yyyRecord, RecordId.YYY.toString(), 2);
    }

    @Test
    public void throws_when_zzz_record_does_not_follow_yyy_record() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertFail(aaaRecord + "\n" + zzzRecord, RecordId.ZZZ.toString(), 2);
    }

    @Test
    public void resets_detail_sequence_number_on_batch_boundaries() throws IOException, RapsFormatException {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord1 = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord1 = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String bbbRecord2 = toRapsRecordLength("BBB0000002plan1");
        String cccRecord2 = toRapsRecordLength("CCC0000002100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               "); // sequence number must be 0000001, not 0000002
        assertFail(aaaRecord + "\n" + bbbRecord1 + "\n" + cccRecord1 + "\n" + yyyRecord + "\n" + bbbRecord2 + "\n" + cccRecord2, "CCC sequence number", 6);
    }

    @Test
    public void should_handle_309_error_gracefully() throws Exception {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord1 = toRapsRecordLength("CCC0000001   PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String cccRecord2 = toRapsRecordLength("CCC0000003309PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               "); // sequence number must be 0000002, not 0000003
        String cccRecord3 = toRapsRecordLength("CCC0000003   PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               "); // this 3rd record checks that consequent CCC records are being parsed with correct sequence numbers, i.e. parser's counter is incremented correctly.
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000003");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertParseable(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord1 + "\n" + cccRecord2 + "\n" + cccRecord3 + "\n" + yyyRecord + "\n" + zzzRecord);
    }

    @Test
    public void should_handle_311_error_gracefully() throws Exception {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620150401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022015010120150102        311";
        String cccRecord = toRapsRecordLength("CCC0000001   PatientControlNumber00001               123456789A                  19280101   "
                + toDiagnosisClustersLength(diagnosisCluster));
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertParseable(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord);
    }

    @Test
    public void should_skip_ccc_records_with_310_error() throws Exception {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620150401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022015010120150102        311";
        String cccRecord = toRapsRecordLength("CCC0000001   PatientControlNumber00001                                        31019280101   "
                + toDiagnosisClustersLength(diagnosisCluster));
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertParseable(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord);
    }

    @Test
    public void throws_on_empty_hic_without_310_error() throws Exception {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620150401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022015010120150102        311";
        String cccRecord = toRapsRecordLength("CCC0000001   PatientControlNumber00001                                           19280101   "
                + toDiagnosisClustersLength(diagnosisCluster));
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord + "\n" + yyyRecord + "\n" + zzzRecord, "CCC HIC number", 3);
    }

    @Test
    public void should_throw_when_sequence_number_is_wrong_and_there_is_no_309_error() throws Exception {
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PRODICD9 ");
        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        String diagnosisCluster = "022013010120170101D42732  100100";
        String cccRecord1 = toRapsRecordLength("CCC0000001   PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               ");
        String cccRecord2 = toRapsRecordLength("CCC0000003   PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster + diagnosisCluster) + "corhicn123               "); // sequence number must be 0000001, not 0000002
        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        assertFail(aaaRecord + "\n" + bbbRecord + "\n" + cccRecord1 + "\n" + cccRecord2 + "\n" + yyyRecord + "\n" + zzzRecord, "CCC sequence number", 4);
    }

    private Record parseAt(String rapsData, int lineNo) throws IOException, RapsFormatException {
        RapsParser parser = new RapsParser(toReader(rapsData));
        Record record = null;
        for (int i = 0; i < lineNo; i++) {
            record = parser.next();
        }
        return record;
    }

    private void assertParseable(String rapsData) throws IOException, RapsFormatException {
        RapsParser parser = new RapsParser(toReader(rapsData));
        Record record = null;
        while (parser.hasNext())
        {
            parser.next();
        }
    }

    private Record parseFirst(String rapsData) throws IOException, RapsFormatException {
        return parseAt(rapsData, 1);
    }

    private void assertFail(String rapsData, String what, long lineNo) throws IOException, RapsFormatException {
        RapsParser parser = new RapsParser(toReader(rapsData));
        thrown.expect(RapsFormatException.class);
        thrown.expectMessage("Invalid " + what);
        thrown.expectMessage("at line " + lineNo);
        while(parser.hasNext()) {
            parser.next();
        }
    }

    private RapsParser assertParseable(String rapsData, int stop) throws IOException, RapsFormatException {
        RapsParser parser = new RapsParser(toReader(rapsData));
        for (int i = 0; i < stop - 1; i++) {
            parser.next();
        }
        return parser;
    }

}
