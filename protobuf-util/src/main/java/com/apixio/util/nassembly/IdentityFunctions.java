package com.apixio.util.nassembly;

import com.apixio.datacatalog.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.apixio.util.nassembly.DataCatalogProtoUtils.convertUuid;


public class IdentityFunctions {

    public static String getIdentity(ParsingDetailsOuterClass.ParsingDetails proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("uploadBatchId").append(proto.getUploadBatchId())
                .append("sourceFileArchiveUUID").append(proto.getSourceFileArchiveUUID().getUuid())
                .append("version").append(proto.getVersion())
                .append("date").append(proto.getParsingDate())
                .append("parserType").append(proto.getParserType())
                .toString();
    }

    public static String getIdentity(ClinicalActorInfoOuterClass.ClinicalActorInfo proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("primaryId").append(getIdentity(proto.getPrimaryId()))
                .append("role").append(proto.getActorRole())
                .toString();
    }

    public static String getIdentity(ExternalIdOuterClass.ExternalId proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("id").append(proto.getId())
                .append("assignAuthority").append(proto.getAssignAuthority())
                .toString();
    }

    public static String getIdentity(OrganizationOuterClass.Organization proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("primaryId").append(getIdentity(proto.getPrimaryId()))
                .append("name").append(proto.getName())
                .append("alternateIds").append(sortAndJoinExternalIds(proto.getAlternateIdsList()))
                .append("contactDetails").append(getIdentity(proto.getContactDetails()))
                .toString();
    }

    public static String getIdentity(SummaryObjects.EncounterSummary proto) {
        if (proto == null)
            return "";
        // Comes from base encounter info
        return getIdentity(proto.getEncounterInfo());
    }

    public static String getIdentity(EncounterInfoOuterClass.EncounterInfo proto) {
        if (proto == null)
            return "";
        return new StringBuilder()
                .append("primaryId").append(getIdentity(proto.getPrimaryId()))
                .toString();

    }

    public static String getIdentity(ClinicalCodeOuterClass.ClinicalCode proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("system").append(proto.getSystem())
                .append("systemOid").append(proto.getSystemOid())
                .append("code").append(proto.getCode())
                .append("systemVersion").append(proto.getSystemVersion())
                .toString();
    }



    public static String getIdentity(ContactInfoOuterClass.TelephoneNumber proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("number").append(proto.getPhoneNumber())
                .append("type").append(proto.getTelephoneType())
                .toString();
    }

    public static String getIdentity(CareSiteOuterClass.CareSite proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("name").append(proto.getName())
                .append("address").append(getIdentity(proto.getAddress()))
                .append("type").append(proto.getType())
                .toString();
    }


    public static String getIdentity(SourceOuterClass.Source proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("system").append(proto.getSystem())
                .append("type").append(proto.getType())
                .append("clinicalActor").append(getIdentity(proto.getClinicalActorId()))
                .append("organization").append(getIdentity(proto.getOrganization()))
                .append("parsingDetailsId").append(getIdentity(proto.getParsingDetailsId()))
                .toString();
    }

    public static String getIdentity(UUIDOuterClass.UUID uuid) {
        if (uuid == null)
            return "";

        return new StringBuilder()
                .append("uuid").append(uuid.getUuid())
                .toString();

    }


    public static String getIdentity(AddressOuterClass.Address proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("streetAddresses").append(sortAndJoinStrings(proto.getStreetAddressesList()))
                .append("city").append(proto.getCity())
                .append("state").append(proto.getState())
                .append("country").append(proto.getCountry())
                .append("zip").append(proto.getZip())
                .toString();
    }


    public static String getIdentity(NameOuterClass.Name proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("givenNames").append(sortAndJoinStrings(proto.getGivenNamesList()))
                .append("familyNames").append(sortAndJoinStrings(proto.getFamilyNamesList()))
                .append("prefixes").append(sortAndJoinStrings(proto.getPrefixesList()))
                .append("suffixes").append(sortAndJoinStrings(proto.getSuffixesList()))
                .toString();
    }

    public static String getIdentity(ContactInfoOuterClass.ContactInfo proto) {
        if (proto == null)
            return "";

        return new StringBuilder()
                .append("address").append(getIdentity(proto.getAddress()))
                .append("alternateAddresses").append(sortAndJoinAddresses(proto.getAlternateAddressesList()))
                .append("primaryEmail").append(proto.getPrimaryEmail())
                .append("alternateEmails").append(sortAndJoinStrings(proto.getAlternateEmailsList()))
                .append("primaryPhone").append(getIdentity(proto.getPrimaryPhone()))
                .append("alternatePhones").append(sortAndJoinPhones(proto.getAlternatePhonesList()))
                .toString();
    }

    private static String sortAndJoinExternalIds(List<ExternalIdOuterClass.ExternalId> protos) {
        List<String> strings = protos.stream().map(IdentityFunctions::getIdentity).collect(Collectors.toList());
        return sortAndJoinStrings(strings);
    }

    private static String sortAndJoinAddresses(List<AddressOuterClass.Address> protos) {
        List<String> strings = protos.stream().map(IdentityFunctions::getIdentity).collect(Collectors.toList());
        return sortAndJoinStrings(strings);
    }

    private static String sortAndJoinPhones(List<ContactInfoOuterClass.TelephoneNumber> protos) {
        List<String> strings = protos.stream().map(IdentityFunctions::getIdentity).collect(Collectors.toList());
        return sortAndJoinStrings(strings);
    }

    private static String sortAndJoinStrings(List<String> strings) {
        return strings.stream().sorted().collect(Collectors.joining(","));
    }

    public static UUIDOuterClass.UUID identityToUUID(String identity) {
        return convertUuid(UUID.nameUUIDFromBytes(identity.getBytes()));
    }


}