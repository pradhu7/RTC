package com.apixio.converters.ccda.creator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.eclipse.mdht.uml.cda.CDAFactory;
import org.eclipse.mdht.uml.hl7.datatypes.AD;
import org.eclipse.mdht.uml.hl7.datatypes.ANY;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.CE;
import org.eclipse.mdht.uml.hl7.datatypes.CS;
import org.eclipse.mdht.uml.hl7.datatypes.DatatypesFactory;
import org.eclipse.mdht.uml.hl7.datatypes.ED;
import org.eclipse.mdht.uml.hl7.datatypes.ENXP;
import org.eclipse.mdht.uml.hl7.datatypes.II;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.IVXB_TS;
import org.eclipse.mdht.uml.hl7.datatypes.ON;
import org.eclipse.mdht.uml.hl7.datatypes.PN;
import org.eclipse.mdht.uml.hl7.datatypes.SC;
import org.eclipse.mdht.uml.hl7.datatypes.ST;
import org.eclipse.mdht.uml.hl7.datatypes.TEL;
import org.eclipse.mdht.uml.hl7.datatypes.TS;
import org.eclipse.mdht.uml.hl7.vocab.NullFlavor;
import org.eclipse.mdht.uml.hl7.vocab.TelecommunicationAddressUse;

import com.apixio.model.patient.Address;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.MaritalStatus;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Organization;
import com.apixio.model.patient.TelephoneNumber;
import com.apixio.model.patient.TelephoneType;

public class CCDCreatorUtils {

	public static final String cdaDateFormat = "yyyyMMddHHmmssZ";

	public static CE getGender(Gender gender) {
		CE genderCode = getCE("Unknown", "U", "2.16.840.1.113883.5.1", "AdministrativeGender");
		if (gender.equals(Gender.MALE))
			genderCode = getCE("Male", "M", "2.16.840.1.113883.5.1", "AdministrativeGender");
		if (gender.equals(Gender.FEMALE))
			genderCode = getCE("Female", "F", "2.16.840.1.113883.5.1", "AdministrativeGender");
		if (gender.equals(Gender.TRANSGENDER))
			genderCode = getCE("Undifferentiated", "UN", "2.16.840.1.113883.5.1", "AdministrativeGender");
		return genderCode;
	}

	public static CE getGender(String sex) {
		CE genderCode = getCE("Unknown", "U", "2.16.840.1.113883.5.1", "AdministrativeGender");
		if (sex.equalsIgnoreCase("M"))
			genderCode = getCE("Male", "M", "2.16.840.1.113883.5.1", "AdministrativeGender");
		if (sex.equalsIgnoreCase("F"))
			genderCode = getCE("Female", "F", "2.16.840.1.113883.5.1", "AdministrativeGender");
		return genderCode;
	}

	public static CE getCE(String displayName, String code, String codeSystem, String codeSystemName) {
		CE ce = DatatypesFactory.eINSTANCE.createCE();
		if (displayName != null && !displayName.equals(""))
			ce.setDisplayName(displayName);
		if (code != null && !code.equals(""))
			ce.setCode(code);
		if (codeSystem != null && !codeSystem.equals(""))
			ce.setCodeSystem(codeSystem);
		if (codeSystemName != null && !codeSystemName.equals(""))
			ce.setCodeSystemName(codeSystemName);
		return ce;
	}

	public static PN getName(String firstName, String lastName) {
		PN name = DatatypesFactory.eINSTANCE.createPN();
		name.getGivens().add(getENXP(firstName));
		name.getFamilies().add(getENXP(lastName));
		return name;
	}

	public static PN getPNFromName(Name name) {
		PN pn = DatatypesFactory.eINSTANCE.createPN();
		pn.getGivens().addAll(getENXPList(name.getGivenNames()));
		pn.getFamilies().addAll(getENXPList(name.getFamilyNames()));
		pn.getPrefixes().addAll(getENXPList(name.getPrefixes()));
		pn.getSuffixes().addAll(getENXPList(name.getSuffixes()));
		return pn;
	}

	public static PN getName(String fullName) {
		PN name = DatatypesFactory.eINSTANCE.createPN();
		name.addText(fullName);
		return name;
	}

	public static List<ENXP> getENXPList(List<String> nameList) {
		List<ENXP> enxpList = new LinkedList<ENXP>();
		for (String name : nameList) {
			ENXP namePart = DatatypesFactory.eINSTANCE.createENXP();
			namePart.addText(name);
			enxpList.add(namePart);
		}
		return enxpList;
	}

	public static ENXP getENXP(String nameText) {
		ENXP namePart = DatatypesFactory.eINSTANCE.createENXP();
		namePart.addText(nameText);
		return namePart;
	}

	public static TS getTS(DateTime dateTime) {
		TS ts = DatatypesFactory.eINSTANCE.createTS();
		if (dateTime != null)
			ts.setValue(dateTime.toString(cdaDateFormat));
		return ts;
	}

	public static II createId(String root) {
		return createId(root, "", "");
	}

	public static II createId(String root, String extension) {
		return createId(root, extension, "");
	}

	public static II createId(String root, String extension, String assigningAuthorityName) {
		II id = DatatypesFactory.eINSTANCE.createII();
		if (!root.equals(""))
			id.setRoot(root);
		if (!extension.equals(""))
			id.setExtension(extension);
		if (!assigningAuthorityName.equals(""))
			id.setAssigningAuthorityName(assigningAuthorityName);
		return id;
	}

	public static CS createCS(String code) {
		CS cs = DatatypesFactory.eINSTANCE.createCS();
		cs.setCode(code);
		return cs;
	}

	public static CD createCode(String displayName, String codeValue, String codeSystem, String codeSystemName) {
		CD code = DatatypesFactory.eINSTANCE.createCD();
		if (displayName != null && !displayName.equals(""))
			code.setDisplayName(displayName);
		if (codeValue != null && !codeValue.equals(""))
			code.setCode(codeValue);
		if (codeSystem != null && !codeSystem.equals(""))
			code.setCodeSystem(codeSystem);
		if (codeSystemName != null && !codeSystemName.equals(""))
			code.setCodeSystemName(codeSystemName);
		return code;
	}

	public static CE createCode(NullFlavor nullFlavor)
	{
		CE code = DatatypesFactory.eINSTANCE.createCE();
		code.setNullFlavor(nullFlavor);
		return code;
	}

	public static TS getCurrentTime() {
		TS currentTime = DatatypesFactory.eINSTANCE.createTS();
		String currentTimeString = DateTime.now().toString(cdaDateFormat);
		currentTime.setValue(currentTimeString);
		return currentTime;
	}

	public static II getRandomId() {
		String randomUuid = UUID.randomUUID().toString();
		II id = DatatypesFactory.eINSTANCE.createII(randomUuid);
		return id;
	}

	public static ED getTextWithReference(String string) {
		ED ed = DatatypesFactory.eINSTANCE.createED();
		ed.setReference(createReference(string));
		return ed;
	}

	public static TEL createReference(String string) {
		TEL reference = DatatypesFactory.eINSTANCE.createTEL();
		reference.setValue(string);
		return reference;
	}

	public static ANY createCD(String displayName, String codeValue, String codeSystem, String codeSystemName)
	{
		CD code = DatatypesFactory.eINSTANCE.createCD();
		code.setDisplayName(displayName);
		code.setCode(codeValue);
		code.setCodeSystem(codeSystem);
		code.setCodeSystemName(codeSystemName);
		return code;
	}

	public static ED getText(String text) {
		ED ed = DatatypesFactory.eINSTANCE.createED();
		if (text != null)
			ed.addText(text);
		return ed;
	}

	public static IVL_TS getIVLTS(DateTime exactDate) {
		IVL_TS exactTime = DatatypesFactory.eINSTANCE.createIVL_TS();
		if (exactDate != null)
			exactTime.setValue(exactDate.toString(cdaDateFormat));
		else
			exactTime.setNullFlavor(NullFlavor.UNK);

		return exactTime;
	}
	public static IVL_TS getIVLTS(DateTime startDate, DateTime endDate) {
		IVL_TS interval = DatatypesFactory.eINSTANCE.createIVL_TS();
		interval.setLow(getIVXBTS(startDate));
		if (endDate != null)
			interval.setHigh(getIVXBTS(endDate));

		return interval;
	}

	public static IVXB_TS getIVXBTS(DateTime date) {
		IVXB_TS datePart = DatatypesFactory.eINSTANCE.createIVXB_TS();
		if (date != null)
			datePart.setValue(date.toString(cdaDateFormat));
		else
			datePart.setNullFlavor(NullFlavor.UNK);
			// datePart.setValue(DateTime.now().toString(cdaDateFormat));
		return datePart;
	}

	public static SC getSC(String string) {
		SC sc = DatatypesFactory.eINSTANCE.createSC();
		sc.addText(string);
		return sc;
	}

	public static ST getString(String string) {
		ST st = DatatypesFactory.eINSTANCE.createST();
		if (!string.equals(""))
			st.addText(string);
		return st;
	}

	public static II getIdFromExternalID(ExternalID primaryId) {
		II ii = null;
		if (primaryId != null)
			ii = getId(primaryId.getId(), primaryId.getSource(), primaryId.getAssignAuthority());
		return ii;
	}

	public static List<II> getIdsFromExternalIDs(Iterable<ExternalID> externalIDs) {
		List<II> iis = new ArrayList<II>();
		for (ExternalID id : externalIDs) {
			iis.add(getId(id.getId(), id.getSource(), id.getAssignAuthority()));
		}
		return iis;
	}

	public static II getId(String root, String extension, String assigningAuthorityName) {
		II id = DatatypesFactory.eINSTANCE.createII();
		if (root != null && !root.equals(""))
			id.setRoot(root);
		if (extension != null && !extension.equals(""))
			id.setExtension(extension);
		if (assigningAuthorityName != null && !assigningAuthorityName.equals(""))
			id.setAssigningAuthorityName(assigningAuthorityName);
		return id;
	}

	public static AD createAddress(Address address) {
		AD ccdAddress = null;
		if (address != null) {
			ccdAddress = DatatypesFactory.eINSTANCE.createAD();
			for (String streetAddress : address.getStreetAddresses()) {
				ccdAddress.addStreetAddressLine(streetAddress);
			}
			ccdAddress.addCity(address.getCity());
			ccdAddress.addState(address.getState());
			ccdAddress.addPostalCode(address.getZip());
			ccdAddress.addCountry(address.getCountry());
		} else {
			ccdAddress = createNullAddress();
		}
		return ccdAddress;
	}

	public static AD createNullAddress() {
		AD nullAddress = DatatypesFactory.eINSTANCE.createAD();
		nullAddress.setNullFlavor(NullFlavor.NI);
		return nullAddress;
	}

	public static TEL createNullTelecom() {
		TEL nullTelecom = DatatypesFactory.eINSTANCE.createTEL();
		nullTelecom.setNullFlavor(NullFlavor.NI);
		return nullTelecom;
	}

	public static TEL createTelecom(TelephoneNumber primaryPhone) {
		TEL ccdTelecom = null;
		if (primaryPhone != null) {
			ccdTelecom = DatatypesFactory.eINSTANCE.createTEL();
			ccdTelecom.setValue(primaryPhone.getPhoneNumber());
			TelecommunicationAddressUse telUse = getTelecommunicationAddressUse(primaryPhone.getPhoneType());
			if (telUse != null)
				ccdTelecom.getUses().add(telUse);
		} else {
			ccdTelecom = createNullTelecom();
		}
		return ccdTelecom;
	}

	private static TelecommunicationAddressUse getTelecommunicationAddressUse(TelephoneType phoneType) {
		TelecommunicationAddressUse telUse = null;
		if (phoneType != null) {
			if (phoneType.equals(TelephoneType.HOME_PHONE))
				telUse = TelecommunicationAddressUse.HP;
			if (phoneType.equals(TelephoneType.WORK_PHONE))
				telUse = TelecommunicationAddressUse.WP;
		}
		return telUse;
	}

	public static CE getMaritalStatus(MaritalStatus maritalStatus) {
		CE maritalStatusCode = null;
		if (maritalStatus != null) {
			String codeValue = "";
			if (maritalStatus.equals(MaritalStatus.ANNULLED))
				codeValue = "A";
			if (maritalStatus.equals(MaritalStatus.DIVORCED))
				codeValue = "D";
			if (maritalStatus.equals(MaritalStatus.INTERLOCUTORY))
				codeValue = "I";
			if (maritalStatus.equals(MaritalStatus.LEGALLY_SEPARATED))
				codeValue = "L";
			if (maritalStatus.equals(MaritalStatus.MARRIED))
				codeValue = "M";
			if (maritalStatus.equals(MaritalStatus.POLYGAMOUS))
				codeValue = "P";
			if (maritalStatus.equals(MaritalStatus.NEVER_MARRIED))
				codeValue = "S";
			if (maritalStatus.equals(MaritalStatus.DOMESTIC_PARTNER))
				codeValue = "T";
			if (maritalStatus.equals(MaritalStatus.WIDOWED))
				codeValue = "W";
			if (!codeValue.equals(""))
				maritalStatusCode = getCE(maritalStatus.toString(), codeValue, "2.16.840.1.113883.5.2", "");
		}
		return maritalStatusCode;
	}

	public static org.eclipse.mdht.uml.cda.Organization getOrganization(Organization organization) {
		org.eclipse.mdht.uml.cda.Organization cdaOrganization = CDAFactory.eINSTANCE.createOrganization();
		cdaOrganization.getIds().add(getIdFromExternalID(organization.getPrimaryId()));
		ContactDetails orgContact = organization.getContactDetails();
		if (orgContact != null) {
			cdaOrganization.getAddrs().add(createAddress(orgContact.getPrimaryAddress()));
			cdaOrganization.getTelecoms().add(createTelecom(orgContact.getPrimaryPhone()));
		}
		cdaOrganization.getNames().add(createON(organization.getName()));
		return cdaOrganization;
	}

	public static ON createON(String string) {
		ON on = DatatypesFactory.eINSTANCE.createON();
		on.addText(string);
		return on;
	}
}
