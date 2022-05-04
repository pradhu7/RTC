package com.apixio.model.converter.utils;

import com.apixio.model.converter.exceptions.InsuranceClaimFilteringException;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by jctoledo on 6/9/16.
 */
public class InsuranceClaimFilterUtilTest {
    private static File sampleInputCSVFile, sampleIncorrectInputCSVFile;
    private static InputStream sampleInputStream, errorInputStream;
    private static String csvString, errorCsvString;
    private static RDFModel m, mError;

    @BeforeClass
    public static void setupOnce() throws IOException {
        boolean runTest = true;
        //load the csv files
        ClassLoader cl = InsuranceClaimFilterUtilTest.class.getClassLoader();
        sampleInputCSVFile = new File(cl.getResource("claim_filters/insurance_claim_filter_correct.csv").getFile());
        sampleIncorrectInputCSVFile = new File(cl.getResource("claim_filters/insurance_claim_incorrect.csv").getFile());
        sampleInputStream = FileUtils.openInputStream(sampleInputCSVFile);
        errorInputStream = FileUtils.openInputStream(sampleIncorrectInputCSVFile);
        csvString = makeStringFromInputStream(sampleInputStream);
        errorCsvString = makeStringFromInputStream(errorInputStream);
    }

    @AfterClass
    public static void tearDown() {
        try {
            sampleInputStream.close();
            errorInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test0() throws IOException{
        //passing in a null filter file
        m = InsuranceClaimFilterUtil.getRAClaimFilterModel(null);
        assertEquals(true, m == null);
    }

    @Test
    public void test00() throws IOException{
        //passing in a null filter file
        m = InsuranceClaimFilterUtil.getRAClaimFilterModel("");
        assertEquals(true, m == null);
    }

    @Test
    public void test1() throws IOException {
        m = InsuranceClaimFilterUtil.getRAClaimFilterModel(csvString);
        assertEquals(false, m.isEmpty());
        //now run a SPARQL query to get all filterable events
        String q = "SELECT distinct (count(?route) AS ?count) WHERE { "
                + " ?claim_filter rdf:type <" + OWLVocabulary.OWLClass.InsuranceClaimErrorCodeFilter.uri() + "> ."
                + " ?claim_filter <" + OWLVocabulary.OWLDataProperty.RouteToCoder.uri() + "> ?route. "
                + " filter (?route = true)"
                + " }";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(m, q);
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            int totalCounts = sol.get("count").asLiteral().getInt();
            boolean checkme = false;
            if (totalCounts > 91) {
                checkme = true;
            }
            assertTrue(checkme);
            break;
        }
    }

    @Test
    public void test4(){
        boolean checkMe = false;
        try {
            mError = InsuranceClaimFilterUtil.getRAClaimFilterModel(errorCsvString);
            checkMe = true;
        }catch (InsuranceClaimFilteringException e){
            assertEquals(false, checkMe);
        }
    }


    private static String makeStringFromInputStream(InputStream ais){
        StringBuilder sb=new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(ais));
        String read;
        try {
            while((read=br.readLine()) != null) {
                sb.append(read.trim()+"\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


}
