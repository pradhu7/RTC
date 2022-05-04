package com.apixio.nassembly.apo;

import com.apixio.datacatalog.*;
import com.apixio.model.patient.*;
import com.apixio.nassembly.demographics.DemographicsMetadataKeys;
import com.apixio.util.nassembly.DataCatalogProtoUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.apixio.util.nassembly.DataCatalogProtoUtils.convertUuid;

public class BaseConverter {

    public static void applyDataCatalogMeta(BaseObject object, DataCatalogMetaOuterClass.DataCatalogMeta meta) {
        if (meta.getSerializedSize() > 0) {
            object.setOtherOriginalIds(meta.getOtherOriginalIdsList()
                    .stream()
                    .filter(id -> id.getSerializedSize() > 0)
                    .map(BaseConverter::convertExternalId)
                    .collect(Collectors.toList())
            );

            if (meta.getOriginalId().getSerializedSize() > 0)
                object.setOriginalId(convertExternalId(meta.getOriginalId()));

            object.setLastEditDateTime(new DateTime(meta.getLastEditTime()));
            object.setEditType(convertEditType(meta.getEditType()));
        }
    }

    public static ParsingDetail convertParsingDetails(ParsingDetailsOuterClass.ParsingDetails proto) {
        if (proto == null) return null;

        ParsingDetail detail = new ParsingDetail();
        if (StringUtils.isNotBlank(proto.getParserType())) detail.setParser(ParserType.valueOf(proto.getParserType()));
        if (StringUtils.isNotBlank(proto.getVersion())) detail.setParserVersion(proto.getVersion());
        if (proto.hasInternalId()) detail.setParsingDetailsId(convertUuid(proto.getInternalId()));
        detail.setParsingDateTime(new DateTime(proto.getParsingDate()));
        if (proto.hasSourceFileArchiveUUID()) detail.setSourceFileArchiveUUID(convertUuid(proto.getSourceFileArchiveUUID()));
        if (StringUtils.isNotBlank(proto.getSourceFileHash())) detail.setSourceFileHash(proto.getSourceFileHash());

        detail.setMetaTag("sourceUploadBatch", proto.getUploadBatchId());

        return detail;
    }

    public static Source convertSource(SourceOuterClass.Source proto, Optional<CoverageInfoOuterClass.CoverageInfo> coverageWithSpanOption) {
        if (proto == null) return null;

        Source source = new Source();


        // Backwards compatible stuff for legacy coverage
        if (coverageWithSpanOption.isPresent()) {
            CoverageInfoOuterClass.CoverageInfo coverageWithSpan = coverageWithSpanOption.get();

            if (coverageWithSpan.hasDciStartDate()) {
                source.setDciStart(DataCatalogProtoUtils.toDateTime(coverageWithSpan.getDciStartDate()).toLocalDate());
            } else {
                // Fall back to start date
                source.setDciStart(DataCatalogProtoUtils.toDateTime(coverageWithSpan.getStartDate()).toLocalDate());
            }

            if (coverageWithSpan.hasDciEndDate()) {
                source.setDciEnd(DataCatalogProtoUtils.toDateTime(coverageWithSpan.getDciEndDate()).toLocalDate());
            } else if (coverageWithSpan.hasEndDate()) {
                // Fall back to end date
                source.setDciEnd(DataCatalogProtoUtils.toDateTime(coverageWithSpan.getEndDate()).toLocalDate());
            }
            
            coverageWithSpanOption
                    .filter(c -> !c.getHealthPlanName().isEmpty())
                    .ifPresent(c -> source.setMetaTag("HEALTH_PLAN", c.getHealthPlanName()));

        }


        source.setSourceId(convertUuid(proto.getInternalId()));
        source.setSourceSystem(proto.getSystem());
        source.setSourceType(proto.getType());
        source.setCreationDate(new DateTime(proto.getCreationDate()));
        if (proto.hasParsingDetailsId()) source.setParsingDetailsId(convertUuid(proto.getParsingDetailsId()));
        source.setInternalUUID(convertUuid(proto.getInternalId()));
        return source;
    }

    public static ClinicalActor convertClinicalActor(BaseObjects.ClinicalActor proto) {
        if (proto == null) return null;

        ClinicalActorInfoOuterClass.ClinicalActorInfo actorInfoProto = proto.getClinicalActorInfo();


        ClinicalActor actor = new ClinicalActor();
        if (!actorInfoProto.getPrimaryId().equals(ExternalIdOuterClass.ExternalId.getDefaultInstance())) {
            ExternalID id = convertExternalId(actorInfoProto.getPrimaryId());
            actor.setPrimaryId(id);
            actor.setOriginalId(id);
        }

        actor.setAlternateIds(
                actorInfoProto
                        .getAlternateIdsList()
                        .stream().map(BaseConverter::convertExternalId)
                        .collect(Collectors.toList())
        );


        if (StringUtils.isNotBlank(actorInfoProto.getActorRole())) actor.setRole(ActorRole.valueOf(actorInfoProto.getActorRole()));
        actor.setTitle(actorInfoProto.getTitle());
        if (actorInfoProto.hasActorGivenName()) actor.setActorGivenName(convertName(actorInfoProto.getActorGivenName()));
        actor.setActorSupplementalNames(
                actorInfoProto
                        .getActorSupplementalNamesList()
                        .stream().map(BaseConverter::convertName)
                        .collect(Collectors.toList())
        );
        if (actorInfoProto.hasContactDetails()) actor.setContactDetails(convertContactDetails(actorInfoProto.getContactDetails()));
        actor.setClinicalActorId(convertUuid(proto.getInternalId()));
        actor.setInternalUUID(convertUuid(proto.getInternalId()));

        if (actorInfoProto.hasAssociatedOrg()) actor.setAssociatedOrg(convertOrganization(actorInfoProto.getAssociatedOrg()));

        normalizeAndSetBaseData(proto.getBase(), actor);

        return actor;
    }

    public static ContactDetails convertContactDetails(ContactInfoOuterClass.ContactInfo proto) {
        if (proto == null) return null;

        ContactDetails details = new ContactDetails();
        if (proto.hasAddress()) details.setPrimaryAddress(convertAddress(proto.getAddress()));
        details.setAlternateAddress(proto
                .getAlternateAddressesList()
                .stream()
                .map(BaseConverter::convertAddress)
                .collect(Collectors.toList())
        );
        if (proto.hasPrimaryPhone()) details.setPrimaryEmail(proto.getPrimaryEmail());
        details.setAlternateEmails(proto.getAlternateEmailsList());
        if (proto.hasPrimaryPhone()) details.setPrimaryPhone(convertTelephoneNumber(proto.getPrimaryPhone()));
        details.setAlternatePhone(proto
                .getAlternatePhonesList()
                .stream()
                .map(BaseConverter::convertTelephoneNumber)
                .collect(Collectors.toList())
        );

        return details;
    }

    public static Address convertAddress(AddressOuterClass.Address proto) {
        if (proto == null) return null;

        Address address = new Address();
        address.setStreetAddresses(proto.getStreetAddressesList());
        address.setCity(proto.getCity());
        address.setState(proto.getState());
        address.setZip(proto.getZip());
        address.setCountry(proto.getCountry());
        address.setCounty(proto.getCounty());
        if (StringUtils.isNotBlank(proto.getAddressType())) address.setAddressType(AddressType.valueOf(proto.getAddressType()));
        return address;
    }

    public static TelephoneNumber convertTelephoneNumber(ContactInfoOuterClass.TelephoneNumber proto) {
        if (proto == null) return null;

        TelephoneNumber number = new TelephoneNumber();
        number.setPhoneNumber(proto.getPhoneNumber());
        if (StringUtils.isNotBlank(proto.getTelephoneType())) number.setPhoneType(TelephoneType.valueOf(proto.getTelephoneType()));

        return number;
    }

    public static CareSite convertCareSite(CareSiteOuterClass.CareSite proto) {
        if (proto == null) return null;

        CareSite site = new CareSite();
        if (proto.hasAddress()) site.setAddress(convertAddress(proto.getAddress()));
        site.setCareSiteName(proto.getName());
        if (StringUtils.isNotBlank(proto.getType())) site.setCareSiteType(CareSiteType.valueOf(proto.getType()));

        site.setInternalUUID(convertUuid(proto.getInternalId()));

        return site;
    }

    public static ClinicalCode convertCode(ClinicalCodeOuterClass.ClinicalCode codeProto){
        if (codeProto == null) return null;

        ClinicalCode clinicalCode = new ClinicalCode();
        clinicalCode.setCode(codeProto.getCode());
        clinicalCode.setCodingSystemVersions(codeProto.getSystemVersion());
        clinicalCode.setDisplayName(codeProto.getDisplayName());
        clinicalCode.setCodingSystem(codeProto.getSystem());
        clinicalCode.setCodingSystemOID(codeProto.getSystemOid());
        return clinicalCode;
    }

    public static Anatomy convertAnatomy(AnatomyOuterClass.Anatomy proto){
        if (proto == null) return null;

        Anatomy anatomy = new Anatomy();
        if (proto.hasBodySite()) anatomy.setCode(convertCode(proto.getBodySite()));
        anatomy.setAnatomicalStructureName(proto.getAnatomicalStructureName());
        return anatomy;
    }

    public static EditType convertEditType(EditTypeOuterClass.EditType editTypeProto) {
        switch (editTypeProto.getNumber()) {
            case 0: return EditType.ACTIVE;
            case 1: return EditType.ARCHIVE;
            case 2: return EditType.DELETE;
        }
        return EditType.ACTIVE; //Default
    }

    public static ExternalID convertExternalId(ExternalIdOuterClass.ExternalId idProto){
        if (idProto == null) return null;

        ExternalID id = new ExternalID();
        id.setId(idProto.getId());
        id.setAssignAuthority(idProto.getAssignAuthority());
        return id;
    }

    public static Name convertName(NameOuterClass.Name proto) {
        if (proto == null) return null;

        Name name = new Name();
        name.setGivenNames(proto.getGivenNamesList());
        name.setFamilyNames(proto.getFamilyNamesList());
        name.setPrefixes(proto.getPrefixesList());
        name.setSuffixes(proto.getSuffixesList());
        if (StringUtils.isNotBlank(proto.getNameType())) name.setNameType(NameType.valueOf(proto.getNameType()));
        return name;
    }

    public static Organization convertOrganization(OrganizationOuterClass.Organization proto) {
        if (proto == null) return null;

        Organization org = new Organization();
        org.setName(proto.getName());
        if (proto.hasPrimaryId()) org.setPrimaryId(convertExternalId(proto.getPrimaryId()));
        org.setAlternateIds(
                proto
                        .getAlternateIdsList()
                        .stream().map(BaseConverter::convertExternalId)
                        .collect(Collectors.toList())
        );
        if (proto.hasContactDetails()) org.setContactDetails(convertContactDetails(proto.getContactDetails()));
        return org;
    }

    public static Coverage convertCoverage(BaseObjects.Coverage proto) {
        if (proto == null) return null;

        Coverage coverage = new Coverage();

        CoverageInfoOuterClass.CoverageInfo coverageInfo = proto.getCoverageInfo();

        coverage.setSequenceNumber(coverageInfo.getSequenceNumber());
        coverage.setHealthPlanName(coverageInfo.getHealthPlanName());
        if (!coverageInfo.getCoverageType().isEmpty())
            coverage.setType(CoverageType.valueOf(coverageInfo.getCoverageType()));
        if (coverageInfo.hasGroupNumber())
            coverage.setGroupNumber(convertExternalId(coverageInfo.getGroupNumber()));
        if (coverageInfo.hasSubscriberNumber())
            coverage.setSubscriberID(convertExternalId(coverageInfo.getSubscriberNumber()));
        if (coverageInfo.hasBeneficiaryId())
            coverage.setBeneficiaryID(convertExternalId(coverageInfo.getBeneficiaryId()));

        // Restrict dates to dci
        if (coverageInfo.hasStartDate()) {
            boolean isAfter = coverageInfo.getStartDate().getEpochMs() > coverageInfo.getDciStartDate().getEpochMs();
            YearMonthDayOuterClass.YearMonthDay start = (isAfter) ? coverageInfo.getStartDate() : coverageInfo.getDciStartDate();
            coverage.setStartDate(DataCatalogProtoUtils.toDateTime(start).toLocalDate());
        }
        else {
            coverage.setStartDate(DataCatalogProtoUtils.toDateTime(coverageInfo.getDciStartDate()).toLocalDate());
        }


        if (coverageInfo.hasEndDate()) {
            boolean hasDciEnd = coverageInfo.hasDciEndDate();
            boolean isBeforeDCI = coverageInfo.getEndDate().getEpochMs() < coverageInfo.getDciEndDate().getEpochMs();
            YearMonthDayOuterClass.YearMonthDay end = (!hasDciEnd || isBeforeDCI) ? coverageInfo.getEndDate() : coverageInfo.getDciEndDate();
            coverage.setEndDate(DataCatalogProtoUtils.toDateTime(end).toLocalDate());
        }
        else {
            coverage.setEndDate(DataCatalogProtoUtils.toDateTime(coverageInfo.getDciEndDate()).toLocalDate());
        }

        normalizeAndSetBaseData(proto.getBase(), coverage);

        return coverage;
    }

    public static Demographics convertDemographic(BaseObjects.PatientDemographics proto) {
        if (proto == null) return null;

        Demographics demographics = new Demographics();

        DemographicsInfoProto.DemographicsInfo demographicsInfo = proto.getDemographicsInfo();

        demographics.setName(convertName(demographicsInfo.getName()));
        if (demographicsInfo.hasDob())
            demographics.setDateOfBirth(DataCatalogProtoUtils.toDateTime(proto.getDemographicsInfo().getDob()));
        if (demographicsInfo.hasDod())
            demographics.setDateOfDeath(DataCatalogProtoUtils.toDateTime(proto.getDemographicsInfo().getDod()));
        demographics.setGender(DataCatalogProtoUtils.convertGender(proto.getDemographicsInfo().getGender()));
        if (demographicsInfo.getLanguagesCount() > 0)
            demographics.setLanguages(proto.getDemographicsInfo().getLanguagesList());
        if (StringUtils.isNotBlank(demographicsInfo.getReligiousAffiliation()))
            demographics.setReligiousAffiliation(demographicsInfo.getReligiousAffiliation());
        if (demographicsInfo.hasRace())
            demographics.setRace(convertCode(proto.getDemographicsInfo().getRace()));
        if (demographicsInfo.hasEthnicity())
            demographics.setEthnicity(convertCode(proto.getDemographicsInfo().getEthnicity()));
        if (demographicsInfo.getMartialStatus() != MaritalStatusOuterClass.MaritalStatus.UNKNOWN_MARITAL_STATUS)
            demographics.setMaritalStatus(DataCatalogProtoUtils.convertMaritalStatus(proto.getDemographicsInfo().getMartialStatus()));

        if (demographicsInfo.hasPrimaryCareProvider()) {
            ClinicalCodeOuterClass.ClinicalCode pcpCode = demographicsInfo.getPrimaryCareProvider();
            String pcp = pcpCode.getCode() + "^^" + pcpCode.getSystem();
            demographics.setMetaTag(DemographicsMetadataKeys.PRIMARY_CARE_PROVIDER().toString(), pcp);
        }

        normalizeAndSetBaseData(proto.getBase(), demographics);

        return demographics;
    }

    public static ContactDetails convertContactDetails(BaseObjects.ContactDetails cdProto) {
        if (cdProto == null || cdProto.getSerializedSize() == 0) return null;

        ContactDetails contactDetails = new ContactDetails();

        ContactInfoOuterClass.ContactInfo contactInfo = cdProto.getContactInfo();
        // info
        contactDetails.setPrimaryEmail(contactInfo.getPrimaryEmail());
        contactDetails.setPrimaryAddress(BaseConverter.convertAddress(contactInfo.getAddress()));
        contactDetails.setPrimaryPhone(BaseConverter.convertTelephoneNumber(contactInfo.getPrimaryPhone()));

        List<Address> addresses = contactInfo.getAlternateAddressesList()
                .stream()
                .map(BaseConverter::convertAddress)
                .collect(Collectors.toList());
        contactDetails.setAlternateAddress(addresses);

        List<TelephoneNumber> phones = contactInfo.getAlternatePhonesList()
                .stream()
                .map(BaseConverter::convertTelephoneNumber)
                .collect(Collectors.toList());
        contactDetails.setAlternatePhone(phones);

        contactDetails.setAlternateEmails(contactInfo.getAlternateEmailsList());

        normalizeAndSetBaseData(cdProto.getBase(), contactDetails);

        return contactDetails;
    }

    public static void normalizeAndSetBaseData(BaseObjects.Base base,
                                               BaseObject baseObject) {
        if (base.getParsingDetailsIdsCount() > 0)
            baseObject.setParsingDetailsId(convertUuid(base.getParsingDetailsIds(0))); // Default to the

        // Default to the head of the list
        if (base.getSourceIdsCount() > 0)
            baseObject.setSourceId(convertUuid(base.getSourceIds(0))); // Default to the head of the list

        if (base.hasDataCatalogMeta())
            applyDataCatalogMeta(baseObject, base.getDataCatalogMeta());
    }

    public static LabFlags convertLabFlag(LabResultProto.LabFlag labFlag) {
        if (labFlag == null || labFlag == LabResultProto.LabFlag.UNKNOWN_FLAG) return null;
        return LabFlags.valueOf(labFlag.name());
    }

    /**
     * Only set the value if it's not-null or non-empty
     *
     * @param metadata model.patient.Document::metadata
     * @param key      Key
     * @param value    Value
     */
    public static void wrapMetadataUpdate(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isEmpty()) {
            metadata.put(key, value);
        }
    }

    public static void cleanseMetadata(Map<String,String> metadata) {
        for (Map.Entry<String, String> entry : metadata.entrySet())
        {
            if (entry.getValue().isEmpty()) {
                metadata.remove(entry.getKey());
            }
        }
    }
}