package com.apixio.bizlogic.patient.assembly;

import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.Address;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.TelephoneNumber;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by dyee on 4/27/17.
 */
public class SummaryTestUtils {

    private final static String [] datePatterns = new String [] {
        "yyyyMMdd",
        "yyyyMMddkkmmssZ",
        "yyyyMMddkkmmss",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd",
        "MM/dd/yyyy HH:mm:ss aaa",
        "MM/dd/yyyy",
        "yyyy-MM-dd hh:ss:mm.sss",
        "yyyy-MM-dd hh:ss:mm"
    };

    public static DateTime parseDateTime(String date) {
        DateTime dateTime = null;
        if(date != null && date.trim().length() > 0) {
            try {
                return new DateTime(DateUtils.parseDate(date, datePatterns));
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return dateTime;
    }

    public static Name createName(String first, String last) {

        Name name = new Name();
        name.getGivenNames().add(first);
        name.getFamilyNames().add(last);
        return name;
    }

    public static UUID addClinicalActor(Patient patient, String first, String last, String id, String assign_authority, ActorRole role) {

        ClinicalActor act1 = new ClinicalActor();
        act1.setActorGivenName(SummaryTestUtils.createName(first, last));
        act1.setOriginalId(createExternalId(id, assign_authority));
        patient.addClinicalActor(act1);
        return act1.getInternalUUID();
    }

    public static ExternalID createExternalId(String id, String assignAuthority) {
        ExternalID extId = new ExternalID();
        extId.setId(id);
        extId.setAssignAuthority(assignAuthority);
        return extId;
    }

    public static ClinicalCode createClinicalCode(String displayName, String code, String codeSystem) {
        ClinicalCode clinicalCode = new ClinicalCode();
        clinicalCode.setCode(code);
        clinicalCode.setDisplayName(displayName);
        clinicalCode.setCodingSystem(codeSystem);
        return clinicalCode;
    }

    public static Demographics createDemographics(String firstName, String middleName, String lastName, String dob, String gender) {
        Demographics demographics = new Demographics();
        Name name = new Name();
        name.getGivenNames().add(firstName);
        if (!StringUtils.isEmpty(middleName))
            name.getGivenNames().add(middleName);
        name.getFamilyNames().add(lastName);
        demographics.setName(name);
        demographics.setDateOfBirth(parseDateTime(dob));
        demographics.setGender(getGender(gender));
        return demographics;
    }

    public static List<Encounter> createEncounters(int num_encounters){
        List<Encounter> results = new ArrayList<>();
        for (int i = 0; i < num_encounters; i++){
            Encounter e = new Encounter();

            ExternalID eid = new ExternalID();
            eid.setId(UUID.randomUUID().toString());
            eid.setAssignAuthority("Random UUID");

            e.setOriginalId(eid);

            e.setEncounterStartDate(DateTime.now());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            e.setEncounterEndDate(DateTime.now());
            CareSite c = new CareSite();
            c.setCareSiteName("C" + DateTime.now().getMillisOfDay());
            e.setSiteOfService(new CareSite());
            results.add(e);
        }
        return results;
    }

    public static List<ClinicalActor> createLogicallyDuplicateActors(UUID id, int num_actors){
        List<ClinicalActor> results = new ArrayList<>();
        for (int i = 0; i < num_actors; i++){
            ClinicalActor c = new ClinicalActor();
            c.setInternalUUID(UUID.randomUUID());
            ExternalID exID = new ExternalID();
            exID.setId("exID" + id);
            exID.setAssignAuthority("DAVIDYEE");
            c.setPrimaryId(exID);
            c.setRole(ActorRole.CONSULTING_PROVIDER);
            results.add(c);
        }
        return results;
    }

    public static List<ClinicalActor> createClinicalActors(int num_actors){
        List<ClinicalActor> results = new ArrayList<>();
        for (int i = 0; i < num_actors; i++){
            ClinicalActor c = new ClinicalActor();
            c.setInternalUUID(UUID.randomUUID());
            ExternalID exID = new ExternalID();
            exID.setId("exID" + UUID.randomUUID());
            exID.setAssignAuthority("DAVIDYEE");
            c.setPrimaryId(exID);
            c.setRole(ActorRole.CONSULTING_PROVIDER);
            results.add(c);
        }
        return results;
    }

    private static Gender getGender(String sex) {
        Gender gender = Gender.UNKNOWN;
        if (sex.toLowerCase().startsWith("m")) {
            gender = Gender.MALE;
        } else if (sex.toLowerCase().startsWith("f")) {
            gender = Gender.FEMALE;
        }
        return gender;
    }

    public static ContactDetails createContactDetails(String streetAddressLine1, String city, String state, String zip, String email, String phone) {
        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setPrimaryAddress(createAddress(streetAddressLine1, city, state, zip));
        if (!StringUtils.isBlank(email))
            contactDetails.setPrimaryEmail(email);
        if (!StringUtils.isBlank(phone))
            contactDetails.setPrimaryPhone(createPhoneNumber(phone));
        return contactDetails;
    }

    private static TelephoneNumber createPhoneNumber(String phone) {
        TelephoneNumber phoneNumber = new TelephoneNumber();
        phoneNumber.setPhoneNumber(phone);
        return phoneNumber;
    }

    private static Address createAddress(String streetAddressLine1, String city, String state, String zip) {
        Address address = new Address();
        address.getStreetAddresses().add(streetAddressLine1);
        address.setCity(city);
        address.setState(state);
        address.setZip(zip);
        return address;
    }

    public static Source createSource(DateTime dateTime) {
        Source s = new Source();
        s.setSourceSystem("SummaryTestUtils");
        s.setCreationDate(dateTime);
        return s;
    }
}
