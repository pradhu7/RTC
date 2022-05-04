package com.apixio.model.converter.utils;

import com.apixio.XUUID;
import com.apixio.model.blob.BlobType;
import com.apixio.model.converter.exceptions.OldToNewAPOConversionException;
import com.apixio.model.converter.implementations.NewDate;
import com.apixio.model.patient.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.*;

/**
 * Created by jctoledo on 3/11/16.
 */
public class ConverterUtils {

    public static final Map<String, String> ENCOUNTER_CLAIM_TYPE;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("O", "HOSPITAL_OUTPATIENT");
        aMap.put("I", "HOSPITAL_INPATIENT");
        aMap.put("P", "PROFESSIONAL");
        ENCOUNTER_CLAIM_TYPE = Collections.unmodifiableMap(aMap);
    }

    /**
     * ENCOUNTER SWITCH TYPE STATIC MAP TO BE USED FOR MAO-004 files
     */
    public static final Map<String, String> ENCOUNTER_TYPE_SWITCH;
    private static final Logger logger = Logger.getLogger(ConverterUtils.class);

    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("1", "ORIGINAL_ENCOUNTER");
        aMap.put("2", "VOID_TO_ORIGINAL_ENCOUNTER");
        aMap.put("3", "REPLACEMENT_TO_AN_ORIGINAL_ENCOUNTER");
        aMap.put("4", "LINKED_CHART_REVIEW");
        aMap.put("5", "VOID_TO_A_LINKED_CHART_REVIEW");
        aMap.put("6", "REPLACEMENT_TO_A_LINKED_CHART_REVIEW");
        aMap.put("7", "UNLINKED_CHART_REVIEW");
        aMap.put("8", "VOID_TO_AN_UNLINKED_CHART_REVIEW");
        aMap.put("9", "REPLACEMENT_TO_AN_UNLINKED_CHART_REVIEW");
        ENCOUNTER_TYPE_SWITCH = Collections.unmodifiableMap(aMap);
    }

    public static ClinicalActor getClinicalActorFromIterable(UUID needle, Iterable<ClinicalActor> haystack) {
        for (ClinicalActor ca : haystack) {
            if (ca.getClinicalActorId().equals(needle)) {
                return ca;
            }
        }
        logger.debug("Null Clinical Actor object returned with this needle: \n" + needle);
        return null;
    }



    /**
     * Evaluate the Problem object to assess whether it corresponds to a Standard RAPS risk adjusted insurance claim
     * @param p a problem object
     * @return true if problem corresponds to a standard RAPS claim
     */
    public static boolean isStandardRAPSClaim(Problem p){
        String ct = p.getMetaTag("CLAIM_TYPE");
        if(ct != null && ct.toUpperCase().equalsIgnoreCase("RISK_ADJUSTMENT_CLAIM")){
            if(p.getOriginalId() != null){
                ExternalID ei = p.getOriginalId();
                if(ei.getAssignAuthority() != null && ei.getAssignAuthority().toUpperCase().contains("APIXIO_RAPS_PARSER")){
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }



    /**
     * Iterate over the metadata of a problem object to return a list with all of the values of keys that contain the word 'error'
     *
     * @param someProblemMetadata a metadata map for a problem object
     * @return a list of all error codes found in this metadata list
     */
    public static List<String> getListOfErrorCodesFromMetadata(Map<String, String> someProblemMetadata) {
        List<String> rm = new ArrayList<>();
        for (Map.Entry<String, String> entry : someProblemMetadata.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key != null && key.toUpperCase().contains("_ERROR")) {
                //this value is the one that has an error code
                if (val != null && !rm.contains(val)) {
                    rm.add(val);
                }
            }
        }
        return rm;
    }


    /**
     * Iterate over the metadata of a problem object to return true if one of keys that contain the word 'delete'
     *
     * @param someProblemMetadata a metadata map for a problem object
     * @return true if a delete indicator is found, false otherwise
     */
    public static boolean hasRAPSDeleteIndicator(Map<String, String> someProblemMetadata) {

        for (Map.Entry<String, String> entry : someProblemMetadata.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key != null && key.toUpperCase().equals("DELETE_INDICATOR")) {
                //this value is the one that has an error code
                if (val != null) {
                    return Boolean.parseBoolean(val);
                }
            }
        }
        return false;
    }


    /**
     * Evaluate the Problem object to assess whether it corresponds to a NON Standard RAPS risk adjusted insurance claim
     * @param p a problem object
     * @return true if problem corresponds to a NON standard RAPS claim
     */
    public static boolean isNonStandardRAPSClaim(Problem p){
        String ct = p.getMetaTag("CLAIM_TYPE");
        if (ct != null && ct.toUpperCase().equals("RISK_ADJUSTMENT_CLAIM")){
            if(p.getOriginalId() != null){
                ExternalID ei = p.getOriginalId();
                if(ei.getAssignAuthority() != null && !ei.getAssignAuthority().toUpperCase().contains("APIXIO_RAPS_PARSER")){
                    return true;
                }
            } else {
                //no original id was found
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluate the Problem object to assess whether it corresponds to an MAO-004 risk adjusted insurance claim
     * @param p a Problem object
     * @return true if Problem corresponds to a NON standard RAPS claim
     */
    public static boolean isMAO004Claim(Problem p){
        String ct = p.getMetaTag("CLAIM_TYPE");
        if(ct != null && ct.toUpperCase().equals("MAO_004_ENCOUNTER_CLAIM")){
            if(p.getOriginalId() !=null){
                ExternalID ei = p.getOriginalId();
                if(ei.getAssignAuthority() != null && ei.getAssignAuthority().toUpperCase().contains("MAO_004_PARSER")){
                    return true;
                } else {
                    //no original id was found
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Evaluate the Procedure object to assess whether it corresponds to an Fee for service insurance claim
     * @param p a procedure object
     * @return true if procedure corresponds to a Fee for service insurance claim
     */
    public static boolean isFeeForServiceClaim(Procedure p){
        Map<String, String> metadata = p.getMetadata();
        for (String key: metadata.keySet()){
            String value = metadata.get(key);
            if (key.toUpperCase().equals("CLAIM_TYPE")){
                if(value != null && value.toUpperCase().contains("FEE_FOR_SERVICE_CLAIM")){
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Return the Source from the haystack that matches the needle UUID
     *
     * @param haystack the iterable to search
     * @param needle   the uuid of the object to find
     * @return the Source object
     */
    public static Source getSourceFromIterable(UUID needle, Iterable<Source> haystack) {
        for (Source s : haystack) {
            if (s.getInternalUUID().equals(needle)) {
                return s;
            }
        }
        logger.debug("Null source object returned with this needle: \n" + needle);
        return null;
    }

    /**
     * evaluate if the problem object is a MAO-004 risk adjustment insurance claim
     *
     * @param p An old apo problem object
     * @return true if the @param is a MAO-004 ra claim
     */
    public static Boolean isMAORAClaim(Problem p) {
        Map<String, String> m = p.getMetadata();
        List<String> keyList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : m.entrySet()) {
            String k = entry.getKey();
            if (k.equalsIgnoreCase("MA_CONTRACT_ID") || k.equalsIgnoreCase("ENCOUNTER_ICN") ||
                    k.equalsIgnoreCase("ENCOUNTER_TYPE_SWITCH") || k.equalsIgnoreCase("ORIGINAL_ENCOUNTER_ID") ||
                    k.equalsIgnoreCase("PLAN_SUBMISSION_DATE") || k.equalsIgnoreCase("PROCESSING_DATE") ||
                    k.equalsIgnoreCase("ENCOUNTER_CLAIM_DATE") || k.equalsIgnoreCase("ADD_OR_DELETE_FLAG")) {
                keyList.add(k);
            }
        }
        if (keyList.size() > 1) {
            return true;
        }
        return false;
    }


    /**
     * evaluate if the problem object is a RAPS risk adjustment insurance claim
     *
     * @param p An old apo problem object
     * @return true if the @param is a RAPS ra claim
     */
    public static Boolean isRAPSClaim(Problem p) {
        Map<String, String> m = p.getMetadata();
        List<String> keyList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : m.entrySet()) {
            String k = entry.getKey();
            if (k.equalsIgnoreCase("DELETE_INDICATOR") || k.equalsIgnoreCase("DIAGNOSIS_CLUSTER_ERROR1") ||
                    k.equalsIgnoreCase("DIAGNOSIS_CLUSTER_ERROR2") || k.equalsIgnoreCase("TRANSACTION_DATE") ||
                    k.equalsIgnoreCase("PLAN_NUMBER") || k.equalsIgnoreCase("FILE_MODE") ||
                    k.equalsIgnoreCase("PAYMENT_YEAR") || k.equalsIgnoreCase("PAYMENT_YEAR_ERROR") ||
                    k.equalsIgnoreCase("DETAIL_NUMBER_ERROR") || k.equalsIgnoreCase("PATIENT_CONTROL_NUMBER") ||
                    k.equalsIgnoreCase("PATIENT_HIC_NUMBER_ERROR") || k.equalsIgnoreCase("CORRECTED_HIC_NUMBER") ||
                    k.equalsIgnoreCase("PATIENT_DATE_OF_BIRTH") || k.equalsIgnoreCase("PATIENT_DATE_OF_BIRTH_ERROR") ||
                    k.equalsIgnoreCase("PROVIDER_TYPE") || k.equalsIgnoreCase("RISK_ASSESSMENT_CODE_CLUSTERS") ||
                    k.equalsIgnoreCase("OVERPAYMENT_ID") || k.equalsIgnoreCase("OVERPAYMENT_ID_ERROR_CODE")) {
                keyList.add(k);
            }
        }
        if (keyList.size() > 1) {
            return true;
        }
        return false;
    }

    /**
     * Get the source system of a particular Base Object
     *
     * @param aSourceId
     * @param sources
     * @return
     */
    public static String getSourceSystem(UUID aSourceId, Iterable<Source> sources) {
        String rm = "";
        Source s = getSourceFromIterable(aSourceId, sources);
        if (s != null) {
            if (s.getSourceSystem() != null) {
                rm = s.getSourceSystem();
            }
        } else {
            return null;
        }
        return rm;
    }

    /**
     * Retrieve old APO procedures from a patient that are in fact procedures and not FFS claims
     *
     * @param candidates a list of potential old apo procedure objects
     * @param sources    a source iterable
     * @return a collection of Procedures that are not ffs claims
     */
    public static List<Procedure> getOnlyProcedures(Iterable<Procedure> candidates, Iterable<Source> sources) {
        List<Procedure> rm = new ArrayList<>();
        for (Procedure p : candidates) {
            UUID pSourceId = p.getSourceId();
            Source s = getSourceFromIterable(pSourceId, sources);
            if (s == null) {
                logger.debug("No Source found for procedure : \n" + p.getInternalUUID());
                rm.add(p);
                continue;
            }
            if (s.getSourceType() == null || !s.getSourceType().toUpperCase().contains("CLAIM")) {
                rm.add(p);
            }
            if(p.getMetadata() != null) {
                if (!p.getMetadata().containsKey("CLAIM_TYPE")) {
                    if(!rm.contains(p)){
                        rm.add(p);
                    }
                }
            }
        }
        if (rm.size() == 0) {
            logger.debug("No procedures (not claims!) found here ");
        }
        return rm;
    }

    /**
     * Identify problems that have a metadata field "ISFFS" which has as a value of TRUE - these correspond to ffs claims
     * @param candidates a list of problems
     * @return a list of problem objects that have CMS_KNOWN as a value in their Sources' source type instance variable
     */
    public static List<Procedure> getFFSCandidatesFromProcedures(Iterable<Procedure> candidates) {
        List<Procedure> rm = new ArrayList<>();
        for (Procedure p : candidates) {
            String ct = p.getMetaTag("CLAIM_TYPE");
            if(ct != null && ct.toUpperCase().equals("FEE_FOR_SERVICE_CLAIM")){
                rm.add(p);
            }
        }
        if (rm.size() == 0) {
            logger.debug("No FFS claim Procedure found here ");
        }
        return rm;
    }


    /**
     * Identify problems that have CMS_KNOWN as a value in their Sources' source type instance variable
     *
     * @param candidates an iterable of problems to check
     * @return a list of procedures that have CMS_KNOWN as a value in their Sources' source type instance variable
     */
//    public static List<Problem> getRAClaimCandidatesFromProblems(Iterable<Problem> candidates) {
//        List<Problem> rm = new ArrayList<>();
//        List<Problem> nonRA = new ArrayList<>();
//        for (Problem p : candidates) {
//            String ct = p.getMetaTag("CLAIM_TYPE");
//
//            if(ct != null && (ct.toUpperCase().equals("RISK_ADJUSTMENT_CLAIM") || ct.toUpperCase().equals("MAO_004_ENCOUNTER_CLAIM"))){
//                rm.add(p);
//            } else {
//                nonRA.add(p);
//                logger.warn("Non RA problem with id : "+p.getInternalUUID());
//            }
//            if (rm.size() == 0) {
//                logger.warn("No RA claim Problems found here ");
//
//            }
//        }
//        return rm;
//    }

    public static List<Problem> getRAClaimCandidatesFromProblems(Iterable<Problem> candidates, Iterable<Source> sources) {
        List<Problem> rm = new ArrayList<>();
        for (Problem p : candidates) {
            UUID pSourceId = p.getSourceId();
            Source s = getSourceFromIterable(pSourceId, sources);
            if (s == null) {
                logger.debug("No Source found for problem : \n" + p.getInternalUUID());
                continue;
            }
            if (s.getSourceType() != null) {
                if (s.getSourceType().equalsIgnoreCase("CMS_KNOWN")) {
                    rm.add(p);
                }
            }
        }
        return rm;
    }

    public static Collection<XUUID> createXUUIDCollectionFromUUIDList(List<UUID> someUUIDs) {
        Collection<XUUID> rm = new ArrayList<>();
        for (UUID u : someUUIDs) {
            if (u != null) {
                XUUID xu = XUUID.fromString(u.toString());
                rm.add(xu);
            }
        }
        return rm;
    }

    /**
     * Create an XUUID from a Base Object's Internal UUID
     *
     * @param bo a base object from the old apo
     * @return an "internal" XUUID
     */
    public static XUUID createXUUIDFromInternalUUID(BaseObject bo) {
        if (bo != null) {
            if (bo.getInternalUUID() != null) {
                return XUUID.fromString(bo.getInternalUUID().toString());
            } else {
                System.out.println("invalid internal Id: \n" + bo);
                throw new OldToNewAPOConversionException("No internal id found for " + bo.getClass() + " : " + bo.getInternalUUID());
            }
        } else {
            logger.debug("returning random XUUID! ");
            return XUUID.fromString(UUID.randomUUID().toString());
        }
    }

    public static com.apixio.model.owl.interfaces.Date createApixioDate(DateTime dt) {
        NewDate rm = new NewDate();
        XUUID id = XUUID.create("NewDate");
        rm.setInternalXUUID(id);
        if (dt != null) {
            rm.setDateValue(dt);
        } else {
            logger.debug("Datetime with a null value was found! :\n");
        }
        //TODO -check that I am dealing with timezones correctly with Vishnu
        String timezoneid = dt.getChronology().getZone().getID();
        if (timezoneid != null) {
            rm.setTimeZone(timezoneid);
        }
        return rm;
    }


    //TODO write an add provenance method with all auxiliary methods as well

    /**
     * Retrieve old APO problems form a patient that are in fact problems and not RA claims
     *
     * @param candidates a list of potential old apo problem objects
     * @param sources    a source iterable
     * @return a collection of problems that are not ra claims
     */
    public static List<Problem> getOnlyProblems(Iterable<Problem> candidates, Iterable<Source> sources) {
        List<Problem> rm = new ArrayList<>();
        for (Problem p : candidates) {
            UUID pSourceId = p.getSourceId();
            Source s = getSourceFromIterable(pSourceId, sources);
            if (s == null) {
                logger.debug("No Source found for problem : \n" + p.getInternalUUID());
                rm.add(p);
                continue;
            }
            if (s.getSourceType() == null || !s.getSourceType().equalsIgnoreCase("CMS_KNOWN") || s.getSourceType().length() == 0) {
                rm.add(p);
            }
        }
        if (rm.size() == 0) {
            logger.debug("No problems (not claims!) found here ");
        }
        return rm;
    }

    /**
     * Retrieve the logical identifier for a page
     *
     * @param docuuid    a document uuid
     * @param imageType  an image type - typically png and jpg
     * @param pageNumber the number of the page
     * @return the logical identifier of the page
     */
    public static String getPageID(UUID docuuid, String imageType, int pageNumber) {
        BlobType bt = new BlobType.Builder(docuuid, imageType).pageNumber(pageNumber).build();
        return bt.getModelID();
    }

    public static ParsingDetail getParsingDetailFromIterable(UUID needle, Iterable<ParsingDetail> haystack) {
        if (needle != null && haystack != null) {
            for (ParsingDetail p : haystack) {
                if (p.getParsingDetailsId().equals(needle)) {
                    return p;
                }
            }
        }
        logger.debug("Null parsing detail object returned with this needle: \n" + needle);
        return null;
    }


}
