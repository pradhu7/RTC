package com.apixio.model.converter.bin.converters;

import com.apixio.XUUID;
import com.apixio.model.converter.exceptions.InsuranceClaimException;
import com.apixio.model.converter.exceptions.RDFModelToEventTypeException;
import com.apixio.model.converter.implementations.NewExternalIdentifier;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.utils.SPARQLUtils;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.apixio.model.event.*;
import com.apixio.model.event.transformer.EventTypeAttributesBuilder;
import com.apixio.security.Security;
import com.apixio.security.exception.ApixioSecurityException;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jctoledo on 4/19/16.
 */
public class RDFModelToEventType implements Closeable {
    private static final Logger logger = Logger.getLogger(RDFModelToEventType.class);
    private static final String VOCABULARY_VERSION = "0.0.1";
    private List<EventType> eventTypeList = null;
    private RDFModel model = null;
    private String claimRulesStr = null;
    private Set<String> hashes = new HashSet<>();
    /**
     * A map where the key is a codable entity (or insurance claimable event ) class from ALO and the value
     * is its corresponding rdfs:label
     */
    private Map<Resource, String> codedEntityClassesAndLabels = null;
    /**
     * A map where the key is the resource corresponding to a coded entity (or insurance claimable event) class
     * and the value is a list of all corresponding instances in this graph
     */
    private Map<Resource, List<Resource>> codedEntityInstances = null;
    /**
     * A map where the key is a OWL class resource found in this model and the value is its corresponding label
     */
    private Map<Resource, String> instanceClassAndLabels = null;



    public RDFModelToEventType(RDFModel nam, String claim_rules_filter) {
        this();
        this.model = nam;
        this.claimRulesStr = claim_rules_filter;
        initialize();
        createEventTypes();
    }


    public RDFModelToEventType() {
        model = new RDFModel();
        eventTypeList = new ArrayList<>();
    }

    private void initialize() {
        initializeReasoner();
        populateInstanceClassList();
        populateCodedEntityClassList();
        populateCodedEntityInstanceMap();
    }


    /**
     * Creates an instance of a reasoner from a provided rule file and sets this model's reasoner
     */
    private void initializeReasoner()  {
        try {
            if (this.claimRulesStr != null && this.claimRulesStr.length()>0) {
                List<Rule> rules = Rule.parseRules(this.claimRulesStr);
                GenericRuleReasoner reasoner = (GenericRuleReasoner)GenericRuleReasonerFactory.theInstance().create(null);
                reasoner.setRules(rules);
                reasoner.setMode(GenericRuleReasoner.FORWARD);
                this.model.setReasoner(reasoner);
            } else {
                throw new RDFModelToEventTypeException("Could not find or open reasoner string! ");
            }
        } catch (Exception e) {
            throw new RDFModelToEventTypeException("Could not initialize jena reasoner !" +e.getStackTrace());
        }
    }

    private void createEventTypes() {
        for (Map.Entry<Resource, List<Resource>> entry : codedEntityInstances.entrySet()) {
            List<Resource> instances = entry.getValue();
            for (Resource instance : instances) {
                List<EventType> events = createEventList(instance);
                if(events != null && events.size()>0){
                    this.eventTypeList.addAll(events);
                }else{
                    logger.debug("could not make any events for this instance :"+instance);
                    continue;
                }
            }
        }
    }

    /**
     * Create a list of all classes that have been instanciated in this APO model
     */
    private void populateInstanceClassList() {
        String q = "SELECT distinct ?c  ?lbl" +
                " WHERE { " +
                " ?s rdf:type ?c . " +
                " optional {?c rdfs:label ?lbl.}" +
                "}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(model, q);
        instanceClassAndLabels = new HashMap<>();
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            Resource r = sol.getResource("c");
            String l = "";
            try {
                l = sol.getLiteral("lbl").getLexicalForm();
            } catch (NullPointerException e) {
                l = "";
            }
            instanceClassAndLabels.put(r, l);
        }
    }

    /**
     * Get a list of all owl:classes that are rdfs:subclassesOf  alo:CodedEntity
     */
    private void populateCodedEntityClassList() {
        //I need to add InsuranceClaimableEvent to the result set
        OntModel o = model.getOntology();
        String q = "SELECT distinct ?c ?l" +
                " WHERE {  " +
                " ?c rdfs:subClassOf* <" + OWLVocabulary.OWLClass.CodedEntity.uri() + "> ." +
                " ?c rdfs:label ?l .  " +
                "}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(o, q);
        codedEntityClassesAndLabels = new HashMap<>();
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            if (!codedEntityClassesAndLabels.containsKey(sol.getResource("c"))) {
                codedEntityClassesAndLabels.put(sol.getResource("c"), sol.getLiteral("l").getLexicalForm());
            }
        }
        if (codedEntityClassesAndLabels.size() == 0) {
            logger.error("could not find classes from alo! ");
            throw new RDFModelToEventTypeException("could not find classes from alo! ");
        }
    }

    /**
     * Query the assertional layer ontology for all instances of either coded entities
     * Populates a list of Resources with the corresponding named individuals
     */
    private void populateCodedEntityInstanceMap() {
        codedEntityInstances = new HashMap<>();
        for (Map.Entry<Resource, String> entry : codedEntityClassesAndLabels.entrySet()) {
            Resource r = entry.getKey();
            String label = entry.getValue();
            // add optional for insurance claimable event
            String q2 = "SELECT ?a WHERE {" +
                    " ?a rdf:type <" + r.getURI() + "> . " +
                    "}";
            ResultSet rs2 = SPARQLUtils.executeSPARQLSelect(model, q2);
            while (rs2.hasNext()) {
                QuerySolution sol2 = rs2.next();
                if (codedEntityInstances.containsKey(r)) {
                    List<Resource> instances = codedEntityInstances.get(r);
                    if (!instances.contains(sol2.getResource("a"))) {
                        instances.add(sol2.getResource("a"));
                    }
                    codedEntityInstances.put(r, instances);
                } else {
                    List<Resource> l = new ArrayList<>();
                    l.add(sol2.getResource("a"));
                    codedEntityInstances.put(r, l);
                }
            }
        }
        if (codedEntityInstances.size() == 0) {
            logger.debug("could not find any instances of alo codable entities!");
        }
    }



    private List<EventType> createEventList(Resource res){
        List<EventType> rmList = new ArrayList<>();

        Security encryptor = Security.getInstance();

        boolean isRAPS = isRAPSClaim(res);
        boolean isMAO = isMAOClaim(res);
        boolean isFFS = isFFSClaim(res);

        List<Boolean> boolz = new ArrayList<>();
        boolz.add(isFFS); boolz.add(isMAO);
        boolz.add(isRAPS);
        int trueCount = 0;
        int falseCount = 0;
        for (Boolean b:boolz){
            if(b){
                trueCount ++;
            }else{
                falseCount ++;
            }
        }

        if (falseCount != 2 && trueCount != 1){
            String msg = "Found an Insurance Claim (problem APO) that cannot be uniquely identified as being one of (FFS, RAPS or MAO)! "
                    +" with patient uuid : "+this.model.getPatientXUUID();

            logger.debug(msg);
            return rmList;
            //throw new InsuranceClaimException(msg);
        }


        List<Resource> types = SPARQLUtils.getRDFTypes(this.model, res);
        String originalEncounterICN = null;
        if (isMAO){
            originalEncounterICN = getOriginalEncounterICN(res);
            if(originalEncounterICN != null && originalEncounterICN.length()>0){
                try {
                    originalEncounterICN = encryptor.encrypt(originalEncounterICN);
                } catch (ApixioSecurityException e) {
                    throw new RDFModelToEventTypeException(e);
                }
            }
        }


        boolean claimableEventFlag = false, routeToCoder = false, showInOutputReport = false, deleteIndicator = false, hasErrorCode = false, addIndicator = false;
        String resRDFLabel = "";
        String errorCodes = "";
        String res_type = "";
        Resource claim_dx, claim_type, claim;
        claim_dx = claim_type  = null;

        String claim_xuuid, claim_dpd_xuuid, claim_last_edit_date_val, transactionDate,encounterICN,
                claim_start_date_val, claim_end_date_val, claim_source_xuuid, source_system, patEncSwTypeVal,
                claim_orig_id_val, claimOriginalIDAssignAuthority, claim_source_encounter_xuuid, claim_ra_type_val, parent_source_id,

                code_value, code_display_name, code_source_xuuid, coding_system_version,
                oid, problem_name, code_dpd_xuuid, code_xuuid, claim_source_type_val,
                cs_display_name, code_last_edit_date_value,
                code_source_encounter_id, code_dpd_id;

        claim_xuuid = claim_dpd_xuuid = claim_last_edit_date_val = code_source_xuuid = patEncSwTypeVal = transactionDate=
                claim_start_date_val = claim_end_date_val = claim_source_xuuid = source_system = claim_ra_type_val =
                        claim_orig_id_val = claimOriginalIDAssignAuthority = claim_source_encounter_xuuid = parent_source_id=

                                code_value = code_display_name = coding_system_version = claim_source_type_val=
                                        oid = code_dpd_id = code_xuuid = problem_name = code_dpd_xuuid =
                                                cs_display_name = code_last_edit_date_value = code_source_encounter_id = claim_xuuid = claimOriginalIDAssignAuthority = "";



        if ((isRAPS || isMAO || isFFS) && types !=null){
            String claim_query ="select distinct ?claim ?addIndicator ?deleteIndicator ?claim_xuuid ?claim_dpd_xuuid " +
                    " ?claim_last_edit_date_val ?claim_start_date_val ?claim_processing_date_val " +
            " where {\n" +
            "  ?claim <" + OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n";
            claim_query +="  ?claim <" + OWLVocabulary.OWLDataProperty.HasXUUID.property() + "> ?claim_xuuid . \n" +
            "  ?claim <" + OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property() + "> ?dpd. \n" +
            "  ?dpd <" + OWLVocabulary.OWLDataProperty.HasXUUID.property() + "> ?claim_dpd_xuuid . \n" +
            "  ?claim <" + OWLVocabulary.OWLObjectProperty.HasLastEditDate.property() + "> ?claim_last_edit_date. \n " +
            "  ?claim_last_edit_date <" + OWLVocabulary.OWLDataProperty.HasDateValue.property() + "> ?claim_last_edit_date_val . \n" +
            "  ?claim <" + OWLVocabulary.OWLObjectProperty.HasDateOfServiceRange.property() + "> ?dos_range .\n" +
            "  ?dos_range <" + OWLVocabulary.OWLObjectProperty.HasStartDate.property() + "> ?claim_start_date .\n " +
            "  ?claim_start_date <" + OWLVocabulary.OWLDataProperty.HasDateValue.property() + "> ?claim_start_date_val .\n" ;
            if (isMAO || isFFS) {
                claim_query+="  optional{ ?claim <" + OWLVocabulary.OWLObjectProperty.HasInsuranceClaimProcessingDate.property() + "> ?processing_date .\n" +
                        "?processing_date <" + OWLVocabulary.OWLDataProperty.HasDateValue.property() + "> ?claim_processing_date_val.}\n" ;
            } else if (isRAPS){
                claim_query += " optional{ ?claim <"+OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property()+"> ?cmsValidation. \n" +
                        "?cmsValidation <"+OWLVocabulary.OWLObjectProperty.HasValidatedClaimTransactionDate.property() + "> ?claim_processing_date .\n" +
                        "?claim_processing_date <"+OWLVocabulary.OWLDataProperty.HasDateValue.property()+"> ?claim_processing_date_val .}";
            }

            claim_query += "} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, claim_query);
            long prevIteration = -1;
            while(rs.hasNext()){
                if (prevIteration != -1){
                    Long diff = System.currentTimeMillis() - prevIteration;
                    logger.debug("Took "+diff.toString()+" ms to retrieve result set");
                }
                QuerySolution sol = rs.next();
                claim = SPARQLUtils.getQuerySolutionVariableResource(sol, "claim");
                if (isMAO) {
                    patEncSwTypeVal = getPatientEncounterSwitchTypeValue(claim);
                }
                List<NewExternalIdentifier> otherOriginalIds = getOtherOriginalIds(claim);
                if(isRAPS){
                    transactionDate = getTransactionDate(claim, true);
                }else {
                    transactionDate = getTransactionDate(claim, false);
                }

                claim_xuuid = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_xuuid");
                claim_dpd_xuuid = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_dpd_xuuid");
                claim_last_edit_date_val = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_last_edit_date_val");
                claim_start_date_val = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_start_date_val");
                claim_end_date_val = getClaimEndDateValue(claim);
                claim_orig_id_val = getClaimOriginalIdValue(claim);
                claimOriginalIDAssignAuthority = getClaimOriginalIdAssignAuthority(claim);
                deleteIndicator = getClaimDeleteIndicator(claim);
                addIndicator = getClaimAddIndicator(claim);
                claim_source_type_val = getClaimSourceTypeValue(claim);
                XUUID pid = this.model.getPatientXUUID();
                UUID patUUID = XUUID.fromXUUID(pid.toString(), '\0');

                try {
                    claim_orig_id_val = encryptor.encrypt(claim_orig_id_val, "");
                } catch (ApixioSecurityException e) {
                    throw new RDFModelToEventTypeException(e);
                }

                if (isFFS && claim_source_type_val == ""){
                    claim_source_type_val = "INSURANCE_CLAIM";
                }

                ReferenceType patientRT = new ReferenceType();
                patientRT.setType("patient");
                patientRT.setUri(patUUID.toString());

                /*****/
                //prepare the fact type for a provider
                FactType provFt = null;
                if(isRAPS|| isFFS || isMAO){
                    Resource provTypeRes = getProviderType(claim);
                    if (provTypeRes != null){
                        provFt = makeFactTypeFromClinicalCode(provTypeRes, claim_start_date_val, claim_end_date_val);
                    } else {
                        if(isRAPS) {
                            String msg = "found raps claim without provider type! claim :" + claim + " patient :" + this.model.getPatientXUUID();
                            msg += " using default value for Provider type ! ";
                            logger.debug(msg);
                        }else if(isFFS) {
                            String msg = "found ffs claim without provider type! claim :" + claim + " patient :" + this.model.getPatientXUUID();
                            logger.debug(msg);
                        } else if(isMAO){
                            String msg = "found MAO claim without provider type! claim :" + claim + " patient :" + this.model.getPatientXUUID();
                            logger.debug(msg);
                        }
                    }
                }
                //prepare the fact type for a bill type
                FactType billFt = null;
                if(isFFS){
                    Resource billTypeRes = getBillType(claim);
                    if(billTypeRes != null){
                        billFt = makeFactTypeFromClinicalCode(billTypeRes, claim_start_date_val, claim_end_date_val);
                    } else {
                        String msg ="found ffs claim without bill type! claim :"+claim+" patient :"+this.model.getPatientXUUID();
                        logger.debug(msg);
                    }
                }

                List<FactType> procedureFactTypes = null;
                if (isFFS){
                    List <Resource> procedures =getProcedures(claim);
                    if (procedures != null){
                        procedureFactTypes = new ArrayList<>();
                        for(Resource r : procedures){
                            FactType ap = makeFactTypeFromClinicalCode(r, claim_start_date_val, claim_end_date_val);
                            if (ap != null) {
                                procedureFactTypes.add(ap);
                            }
                        }
                    }else{
                        String msg ="found ffs claim without procedures:"+claim+" patient :"+this.model.getPatientXUUID();
                        logger.debug(msg);
                    }
                }

                if(isMAO || isRAPS || isFFS){
                    List<Resource> ccs = getRAClaimsDiagnosisCodes(claim);
                    ReferenceType sourceT = new ReferenceType();
                    ReferenceType eSourceT = new ReferenceType();
                    AttributeType transactionType = new AttributeType();
                    String renderingProviderId = null;
                    if (isFFS) {
                        renderingProviderId = getRenderingProvider(claim);
                        if (renderingProviderId == null) {
                            String msg = "found ffs claim without a rendering provider! " + claim;
                            logger.debug(msg);
                        }
                    }

                    for (Resource f : ccs){
                        EventType et = new  EventType();
                        FactType ft = makeFactTypeFromClinicalCode(f, claim_start_date_val, claim_end_date_val);
                        if (ft != null) {
                            et.setFact(ft);


                            if (isMAO || isRAPS) {
                                if (isRAPS) {
                                    if (claim_start_date_val != "" && claim_end_date_val != "" && ft != null) {
                                        if (provFt != null) {
                                            sourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_start_date_val + claim_end_date_val + ft.getCode().getCode() + ft.getCode().getCodeSystem() + provFt.getCode().getCode())));
                                        } else {
                                            sourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_start_date_val + claim_end_date_val + ft.getCode().getCode() + ft.getCode().getCodeSystem() + "PT")));
                                            String msg = "using a default value for provider type as it was not specified in this claim! " + claim;
                                            logger.debug(msg);
                                        }
                                    } else {
                                        String msg = "found a raps claim for which I cannot create a source type uri " + claim;
                                        logger.error(msg);
                                        throw new InsuranceClaimException(msg);
                                    }
                                } else if (isMAO) {
                                    int check = 0;
                                    for (NewExternalIdentifier nei : otherOriginalIds) {
                                        if (nei.getAssignAuthority().equals("ENCOUNTER_ICN") && nei.getIDValue().length() > 0) {
                                            try {
                                                String encICNEncrypted = encryptor.encrypt(nei.getIDValue());
                                                sourceT.setUri(encICNEncrypted);
                                                check++;
                                                break;
                                            } catch (ApixioSecurityException e) {
                                                throw new RDFModelToEventTypeException(e);
                                            }
                                        }
                                    }
                                    if (check == 0) {
                                        String msg = "no encounter icn found in MAO claim ! " + claim_xuuid + " pat: " + patUUID;
                                        logger.error(msg);
                                        throw new InsuranceClaimException(msg);
                                    }
                                }
                                sourceT.setType("RiskAdjustmentInsuranceClaim");
                            } else if (isFFS) {
                                sourceT.setUri(claim_orig_id_val);
                                sourceT.setType("FeeForServiceInsuranceClaim");
                            } else {
                                //do not generate an event
                                logger.debug("found an event that is not one of (FFS, RAPS or MAO) skipping!");
                                continue;
                            }
                            et.setSource(sourceT);
                            et.setSubject(patientRT);


                            //atributes
                            AttributesType ats = new AttributesType();
                            AttributeType at = new AttributeType();
                            //adding the bucket type
                            at.setName("bucketType");
                            at.setValue("StructuredConditionExtractor");
                            ats.getAttribute().add(at);

                            if (isRAPS || isMAO) {
                                at = new AttributeType();
                                at.setName("sourceType");
                                at.setValue("CMS_KNOWN");
                                ats.getAttribute().add(at);
                                at = new AttributeType();
                                at.setName("SOURCE_TYPE");
                                at.setValue("CMS_KNOWN");
                                ats.getAttribute().add(at);


                            } else if (isFFS) {
                                at = new AttributeType();
                                at.setName("sourceType");
                                at.setValue("FeeForServiceInsuranceClaim");
                                ats.getAttribute().add(at);
                            }
                            at = new AttributeType();
                            if (claim_dpd_xuuid != "") {
                                at.setName("dataProcessingDetailId");
                                at.setValue(claim_dpd_xuuid);
                            }
                            ats.getAttribute().add(at);
                            et.setAttributes(ats);
                            //attributes done

                            //evidence attributes
                            EvidenceType evt = new EvidenceType();
                            evt.setInferred(false);


                            if (isMAO) {
                                int check = 0;
                                for (NewExternalIdentifier nei : otherOriginalIds) {
                                    if (nei.getAssignAuthority().equals("ENCOUNTER_ICN")) {
                                        eSourceT.setUri(String.valueOf(DigestUtils.md5Hex(nei.getIDValue() + transactionDate)));
                                        check++;
                                        break;
                                    }
                                }
                                if (check == 0) {
                                    String msg = "no encounter icn found in MAO claim ! " + claim_xuuid + " pat: " + patUUID;
                                    logger.error(msg);
                                    throw new InsuranceClaimException(msg);

                                }
                                eSourceT.setType("EDPS");
                            }
                            if (isRAPS) {
                                if (claim_start_date_val != "" && claim_end_date_val != "") {
                                    if (ft != null && provFt != null && provFt.getCode().getCode() != null && transactionDate != "") {
                                        // md5hash(startdate, enddate, dxcodevalue, dxcodesystem, provider_type, transaction_date)
                                        eSourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_start_date_val + claim_end_date_val + ft.getCode().getCode() + ft.getCode().getCodeSystem() + provFt.getCode().getCode() + transactionDate)));
                                    } else if (transactionDate != "" && provFt == null && ft != null) {
                                        eSourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_start_date_val + claim_end_date_val + ft.getCode().getCode() + ft.getCode().getCodeSystem() + "PT" + transactionDate)));
                                        String msg = "creating a evidence source uri with a default provider type value for a RAPS claim!";
                                        logger.debug(msg);
                                    } else if (transactionDate == "" && ft != null) {
                                        eSourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_start_date_val + claim_end_date_val + ft.getCode().getCode() + ft.getCode().getCodeSystem() + "PT" + claim_end_date_val)));
                                        String msg = "creating a evidence source uri with a default provider type value AND  using claim end date instead of processing date for a RAPS claim!";
                                        logger.debug(msg);
                                    } else {
                                        eSourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_start_date_val + claim_end_date_val + "PT" + claim_end_date_val)));
                                        String msg = "creating a evidence source uri with a default provider type value AND  using claim end date instead of processing date for a RAPS claim!";
                                        logger.debug(msg);
                                    }
                                } else {
                                    String msg = "found a raps claim for which I cannot create a source type uri " + claim;
                                    logger.error(msg);
                                    throw new InsuranceClaimException(msg);
                                }
                                eSourceT.setType("RAPS_RETURN");
                            }
                            if (isFFS) {
                                if (claim_orig_id_val != "" && claimOriginalIDAssignAuthority != "" && transactionDate != "") {
                                    eSourceT.setUri(String.valueOf(DigestUtils.md5Hex(claim_orig_id_val + claimOriginalIDAssignAuthority + transactionDate)));
                                    eSourceT.setType("FeeForServiceInsuranceClaim");
                                } else {
                                    String msg = "found ffs claim for which I cannot create a source type uri " + claim;
                                    logger.error(msg);
                                    throw new InsuranceClaimException(msg);
                                }
                            }
                            evt.setSource(eSourceT);

                            AttributesType eats = new AttributesType();
                            at = new AttributeType();

                            //version goes here
                            at.setName("version");
                            at.setValue(VOCABULARY_VERSION);
                            eats.getAttribute().add(at);
                            //transactionstatuscode
                            if (isRAPS) {
                                at = new AttributeType();
                                errorCodes = getErrorCodesFromClaim(res);
                                if (errorCodes != null && errorCodes.length() > 0) {
                                    at.setName("transactionStatusCode");
                                    at.setValue(errorCodes);
                                    eats.getAttribute().add(at);
                                }
                            }
                            //transactiontype
                            boolean isok = false;

                            transactionType.setName("transactionType");
                            if (deleteIndicator == false) {
                                //this should imply add is true
                                if (addIndicator) {
                                    transactionType.setValue("ADD");
                                    isok = true;
                                } else {
                                    String msg = "found a claim with a delete ind of false yet no corresponding add = true indicator! review claims for patient id : "+this.model.getPatientXUUID();
                                    logger.error(msg);
                                }
                            } else {
                                if (!addIndicator) {
                                    transactionType.setValue("DELETE");
                                    isok = true;
                                } else {
                                    String msg = "found a  claim with a delete ind of false yet no corresponding add = true indicator! review claims for patient id : "+this.model.getPatientXUUID();
                                    logger.error(msg);
                                }
                            }
                            if (isok) {
                                eats.getAttribute().add(transactionType);
                            }

                            //parentsource id
                            if (isMAO) {
                                at = new AttributeType();
                                at.setName("parentSourceId");
                                at.setValue(originalEncounterICN);
                                eats.getAttribute().add(at);
                            }

                            //processingDate
                            at = new AttributeType();
                            at.setName("processingDate");
                            at.setValue(claim_last_edit_date_val);
                            eats.getAttribute().add(at);


                            //transactiondate
                            if (isRAPS || isFFS) {
                                if (transactionDate != null && transactionDate.length() > 0) {
                                    at = new AttributeType();
                                    at.setName("transactionDate");
                                    at.setValue(transactionDate);
                                    eats.getAttribute().add(at);
                                }
                                if (isRAPS && transactionDate != null && transactionDate.length() == 0) {
                                    logger.debug("Found a RAPS claim uri: " + claim + " without a transaction date for patient: " + this.model.getPatientXUUID());
                                }
                            } else if (isMAO) {
                                at = new AttributeType();
                                at.setName("transactionDate");
                                at.setValue(transactionDate);
                                eats.getAttribute().add(at);
                            }

                            //encounter type switch
                            if (isMAO) {
                                if (patEncSwTypeVal.length() > 0) {
                                    at = new AttributeType();
                                    at.setName("encounterTypeSwitch");
                                    at.setValue(patEncSwTypeVal);
                                    eats.getAttribute().add(at);
                                }
                            } else {
                                if (isMAO) {
                                    String s = "Found a MAO-004 claim without an encounter type switch! Claim : " + claim + " patientid : " + this.model.getPatientXUUID();
                                    logger.debug(s);
                                }
                            }
                            evt.setAttributes(eats);
                            et.setEvidence(evt);

                            //create a separate event for provider types
                            if (isRAPS || isFFS || isMAO) {
                                if (provFt != null) {
                                    EventType provet = new EventType();
                                    provet.setSubject(patientRT);
                                    provet.setSource(sourceT);
                                    provet.setFact(provFt);

                                    EventTypeAttributesBuilder b = new EventTypeAttributesBuilder();
                                    provet.setAttributes(b.build());

                                    EvidenceType pet = new EvidenceType();
                                    pet.setAttributes(b.build());
                                    pet.setInferred(false);
                                    ReferenceType rt = new ReferenceType();
                                    rt.setUri(eSourceT.getUri());
                                    rt.setType(eSourceT.getType());
                                    pet.setSource(rt);
                                    eats = new AttributesType();
                                    at = new AttributeType();
                                    at.setName("processingDate");
                                    at.setValue(claim_last_edit_date_val);
                                    eats.getAttribute().add(at);

                                    at = new AttributeType();
                                    at.setName("transactionDate");
                                    at.setValue(transactionDate);
                                    eats.getAttribute().add(at);

                                    at = new AttributeType();
                                    at.setName("version");
                                    at.setValue(VOCABULARY_VERSION);
                                    eats.getAttribute().add(at);

                                    eats.getAttribute().add(transactionType);
                                    if (renderingProviderId != null) {
                                        at = new AttributeType();
                                        at.setName("renderingProviderId");
                                        at.setValue(renderingProviderId);
                                        eats.getAttribute().add(at);
                                    }
                                    pet.setAttributes(eats);
                                    provet.setEvidence(pet);
                                    rmList.add(provet);
                                }
                            }
                            rmList.add(et);
                        }
                    }

                    if(isFFS){
                        if(billFt != null) {
                            EventType billet = new EventType();
                            billet.setSubject(patientRT);
                            billet.setSource(sourceT);
                            billet.setFact(billFt);

                            EventTypeAttributesBuilder b = new EventTypeAttributesBuilder();
                            billet.setAttributes(b.build());

                            EvidenceType pet = new EvidenceType();
                            pet.setAttributes(b.build());
                            pet.setInferred(false);
                            ReferenceType aST = new ReferenceType();
                            aST.setUri(eSourceT.getUri());
                            aST.setType(eSourceT.getType());
                            pet.setSource(aST);
                            billet.setSource(sourceT);

                            AttributesType eats = new AttributesType();

                            AttributeType at = new AttributeType();
                            at.setName("processingDate");
                            at.setValue(claim_last_edit_date_val);
                            eats.getAttribute().add(at);

                            at = new AttributeType();
                            at.setName("version");
                            at.setValue(VOCABULARY_VERSION);
                            eats.getAttribute().add(at);

                            if(renderingProviderId != null){
                                at = new AttributeType();
                                at.setName("renderingProviderId");
                                at.setValue(renderingProviderId);
                                eats.getAttribute().add(at);
                            }
                            at = new AttributeType();
                            at.setName("transactionDate");
                            at.setValue(transactionDate);
                            eats.getAttribute().add(at);
                            eats.getAttribute().add(transactionType);

                            pet.setAttributes(eats);
                            billet.setEvidence(pet);
                            rmList.add(billet);
                        }
                    }

                    if(isFFS) {
                        if (procedureFactTypes != null && procedureFactTypes.size() > 0) {
                            for (FactType p : procedureFactTypes) {
                                EventType procET = new EventType();
                                procET.setSubject(patientRT);

                                EventTypeAttributesBuilder b = new EventTypeAttributesBuilder();
                                procET.setAttributes(b.build());
                                ReferenceType rt = new ReferenceType();
                                rt.setUri(sourceT.getUri());
                                rt.setType("FeeForServiceInsuranceClaim");
                                procET.setSource(rt);
                                procET.setFact(p);
                                EvidenceType pet = new EvidenceType();
                                pet.setInferred(false);
                                rt = new ReferenceType();
                                rt.setType(sourceT.getType());
                                rt.setUri(DigestUtils.md5Hex(claim_orig_id_val+claimOriginalIDAssignAuthority+transactionDate));
                                pet.setSource(rt);
                                AttributesType eats = new AttributesType();
                                AttributeType at = new AttributeType();
                                at.setName("transactionDate");
                                at.setValue(transactionDate);
                                eats.getAttribute().add(at);

                                at = new AttributeType();
                                at.setName("version");
                                at.setValue(VOCABULARY_VERSION);
                                eats.getAttribute().add(at);

                                at = new AttributeType();
                                at.setName("processingDate");
                                at.setValue(claim_last_edit_date_val);
                                eats.getAttribute().add(at);
                                eats.getAttribute().add(transactionType);
                                if(renderingProviderId != null){
                                    at.setName("renderingProviderId");
                                    at.setValue(renderingProviderId);
                                    eats.getAttribute().add(at);
                                }
                                pet.setAttributes(eats);
                                procET.setEvidence(pet);
                                rmList.add(procET);

                            }
                        }
                    }
                }
                prevIteration = System.currentTimeMillis();
            }
        }
        return rmList;
    }


    private String getOriginalEncounterICN(Resource aPE){
        String q = "select distinct ?idval \n";
        q+= "where {";
        q+="  ?claim <" + OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + aPE + "> . \n";
        q+= " ?claim <"+ OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property()+"> ?ooi . \n";
        q+= " ?ooi <"+OWLVocabulary.OWLDataProperty.HasAssignAuthority.property()+"> ?aa . \n";
        q+= " ?ooi <"+OWLVocabulary.OWLDataProperty.HasIDValue.property()+"> ?idval . \n";
        q+= " filter (?aa=\"ORIGINAL_ENCOUNTER_ICN\") . \n";
        q+= "} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            Literal idval = sol.getLiteral("idval");
            if(idval!=null){
                return idval.getLexicalForm();
            }
        }
        return "";
    }
    private List<NewExternalIdentifier> getOtherOriginalIds(Resource res){
        List<NewExternalIdentifier> rm = new ArrayList<>();
        String q = "select distinct ?ooidval ?ooidaa \n"+
                " WHERE { "+
                " <"+res.toString()+"> <"+OWLVocabulary.OWLObjectProperty.HasOtherOriginalIDs.property()+"> ?ooid. \n"+
                " ?ooid <"+OWLVocabulary.OWLDataProperty.HasIDValue.property()+"> ?ooidval .\n"+
                " ?ooid <"+OWLVocabulary.OWLDataProperty.HasAssignAuthority.property()+"> ?ooidaa .\n"+
                "}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q );
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            Literal aa = sol.getLiteral("ooidaa");
            Literal val = sol.getLiteral("ooidval");
            if (aa != null&& val !=null){
                String aao = aa.getLexicalForm();
                String aav = val.getLexicalForm();
                NewExternalIdentifier nei = new NewExternalIdentifier();
                nei.setAssignAuthority(aao);
                nei.setIDValue(aav);
                if(!rm.contains(nei)) rm.add(nei);
            }
        }
        return rm;
    }
    private String getErrorCodesFromClaim(Resource res){
        List<String>errors = new ArrayList<>();
        String q = "select distinct ?errVal  \n"+
                " WHERE {"+
                " ?claim <"+OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property()+"> <"+res+"> .\n"+
                " ?claim <"+OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property()+"> ?cmsValidation. \n" +
                " optional {\n"+
                " ?cmsValidation <"+OWLVocabulary.OWLObjectProperty.HasCMSValidationError.property()+"> ?cmsError .\n "+
                " ?cmsError <"+OWLVocabulary.OWLDataProperty.HasCMSValidationErrorCodeValue.property()+"> ?errVal .\n}}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            Literal errValLit = qs.getLiteral("errVal");
            if(errValLit != null){
                String err = errValLit.getLexicalForm();
                if (!errors.contains(err)){
                    errors.add(err);
                }
            }
        }
        if (errors.size()>0) {
           return StringUtils.join(errors, ",");
        }
        return "";
    }


    private boolean isMAOClaim(Resource res){
        String q1 = "select distinct ?ct where{"+
                " ?claim <"+RDF.type+"> ?ct ." +
                " ?claim <"+OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n" +
                "}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q1);
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            Resource t = SPARQLUtils.getQuerySolutionVariableResource(sol, "ct");
            if (t.toString().contains("MAO004RAInsuranceClaim")){
                return true;
            }
        }
        //backwards compatible checks below
        String q = "select distinct ?claim_ra_type_val where {" +
                " ?claim <" + OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n" +
                " ?claim <" + OWLVocabulary.OWLDataProperty.HasRiskAdjustmentInsuranceClaimTypeValue.property() + "> ?claim_ra_type_val. \n" +
                " }";
        rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            String claim_type = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_ra_type_val");
            if (claim_type.toUpperCase().contains("MAO")) {
                return true;
            }
        }
        String q2 = "select distinct ?claim_ra_type_val where {" +
                " ?claim <" + OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n" +
                " ?claim <" + OWLVocabulary.OWLObjectProperty.HasRiskAdjustmentInsuranceClaimType.property() + "> ?claim_ra_claim_type. \n" +
                " ?claim <" + OWLVocabulary.OWLDataProperty.HasRiskAdjustmentInsuranceClaimTypeValue.property() + "> ?claim_ra_type_val.\n" +
                " }";
        rs = SPARQLUtils.executeSPARQLSelect(this.model, q2);
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            String claim_type = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_ra_type_val");
            if (claim_type.toUpperCase().contains("MAO")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRAPSClaim(Resource res) {
        String q1 = "select distinct ?ct where{"+
                " ?claim <"+RDF.type+"> ?ct ." +
                " ?claim <"+OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n" +
                "}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q1);
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            Resource t = SPARQLUtils.getQuerySolutionVariableResource(sol, "ct");
            if (t.toString().contains("RAPSRAInsuranceClaim")){
                return true;
            }
        }
        //backwards compatible checks below
        String q = "select distinct ?claim_ra_type_val where {" +
                " ?claim <" + OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n" +
                " ?claim <" + OWLVocabulary.OWLDataProperty.HasRiskAdjustmentInsuranceClaimTypeValue.property() + "> ?claim_ra_type_val. \n" +
                " }";
        rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            String claim_type = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_ra_type_val");
            if (claim_type.equalsIgnoreCase("RAPS")) {
                return true;
            }
        }
        String q2 = "select distinct ?claim_ra_type_val where {" +
                " ?claim <" + OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property() + "> <" + res + "> .\n" +
                " ?claim <" + OWLVocabulary.OWLObjectProperty.HasRiskAdjustmentInsuranceClaimType.property() + "> ?claim_ra_claim_type. \n" +
                " ?claim <" + OWLVocabulary.OWLDataProperty.HasRiskAdjustmentInsuranceClaimTypeValue.property() + "> ?claim_ra_type_val.\n" +
                " }";
        rs = SPARQLUtils.executeSPARQLSelect(this.model, q2);
        while (rs.hasNext()) {
            QuerySolution sol = rs.next();
            String claim_type = SPARQLUtils.getQuerySolutionVariableStringValue(sol, "claim_ra_type_val");
            if (claim_type.equalsIgnoreCase("RAPS")) {
                return true;
            }
        }
        return false;
    }


    private boolean isFFSClaim(Resource res){
        String q = "select distinct ?claimType where { "+
                " ?claim <"+OWLVocabulary.OWLObjectProperty.HasClaimedEvent.property()+"> <"+res+"> .\n"+
                " ?claim <"+RDF.type+"> ?claimType .\n} ";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            Resource t = SPARQLUtils.getQuerySolutionVariableResource(sol, "claimType");
            if(t.toString().contains(OWLVocabulary.OWLClass.FeeForServiceInsuranceClaim.toString())){
                return true;
            }
        }
        return false;
    }

    private String getCodingSystemOIDString(Resource r){
        if (r != null){
            String q = "select distinct ?v where { <"+r+"> <"+OWLVocabulary.OWLDataProperty.HasClinicalCodingSystemOID.property()+"> ?v} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String addme = qs.getLiteral("v").getLexicalForm();
                return addme;
            }
        }
        return "";
    }

    private String getCodingSystemNameString(Resource r){
        if (r != null){
            String q = "select distinct ?v where { <"+r+"> <"+OWLVocabulary.OWLDataProperty.HasClinicalCodingSystemName.property()+"> ?v} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String addme = qs.getLiteral("v").getLexicalForm();
                return addme;
            }
        }
        return "";
    }

    private String getCodingSystemVersionString(Resource r){
        if (r != null){
            String q = "select distinct ?v where { <"+r+"> <"+OWLVocabulary.OWLDataProperty.HasClinicalCodingSystemVersion.property()+"> ?v} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String addme = qs.getLiteral("v").getLexicalForm();
                return addme;
            }
        }
        return "";
    }

    private String getClinicalCodeValueString(Resource r){
        if (r != null){
            String q = "select distinct ?v where { <"+r+"> <"+OWLVocabulary.OWLDataProperty.HasClinicalCodeValue.property()+"> ?v} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String addme = qs.getLiteral("v").getLexicalForm();
                return addme;
            }
        }
        return "";
    }

    private String getClinicalCodeDisplayNameString(Resource r){
        if (r != null){
            String q = "select distinct ?v where { <"+r+"> <"+OWLVocabulary.OWLDataProperty.HasClinicalCodeDisplayName.property()+"> ?v} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String addme = qs.getLiteral("v").getLexicalForm();
                return addme;
            }
        }
        return "";
    }


    private String getClinicalCodeLastEditDateValueString(Resource r){
        if (r != null){
            String q = "select distinct ?v where { <"+r+"> <"+OWLVocabulary.OWLObjectProperty.HasLastEditDate.property()+"> ?d." +
                    " ?d <"+OWLVocabulary.OWLDataProperty.HasDateValue.property() + ">  ?v.} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String addme = qs.getLiteral("v").getLexicalForm();
                return addme;
            }
        }
        return "";
    }


    private Map<String,String> getClinicalCodeSourceXUUIDAndDataProcessingDetailXUUIDString(Resource r){
        Map<String, String> rm = new HashMap<>();
        rm.put("SOURCEID", "");
        rm.put("DPDID", "");
        if (r != null){
            String q = "select distinct ?id  ?dpd_xuuid where { <"+r+"> <"+OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property()+"> ?dpd. " +
                    " ?dpd <"+OWLVocabulary.OWLObjectProperty.HasSource.property()  + ">  ?s." +
                    " ?dpd <"+OWLVocabulary.OWLDataProperty.HasXUUID.property() +"> ?dpd_xuuid . "+
                    " ?s <"+ OWLVocabulary.OWLDataProperty.HasXUUID.property() +"> ?id. } limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
            while(rs.hasNext()){
                QuerySolution qs = rs.next();
                String sourceid = qs.getLiteral("id").getLexicalForm();
                String dpdId = qs.getLiteral("dpd_xuuid").getLexicalForm();
                rm.put("SOURCEID", sourceid);
                rm.put("DPDID", dpdId);
            }
        }
        return rm;
    }

    private Map<String, String> getCodingSystemDetails(Resource aCodingSystem){
        Map <String, String > rm = new HashMap<>();
        rm.put("OID", getCodingSystemOIDString(aCodingSystem));
        rm.put("VERSION", getCodingSystemVersionString(aCodingSystem));
        rm.put("NAME", getCodingSystemNameString(aCodingSystem));
        return rm;
    }

    private Map<String, String> getClinicalCodeDetails(Resource aClinicalCode){
        Map<String,String> rm = new HashMap<>();
        rm.put("CODEVALUE", getClinicalCodeValueString(aClinicalCode));
        rm.put("CODEDISPLAYNAME", getClinicalCodeDisplayNameString(aClinicalCode));
        rm.put("CODELASTEDITDATE", getClinicalCodeLastEditDateValueString(aClinicalCode));
        Map<String, String> otherIds = getClinicalCodeSourceXUUIDAndDataProcessingDetailXUUIDString(aClinicalCode);
        rm.put("SOURCEID", otherIds.get("SOURCEID"));
        rm.put("DPDID", otherIds.get("DPDID"));
        return rm;
    }

    private FactType makeFactTypeFromClinicalCode(Resource aClinicalCode, String startDateVal, String endDateVal){
        FactType rm = new FactType();
        String q = "select distinct " +
            " ?coding_system  \n" +
            " where { \n";
            q += " <" + aClinicalCode + "> <" + OWLVocabulary.OWLObjectProperty.HasClinicalCodingSystem.property() + "> ?coding_system . \n} limit 1" ;
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()) {
            QuerySolution sol = rs.next();
            Resource codingSystem = SPARQLUtils.getQuerySolutionVariableResource(sol, "coding_system");

            Map<String, String> csValz = getCodingSystemDetails(codingSystem);
            String code_system_version = csValz.get("VERSION");
            String cs_display_name = csValz.get("NAME");
            String oid = csValz.get("OID");


            Map<String, String> codeValz = getClinicalCodeDetails(aClinicalCode);
            String code_value = codeValz.get("CODEVALUE");
            String code_dn = codeValz.get("CODEDISPLAYNAME");
            String code_dpd_id = codeValz.get("DPDID");
            String code_last_edit_date= codeValz.get("CODELASTEDITDATE");
            String code_source_xuuid = codeValz.get("SOURCEID");
            if (code_value == "" || cs_display_name ==""){
                logger.debug("Found a code without a code system or code value");
                return null;
            }
            CodeType ct = createCodeType(code_value, code_dn, oid, code_system_version, cs_display_name);
            rm.setCode(ct);

            AttributesType ats = new AttributesType();
            AttributeType at = new AttributeType();
            if (code_value != null && code_value.length()>0){
                at= new AttributeType();
                at.setName("problemName");
                at.setValue(code_value);
                ats.getAttribute().add(at);
            }
            if (code_source_xuuid != null && code_source_xuuid.length() > 0) {
                at = new AttributeType();
                at.setName("code_source_id");
                at.setValue(code_source_xuuid);
                ats.getAttribute().add(at);
            }
            if (code_dpd_id != null && code_dpd_id.length() > 0) {
                at = new AttributeType();
                at.setName("code:dataProcessingDetailId");
                at.setValue(code_dpd_id);
                ats.getAttribute().add(at);
            }
            if(code_last_edit_date != null && code_last_edit_date.length()>0){
                at = new AttributeType();
                at.setName("code:lastEditDate");
                at.setValue(code_last_edit_date);
                ats.getAttribute().add(at);
            }
            rm.setValues(ats);
            try {
                DateFormat formatter;
                Date sdate, edate;
                formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");//yyyy-MM-dd'T'HH:mm:ss.SSSZ yyyy-MM-dd'T'HH:mm:ss.SSSXXX
                sdate = formatter.parse(startDateVal);
                edate = formatter.parse(endDateVal);
                TimeRangeType trt = createTimeRangeType(sdate, edate);
                rm.setTime(trt);
            } catch (ParseException e) {
                String m = "could not create timerante type for a fact type !";
                logger.debug(m);
                logger.debug(e.getStackTrace());
            }
            return rm;
        }
        return null;
    }

    private List<Resource> getRAClaimsDiagnosisCodes(Resource aClaim){
        List<Resource> rm = new ArrayList<>();
        logger.debug("getting ra claims diagnosis codes");
        String q = "select distinct ?cc where { <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasDiagnosisCode.property()+"> ?cc}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            Resource addme = qs.getResource("cc");
            if(!rm.contains(addme)){
                rm.add(addme);
            }
        }
        logger.debug("done getting ra claims diagnosis codes");
        return rm;
    }

    private Resource getProviderType(Resource aClaim){
        logger.debug("getting provider type");
        String q = "select distinct ?cc where { <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasProviderType.property()+"> ?cc} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            Resource addme = qs.getResource("cc");
            logger.debug("done getting provider type");
            return addme;
        }
        return null;
    }

    private List<Resource> getProcedures(Resource aClaim){
        List<Resource> rm = new ArrayList<>();
        logger.debug("getting procedure codes");
        String q = "select distinct ?cc where { <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasServicesRendered.property()+"> ?cc} ";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            Resource addme = qs.getResource("cc");
            rm.add(addme);
        }
        logger.debug("done getting procedure codes");
        if(rm.size() ==0){
            return null;
        }

        return rm;
    }



    private Resource getBillType(Resource aClaim){
        logger.debug("getting bill type");
        String q = "select distinct ?cc where { <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasBillType.property()+"> ?cc} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            Resource addme = qs.getResource("cc");
            logger.debug("done getting bill type");
            return addme;
        }
        return null;
    }

    private CodeType createCodeType(String codeVal, String displayName, String oid, String codeSysVersion, String codeSysName) {
        CodeType rm = new CodeType();
        if (codeVal.equals(displayName)){
            displayName = "N/A";
        }
        if (codeVal != null && codeVal.length() > 0) {
            rm.setCode(codeVal);
        }
        if (codeSysName != null && codeSysName.length() > 0) {
            rm.setCodeSystemName(codeSysName);
        }
        if (oid != null && oid.length() > 0) {
            rm.setCodeSystem(oid);
        }
        if (codeSysVersion != null && codeSysVersion.length() > 0) {
            rm.setCodeSystemVersion(codeSysVersion);
        }
        if (displayName != null && displayName.length() > 0) {
            rm.setDisplayName(displayName);
        }
        return rm;
    }

    private TimeRangeType createTimeRangeType(Date startDate, Date endDate) {
        TimeRangeType rm = new TimeRangeType();
        if (startDate != null) {
            rm.setStartTime(startDate);
        } else {
            String msg = "Invalid start date found!";
            logger.error(msg);
        }
        if (endDate != null) {
            rm.setEndTime(endDate);
        } else {
            logger.debug("Invalid end date found!");
        }
        return rm;
    }

    private String getTransactionDate(Resource aClaim, boolean isRAPS){
        String q = "select ?claim_processing_date_val where { ";
        if (isRAPS){
            q += " <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasCMSClaimValidationResult.property()+"> ?cmsValidation. \n"+
                    "?cmsValidation <"+OWLVocabulary.OWLObjectProperty.HasValidatedClaimTransactionDate.property() + "> ?claim_processing_date .\n" +
                    "?claim_processing_date <"+OWLVocabulary.OWLDataProperty.HasDateValue.property()+"> ?claim_processing_date_val .}";
        } else {
            q+= " <"+aClaim+"> <"+ OWLVocabulary.OWLObjectProperty.HasInsuranceClaimProcessingDate.property() + "> ?processing_date. \n"+
                    "?processing_date <" + OWLVocabulary.OWLDataProperty.HasDateValue.property() + "> ?claim_processing_date_val.}\n";
        }
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("claim_processing_date_val")){
                return qs.getLiteral("claim_processing_date_val").getLexicalForm();
            }
        }
        return "";
    }

    private String getClaimOriginalIdAssignAuthority(Resource aClaim){
        String q = "select ?claim_originalid_aa where{ " +
                " <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasOriginalID.property()+"> ?claim_origid . " +
                " ?claim_origid <" + OWLVocabulary.OWLDataProperty.HasAssignAuthority.property()  + "> ?claim_originalid_aa .\n} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("claim_originalid_aa")){
                return qs.getLiteral("claim_originalid_aa").getLexicalForm();
            }
        }
        return "";
    }


    private String getPatientEncounterSwitchTypeValue(Resource aClaim){
        /**
         * claim_query += " optional {?claim <" + OWLVocabulary.OWLObjectProperty.HasPatientEncounterType.property() + "> ?pat_enc_sw_type . \n" +
         "?pat_enc_sw_type <"+OWLVocabulary.OWLDataProperty.HasPatientEncounterSwitchTypeValue.property() +"> ?patEncSwTypeVal.} \n ";

         */
        String q = "select ?patEncSwTypeVal where{ " +
                " <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasPatientEncounterType.property()+"> ?pat_enc_sw_type . " +
                " ?pat_enc_sw_type <" + OWLVocabulary.OWLDataProperty.HasPatientEncounterSwitchTypeValue.property()  + "> ?patEncSwTypeVal .\n} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("patEncSwTypeVal")){
                return qs.getLiteral("patEncSwTypeVal").getLexicalForm();
            }
        }
        return "";
    }

    private String getClaimOriginalIdValue(Resource aClaim){
        /**
         * "   ?claim <" + OWLVocabulary.OWLObjectProperty.HasOriginalID.property() + "> ?claim_origid. \n " +
         "   optional { ?claim_origid <" + OWLVocabulary.OWLDataProperty.HasIDValue.property() + "> ?claim_orig_id_val. } \n" +
         */
        String q = "select ?claim_orig_id_val where{ " +
                " <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasOriginalID.property()+"> ?claim_origid . " +
                " ?claim_origid <" + OWLVocabulary.OWLDataProperty.HasIDValue.property()  + "> ?claim_orig_id_val .\n} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("claim_orig_id_val")){
                return qs.getLiteral("claim_orig_id_val").getLexicalForm();
            }
        }
        return "";
    }

    private String getClaimEndDateValue(Resource aClaim){
        String q = "select ?end_date_value where{ " +
                " <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasDateOfServiceRange.property()+"> ?dos_range . " +
                " ?dos_range <" + OWLVocabulary.OWLObjectProperty.HasEndDate.property()  + "> ?end_date .\n " +
                " ?end_date <" + OWLVocabulary.OWLDataProperty.HasDateValue.property() + "> ?end_date_value. \n}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("end_date_value")){
                return qs.getLiteral("end_date_value").getLexicalForm();
            }
        }
        return "";
    }

    private String getClaimSourceTypeValue(Resource aClaim){

        String q = "select ?claim_source_type_val where{ " +
                " <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasDataProcessingDetail.property()+"> ?dpd . " +
                " ?dpd <" + OWLVocabulary.OWLObjectProperty.HasSource.property() + "> ?claim_source .\n " +
                " ?claim_source <" + OWLVocabulary.OWLDataProperty.HasXUUID.property() + "> ?claim_source_xuuid. \n" +
                " ?claim_source <"+OWLVocabulary.OWLObjectProperty.HasSourceType.property()+ "> ?claim_source_type. \n"+
                " ?claim_source_type <"+OWLVocabulary.OWLDataProperty.HasSourceTypeStringValue.property()+"> ?claim_source_type_val. \n}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("claim_source_type_val")){
                return qs.getLiteral("claim_source_type_val").getLexicalForm();
            }
        }
        return "";
    }


    private boolean getClaimDeleteIndicator(Resource aClaim){
        //" optional{ ?claim <" + OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property() + "> ?deleteIndicator.} \n" +
        String q = "select ?deleteIndicator where {<"+aClaim+"> <"+OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property()+"> ?deleteIndicator. } limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("deleteIndicator")){
                return qs.getLiteral("deleteIndicator").getBoolean();
            }
        }
        return false;
    }


    private boolean getClaimAddIndicator(Resource aClaim){
        //" optional{ ?claim <" + OWLVocabulary.OWLDataProperty.HasDeleteIndicator.property() + "> ?deleteIndicator.} \n" +
        String q = "select ?addIndicator where {<"+aClaim+"> <"+OWLVocabulary.OWLDataProperty.HasAddIndicator.property()+"> ?addIndicator. } limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("addIndicator")){
                return qs.getLiteral("addIndicator").getBoolean();
            }
        }
        return false;
    }

    /**
     * Construct a string of the form : "id^^assignAuthority" for an ffs's rendering provider
     * @param aClaim
     * @return
     */
    private String getRenderingProvider(Resource aClaim){
        logger.debug("getting rendering provider");
        String q = "select distinct ?idval ?idaa where { <"+aClaim+"> <"+OWLVocabulary.OWLObjectProperty.HasRenderingProvider.property()+"> ?rendProv. \n" +
                " ?rendProv <"+OWLVocabulary.OWLObjectProperty.HasOriginalID.property()+"> ?oid. \n"
                + " ?oid <"+OWLVocabulary.OWLDataProperty.HasIDValue.property()+"> ?idval . \n"+
                " ?oid <"+OWLVocabulary.OWLDataProperty.HasAssignAuthority.property()+"> ?idaa.\n"+
                "} limit 1";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(this.model, q);
        while(rs.hasNext()){
            QuerySolution qs = rs.next();
            if(qs.contains("idval") && qs.contains("idaa")){
                logger.debug("done getting rendering provider");
                return qs.getLiteral("idval").getLexicalForm()+"^^"+qs.getLiteral("idaa").getLexicalForm();
            }
        }
        return null;
    }

    public RDFModel getModel() {
        return this.model;
    }

    public List<EventType> getEventTypes() {
        return this.eventTypeList;
    }

    @Override
    public void close() throws IOException {
        if (!this.model.isClosed()) {
            this.model.close();
        }
    }
}
