package com.apixio.converters.ccda.utils;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

import com.apixio.model.patient.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.ecore.xml.type.AnyType;
import org.eclipse.mdht.uml.cda.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
//import org.eclipse.mdht.uml.cda.ccd.MedicationStatusObservation;
//import org.eclipse.mdht.uml.cda.ccd.ProblemStatusObservation;
import org.eclipse.mdht.uml.hl7.datatypes.AD;
import org.eclipse.mdht.uml.hl7.datatypes.ADXP;
import org.eclipse.mdht.uml.hl7.datatypes.ANY;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.CE;
import org.eclipse.mdht.uml.hl7.datatypes.CS;
import org.eclipse.mdht.uml.hl7.datatypes.ED;
import org.eclipse.mdht.uml.hl7.datatypes.EIVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.EIVL_event;
import org.eclipse.mdht.uml.hl7.datatypes.EN;
import org.eclipse.mdht.uml.hl7.datatypes.ENXP;
import org.eclipse.mdht.uml.hl7.datatypes.II;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_INT;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_PQ;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.ON;
import org.eclipse.mdht.uml.hl7.datatypes.PIVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.PN;
import org.eclipse.mdht.uml.hl7.datatypes.PQ;
import org.eclipse.mdht.uml.hl7.datatypes.SXCM_TS;
import org.eclipse.mdht.uml.hl7.datatypes.TEL;
import org.eclipse.mdht.uml.hl7.datatypes.TS;
import org.eclipse.mdht.uml.hl7.vocab.NullFlavor;
import org.eclipse.mdht.uml.hl7.vocab.PostalAddressUse;
import org.eclipse.mdht.uml.hl7.vocab.TelecommunicationAddressUse;

import com.apixio.converters.base.BaseUtils;

public class CCDAUtils {

	public static enum HL7Code { TIMINGEVENT };
	
	public static final String[] datePatterns = {
        "yyyy-MM-dd'T'HH:mm:ssZ", //ISO8601 long RFC822 zone
        "yyyy-MM-dd'T'HH:mm:ssz", //ISO8601 long long form zone
        "yyyy-MM-dd'T'HH:mm:ss", //ignore timezone
        "yyyyMMddHHmmssZ", //ISO8601 short
        "yyyyMMddHHmm",
        "yyyyMMdd", //birthdate from NIST IHE C32 sample
        "yyyyMM",
        "yyyy" //just the year
    };

	public static Address getAddressFromAD(AD ad) {
		Address address = new Address();
		if (ad != null) {
			address.setAddressType(AddressType.CURRENT);
			address.setCity(getFirstADXP(ad.getCities()));
			address.setCountry(getFirstADXP(ad.getCountries()));
			address.setCounty(getFirstADXP(ad.getCounties()));
			address.setState(getFirstADXP(ad.getStates()));
			address.getStreetAddresses().addAll(getStringListFromADXPList(ad.getStreetAddressLines()));
			address.setZip(getFirstADXP(ad.getPostalCodes()));
		}
		return address;
	}

	public static String getAllergen(Observation observation) {
		String allergen = null;
		PlayingEntity alertPlayingEntity = getObservationPlayingEntity(observation);
		
		if (alertPlayingEntity != null) {
			// look a few places for the allergy name TODO: Is this the right order: entity name, code display name, original text?
			allergen = CCDAUtils.getName(alertPlayingEntity.getNames());
			if (allergen.equals(""))
				allergen = alertPlayingEntity.getCode().getDisplayName();
			if (allergen == null || allergen.equals(""))
				allergen = CCDAUtils.getTextOrReference(observation.getSection(), alertPlayingEntity.getCode().getOriginalText());
		}
		return allergen;
	}

	public static ClinicalCode getAllergyReaction(Observation observation) {
		ClinicalCode allergyReaction = new ClinicalCode();
		if (observation != null) {
			if (observation.getValues().size() > 0) {	
				CD value = (CD) observation.getValues().get(0);
				allergyReaction = CCDAUtils.getClinicalCode(value);
			} else if (observation.getText() != null){
				allergyReaction = BaseUtils.getClinicalCode(observation.getText().getText());
			}
		}
		return allergyReaction;
	}
	
	public static List<ClinicalCode> getAllergyReactions(Observation allergyObservation) {
		List<ClinicalCode> reactions = new LinkedList<ClinicalCode>();
		if (allergyObservation.getObservations() != null) {
			for (Observation observation : allergyObservation.getObservations()) {
				reactions.add(getAllergyReaction(observation));
				//getFirstTextFromCDList(alertObservation.getSection(), reactionObservation.getValues());
			}
		}
		// since we create one allergy per reaction, we want to create a dummy entry even if there are none.
		if (reactions.size() == 0)
			reactions.add(new ClinicalCode());
		return reactions;
	}
	
	public static ClinicalCode getClinicalCode(CD code) {
		ClinicalCode clinicalCode = null;
		// TODO: how should nullFlavor be handled? For now just ignore "No Information"
		if (code != null && code.getNullFlavor() != NullFlavor.NI)
		{
			clinicalCode = new ClinicalCode();
			clinicalCode.setCode(code.getCode());
			clinicalCode.setCodingSystem(code.getCodeSystemName());
			clinicalCode.setCodingSystemOID(code.getCodeSystem());
			clinicalCode.setCodingSystemVersions(code.getCodeSystemVersion());
			clinicalCode.setDisplayName(code.getDisplayName());
		}
		return clinicalCode;
	}

	public static List<ClinicalCode> getClinicalCodes(CD value) {
		List<ClinicalCode> clinicalCodes = new LinkedList<ClinicalCode>();
		ClinicalCode primaryCode = CCDAUtils.getClinicalCode(value);
		if (primaryCode != null) {
			clinicalCodes.add(primaryCode);
		}
		List<ClinicalCode> codeTranslations = CCDAUtils.getCodeTranslations(value);
		if (codeTranslations != null && codeTranslations.size() > 0) {
			clinicalCodes.addAll(codeTranslations);
		}
		return clinicalCodes;
	}
	 public static String getCodeDisplayName(CE code) {
		String codeDisplayName = null;
		if (code != null)
		{
			codeDisplayName = code.getDisplayName();
		}
		return codeDisplayName;
	}

	public static List<ClinicalCode> getCodeTranslations(CD value) {
		List<ClinicalCode> codeTranslations = new LinkedList<ClinicalCode>();
		if (value != null && value.getTranslations() != null)
		{
			for (CD translation : value.getTranslations())
			{
				ClinicalCode codeTranslation = getClinicalCode(translation);
				if (codeTranslation != null) {
					codeTranslations.add(codeTranslation);
				}
			}
		}
		return codeTranslations;
	}

	public static String getCodeValue(CE code) {
		String codeValue = "";
		if (code != null && code.getCode() != null)
		{
			codeValue = code.getCode();
		}
		return codeValue;
	}

	public static DateTime getDateTimeFromTS(TS ts) {
		DateTime dateTime = null;
		if (ts != null)
		{
			// What to do with null flavor?
			//if (birthTime.getNullFlavor(). == null)
			try {
				//DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmssZ");
				//dateTime = format.parseDateTime(ts.getValue());		
				dateTime = parseDate(ts.getValue());
				if (dateTime != null && dateTime.toString("yyyyMMdd").equals("19000101"))
					dateTime = null;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return dateTime;
	}

	public static ExternalID getEidFromII(II ii) {
		ExternalID id = new ExternalID();
		id.setId(ii.getExtension());
		id.setAssignAuthority(ii.getRoot());
		id.setSource(ii.getRoot());
		return id;
	}

	public static ExternalID getFirstEidFromIIList(EList<II> iiList) {
		ExternalID firstId = null;
		if (iiList != null && iiList.size() > 0) {
			firstId = getEidFromII(iiList.get(0));
		}
		return firstId;
	}

	public static Set<ExternalID> getEidsFromIIList(EList<II> iiList) {
		Set<ExternalID> ids = new HashSet<ExternalID>();
		if (iiList != null) {
			for (II ii : iiList) {
				if (ii != null) {
					ids.add(getEidFromII(ii));
				}
			}
		}
		return ids;
	}
	
	public static String getFirstADXP(EList<ADXP> adxp) {
		String firstAdxp = null;
		if (adxp != null && adxp.size() > 0) {
			firstAdxp = adxp.get(0).getText();
		}
		return firstAdxp;
	}

	public static String getFirstReactionSeverity(Observation allergyObservation) {
		String reactionSeverity = null;
		for (Observation reactionObservation : allergyObservation.getObservations()) {
			if (hasTemplateId(reactionObservation.getTemplateIds(), CCDAConstants.ALLERGY_REACTION_OBSERVATION)) {
				for (Observation severityObservation : reactionObservation.getObservations()) {
					if (hasTemplateId(severityObservation.getTemplateIds(), CCDAConstants.ALLERGY_SEVERITY_OBSERVATION)) {
						if (severityObservation.getText() != null) {
							reactionSeverity = getTextOrReference(severityObservation.getSection(), severityObservation.getText());
						} else {
							for (ANY value : severityObservation.getValues()) {
								if (value instanceof CD) {
									CD valueCode = (CD) value;
									reactionSeverity = valueCode.getDisplayName();
								}
							}
						}
					}
				}
			}
		}
		return reactionSeverity;
	}

	public static ClinicalCode getFirstAllergyReaction(Observation allergyObservation) {
		ClinicalCode reaction = null;
		for (Observation observation : allergyObservation.getObservations()) {
			if (hasTemplateId(observation.getTemplateIds(), CCDAConstants.ALLERGY_REACTION_OBSERVATION)){
				reaction = getAllergyReaction(observation);
			}
		}
		return reaction;
	}

	public static String getFirstNameFromONList(EList<ON> names) {
		String orgName = null;
		if (names != null && names.size() > 0) {
			orgName = names.get(0).getText();
		}
		return orgName;
	}

	public static String getFrequencyString(SXCM_TS sxcm_TS) {
		String frequencyString = null;
		if (sxcm_TS != null) {
			if (sxcm_TS instanceof PIVL_TS) {
				PIVL_TS medicationTime = (PIVL_TS) sxcm_TS;
				PQ period = medicationTime.getPeriod();
				if (period != null) {
					String value = String.valueOf(period.getValue());
					String unit = String.valueOf(period.getUnit());
					frequencyString = "every " + value + unit;
				}
			} else if (sxcm_TS instanceof EIVL_TS) {
				EIVL_TS medicationTime = (EIVL_TS) sxcm_TS;
				EIVL_event event = medicationTime.getEvent();
				if (event != null) {
					frequencyString = getHL7CodeValue(HL7Code.TIMINGEVENT, event.getCode());					
				}
				
//			} else if (sxcm_TS instanceof PIVL_PPD_TS) {
//				
//			} else if (sxcm_TS instanceof SXPR_TS) {
//				
			} else if (sxcm_TS instanceof TS) {
				
			}
		}
		return frequencyString;
	}
	
	private static String getHL7CodeValue(HL7Code codeSet, String code) {
		String codeValue = null;
		if (codeSet.equals(HL7Code.TIMINGEVENT))
			codeValue = HL7Codes.TimingEvent.get(code);
		return codeValue;
	}

	public static Gender getGender(CE administrativeGenderCode) {
		Gender gender = Gender.UNKNOWN;
		if (administrativeGenderCode != null && administrativeGenderCode.getCode() != null) {
			String genderCode = administrativeGenderCode.getCode();
			if (genderCode.equalsIgnoreCase("M"))
				gender = Gender.MALE;
			else if (genderCode.equalsIgnoreCase("F"))
				gender = Gender.FEMALE;
			else if (genderCode.equalsIgnoreCase("U"))
				gender = Gender.TRANSGENDER;
		}
		return gender;
	}
	
	public static ActorRole getHealthCareProviderRole(CE functionCode) {
		ActorRole actorRole = null;
		String providerType = getCodeValue(functionCode);
		if (providerType.equalsIgnoreCase("CP"))
			actorRole = ActorRole.CONSULTING_PROVIDER;
		else if (providerType.equalsIgnoreCase("PP"))
			actorRole = ActorRole.PRIMARY_CARE_PROVIDER;
		else if (providerType.equalsIgnoreCase("RP"))
			actorRole = ActorRole.REFERRING_PROVIDER;
		else if (providerType.equalsIgnoreCase("MP"))
			actorRole = ActorRole.MEDICAL_HOME_PROVIDER;
		return actorRole;
	}

	public static DateTime getHighTime(IVL_TS effectiveTime) {
		DateTime highTime = null;
		if (effectiveTime != null && effectiveTime.getHigh() != null)
			highTime = getDateTimeFromTS(effectiveTime.getHigh());
		else
			highTime = getDateTimeFromTS(effectiveTime);
		return highTime;
	}
	
	public static List<String> getLanguages(EList<LanguageCommunication> languageCommunications) {
		List<String> languages = new LinkedList<String>();
		if (languageCommunications != null && languageCommunications.size() > 0) {
			LanguageCommunication languageCommunication = languageCommunications.get(0);
			if (languageCommunication != null) {
				CS languageCode = languageCommunication.getLanguageCode();
				if (languageCode != null)
				{
					languages.add(languageCode.getCode());
				}
			}
		}
		return languages;
	}

	public static List<String> getListFromENXP(EList<ENXP> enxpList) {
		List<String> names = new LinkedList<String>();
		if (enxpList != null) {
			for (ENXP enxp : enxpList) {
				names.add(enxp.getText());
			}
		}
		return names;
	}

	public static DateTime getLowTime(IVL_TS effectiveTime) {
		DateTime lowTime = null;
		if (effectiveTime != null && effectiveTime.getLow() != null)
			lowTime = getDateTimeFromTS(effectiveTime.getLow());
		else
			lowTime = getDateTimeFromTS(effectiveTime);
		return lowTime;
	}

	public static ResolutionStatus getProblemStatus(CS statusCode) {
		ResolutionStatus resolutionStatus = null;
		if (statusCode != null && statusCode.getCode() != null) {
			if (statusCode.getCode().equals("completed")) {
				resolutionStatus = ResolutionStatus.RESOLVED;
			} else if (statusCode.getCode().equals("aborted")) {
				resolutionStatus = ResolutionStatus.INACTIVE;
			} else if (statusCode.getCode().equals("active")) {
				resolutionStatus = ResolutionStatus.ACTIVE;
			} else if (statusCode.getCode().equals("suspended")) {
				resolutionStatus = ResolutionStatus.INACTIVE;
			}
		}
		return resolutionStatus;
	}
	
	public static MaritalStatus getMaritalStatus(CE maritalStatusCode) {
		MaritalStatus maritalStatus = null;
		if (maritalStatusCode != null && maritalStatusCode.getCode() != null) {
			String maritalStatusCodeValue = maritalStatusCode.getCode();
			if (maritalStatusCodeValue.equalsIgnoreCase("A"))
				maritalStatus = MaritalStatus.ANNULLED;
			else if (maritalStatusCodeValue.equalsIgnoreCase("D"))
				maritalStatus = MaritalStatus.DIVORCED;
			else if (maritalStatusCodeValue.equalsIgnoreCase("I"))
				maritalStatus = MaritalStatus.INTERLOCUTORY;
			else if (maritalStatusCodeValue.equalsIgnoreCase("L"))
				maritalStatus = MaritalStatus.LEGALLY_SEPARATED;
			else if (maritalStatusCodeValue.equalsIgnoreCase("M"))
				maritalStatus = MaritalStatus.MARRIED;
			else if (maritalStatusCodeValue.equalsIgnoreCase("P"))
				maritalStatus = MaritalStatus.POLYGAMOUS;
			else if (maritalStatusCodeValue.equalsIgnoreCase("S"))
				maritalStatus = MaritalStatus.NEVER_MARRIED;
			else if (maritalStatusCodeValue.equalsIgnoreCase("T"))
				maritalStatus = MaritalStatus.DOMESTIC_PARTNER;
			else if (maritalStatusCodeValue.equalsIgnoreCase("W"))
				maritalStatus = MaritalStatus.WIDOWED;
		}
		return maritalStatus;
	}

	public static String getName(EList<PN> eList) {
		String name = "";
		if (eList.size() > 0)
			name = eList.get(0).getText();
		return name;
	}

	public static String getNameFromEN(EN en) {
		String name = "";
		if (en != null) {
			name = en.getText();
		}
		return name;
	}

	public static String getNameFromON(ON on) {
		String name = "";
		if (on != null) {
			name = on.getText();
		}
		return name;
	}

	public static PlayingEntity getObservationPlayingEntity(Observation observation) {
		PlayingEntity alertPlayingEntity = null;
		try {
			alertPlayingEntity = observation.getParticipants().get(0).getParticipantRole().getPlayingEntity();
		} catch (Exception ex) {
			
		}
		return alertPlayingEntity;
	}

//	public static Boolean getPrescriptionActive(MedicationStatusObservation medicationStatusObservation) {
//		Boolean active = null;
//		if (medicationStatusObservation != null) {
//			if (medicationStatusObservation.getValues() != null && medicationStatusObservation.getValues().size() > 0) {
//				try {
//					CE statusValue = (CE) medicationStatusObservation.getValues().get(0);
//					if (statusValue != null) {
//						String statusValueCode = statusValue.getCode();
//						if (statusValueCode.equalsIgnoreCase("55561003"))
//							active = true;
//						else if (statusValueCode.equalsIgnoreCase("421139008")) // on hold
//							active = false;
//						else if (statusValueCode.equalsIgnoreCase("392521001")) // prior history
//							active = false;
//						else if (statusValueCode.equalsIgnoreCase("73425007")) // no longer active
//							active = false;
//					}
//				} catch (Exception ex){
//
//				}
//			}
//		}
//		return active;
//	}

	public static Integer getRefills(IVL_INT repeatNumber) {
		Integer refills = null;
		if (repeatNumber != null) {
			BigInteger fills = repeatNumber.getValue();
			if (fills != null && fills.intValue() > 0) {
				refills = fills.intValue() - 1;
			}
		}
		return refills;
	}

//	public static ResolutionStatus getResolutionStatus(ProblemStatusObservation problemStatus) {
//		ResolutionStatus resolutionStatus = null;
//		if (problemStatus != null) {
//			if (problemStatus.getValues() != null && problemStatus.getValues().size() > 0) {
//				CE problemStatusValue = (CE) problemStatus.getValues().get(0);
//				String code = problemStatusValue.getCode();
//				if (code.equals("55561003")) {
//					resolutionStatus = ResolutionStatus.ACTIVE;
//				} else if (code.equals("73425007")) {
//					resolutionStatus = ResolutionStatus.INACTIVE;
//				} else if (code.equals("413322009")) {
//					resolutionStatus = ResolutionStatus.RESOLVED;
//				}
//			}
//		}
//		return resolutionStatus;
//	}

	public static List<String> getStringListFromADXPList(EList<ADXP> adxpList) {
		List<String> list = new LinkedList<String>();
		if (adxpList != null) {
			for (ADXP adxp : adxpList) {
				list.add(adxp.getText());
			}
		}
		return list;
	}

	public static String getFirstTextFromCDList(Section section, EList<ANY> values) {
		String text = "";
		if (values != null && values.size() > 0) {
			CD value = (CD) values.get(0);
			text = getTextFromCD(section, value);
		}
		return text;
	}

	public static String getTextFromCD(Section section, CD cd) {
		String text = "";
		// first try the displayName
		if (cd != null) {
			if (!StringUtils.isEmpty(cd.getDisplayName()))
				text = cd.getDisplayName();
			else if (cd.getOriginalText() != null)
				text = getTextOrReference(section, cd.getOriginalText());
		}
		return text;
	}

//	public static String getStatus(StatusObservation statusObservation) {
//		String status = "";
//		if (statusObservation != null) {
//			EList<ANY> statusValueList = statusObservation.getValues();
//			if (statusValueList.size() > 0)
//			{			
//				CD value = (CD)statusValueList.get(0);
//				status = value.getDisplayName();
//			}
//		}
//		return status;
//	}

	// TODO: Our enumeration is insufficient for this
//	public static AddressType getAddressType(EList<PostalAddressUse> uses) {
//		AddressType addressType = null;
//		if (uses != null && uses.size() > 0) {
//			PostalAddressUse use = uses.get(0);
//			if (use.getName().equalsIgnoreCase("BAD"))
//				addressType = AddressType
//			0-L	BAD	bad address	AddressUse
//			0-L	CONF	confidential	AddressUse
//			0-L	DIR	direct	AddressUse
//			0-L	H	home address	AddressUse
//			0-L	HP	primary home	AddressUse
//			0-L	HV	vacation home	AddressUse
//			0-L	PHYS	physical visit address	AddressUse
//			0-L	PST	postal address	AddressUse
//			0-L	PUB	public	AddressUse
//			0-L	TMP	temporary	AddressUse
//			0-L	WP	work place	AddressUse
//		} else {
//
//		}
//		return null;
//	}


	public static TelephoneType getTelephoneType(EList<TelecommunicationAddressUse> uses) {
		TelephoneType telephoneType = null;
		if (uses != null && uses.size() > 0) {
			TelecommunicationAddressUse use = uses.get(0);
			if (use.equals(TelecommunicationAddressUse.H))
				telephoneType = TelephoneType.HOME_PHONE;
			else if (use.equals(TelecommunicationAddressUse.HP))
				telephoneType = TelephoneType.HOME_PHONE;
			else if (use.equals(TelecommunicationAddressUse.WP))
				telephoneType = TelephoneType.WORK_PHONE;
			else if (use.equals(TelecommunicationAddressUse.MC))
				telephoneType = TelephoneType.CELL_PHONE;
		}
		return telephoneType;
	}

	public static String getTextOrReference(Section section, ED textSection) {
		String text = null;
		if (textSection != null) {
			TEL reference = textSection.getReference();
			// if we have a reference
			if (reference != null) {
				String referenceValue = reference.getValue();
				if (referenceValue != null && referenceValue.length() > 1) {
				    if (referenceValue.startsWith("#")) {
                        referenceValue = referenceValue.substring(1);
                    }
					StrucDocText strucDocText = section.getText();					
					text = strucDocText.getText(referenceValue);
					String featureMapText = getTextFromFeatureMap(strucDocText.getMixed(), referenceValue);
					text = featureMapText;

				}			
			}
			else
				text = textSection.getText();
		}
		if (text != null)
			text = text.trim();
		return text;
	}
	private static String getTextFromFeatureMap(FeatureMap root, String id) {
		Stack<FeatureMap> stack = new Stack<FeatureMap>();
		stack.push(root);
		while (!stack.isEmpty()) {
			FeatureMap featureMap = stack.pop();
			for (FeatureMap.Entry entry : featureMap) {
				if (entry.getEStructuralFeature() instanceof EReference) {
					AnyType anyType = (AnyType) entry.getValue();
					String attributeValue = getAttributeValue(anyType.getAnyAttribute(), "ID");
					if (attributeValue != null && attributeValue.equals(id)) {
						return getTextFromFeatureMap(anyType.getMixed());
					}
					stack.push(anyType.getMixed());
				}
			}
		}
		return null;
	}
	private static String getAttributeValue(FeatureMap featureMap, String name) {
		for (FeatureMap.Entry entry : featureMap) {
			EStructuralFeature feature = entry.getEStructuralFeature();
			if (feature instanceof EAttribute && feature.getName().equals(name)) {
				return entry.getValue().toString();
			}
		}
		return null;
	}

	private static String getTextFromFeatureMap(FeatureMap featureMap) {
		StringBuffer buffer = new StringBuffer("");					
		for (FeatureMap.Entry entry : featureMap) {
			if (FeatureMapUtil.isText(entry)) {
				buffer.append(entry.getValue().toString());
			} else if (entry.getValue() instanceof AnyType) {
				AnyType anyValue = (AnyType)entry.getValue();
				buffer.append(getTextFromFeatureMap(anyValue.getMixed()));
			} else {
				String whatever1 = entry.getValue().toString();
			}
				
		}

		return buffer.toString().trim();
	}

	public static boolean hasTemplateId(EList<II> templateIds, String matchString) {
		boolean hasId = false;
		if (templateIds != null) {
			for (II ii : templateIds) {
				if (ii.getRoot().equals(matchString))
					hasId = true;
			}
		}
		return hasId;
	}

	public static DateTime parseDate(String date) {
		  
		  // if its a blank string, then simply return null.
		  if(StringUtils.isBlank(date))
		   return null;
		  
		  try {
		   return new DateTime(DateUtils.parseDate(date, datePatterns));
		  } catch (ParseException exc) {
		   // this happens when the date was not parsable by our default date
		   // time parser
		   // so we need to try our other parser.
		   try {
		    DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();
		    return fmt.parseDateTime(date);
		   } catch (IllegalArgumentException e) {
		    //e.printStackTrace();
		    return null;
		   }
		  }
	 }

	public static void setBaseObjectCodes(CodedBaseObject codedBaseObject, CD cd) {
		List<ClinicalCode> codes = new ArrayList<ClinicalCode>();
		ClinicalCode baseCode = CCDAUtils.getClinicalCode(cd);
		if (baseCode != null)
			codes.add(baseCode);
		codes.addAll(CCDAUtils.getCodeTranslations(cd));
		for (int i = 0; i < codes.size(); i++) {
			if (i == 0)
				codedBaseObject.setCode(codes.get(i));
			else
				codedBaseObject.getCodeTranslations().add(codes.get(i));
			
		}
	}

	public static void setBaseObjectIds(BaseObject baseObject, EList<II> iiList) {
		if (baseObject != null) {
			Set<ExternalID> ids = getEidsFromIIList(iiList);
			int idNum = 0;
			if (baseObject.getOriginalId() != null)
				idNum = 1;
			for (ExternalID id : ids) { //.size() > 0) {
				if (idNum == 0)
					baseObject.setOriginalId(id);
				else
					baseObject.getOtherOriginalIds().add(id);
				idNum++;
			}
		}
	}

	public static Section getFirstSectionByTemplateId(EList<Section> sections, String templateId) {
		Section foundSection = null;
		List<Section> foundSections = getSectionsByTemplateId(sections, templateId);
		if (foundSections.size() > 0)
			foundSection = foundSections.get(0);
		return foundSection;
	}
	public static List<Section> getSectionsByTemplateId(EList<Section> sections, String templateId) {
		List<Section> foundSections = new ArrayList<Section>();
		if (sections != null) {
			for (Section section : sections) {
				if (section.hasTemplateId(templateId))
					foundSections.add(section);
			}
		}
		return foundSections;
	}

	public static DateTime getFirstDateTimeFromList(EList<SXCM_TS> effectiveTimes) {
		DateTime dateTime = null;
		if (effectiveTimes != null && effectiveTimes.size() > 0)
			dateTime = getDateTimeFromTS(effectiveTimes.get(0));
		return dateTime;
	}

	public static LabFlags getLabFlag(String flagText) {
		LabFlags labFlag = null;
		if (flagText != null) {
			if (flagText.equalsIgnoreCase("A"))
				labFlag = LabFlags.ABNORMAL;
			else if (flagText.equalsIgnoreCase("HX") || flagText.equalsIgnoreCase("H"))
				labFlag = LabFlags.HIGH;
			else if (flagText.equalsIgnoreCase("LX") || flagText.equalsIgnoreCase("L"))
				labFlag = LabFlags.LOW;
			else if (flagText.equalsIgnoreCase("N"))
				labFlag = LabFlags.NORMAL;
		}
		return labFlag;
	}

	public static String getExternalIDComparable(ExternalID id) {
		String comparableId = "";
		if (id.getId() != null)
			comparableId = id.getId();
		comparableId += "^";
		if (id.getAssignAuthority() != null)
			comparableId += id.getAssignAuthority();
		return comparableId.trim();
	}

	public static String getDoseQuantityString(IVL_PQ doseQuantity) {
		String doseQuantityString = null;
		if (doseQuantity != null && doseQuantity.getValue() != null) {
			doseQuantityString = doseQuantity.getValue().toString();
			if (doseQuantity.getUnit() != null && !doseQuantity.getUnit().equals("1"))
				doseQuantityString += " " + doseQuantity.getUnit();
			doseQuantityString = doseQuantityString.trim();
		}
		return doseQuantityString;
	}

	public static CoverageType getCoverageType(CD coverageCode) {
		CoverageType coverageType = null;
		for (ClinicalCode code : CCDAUtils.getClinicalCodes(coverageCode)) {
            // If this is X12N Insurance Type Code:
            if (code.getCodingSystemOID() != null && code.getCodingSystemOID().equals("2.16.840.1.113883.6.255.1336")) {
                if (code.getCode().equalsIgnoreCase("MB")) {
                    // Medicare Part B is not necessarily Medicare advantage
                    coverageType = CoverageType.MedicareAdvantage;
                }
                // TODO: Finish mappings for existing enumerations
                // HMO,
                // PPO,
                // CMS,
                // EPO,
                // POS,
                // CR;
            }
            // If this is Source of Payment Typology (PHDSC)
			else if (code.getCodingSystemOID() != null && code.getCodingSystemOID().equals("2.16.840.1.114222.4.11.3591")) {
                // TODO: Expand our enumeration and Map the entire value set to our enumeration.
                if (code.getCode().equalsIgnoreCase("11")) {
                    // Medicare Part B is not necessarily Medicare advantage
                    coverageType = CoverageType.MedicareAdvantage;
                }
                // We've observed a code "C1" with a value of Commercial from health net but I'm not sure this means CR
//                else if (code.getCode().equalsIgnoreCase("C1")) {
//                    coverageType = CoverageType.CR;
//                }
                // TODO: Finish mappings for existing enumerations
                // HMO,
                // PPO,
                // CMS,
                // EPO,
                // POS,
                // CR;
			}
		}
		return coverageType;
	}

	public static String getAuthoringDevice(EList<Author> authors) {
		String authoringDevice = "Unknown";
		for (Author author : authors) {
			if (author.getAssignedAuthor() != null && author.getAssignedAuthor().getAssignedAuthoringDevice() != null) {
				AuthoringDevice device = author.getAssignedAuthor().getAssignedAuthoringDevice();
				if (device.getSoftwareName() != null) {
					authoringDevice = device.getSoftwareName().getText();
				}
			}
		}
		return authoringDevice;
	}
}
