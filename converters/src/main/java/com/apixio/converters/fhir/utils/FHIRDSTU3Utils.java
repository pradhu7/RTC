package com.apixio.converters.fhir.utils;

import com.apixio.converters.fhir.parser.FHIRDSTU3Parser;
import com.apixio.model.patient.*;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Patient;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FHIRDSTU3Utils {
    private static final Logger logger = LoggerFactory.getLogger(FHIRDSTU3Utils.class);

    public static String LIST_TO_STRING_DELIMITER = ", ";

    public static ClinicalCode getClinicalCodeFromDisplay(String display) {
        ClinicalCode code = null;
        if (display != null) {
            code = new ClinicalCode();
            code.setDisplayName(display);
        }
        return code;
    }


    public static ClinicalCode getFirstClinicalCode(List<Coding> coding) {
        ClinicalCode clinicalCode = null;
        if (coding != null && coding.size() > 0) {
            clinicalCode = getClinicalCode(coding.get(0));
        }
        return clinicalCode;
    }
    public static ClinicalCode getClinicalCode(Coding coding) {
        ClinicalCode code = null;
        if (coding != null) {
            code = new ClinicalCode();
            code.setCode(coding.getCode());
            String codingSystemOID = getCodingSystemOID(coding.getSystem());
            code.setCodingSystem(getCodeSystemNameByOID(codingSystemOID));
            code.setCodingSystemOID(codingSystemOID);
            code.setCodingSystemVersions(coding.getVersion());
            code.setDisplayName(coding.getDisplay());
        }
        return code;
    }

    public static String getCodeSystemNameByOID(String oid) {
        String codeSystemName = "";
        switch (oid) {
            case "2.16.840.1.113883.6.96":
                codeSystemName = "SNOMED CT";
                break;
            case "2.16.840.1.113883.6.177":
                codeSystemName = "MeSH";
                break;
            case "2.16.840.1.113883.6.103":
                codeSystemName = "ICD-9CM";
                break;
            case "2.16.840.1.113883.6.90":
                codeSystemName = "ICD-10";
                break;
            case "2.16.840.1.113883.6.88":
                codeSystemName = "RxNorm";
                break;
            case "2.16.840.1.113883.6.69":
                codeSystemName = "NDC";
                break;
            case "2.16.840.1.113883.6.1":
                codeSystemName = "LOINC";
                break;
            case "2.16.840.1.113883.4.9":
                codeSystemName = "Unique Ingredient Identifier";
                break;
            case "2.16.840.1.113883.4.642.2.575":
                codeSystemName = "OMB Race Categories";
                break;
            case "2.16.840.1.113883.6.253":
                codeSystemName = "MDDID";
                break;
            case "2.16.840.1.113883.6.68":
                codeSystemName = "Medispan GPI";
                break;
            case "2.16.840.1.113883.6.162":
                codeSystemName = "Medispan MDDB";
                break;
            default:
                codeSystemName = oid;
                // Only report code systems that aren't rooted in a known base (like epic):
                if (!oid.startsWith("1.2.840.114350")) {
                    logger.warn("Unknown Code System: " + oid);
                }
        }
        return codeSystemName;
    }

    private static String getCodingSystemOID(String system) {
        String cleanOID = system.replace("urn:oid:","");
        // Fix known malformed OIDs
        if (cleanOID.equals("http://snomed.info/sct")) {
            cleanOID = "2.16.840.1.113883.6.96";
        } else if (cleanOID.equals("http://hl7.org/fhir/sid/ndc")) {
            cleanOID = "2.16.840.1.113883.6.69";
        } else if (cleanOID.equals("http://hl7.org/fhir/us/core/ValueSet/omb-race-category")) {
            cleanOID = "2.16.840.1.113883.4.642.2.575";
        } else if (cleanOID.equals("http://www.nlm.nih.gov/research/umls/rxnorm")) {
            cleanOID = "2.16.840.1.113883.6.88";
        } else if (cleanOID.equals("http://loinc.org")) {
            cleanOID = "2.16.840.1.113883.6.1";
        }
        return cleanOID;
    }

    public static List<ClinicalCode> getClinicalCodesFromCodeableConcepts(List<CodeableConcept> codeableConcepts) {
        List<ClinicalCode> codes = new ArrayList<ClinicalCode>();
        for (CodeableConcept codeableConcept : codeableConcepts) {
            codes.addAll(getClinicalCodes(codeableConcept.getCoding()));
        }
        return codes;
    }

    public static List<ClinicalCode> getClinicalCodes(List<Coding> codings) {
        List<ClinicalCode> codes = new ArrayList<ClinicalCode>();
        for (Coding coding : codings) {
            ClinicalCode baseCode = getClinicalCode(coding);
            if (baseCode != null)
                codes.add(baseCode);
        }
        return codes;
    }

    public static void setBaseObjectCodes(CodedBaseObject codedBaseObject, List<Coding> codingList) {
        List<ClinicalCode> codes = getClinicalCodes(codingList);
        for (int i = 0; i < codes.size(); i++) {
            if (i == 0)
                codedBaseObject.setCode(codes.get(i));
            else
                codedBaseObject.getCodeTranslations().add(codes.get(i));
        }
    }

    public static void setBaseObjectIds(BaseObject baseObject, List<Identifier> identifierList) {
        if (baseObject != null) {
            Set<ExternalID> ids = getEidsFromIdentifierList(identifierList);
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

    public static Set<ExternalID> getEidsFromIdentifierList(List<Identifier> identifierList) {
        Set<ExternalID> ids = new HashSet<ExternalID>();
        if (identifierList != null) {
            for (Identifier identifier : identifierList) {
                if (identifier != null) {
                    ids.add(getEidFromIdentifier(identifier));
                }
            }
        }
        return ids;
    }

    public static ExternalID getEidFromIdentifier(Identifier identifier) {
        ExternalID id = new ExternalID();
        id.setId(identifier.getValue());
        // Use the text identifier if available
        if (identifier.getType() != null && identifier.getType().getText() != null) {
            id.setAssignAuthority(identifier.getType().getText());
        } else {
            id.setAssignAuthority(identifier.getSystem());
        }
        return id;
    }

    public static String getAllergySeverities(AllergyIntolerance allergyIntolerance) {
        List<String> allergySeverities = new ArrayList<String>();
        if (allergyIntolerance.getReaction() != null) {
            for (AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent : allergyIntolerance.getReaction()) {
                if (reactionComponent.getSeverity() != null) {
                    allergySeverities.add(reactionComponent.getSeverity().toCode());
                }
            }
        }
        return String.join(LIST_TO_STRING_DELIMITER, allergySeverities);
    }

    public static ClinicalCode getAllergyReactionCode(AllergyIntolerance allergyIntolerance) {
        ClinicalCode allergyReactionCode = null;
        List<String> allergyReactions = getAllergyReactions(allergyIntolerance);
        if (allergyReactions.size() > 0) {
            allergyReactionCode = new ClinicalCode();
            allergyReactionCode.setDisplayName(String.join(LIST_TO_STRING_DELIMITER, allergyReactions));
        }
        return allergyReactionCode;
    }

    public static List<String> getAllergyReactions(AllergyIntolerance allergyIntolerance) {
        List<String> allergyReactions = new ArrayList<String>();
        if (allergyIntolerance.getReaction() != null) {
            for (AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent : allergyIntolerance.getReaction()) {
                allergyReactions.add(reactionComponent.getDescription());
            }
        }
        return allergyReactions;
    }

    public static ActorRole getHealthCareProviderRole(String roleText) {
        ActorRole actorRole = null;
        if (roleText != null) {
            switch (roleText) {
                case "attender":
                    actorRole = ActorRole.ATTENDING_PHYSICIAN;
                    break;
                case "admitter":
                    actorRole = ActorRole.ADMITTING_PARTICIPANT;
                    break;
                case "orderer":
                    actorRole = ActorRole.ORDERING_PROVIDER;
                    break;
                case "performer":
                    actorRole = ActorRole.PERFORMING_PHYSICIAN;
                    break;
                case "recorder":
                    actorRole = ActorRole.RECORDING_PROVIDER;
                    break;
                case "referrer":
                    actorRole = ActorRole.REFERRING_PROVIDER;
                    break;
                case "requester":
                    actorRole = ActorRole.PRESCRIBING_PHYSICIAN;
                    break;
                case "reviewer":
                    actorRole = ActorRole.REVIEWING_PHYSICIAN;
                    break;
                default:
                    try {
                        String enumString = roleText.split("-")[0].trim().toUpperCase().replace(" ","_");
                        actorRole = ActorRole.valueOf(enumString);
                    } catch (Exception ex) {
                        logger.warn("Unknown participant type: " + roleText);
                    }
            }
        }
        return actorRole;
    }

    public static Name getNameFromString(String nameString) {
        Name name = new Name();
        if (nameString != null) {
            // Ex: Attending Inpatient2 MD, MD
            // Ex: Becker, Jon
            // Ex: Rosenberg, Daniel, MD
            String[] segments = nameString.split(",");
            String[] firstSegmentParts = segments[0].trim().split(" ");
            // if there is only one segment, or if the first segment has multiple parts, then the whole first segment is the name
            if (segments.length == 1 || firstSegmentParts.length > 1) {
                // if there is only name  in first part
                name.setFamilyNames(Arrays.asList(firstSegmentParts).subList(0, 1));
                name.setGivenNames(Arrays.asList(firstSegmentParts).subList(1, firstSegmentParts.length));
                if (segments.length > 1) {
                    name.setSuffixes(Arrays.asList(firstSegmentParts).subList(1, firstSegmentParts.length));
                }
            }
            else {
                // else, let's assume that name is in "last, first" format, potentially with a suffix
                name.setFamilyNames(Collections.singletonList(firstSegmentParts[0]));
                // The second segment should contain the given names
                name.setGivenNames(Arrays.asList(segments[1].trim().split(" ")));
                // if we have more than two segments, consider the remaining segments suffixes
                if (segments.length > 2) {
                    name.setSuffixes(Arrays.asList(firstSegmentParts).subList(2, firstSegmentParts.length));
                }
            }
        }
        return name;
    }

    public static EncounterType getEncTypeFromClass(Coding encounterClass) {
        EncounterType encounterType = null;
        if (encounterClass != null) {
            try {
                encounterType = EncounterType.valueOf(encounterClass.getDisplay().toUpperCase().replace(" ","_"));
            } catch (Exception ex) {
                logger.warn("Unknown Encounter class: " + encounterClass.getDisplay());
            }
        }
        return encounterType;
    }

    public static String getDisplayFromDoseQuantity(SimpleQuantity doseQuantity) {
        String value = doseQuantity != null && doseQuantity.hasValue() ? doseQuantity.getValue().toString() : "";
        String unit = doseQuantity != null && doseQuantity.hasUnit() ? doseQuantity.getUnit() : "";
        return value + unit;
    }

    public static List<Name> getNames(List<HumanName> humanNames) {
        List<Name> names = new ArrayList<Name>();
        for (HumanName humanName : humanNames) {
            Name name = new Name();
            // If we don't a structured name, use it:
            if (humanName.getGiven().size() > 0 || humanName.getFamily() != null) {
                name.setGivenNames(stringTypeToString(humanName.getGiven()));
                name.setFamilyNames(Collections.singletonList(humanName.getFamily()));
                name.setSuffixes(stringTypeToString(humanName.getSuffix()));
                name.setPrefixes(stringTypeToString(humanName.getPrefix()));
            } else {
               name = getNameFromString(humanName.getText());
            }
            name.setNameType(getNameTypeFromUse(humanName.getUse()));
            names.add(name);
        }
        return names;
    }

    public static List<String> stringTypeToString(List<StringType> stringTypeList) {
        List<String> strings = new ArrayList<>();
        for (StringType stringType : stringTypeList) {
            strings.add(stringType.toString());
        }
        return strings;
    }

    // TODO: Abstract this out for use in Location addresses and other telephone and email use cases
    public static ContactDetails getContactDetails(List<ContactPoint> contactPoints, List<Address> fhirAddresses) {
        ContactDetails contactDetails = new ContactDetails();
        for (ContactPoint contactPoint : contactPoints) {
            if (contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                if (contactDetails.getPrimaryEmail() == null) {
                    contactDetails.setPrimaryEmail(contactPoint.getValue());
                } else {
                    contactDetails.getAlternateEmails().add(contactPoint.getValue());
                }
            } else if (contactPoint.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                TelephoneNumber telephoneNumber = new TelephoneNumber();
                telephoneNumber.setPhoneNumber(contactPoint.getValue());
                telephoneNumber.setPhoneType(getTelephoneTypeFromUse(contactPoint.getUse()));
                if (contactDetails.getPrimaryPhone() == null) {
                    contactDetails.setPrimaryPhone(telephoneNumber);
                } else {
                    contactDetails.getAlternatePhones().add(telephoneNumber);
                }
            }
        }
        for (Address fhirAddress : fhirAddresses) {
            com.apixio.model.patient.Address address = getApixioAddressfromFHIRAddress(fhirAddress);
            if (contactDetails.getPrimaryAddress() == null) {
                contactDetails.setPrimaryAddress(address);
            } else {
                contactDetails.getAlternateAddresses().add(address);
            }
        }
        return contactDetails;
    }

    public static com.apixio.model.patient.Address getApixioAddressfromFHIRAddress(Address fhirAddress) {
        com.apixio.model.patient.Address address = new com.apixio.model.patient.Address();
        address.setStreetAddresses(stringTypeToString(fhirAddress.getLine()));
        address.setCity(fhirAddress.getCity());
        address.setState(fhirAddress.getState());
        address.setZip(fhirAddress.getPostalCode());
        address.setCountry(fhirAddress.getCountry());
        address.setAddressType(getAddressTypeFromUse(fhirAddress.getUse()));
        return address;
    }

    private static AddressType getAddressTypeFromUse(Address.AddressUse addressUse) {
        AddressType addressType = null;
        if (addressUse != null) {
            switch (addressUse) {
                case HOME:
                    addressType = AddressType.HOME;
                    break;
                case WORK:
                    addressType = AddressType.WORK;
                    break;
                case OLD:
                    addressType = AddressType.OLD;
                    break;
                case TEMP:
                    addressType = AddressType.TEMP;
                    break;
            }
        }
        return addressType;
    }

    private static TelephoneType getTelephoneTypeFromUse(ContactPoint.ContactPointUse contactPointUse) {
        TelephoneType telephoneType = null;
        if (contactPointUse != null) {
            switch (contactPointUse) {
                case HOME:
                    telephoneType = TelephoneType.HOME_PHONE;
                    break;
                case MOBILE:
                    telephoneType = TelephoneType.CELL_PHONE;
                    break;
                case WORK:
                    telephoneType = TelephoneType.WORK_PHONE;
                    break;
                case OLD:
                    telephoneType = TelephoneType.OLD_PHONE;
                    break;
                case TEMP:
                    telephoneType = TelephoneType.TEMP_PHONE;
                    break;
            }
        }
        return telephoneType;
    }

    public static List<String> getLangaugesFromCommunications(List<Patient.PatientCommunicationComponent> patientCommunicationComponents) {
        List<String> languages = new ArrayList<>();
        for (Patient.PatientCommunicationComponent patientCommunicationComponent : patientCommunicationComponents) {
            languages.add(patientCommunicationComponent.getLanguage().getText());
        }
        return languages;
    }

    public static MaritalStatus getMaritalStatus(CodeableConcept maritalStatusCode) {
        MaritalStatus maritalStatus = null;
        if (maritalStatusCode != null) {
            switch (maritalStatusCode.getText()) {
                case "Annulled":
                    maritalStatus = MaritalStatus.ANNULLED;
                    break;
                case "Divorced":
                    maritalStatus = MaritalStatus.DIVORCED;
                    break;
                case "Interlocutory":
                    maritalStatus = MaritalStatus.INTERLOCUTORY;
                    break;
                case "Legally Separated":
                    maritalStatus = MaritalStatus.LEGALLY_SEPARATED;
                    break;
                case "Married":
                    maritalStatus = MaritalStatus.MARRIED;
                    break;
                case "Polygamous":
                    maritalStatus = MaritalStatus.POLYGAMOUS;
                    break;
                case "Never Married":
                    maritalStatus = MaritalStatus.NEVER_MARRIED;
                    break;
                case "Domestic partner":
                    maritalStatus = MaritalStatus.DOMESTIC_PARTNER;
                    break;
                case "unmarried":
                case "Single": // NOTE: This is not an official value but was observed from Epic
                    maritalStatus = MaritalStatus.UNMARRIED;
                    break;
                case "Widowed":
                    maritalStatus = MaritalStatus.WIDOWED;
                    break;
                default:
                    logger.warn("Unknown marital status: " + maritalStatusCode.getText());
            }
        }
        return maritalStatus;
    }

    private static NameType getNameTypeFromUse(HumanName.NameUse nameUse) {
        NameType nameType = null;
        if (nameUse != null) {
            switch (nameUse) {
                case USUAL:
                    nameType = NameType.USUAL;
                    break;
                case OFFICIAL:
                    nameType = NameType.OFFICIAL;
                    break;
                case TEMP:
                    nameType = NameType.TEMP;
                    break;
                case NICKNAME:
                    nameType = NameType.NICK_NAME;
                    break;
                case ANONYMOUS:
                    nameType = NameType.ANONYMOUS;
                    break;
                case OLD:
                    nameType = NameType.OLD;
                    break;
                case MAIDEN:
                    nameType = NameType.MAIDEN_NAME;
                    break;
            }
        }
        return nameType;
    }

    public static CareSiteType getCareSiteTypeFromCode(List<Extension> extensions) {
        CareSiteType careSiteType = null;
        // Loop through each extension cod to try and get CareSiteType
        for (Extension extension : extensions) {
            CodeableConcept extensionCode = (CodeableConcept) extension.getValue();
            for (Coding coding : extensionCode.getCoding()) {
                try {
                    careSiteType = CareSiteType.valueOf(coding.getDisplay().toUpperCase().replace(" ","_"));
                } catch (Exception ex) {
                    logger.warn("Unknown location display: " + coding.getDisplay());
                }
            }

        }
        return careSiteType;
    }

    public static String getRouteofAdministration(List<Dosage> dosageList) {
        Set<String> routeOfAdministrations = new HashSet<>();
        for (Dosage dosage : dosageList) {
            routeOfAdministrations.add(dosage.getRoute().getText());
        }
        return String.join(FHIRDSTU3Utils.LIST_TO_STRING_DELIMITER, routeOfAdministrations);
    }

    public static String getDosage(List<Dosage> dosageList) {
        Set<String> dosages = new HashSet<>();
        for (Dosage dosage : dosageList) {
            if (dosage.hasDoseSimpleQuantity()) {
                dosages.add(FHIRDSTU3Utils.getDisplayFromDoseQuantity(dosage.getDoseSimpleQuantity()));
            } else if (dosage.hasDoseRange()) {
                // What do we do with a range? make a composite?
                String low = FHIRDSTU3Utils.getDisplayFromDoseQuantity(dosage.getDoseRange().getLow());
                String high = FHIRDSTU3Utils.getDisplayFromDoseQuantity(dosage.getDoseRange().getHigh());
                String rangeDisplay = low + " to " + high;
                dosages.add(rangeDisplay);
            }
        }
        return String.join(FHIRDSTU3Utils.LIST_TO_STRING_DELIMITER, dosages);
    }

    public static String getFrequency(List<Dosage> dosageList) {
        Set<String> timings = new HashSet<>();
        for (Dosage dosage : dosageList) {
            timings.add(dosage.getTiming().getCode().getText());
        }
        return String.join(FHIRDSTU3Utils.LIST_TO_STRING_DELIMITER, timings);
    }

    public static String getInstructions(List<Dosage> dosageList) {
        Set<String> instructions = new HashSet<>();
        for (Dosage dosage : dosageList) {
            if (dosage.getPatientInstruction() != null) {
                instructions.add(dosage.getPatientInstruction());
            }
        }
        return String.join(FHIRDSTU3Utils.LIST_TO_STRING_DELIMITER, instructions);
    }

    public static DateTime getDateTimeFromEffective(Type effective) {
        DateTime dateTime = null;
        if (effective instanceof DateTimeType) {
            DateTimeType dateTimeType = (DateTimeType) effective;
            dateTime = new DateTime(dateTimeType.getValue());
        } else if (effective instanceof Period) {
            Period period = (Period) effective;
            // If we got a period for this value, use the end time if available, else use the start
            if (period.hasEnd()) {
                dateTime = new DateTime(period.getEnd());
            } else {
                dateTime = new DateTime(period.getStart());
            }
        }
        return dateTime;
    }

    public static LabFlags getLabFlagFromInterpretation(CodeableConcept interpretation) {
        LabFlags labFlag = null;
        if (interpretation != null && interpretation.hasText()) {
            String labFlagString = interpretation.getText().toUpperCase();
            try {
                labFlag = LabFlags.valueOf(labFlagString);
            } catch (Exception ex) {
                logger.warn("Unknown interpretation: " + labFlagString);
            }
        }
        return labFlag;
    }
}
