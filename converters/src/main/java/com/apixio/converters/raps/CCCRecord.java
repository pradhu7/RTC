package com.apixio.converters.raps;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.apixio.converters.base.Parser;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParserType;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.Source;

/**
 * The CCCRecord or the details record within a RAPS return file is the one that
 * contains the meat of the information including diagnostic clusters, delete information, etc..
 *  
 * @author vvyas
 */
public class CCCRecord extends Parser {
	private String recordLine = null;
	@SuppressWarnings("unused")
	private String seqNumber, seqErrorCode, patientControlNumber;
	private String hicNumber, hicErrorCode, correctedHicNumber;
	@SuppressWarnings("unused")
	private String dobErrorCode;
	private DateTime patientDOB;
	private String fieldDiagType = null;
	private Patient patient;
	private DateTime recordTransactionDate;
	@SuppressWarnings("unused")
	private String submitterId, fileId;
	private Source recordSource;
	private ParsingDetail recordParsingDetail;
	private int lexerLocation = 0;
	private static Map<ActorRole, ClinicalActor> actors = new HashMap<ActorRole, ClinicalActor>();
	
	public static final String CCCRecordParserVersion = "1.0";
	
	private ActorRole convertProviderTypeToActorRole(String providerType) {
		if("01".equalsIgnoreCase(providerType) ) {
			return ActorRole.HOSPITAL_INPATIENT_PRINCIPAL_PROVIDER;
		} else if ("02".equals(providerType)) {
			return ActorRole.HOSPITAL_INPATIENT_OTHER_PROVIDER;
		} else if ("10".equals(providerType)) {
			return ActorRole.HOSPITAL_OUTPATIENT_PROVIDER;
		} else if ("20".equals(providerType)) {
			return ActorRole.PRIMARY_CARE_PROVIDER;
		} else
			throw new RuntimeException(new StateFullRAPSParserException("Unknown provider type " + providerType + "at " + lexerLocation 
					+ " (... " + recordLine.substring(lexerLocation-2, lexerLocation+2) + " ...)"));
	}
	
	private static final String ICD9_OID="2.16.840.1.113883.6.103";
	private static final String ICD10_OID="2.16.840.1.113883.6.90";	// TODO: i am not sure of this, someone needs to verify this
	
	List<Problem> parseDiagnosticClusters(String diagnosticClusters) {
		// since upto 10 clusters are allowed we are going to loop through
		// the string 10 times with start index starting at 0
		
		int n = 0;
		List<Problem> diagClusters = new LinkedList<Problem>();		
		while(lexerLocation < 412) {						
			// if this is a blank diagnostic cluster move on to the next one.
			if(StringUtils.isBlank(diagnosticClusters.substring(lexerLocation, lexerLocation+32))) {
				lexerLocation += 32;
				continue;
			}
			
			ActorRole providerType = convertProviderTypeToActorRole(diagnosticClusters.substring(lexerLocation,lexerLocation+2));
			lexerLocation += 2;
			
			DateTime startDate = DateTime.parse(diagnosticClusters.substring(lexerLocation,lexerLocation+8));
			lexerLocation+=8;
			
			DateTime endDate = DateTime.parse(diagnosticClusters.substring(lexerLocation,lexerLocation+8));
			lexerLocation+=8;
			
			String deleteFlag = diagnosticClusters.substring(lexerLocation,lexerLocation+1);
			lexerLocation += 1;
			
			String diagnosisCode = diagnosticClusters.substring(lexerLocation,lexerLocation+7);
			lexerLocation += 7;
			
			// TODO : understand these errors better and actually parse them appropriately.
			
//			String diagClusterError1 = diagnosticClusters.substring(i, i+3);
//			i += 3;
//			
//			String diagClusterError2 = diagnosticClusters.substring(i, i+3);
//			i += 3;
			
			lexerLocation += 6;
			// construct a problem object from the diagnostic cluster that we have here.
			Problem p = new Problem();
			
			// setup the clinical code to
			ClinicalCode code = new ClinicalCode();
			code.setCode(diagnosisCode.trim());
			if("ICD9".equals(this.fieldDiagType)) {
				code.setCodingSystem(ICD9_OID);
				code.setCodingSystemOID(ICD9_OID);
				code.setCodingSystemVersions("9");
			} else {
				code.setCodingSystem(ICD10_OID);
				code.setCodingSystemOID(ICD10_OID);
				code.setCodingSystemVersions("10");
			}
			
			// unfortunately we don't have the display names set here because it won't
			// be available to use right now.			
			p.setCode(code);
			p.setStartDate(startDate);
			p.setEndDate(endDate);
			p.setDiagnosisDate(startDate);
			
			// set the primary clinical actor based on the provider type - since there are a fixed number of
			// provider types we are going to use them
			if(actors.containsKey(providerType)) {
				p.setPrimaryClinicalActorId(actors.get(providerType).getClinicalActorId());
			} else {
				ClinicalActor actor = new ClinicalActor();
				actor.setRole(providerType);
				actors.put(actor.getRole(),actor);
				p.setPrimaryClinicalActorId(actors.get(providerType).getClinicalActorId());
			}
						
			p.setSourceId(recordSource.getSourceId());
			
			if("D".equals(deleteFlag))
				p.setEditType(EditType.DELETE);
			
			
			// setup external id
			ExternalID id = new ExternalID();
			id.setAssignAuthority("RAPS CCC Record");
			id.setId(submitterId+"/"+seqNumber+"/"+n);
			p.setOriginalId(id);
			
			diagClusters.add(p);
			++n;
			
			p.setParsingDetailsId(recordParsingDetail.getParsingDetailsId());
		}
		return diagClusters;
	}
	
	public CCCRecord(String recordLine, String fieldDiagType, String submitterId, String fileId, DateTime transactionDate) {
		this.recordLine = recordLine;
		this.fieldDiagType = fieldDiagType;
		this.submitterId = submitterId;
		this.fileId = fileId;		
		this.recordTransactionDate = transactionDate;
		this.patient = new Patient();
		
		this.patient.setPatientId(UUID.randomUUID());
		
		parseCCCRecord();
	}
	
	private void parseCCCRecord() {
		// setup parsing details
		ParsingDetail details = new ParsingDetail();
		details.setParser(ParserType.RAPS);
		details.setParserVersion(CCCRecordParserVersion);
		details.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
		details.setParsingDateTime(DateTime.now());
		
		// finish setting up this parsing details
		patient.addParsingDetail(details);
		recordParsingDetail = details;

		lexerLocation = 3;
		seqNumber = recordLine.substring(lexerLocation,lexerLocation+7);
		lexerLocation += 7;
		seqErrorCode = recordLine.substring(lexerLocation,lexerLocation+3);
		lexerLocation += 3;
		
		patientControlNumber = recordLine.substring(lexerLocation,lexerLocation+40);
		lexerLocation += 40;
		
		hicNumber = recordLine.substring(lexerLocation,lexerLocation+25).trim();
		lexerLocation += 25;
		
		hicErrorCode = recordLine.substring(lexerLocation,lexerLocation+3).trim();
		lexerLocation += 3;
		
		
		patientDOB = DateTime.parse(recordLine.substring(lexerLocation,lexerLocation+8));
		lexerLocation += 8;
		
		dobErrorCode = recordLine.substring(lexerLocation,lexerLocation+3);
		lexerLocation+=3;
		
		Demographics demoData = new Demographics();
		demoData.setDateOfBirth(patientDOB);
		patient.setPrimaryDemographics(demoData);
		
		// setup source information
		Source source = new Source();
		source.setCreationDate(recordTransactionDate);
		source.setSourceSystem("RAPS");
		source.setSourceType("RAPS Return File");
		
		// create a file level external id
		ExternalID id = new ExternalID();
		id.setAssignAuthority("CMS RAPS submission");
		id.setId(submitterId);
		id.setType("Sumbitter ID");
		source.setOriginalId(id);
		
		patient.addSource(source);
		recordSource = source;
		
		for(Problem prob : parseDiagnosticClusters(recordLine))
			patient.addProblem(prob);
		
		correctedHicNumber = recordLine.substring(412,437);
		
		// construct the various elements of the patient object directly
		ExternalID patientControlNumberEid = new ExternalID();
		patientControlNumberEid.setAssignAuthority("PATIENT-CONTROL-NO");
		patientControlNumberEid.setId(patientControlNumber);
		
		// TODO : we need better parsing of HIC numbers because the HIC suffix might
		// change when the status of the person changes, thus patient dedup will
		// fail at that point.
		ExternalID hicNummberEid = new ExternalID();
		hicNummberEid.setAssignAuthority("CMS");
		if(StringUtils.isBlank(correctedHicNumber) && StringUtils.isBlank(hicErrorCode))
			hicNummberEid.setId(hicNumber);
		else
			hicNummberEid.setId(correctedHicNumber);
		hicNummberEid.setType("Medicare HIC Number");
		
		patient.addExternalId(hicNummberEid);
		patient.addExternalId(patientControlNumberEid);
		
		for(ClinicalActor actor : actors.values())
			patient.addClinicalActor(actor);
				
	}

	public static Map<ActorRole, ClinicalActor> getActors() {
		return actors;
	}

	public static void setActors(Map<ActorRole, ClinicalActor> actors) {
		CCCRecord.actors = actors;
	}

	@Override
	public void parse(InputStream ccdDocument, CatalogEntry catalogEntry)
			throws Exception {
		throw new UnsupportedOperationException("please use the constructor to parse a CCC Record");		
	}

	@Override
	public Patient getPatient() throws Exception {
		return patient;
	}
}