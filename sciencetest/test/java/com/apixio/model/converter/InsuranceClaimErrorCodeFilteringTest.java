package com.apixio.model.converter;

import com.apixio.model.converter.bin.converters.NewAPOToRDFModel;
import com.apixio.model.converter.bin.converters.OldAPOToNewAPO;
import com.apixio.model.converter.bin.converters.RDFModelToEventType;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.event.AttributeType;
import com.apixio.model.patient.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import testutils.ConverterTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * Created by jctoledo on 6/10/16.
 */
public class InsuranceClaimErrorCodeFilteringTest {
    private static final String outcomesPath = "claim_filters/insurance_claim_filter_test_outcomes.csv";
    private static final String claimFilterFileName = "claim_filters/insurance_claim_filter_correct.csv";
    private static Map<String, String> routingOutcomes = new HashMap<>();
    private static Map<String, String> reportOutcomes = new HashMap<>();
    private static InputStream claimFilterIs = null;
    private static File outcomesFile = null;
    private static List<Source> someSources = new ArrayList<>();
    private static List<ParsingDetail> someParsingDetails = new ArrayList<>();
    private static  String claimFilterString = "";
    private Patient aPatient;

    @BeforeClass
    public static void setupOnce() {
        try {
            org.junit.Assume.assumeTrue(fileExists(outcomesPath));
            org.junit.Assume.assumeTrue(fileExists(claimFilterFileName));
            outcomesFile = getFileFromPath(outcomesPath);
            someSources = initializeSources();
            someParsingDetails = initializeParsingDetails();
            routingOutcomes = addOutcomes("ROUTE");
            reportOutcomes = addOutcomes("OUTPUT_REPORT");

            File claimF = getFileFromPath(claimFilterFileName);
            claimFilterIs = FileUtils.openInputStream(claimF);
            //claimFilterString = makeStringFromInputStream(claimFilterIs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verify that an patient-uuids.txt file exists in the test's resources directory
     *
     * @return true if the file exists
     */
    private static boolean fileExists(String aFileName) {
        try {
            ClassLoader cl = ConverterTest.class.getClassLoader();
            File inputF = new File(cl.getResource(aFileName).getFile());
            if (ConverterTestUtils.countLines(inputF) == 0) {
                return false;
            }
        } catch (Exception e) {
            System.out.println(aFileName + " not found!");
            return false;
        }
        return true;
    }

    private static File getFileFromPath(String aFilePath) {
        File rm;
        try {
            ClassLoader cl = ConverterTest.class.getClassLoader();
            rm = new File(cl.getResource(aFilePath).getFile());
            return rm;
        } catch (Exception e) {
            System.out.println("Could not create file object!");
            return null;
        }
    }

    private static List<Source> initializeSources() {
        List<Source> rm = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Source s = new Source();
            s.setInternalUUID(s.getSourceId());
            s.setSourceSystem("CMS_RAPS");
            s.setCreationDate(new DateTime());
            s.setLastEditDateTime(new DateTime());
            if (i % 2 == 0) {
                s.setSourceType("CMS_KNOWN");
            }
            rm.add(s);
        }
        return rm;
    }

    private static List<ParsingDetail> initializeParsingDetails() {
        int len = 20;
        List<ParsingDetail> rm = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            ParsingDetail pd1 = new ParsingDetail();
            pd1.setParsingDetailsId(UUID.randomUUID());
            pd1.setParsingDateTime(new DateTime());
            pd1.setParser(ParserType.AXM);
            pd1.setParserVersion("0.24");
            rm.add(pd1);
        }
        return rm;
    }

    private static Map<String, String> addOutcomes(String type) {
        Map<String, String> rm = new HashMap<>();
        try {
            int line_count = 0;
            List<String> lines = FileUtils.readLines(outcomesFile);
            for (String l : lines) {
                //skip the first line
                if (line_count == 0) {
                    line_count++;
                    continue;
                }

                String[] lineArr = l.trim().split("\\s*,\\s*");
                if (type.equals("ROUTE")) {
                    rm.put(lineArr[0], lineArr[4]);
                } else if (type.equals("OUTPUT_REPORT")) {
                    rm.put(lineArr[0], lineArr[5]);
                }
                line_count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rm;
    }

    @Before
    public void setUp() {
        //create a patient
        aPatient = new Patient();
        aPatient.setPatientId(UUID.randomUUID());
        aPatient.setSources(someSources);
        aPatient.setParsingDetails(someParsingDetails);
        File claimF = getFileFromPath(claimFilterFileName);
        try {
            claimFilterIs = FileUtils.openInputStream(claimF);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        aPatient = null;
        claimFilterIs = null;
    }
/*
    @Test
    public void testingClaimwithMultipleErrors(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("OVERPAYMENT_ID_ERROR_CODE", "105");
        p1Md.put("DETAIL_NUMBER_ERROR", "502");
        p1Md.put("PATIENT_HIC_NUMBER_ERROR", "500");
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int known_count =0;
        int error_count = 0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_KNOWN")) {
                        known_count++;
                    }
                    if(at.getValue().equals("CMS_ERROR")){
                        error_count++;
                    }
                }
            }
        }
        assertEquals(0, known_count);
        assertEquals(1, error_count);
    }

    @Test
    public void testingClaimWithNoErrors(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_known_count =0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_KNOWN")) {
                        cms_known_count++;
                    }
                }
            }
        }
        assertEquals(1, cms_known_count);
    }


    @Test
    public void testingClaimWithNoErrorsAndADeleteFlag(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("DELETE_INDICATOR", "TRUE");
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_known_count =0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_KNOWN")) {
                        cms_known_count++;
                    }
                }
            }
        }
        assertEquals(1, cms_known_count);
    }


    @Test
    public void testingClaimWithOneError(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("OVERPAYMENT_ID_ERROR_CODE", "310");
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_error_count =0;
        int cms_known_count =0 ;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getName().equals("CMS_KNOWN")){
                        cms_known_count++;
                    }
                    if(at.getValue().equals("CMS_ERROR")) {
                        cms_error_count++;
                    }
                }
            }
        }
        assertEquals(1, cms_error_count);
        assertEquals(0, cms_known_count);
    }


    @Test
    public void testingClaimWithOneErrorAndDeleteIndicator(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("OVERPAYMENT_ID_ERROR_CODE", "310");
        p1Md.put("DELETE_INDICATOR", "TRUE");
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_error_count =0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_ERROR")) {
                        cms_error_count++;
                    }
                }
            }
        }
        assertEquals(1, cms_error_count);
    }


    @Test
    public void testingClaimWithOneErrorAndDeleteIndicatorButSubtract(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("PATIENT_HIC_NUMBER_ERROR", "999");
        p1Md.put("OVERPAYMENT_ID_ERROR_CODE", "310");
        p1Md.put("DELETE_INDICATOR", "TRUE");
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_known_count =0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_KNOWN")) {
                        cms_known_count++;
                    }
                }
            }
        }
        assertEquals(1, cms_known_count);
    }


    @Test
    public void testingClaimWithTwoErrors(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("OVERPAYMENT_ID_ERROR_CODE", "310");
        p1Md.put("DETAIL_NUMBER_ERROR", "999");
        p1.setMetadata(p1Md);
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_error_count =0;
        int cms_known_count = 0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_ERROR")) {
                        cms_error_count++;
                    }
                    if(at.getValue().equals("CMS_KNOWN")){
                        cms_known_count ++;
                    }
                }
            }
        }
        assertEquals(0, cms_error_count);
        assertEquals(1, cms_known_count);
    }

    @Test
    public void testingClaimWithOneErrorShouldSayCMS_KNOWN(){
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("DETAIL_NUMBER_ERROR", "999");
        p1.setMetadata(p1Md);
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(1, events.size());

        int cms_error_count =0;
        int cms_known_count = 0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_ERROR")) {
                        cms_error_count++;
                    }
                    if(at.getValue().equals("CMS_KNOWN")){
                        cms_known_count ++;
                    }
                }
            }
        }
        assertEquals(0, cms_error_count);
        assertEquals(1, cms_known_count);
    }


    @Test
    public void multipleTestsAtOnce() {
        Map<String, String> p1Md = new HashMap<>();
        ParsingDetail pd = null;
        Random generator = new Random();

        //create some problems
        List<Problem> some_problems = new ArrayList<>();
        Problem p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        //get a source from someSomesources that has CMS_KNOWN
        UUID u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        //testing multiple errors
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p1Md.put("OVERPAYMENT_ID_ERROR_CODE", "105");
        p1Md.put("DETAIL_NUMBER_ERROR", "107");
        p1Md.put("PATIENT_HIC_NUMBER_ERROR", "500");
        p1.setMetadata(p1Md);
        some_problems.add(p1);

        Map<String, String> p3Md = new HashMap<>();
        p1 = new Problem();
        p1.setStartDate(new DateTime());
        p1.setEndDate(new DateTime());
        u = getRandomSourceForProblem(true);
        p1.setSourceId(u);
        pd = someParsingDetails.get(generator.nextInt(someParsingDetails.size()));
        p1.setParsingDetailsId(pd.getParsingDetailsId());
        p3Md.put("PATIENT_HIC_NUMBER_ERROR", "418");
        p1.setMetadata(p3Md);
        some_problems.add(p1);

        aPatient.setProblems(some_problems);

       // aPatient.setMetadata(someMetadata);
        //now create some events
        List<com.apixio.model.event.EventType> events = createEventsFromOldAPO(aPatient, claimFilterString);
        assertEquals(2, events.size());
        int error_count =0;
        for(com.apixio.model.event.EventType et:events){
            for(AttributeType at : et.getAttributes().getAttribute()){
                if(at.getName().equals("sourceType")){
                    if(at.getValue().equals("CMS_ERROR")) {
                        error_count++;
                    }
                }
            }
        }
        assertEquals(2, error_count);
    }
*/


}
