package com.apixio.bizlogic.patient.assembly.demographic;

import java.util.List;
import java.util.Set;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.Address;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.TelephoneNumber;

public class EmptyDemographicUtil extends AssemblerUtility
{
    public static boolean isEmpty(Patient apo)
    {
        return ( isEmpty(apo.getPrimaryDemographics()) && isEmptyDemographics((List<Demographics>) apo.getAlternateDemographics()) &&
                 isEmpty(apo.getPrimaryContactDetails()) && isEmptyContacts((List<ContactDetails>) apo.getAlternateContactDetails()) &&
                 isEmpty((Set<ExternalID>) apo.getExternalIDs(), apo.getPrimaryExternalID()) );
    }

    private static boolean isEmptyDemographics(List<Demographics> demographicsList)
    {
        if (demographicsList == null)
            return true;

        for (Demographics demographics: demographicsList)
        {
            if (!isEmpty(demographics))
                return false;
        }

        return true;
    }

    public static boolean isEmpty(Demographics d)
    {
        return !((d != null)
                && ((d.getDateOfBirth() != null) || (d.getGender() != Gender.UNKNOWN) || !isEmpty(d.getName())));
    }

    private static boolean isEmptyContacts(List<ContactDetails> contactsList)
    {
        if (contactsList == null)
            return true;

        for (ContactDetails contacts: contactsList)
        {
            if (!isEmpty(contacts))
                return false;
        }

        return true;
    }

    public static boolean isEmpty(ContactDetails contacts)
    {
        if (contacts == null)
            return true;

        StringBuilder allContacts = new StringBuilder();

        allContacts.append(makeAddress(contacts.getPrimaryAddress()));
        allContacts.append(makeAddress(contacts.getAlternateAddresses()));
        allContacts.append(makeString(contacts.getPrimaryEmail()));
        allContacts.append(makeList(contacts.getAlternateEmails()));
        allContacts.append(makePhoneNumber(contacts.getPrimaryPhone()));
        allContacts.append(makePhoneNumber(contacts.getAlternatePhones()));


        return isEmpty(allContacts.toString());
    }

    public static boolean isEmpty(Name n)
    {
        if (n == null)
            return true;

        StringBuilder allNames = new StringBuilder();

        allNames.append(makeList(n.getFamilyNames()));
        allNames.append(makeList(n.getGivenNames()));

        return isEmpty(allNames.toString());
    }

    public static boolean isEmpty(Set<ExternalID> extIDs, ExternalID primeExtID)
    {
        if (extIDs == null)
            return true;

        StringBuilder allIDs = new StringBuilder();

        String primary = makeExternalId(primeExtID);

        for (ExternalID externalID : extIDs)
        {
            String id = makeExternalId(externalID);
            if (!id.isEmpty() && !id.equals(primary))
                allIDs.append(id);
        }

        return isEmpty(allIDs.toString());
    }

    private static String makeAddress(List<Address> addresses)
    {
        if (addresses == null)
            return "";

        StringBuilder allAddresses = new StringBuilder();

        for (Address address : addresses)
        {
            allAddresses.append(makeAddress(address));
        }

        return allAddresses.toString();
    }

    private static String makeAddress(Address a)
    {
        if (a == null)
            return "";

        StringBuilder allAddresses = new StringBuilder();

        allAddresses.append(makeList(a.getStreetAddresses()));
        allAddresses.append(makeString(a.getCity()));
        allAddresses.append(makeString(a.getState()));
        allAddresses.append(makeString(a.getZip()));
        allAddresses.append(makeString(a.getCounty()));
        allAddresses.append(makeString(a.getCountry()));

        return allAddresses.toString();
    }

    static String makePhoneNumber(List<TelephoneNumber> telephoneNumbers)
    {
        if (telephoneNumbers == null)
            return "";

        StringBuilder allNumbers = new StringBuilder();

        for (TelephoneNumber telephoneNumber : telephoneNumbers)
        {
            allNumbers.append(makePhoneNumber(telephoneNumber));
        }

        return allNumbers.toString();
    }

    static String makePhoneNumber(TelephoneNumber p)
    {
        if (p == null)
            return "";

        return makeString(p.getPhoneNumber());
    }

    static String makeList(List<String> strings)
    {
        if (strings == null)
            return "";

        StringBuilder sb = new StringBuilder();

        for (String s : strings)
        {
            sb.append(makeString(s));
        }

        return sb.toString();
    }
}
