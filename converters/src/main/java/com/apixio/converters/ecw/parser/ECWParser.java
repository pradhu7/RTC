package com.apixio.converters.ecw.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.apixio.converters.base.BaseUtils;
import com.apixio.converters.base.Parser;
import com.apixio.converters.html.creator.HTMLUtils;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentContent;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Organization;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;

public class ECWParser extends Parser {

	private static final String PARSER_VERSION = null;
	private static final String DATE_REGEX = "\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}";
	private static final String DATE_REGEX_PARSER = "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})";

	private Node message;
	private Element root;
	private boolean isTelEncounter = false;
	private CatalogEntry catalogEntry;

	private Patient patient  = new Patient();
	private ParsingDetail currentParsingDetails;
	private Source primarySource;
	private URI sourceDocumentUri;
	private DateTime documentDate;
	private String docTitle;
	private String documentId;
	private String documentSource;
	private byte[] docBytes;

	@Override
	public void parse(InputStream ecwDocument, CatalogEntry catalogEntry) throws Exception {
		this.catalogEntry = catalogEntry;
		this.docBytes = BaseUtils.getBytesFromInputStream(ecwDocument);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document xmlDocument = builder.parse(new ByteArrayInputStream(docBytes));
        root = xmlDocument.getDocumentElement();
		populateMetaData();
		populateParsingDetails();
		populatePrimarySource();
		populatePatient();			
		populateDocument();
	}

	private void populateMetaData() throws XPathExpressionException, DOMException, ParseException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		message =(Node)xpath.compile("/Envelope/Body/return").evaluate(root, XPathConstants.NODE);  
		Node hospitalName =(Node)xpath.compile("HospitalName").evaluate(message, XPathConstants.NODE);
		if (hospitalName != null) {
			documentSource = hospitalName.getTextContent();
		} 
		Node encounterId =(Node)xpath.compile("EncounterId").evaluate(message, XPathConstants.NODE);
		if (encounterId != null) {
			documentId = encounterId.getTextContent();
		}
		Node encDate =(Node)xpath.compile("encDate").evaluate(message, XPathConstants.NODE);
		if (encDate != null) {
			documentDate = GetDateFromString(encDate.getTextContent());
		}
		else {
			Node nonEncDate =(Node)xpath.compile("date").evaluate(message, XPathConstants.NODE);
			if (nonEncDate != null) {
				documentDate = GetDateFromString(nonEncDate.getTextContent());
			}
		}

		docTitle = "Progress Note";
		
		Node encType =(Node)xpath.compile("enctype").evaluate(message, XPathConstants.NODE);
		if (encType != null) {
			String type = encType.getTextContent();
			if (type.equalsIgnoreCase("Telephone Encounter")) {
				isTelEncounter = true;
				docTitle = "Telephone Encounter";				
			}
		}
		
	}

	@Override
	public Patient getPatient() throws Exception {
		return patient;
	}
	private void populateParsingDetails() {
		currentParsingDetails = new ParsingDetail();
		currentParsingDetails.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
		currentParsingDetails.setContentSourceDocumentURI(sourceDocumentUri);
		//currentParsingDetails.setParser(ParserType.ECW);
		currentParsingDetails.setParserVersion(PARSER_VERSION);
		currentParsingDetails.setParsingDateTime(DateTime.now());
		this.patient.addParsingDetail(currentParsingDetails);
	}

	private void populatePrimarySource() {
		primarySource = new Source();
		primarySource.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		//primarySource.setClinicalActorId(clinicalActorId) -- We'll need to add an actor and reference it
		primarySource.setCreationDate(documentDate);
		primarySource.setOrganization(getSourceOrganization());
		primarySource.setOriginalId(BaseUtils.getExternalID(documentId));
		//primarySource.setSourceSystem(sourceSystem)
		//primarySource.setSourceType(sourceType)
		this.patient.addSource(primarySource);		
	}

	private void populatePatient() throws Exception {
//		populateHealthcareProviders();
//		populatePatientDetails();
		populatePatientDemographics();
//		populateAllergies();
//		populateMedications();
//		populateProblems();
//		populateImmunizations();
//		populateLabResults();
//		populateProcedures();
//		populateVitalSigns();
//		populateFamilyHistory();
//		populateSocialHistory();
		
	}

	private void populateDocument() {
		// Here we get the HTML for the document, create a document object, and hang it on the patient
		Document document = new Document();
		document.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		document.setSourceId(primarySource.getSourceId());
		
		//document.setAssociatedEncounter(associatedEncounter);
		document.setDocumentDate(documentDate);
		document.setDocumentTitle(docTitle);
		//document.setMimeType("text/xml");
		document.setOriginalId(BaseUtils.getExternalID(documentId));
		document.setStringContent(getHtmlRender());
		
		DocumentContent documentContent = new DocumentContent();
		documentContent.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		documentContent.setSourceId(primarySource.getSourceId());
//		documentContent.setContent(content);
//		documentContent.setHash(hash);
//		documentContent.setLength(length);
		documentContent.setMimeType("text/xml");
//		documentContent.setOriginalId(originalId);
//		documentContent.setUri(uri);
//		document.setDocumentContents(documentContents);
		
		document.getDocumentContents().add(documentContent);
		patient.addDocument(document);
	}

	private String getHtmlRender() {

		String htmlRender = "";
		
    	String xslPath = "ecw_progressnotes.xsl";
    	if (isTelEncounter)
    		xslPath = "ecw_telencounter.xsl";
		String resourcePath = this.getClass().getClassLoader().getResource(xslPath).toString();
    	
		return HTMLUtils.convertXmlToHtml(ClassLoader.getSystemResourceAsStream("ecw_progressnotes.xsl"), new ByteArrayInputStream(docBytes), resourcePath);//, "");
	}
	
	private void populatePatientDemographics() throws XPathExpressionException, DOMException, ParseException {
		patient.setExternalIDs(getPatientExternalIDs());
		patient.setPrimaryDemographics(getPrimaryDemographics());
		
	}
	private Demographics getPrimaryDemographics() throws XPathExpressionException, DOMException, ParseException {
		Demographics demographics = new Demographics();
		demographics.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		demographics.setSourceId(primarySource.getSourceId());			;
		
		if (message != null)
		{
			String lastName = "";
			String firstName = "";
			String dob = "";
			String gender = "";
			
			XPath xpath = XPathFactory.newInstance().newXPath();
			Node patientNode =(Node)xpath.compile("patient").evaluate(message, XPathConstants.NODE);
			if (patientNode != null) {
				String fullName = patientNode.getTextContent();
				int delimiter = fullName.indexOf(", ");
				if (delimiter > 0)
				{					
					lastName = fullName.substring(0,delimiter);
					firstName = fullName.substring(delimiter + 2);
					demographics.setName(getName(firstName, lastName));
				}
			}
			
			Node dobNode =(Node)xpath.compile("DOB").evaluate(message, XPathConstants.NODE);
			
			if (dobNode != null)
			{
				demographics.setDateOfBirth(GetDateFromString(dobNode.getTextContent()));
				
			}
			Node sex =(Node)xpath.compile("sex").evaluate(message, XPathConstants.NODE);
			
			if (sex != null)
			{
				String sexOriginal = sex.getTextContent();
				if (sexOriginal.length() > 0)
				{
					gender = sexOriginal.substring(0,1);
					demographics.setGender(BaseUtils.getGenderFromText(gender));
				}
			}
		}
		return demographics;
	}

	private Name getName(String firstName, String lastName) {
		Name name = new Name();
		name.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		name.setSourceId(primarySource.getSourceId());		
		name.getGivenNames().add(firstName);
		name.getFamilyNames().add(lastName);
		return name;
	}

	private DateTime GetDateFromString(String dateString) throws ParseException {
		DateTime date = null;
		try {
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MM/dd/yyyy");
			date = fmt.parseDateTime(dateString);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return date;
	}

	private Organization getSourceOrganization() {
		Organization organization = null;
		if (documentSource != null) {
			organization = new Organization();
			organization.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
			organization.setSourceId(primarySource.getSourceId());
			
			//organization.setAlternateIds(alternateIds);
			organization.setName(documentSource);
			//organization.setOriginalId(originalId);
			//organization.setPrimaryId(getExternalID(sourceOrganization.getIDs()));
		}
		return organization;
	}   

	private Set<ExternalID> getPatientExternalIDs() throws XPathExpressionException {
		Set<ExternalID> patientExternalIDs = new HashSet<ExternalID>();
		if (message != null)
		{			
			XPath xpath = XPathFactory.newInstance().newXPath();
			
			Node patientIdNode =(Node)xpath.compile("ControlNo").evaluate(message, XPathConstants.NODE);
			if (patientIdNode != null) {
				String patientIdExtension = patientIdNode.getTextContent();
				String patientIdRoot = "Invision MRN";
				patientExternalIDs.add(BaseUtils.getExternalID(patientIdExtension, patientIdRoot));
				
			}
			Node episodeNumberNode =(Node)xpath.compile("PatientNumber").evaluate(message, XPathConstants.NODE);
			if (episodeNumberNode != null) {
				String episodeNumberNodeExtension = episodeNumberNode.getTextContent();
				String episodeNumberNodeRoot = "Episode Number";
				patientExternalIDs.add(BaseUtils.getExternalID(episodeNumberNodeExtension, episodeNumberNodeRoot));
			}			
		}
		return patientExternalIDs;
	}
}
