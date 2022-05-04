package com.apixio.model.raps.csv;

import com.apixio.model.external.CaretParser;
import com.apixio.model.external.CaretParserException;
import com.apixio.model.raps.core.RapsFormatException;
import com.apixio.model.raps.RapsGenerator;
import com.apixio.model.raps.core.ProdTestIndicator;
import com.apixio.model.csv.DataDefinition;

import org.junit.Test;
import org.junit.Rule;
import org.junit.Before;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static com.apixio.model.raps.RapsUtils.*;
import static com.apixio.model.raps.RapsConstants.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.junit.Assert.*;

public class RapsToCsvParserTest {

    private List<String> rapsLines;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void all_fields_in_prod_mode_are_parsed() throws IOException, RapsFormatException, CaretParserException {
        List<String> rapsLines = RapsGenerator.generateRapsLines(ProdTestIndicator.PROD);
        RapsToCsvParser parser = new RapsToCsvParser(toReader(join(rapsLines, "\n")));
        List<String[]> claims = new ArrayList<>();
        while (parser.hasNext()) {
            claims.add(parser.next());
        }
        assertEquals(1, claims.size());
        String[] claim = claims.get(0);
        assertEquals("RAPS to CSV parser row length", DataDefinition.RAPS_COLS.length, claim.length);
        assertEquals("PATIENT_ID", "hic11^^HIC_NUMBER", claim[0]);
        assertEquals("ORIGINAL_ID", "subm01-file123456-1-1-1^^APIXIO_RAPS_PARSER_V1", claim[1]);
        assertEquals("FROM_DATE", "2014-09-01", claim[2]);
        assertEquals("THROUGH_DATE", "2014-09-02", claim[3]);
        assertEquals("DIAGNOSIS_CODE", "^V2345^ICD_9CM_DIAGNOSIS_CODES^2.16.840.1.113883.6.103^", claim[4]);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(META_KEY_TRANSACTION_DATE, "2014-03-01");
        metadata.put(META_KEY_DETAIL_NUMBER_ERROR, "100");
        metadata.put(META_KEY_PATIENT_CONTROL_NUMBER, "patient ctrl no w spaces");
        metadata.put(META_KEY_PATIENT_HIC_NUMBER_ERROR, "100");
        metadata.put(META_KEY_PATIENT_DOB, "1930-04-05");
        metadata.put(META_KEY_PATIENT_DOB_ERROR, "100");
        metadata.put(META_KEY_PROVIDER_TYPE, "20");
        metadata.put(META_KEY_DELETE_INDICATOR, "false");
        metadata.put(META_KEY_DIAGNOSIS_CLUSTER_ERROR1, "100");
        metadata.put(META_KEY_DIAGNOSIS_CLUSTER_ERROR2, "100");
        metadata.put(META_KEY_RISK_ASSESSMENT_CODE_CLUSTERS, "A");
        assertEquals("META_DATA", metadata, CaretParser.parseMetadata(claim[5]));

        //assertEquals("EDIT_TYPE", "ACTIVE", claim[6]);
    }

    @Test
    public void overpayment_fields_in_overpayment_mode_are_parsed() throws IOException, RapsFormatException {
        List<String> rapsLines = RapsGenerator.generateRapsLines(ProdTestIndicator.OPMT);
        RapsToCsvParser parser = new RapsToCsvParser(toReader(join(rapsLines, "\n")));
        List<String[]> claims = new ArrayList<>();
        while (parser.hasNext()) {
            claims.add(parser.next());
        }
        assertEquals(1, claims.size());
        String[] claim = claims.get(0);
        String metaData = claim[5];
        assertTrue(metaData.contains(META_KEY_OVERPAYMENT_ID + "=overpmt_id01"));
        assertTrue(metaData.contains(META_KEY_OVERPAYMENT_ID_ERROR + "=100"));
        assertTrue(metaData.contains(META_KEY_PAYMENT_YEAR + "=2014"));
        assertTrue(metaData.contains(META_KEY_PAYMENT_YEAR_ERROR + "=100"));
    }

    @Test
    public void claims_from_different_batches_are_parsed() throws IOException, RapsFormatException {
        List<String> rapsLines = RapsGenerator.generateRapsLines(ProdTestIndicator.PROD, 2, 2);
        RapsToCsvParser parser = new RapsToCsvParser(toReader(join(rapsLines, "\n")));
        List<String[]> claims = new ArrayList<>();
        while (parser.hasNext()) {
            claims.add(parser.next());
        }
        assertEquals(4, claims.size());
    }

    @Test
    public void should_assume_coding_system_based_on_dates_of_service() throws Exception
    {
        List<String> rapsLines = new ArrayList<>();
        String aaaRecord = toRapsRecordLength("AAAsubm01file12345620130401PROD");
        rapsLines.add(aaaRecord);

        String bbbRecord = toRapsRecordLength("BBB0000001plan1");
        rapsLines.add(bbbRecord);

        String diagnosisCluster1 = "022013010120150930 42732        ";
        String diagnosisCluster2 = "022013010120151001 42732        ";
        String cccRecord = toRapsRecordLength("CCC0000001100PatientControlNumber00001               hicn1b3121234412         10020130303100"
                + toDiagnosisClustersLength(diagnosisCluster1 + diagnosisCluster2) + "corhicn123               ");
        rapsLines.add(cccRecord);

        String yyyRecord = toRapsRecordLength("YYY0000001plan10000001");
        rapsLines.add(yyyRecord);

        String zzzRecord = toRapsRecordLength("ZZZsubm01file1234560000001");
        rapsLines.add(zzzRecord);

        RapsToCsvParser parser = new RapsToCsvParser(toReader(join(rapsLines, "\n")));

        List<String[]> claims = new ArrayList<>();
        while (parser.hasNext()) {
            claims.add(parser.next());
        }

        assertEquals(2, claims.size());

        assertEquals("clinical code before 2015-10-01 should belong to ICD9", "^42732^ICD_9CM_DIAGNOSIS_CODES^2.16.840.1.113883.6.103^", claims.get(0)[4]);
        assertEquals("clinical code on or after 2015-10-01 should belong to ICD10", "^42732^ICD_10CM_DIAGNOSIS_CODES^2.16.840.1.113883.6.90^", claims.get(1)[4]);
    }
}
