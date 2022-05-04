package com.apixio.bizlogic.patient.logic;

import com.apixio.datacatalog.CodedBaseObjects;
import com.apixio.model.csv.ColumnName;
import com.apixio.model.external.AxmConstants;
import com.apixio.model.patient.*;
import com.apixio.nassembly.exchangeutils.APOToProcedureProtoUtils;
import com.apixio.nassembly.exchangeutils.ApoToProtoConverter;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.UUID;

class APOToProcedureProto
{
  private String externalId = "123";
  private String assignAuthority = "member_id";
  private DateTime performedOn = DateTime.now().minusDays(5);
  private DateTime endDate = DateTime.now().minusDays(2);
  private String procedureName = UUID.randomUUID().toString();
  private String interpretation = UUID.randomUUID().toString();

   //parsing details
  private UUID sourceFileArchiveUUID = UUID.randomUUID();
  private String uploadBatchName = "1030_test_batch";
  private DateTime parsingDateTime  = DateTime.now();
  private String parserVersion = "1.0.0";
  private ParserType parserType = ParserType.AXM;
  private UUID parsingDetailsUUID  = UUID.randomUUID();  // set in createParsingDetails

  //source
  private UUID sourceUUID = UUID.randomUUID();
  private String sourceSystem = "EHR";
  private String sourceType = "ACA_CLAIM";
  private DateTime sourceCreationDate = DateTime.now();
  private LocalDate dciStartDate = LocalDate.now();
  private LocalDate dciEndDate = LocalDate.now();

  //clinical actors
  private String actorOneId = "123";
  private String actorTwoId = "234";
  private String actorAssignAuthority = "NPI";
  private ActorRole actorOneRole = ActorRole.ADMINISTERING_PHYSICIAN;
  private ActorRole actorTwoRole = ActorRole.CONSULTING_PROVIDER;
  private UUID actorOneUUID =  UUID.randomUUID();
  private UUID actorTwoUUID = UUID.randomUUID();

  //code
  private String code = UUID.randomUUID().toString();
  private String codeSystem = UUID.randomUUID().toString();
  private String displayName = UUID.randomUUID().toString();
  private String codeOid = UUID.randomUUID().toString();
  private String codeSystemVersion = UUID.randomUUID().toString();

  //supporting code
  private String sCode = UUID.randomUUID().toString();
  private String sCodeSystem = UUID.randomUUID().toString();
  private String sDisplayName = UUID.randomUUID().toString();
  private String sCodeOid = UUID.randomUUID().toString();
  private String sCodeSystemVersion = UUID.randomUUID().toString();

  // body part code
  private String bCode = UUID.randomUUID().toString();
  private String bCodeSystem = UUID.randomUUID().toString();
  private String bDisplayName = UUID.randomUUID().toString();
  private String bCodeOid = UUID.randomUUID().toString();
  private String bCodeSystemVersion = UUID.randomUUID().toString();

  //billing
  private String billType = "111^111^BILL_TYPE^2.26.724424807521776741141709009703687743312^";
  private String billingProviderId = "";
  private String billingProviderName = "";
  private String billingProviderType = "F^F^PROV_TYPE_FFS^2.26.966811684062365523470895812567751832389^";
  private String billingDate = "2020-03-01";

  private EditType editType = EditType.ACTIVE;
  private boolean deleteIndicator = false;
  private String originalId = "345";
  private String originalIdAssignAuthority = "CLAIM_ID";
  private String claimType = AxmConstants.FEE_FOR_SERVICE_CLAIM;

  private String pdsId = "444";

  private Patient patient;
  CodedBaseObjects.ProcedureCBO procedureProto;

  // create APO and create the procedure proto
  APOToProcedureProto(UUID patientId)  {
    populatePatient(patientId);
    ApoToProtoConverter converter = new ApoToProtoConverter();
    procedureProto = APOToProcedureProtoUtils.convertToProcedureProto(pdsId, patient, patient.getProcedures().iterator().next(), converter);
  }

  Patient getPatient() {
    return patient;
  }

  CodedBaseObjects.ProcedureCBO getProcedureProto() {
    return procedureProto;
  }

  private void populatePatient(UUID patientId) {
    patient = new Patient();
    patient.setPatientId(patientId);
    patient.setPrimaryExternalID(createExternalId(externalId, assignAuthority));
    patient.addParsingDetail(createParsingDetails(parsingDetailsUUID, sourceFileArchiveUUID, uploadBatchName, parsingDateTime, parserVersion, parserType));
    patient.addSource(createSource(sourceUUID, sourceSystem, sourceType, sourceCreationDate, dciStartDate, dciEndDate));
    patient.addClinicalActor(createClinicalActor(actorOneUUID, actorOneId, actorAssignAuthority, actorOneRole));
    patient.addClinicalActor(createClinicalActor(actorTwoUUID, actorTwoId, actorAssignAuthority, actorTwoRole));
    addProcedure(patient,sourceUUID, parsingDetailsUUID, actorOneUUID, actorTwoUUID);
  }

  private void addProcedure(Patient patient , UUID sId, UUID pdId, UUID actorOneId, UUID actorTwoId) {
    com.apixio.model.patient.Procedure p = new com.apixio.model.patient.Procedure();
    //populate the baseObject attributes -- this is akin to populateBaseObject method in the axmParser
    p.setSourceId(sId);
    p.setParsingDetailsId(pdId);

    p.setPrimaryClinicalActorId(actorOneId);
    p.getSupplementaryClinicalActorIds().add(actorTwoId);
    p.setOriginalId(createExternalId(originalId, originalIdAssignAuthority));
    p.setPerformedOn(performedOn);
    p.setEndDate(endDate);
    p.setProcedureName(procedureName);
    p.setCode(createCode(code, codeSystem, codeOid, codeSystemVersion, displayName));
    p.getSupportingDiagnosis().add(createCode(sCode, sCodeSystem, sCodeOid, sCodeSystemVersion, sDisplayName));
    p.setBodySite(createAnatomy(bCode, bCodeSystem, bCodeOid, bCodeSystemVersion, bDisplayName));
    p.setInterpretation(interpretation);
    //metadata props
    p.setMetaTag(AxmConstants.CLAIM_TYPE, claimType);
    p.setMetaTag("DELETE_INDICATOR", String.valueOf(deleteIndicator));
    p.setMetaTag(ColumnName.BILL_TYPE.toString(), billType);
    p.setMetaTag(ColumnName.BILLING_PROVIDER_ID.toString(), billingProviderId);
    p.setMetaTag(ColumnName.BILLING_PROVIDER_NAME.toString(), billingProviderName);
    p.setMetaTag(ColumnName.TRANSACTION_DATE.toString(), billingDate);
    p.setMetaTag("PROVIDER_TYPE", "true");
    p.setEditType(editType);

    patient.addProcedure(p);
  }

  private Source createSource(UUID sId, String system, String sType, DateTime creationDate, LocalDate dciStart, LocalDate dciEnd) {
    Source source = new Source();
    source.setSourceId(sId);
    source.setInternalUUID(sId);
    source.setSourceSystem(system);
    source.setSourceType(sType);
    source.setCreationDate(creationDate);
    source.setDciStart(dciStart);
    source.setDciEnd(dciEnd);
    source.setParsingDetailsId(parsingDetailsUUID);

    return source;
  }

  private ParsingDetail createParsingDetails(UUID pDetailId, UUID archiveId, String uploadbatch, DateTime parseDate, String version, ParserType parserType) {
    ParsingDetail parsingDetail = new ParsingDetail();
    parsingDetail.setParsingDetailsId(pDetailId);
    parsingDetail.setSourceFileArchiveUUID(archiveId);
    parsingDetail.setSourceUploadBatch(uploadbatch);
    parsingDetail.setParsingDateTime(parseDate);
    parsingDetail.setParserVersion(version);
    parsingDetail.setParser(parserType);

    return parsingDetail;
  }

  private ClinicalActor createClinicalActor(UUID aId, String providerId, String assignAuthority, ActorRole role) {
    ClinicalActor actor = new ClinicalActor();
    actor.setClinicalActorId(aId);
    ExternalID actorId  = new ExternalID();
    actorId.setId(providerId);
    actorId.setAssignAuthority(assignAuthority);
    actor.setOriginalId(actorId);
    actor.setPrimaryId(actorId);
    actor.setRole(role);
    actor.setParsingDetailsId(parsingDetailsUUID);
    actor.setSourceId(sourceUUID);

    Name name = new Name();
    name.setNameType(NameType.NICK_NAME);
    actor.setActorGivenName(name);

    actor.setContactDetails(createContactDetails());

    Organization organization = new Organization();
    organization.setContactDetails(createContactDetails());
    actor.setAssociatedOrg(organization);


    System.out.println(actor.toString());

    return actor;
  }


  private ContactDetails createContactDetails() {
    ContactDetails contactDetails = new ContactDetails();

    Address address = new Address();
    address.setAddressType(AddressType.CURRENT);
    address.setCountry("USA");
    address.setCounty("San Francisco");
    address.setCity("San Francisco");
    address.setState("CA");
    address.setZip("94404");
    address.setStreetAddresses(Collections.singletonList("1850 Gateway DR"));
    contactDetails.setPrimaryAddress(address);

    TelephoneNumber telephoneNumber = new TelephoneNumber();
    telephoneNumber.setPhoneType(TelephoneType.CELL_PHONE);
    telephoneNumber.setPhoneNumber("1-555-555-5555");
    contactDetails.setPrimaryPhone(telephoneNumber);

    return contactDetails;
  }


  private ExternalID createExternalId(String id, String assigningAuthority) {
    ExternalID externalId = new ExternalID();
    externalId.setId(id);
    externalId.setAssignAuthority(assigningAuthority);

    return externalId;
  }

  private Anatomy createAnatomy(String bCode, String bCodeSystem, String bCodeOid, String bCodeSystemVersion, String bDisplayName) {
    Anatomy anatomy = new Anatomy();
    anatomy.setCode(createCode(bCode, bCodeSystem, bCodeOid, bCodeSystemVersion, bDisplayName));
    anatomy.setAnatomicalStructureName(bDisplayName);

    return anatomy;
  }

  private ClinicalCode createCode(String code, String codingSystem, String codingSystemOid, String codingSystemVersion, String displayName) {
    ClinicalCode cc = new ClinicalCode();
    cc.setCode(code);
    cc.setCodingSystem(codingSystem);
    cc.setCodingSystemOID(codingSystemOid);
    cc.setCodingSystemVersions(codingSystemVersion);
    cc.setDisplayName(displayName);

    return cc;
  }
}