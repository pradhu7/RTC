package com.apixio.bizlogic.patient.assembly.utility;

import static com.apixio.bizlogic.patient.assembly.PatientAssembler.BATCHID;
import static com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge.MERGE_DOWN_OBJECT_KEY_MAP;

import com.apixio.bizlogic.patient.assembly.demographic.EmptyDemographicUtil;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.CodedBaseObject;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.helpers.LogLog;
import org.joda.time.DateTime;

public class AssemblerUtility
{
    private static AssemblerUtility instance = new AssemblerUtility();

    static public class ClinicalActorMetaInfo
    {
        public List<ClinicalActor> dedupClinicalActors;
        public Map<UUID, UUID>     mergedBaseObjectUUIDToAuthoritativeUUIDMap = new HashMap<>();

        public ClinicalActorMetaInfo(List<ClinicalActor> dedupClinicalActors, Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap)
        {
            this.dedupClinicalActors = dedupClinicalActors;
            this.mergedBaseObjectUUIDToAuthoritativeUUIDMap = mergedBaseObjectUUIDToAuthoritativeUUIDMap;
        }
    }

    public static String getPartID(ExternalID eid)
    {
        String partID = null;

        if (eid != null)
        {
            // if we have both id and aa then make a composite key
            if (StringUtils.isNotBlank(eid.getId()) && StringUtils.isNotBlank(eid.getAssignAuthority()))
            {
                partID = eid.getId() + "^" + eid.getAssignAuthority();
            }
            // else use only assign authority
            else if (StringUtils.isNotBlank(eid.getAssignAuthority()))
            {
                partID = eid.getAssignAuthority();
            }
            // TODO: should we consider the case where id is not null but assign authority is? the parser should not allow this.

            // for legacy data, we may have used source, so fall back on that.
            if (StringUtils.isBlank(partID))
            {
                partID = eid.getSource();
            }
        }

        return partID;
    }

    public static void addSourceEncounter(Patient newPatient, Patient oldPatient, UUID sourceEncounterId)
    {
        if (sourceEncounterId == null)
            return;

        Encounter sourceEncounter = oldPatient.getEncounterById(sourceEncounterId);
        if (sourceEncounter != null
                && newPatient.getEncounterById(sourceEncounterId) == null
                && sourceEncounter.getOriginalId() != null)
        {
            newPatient.addEncounter(sourceEncounter);
        }
    }

    public static Source createSourceWithLatestSourceDate(Patient APO)
    {
        Source source = new Source();
        source.setSourceSystem("Apixio Patient Object Merger");
        DateTime sourceDate = getLatestSourceDate(APO);

        if (sourceDate == null && APO.getParsingDetails() != null)
        {
            Iterator<ParsingDetail> pdit = APO.getParsingDetails().iterator();
            if (pdit.hasNext())
                sourceDate = pdit.next().getParsingDateTime();
        }

        if (sourceDate == null)
            sourceDate = new DateTime(0);

        source.setCreationDate(sourceDate);

        return source;
    }

    public static DateTime getLatestSourceDate(Patient patient)
    {
        //If a patient does not have a source, default to epoch..
        //
        //DECISION WAS MADE TO DEPRIORITIZE patients without sources..
        DateTime sourceDate = new DateTime(0);

        if (patient != null)
        {
            for (Source existingSource : patient.getSources())
            {
                DateTime existingSourceDate = existingSource.getCreationDate();
                if (existingSourceDate != null && existingSourceDate.isAfter(sourceDate))
                {
                    sourceDate = existingSourceDate;
                }
            }
        }

        return sourceDate;
    }

    public static DateTime getLatestSourceDateFromOcr(Patient patient)
    {
        DateTime sourceDate = new DateTime(0);

        if (patient != null && patient.getDocuments() != null)
        {
            for (Document document : patient.getDocuments())
            {
                if(document.getMetadata() != null)
                {
                    String ocrTs = document.getMetadata().get("ocr.ts");
                    if (StringUtils.isNotEmpty(ocrTs))
                    {
                        DateTime ocrTime = new DateTime(Long.valueOf(ocrTs));

                        if (ocrTime.isAfter(sourceDate))
                        {
                            sourceDate = ocrTime;
                        }
                    }
                }
            }
        }

        return sourceDate;
    }


    public static DateTime getLatestSourceDateFromParsingDetails(Patient patient)
    {
        DateTime sourceDate = new DateTime(0);

        if (patient != null && patient.getParsingDetails() != null)
        {
            for (ParsingDetail parsingDetail : patient.getParsingDetails())
            {
                DateTime parsingDateTime = parsingDetail.getParsingDateTime();

                if (parsingDateTime!=null && parsingDateTime.isAfter(sourceDate))
                {
                    sourceDate = parsingDateTime;
                }
            }
        }

        return sourceDate;
    }

    public static Source getLatestSourceFromTheUuids(Patient patient, List<UUID> uuids)
    {
        Source source = null;

        if (patient != null)
        {
            for (Source existingSource : patient.getSources())
            {
                UUID uuid = existingSource.getSourceId();
                if (uuids.contains(uuid))
                {
                    DateTime existingSourceDate = existingSource.getCreationDate();
                    if (source == null || existingSourceDate.isAfter(source.getCreationDate()))
                    {
                        source = existingSource;
                    }
                }
            }
        }

        return source;
    }

    public static void addContact(Patient apo, Patient separatedApo)
    {
        ContactDetails primeContacts = apo.getPrimaryContactDetails();
        List<ContactDetails> altContacts = (List<ContactDetails>) apo.getAlternateContactDetails();

        if (!EmptyDemographicUtil.isEmpty(primeContacts))
        {
            separatedApo.setPrimaryContactDetails(primeContacts);
            AssemblerUtility.addParsingDetails(primeContacts, apo, separatedApo);
        }

        if (altContacts != null)
        {
            for (ContactDetails ac : altContacts)
            {
                if (!EmptyDemographicUtil.isEmpty(ac))
                {
                    separatedApo.addAlternateContactDetails(ac);
                    AssemblerUtility.addParsingDetails(ac, apo, separatedApo);
                }
            }
        }
    }

    public static void addExternalIDs(Patient apo, Patient separatedApo)
    {
        ExternalID primeExtId = apo.getPrimaryExternalID();
        Set<ExternalID> extIDs = (Set<ExternalID>) apo.getExternalIDs();
        String primary = EmptyDemographicUtil.makeExternalId(primeExtId);

        if (!EmptyDemographicUtil.isEmpty(primary))
        {
            separatedApo.setPrimaryExternalID(primeExtId);
        }

        if (extIDs != null)
        {
            for (ExternalID externalID : extIDs)
            {
                String id = EmptyDemographicUtil.makeExternalId(externalID);
                if (id.isEmpty())
                    continue;

                if (EmptyDemographicUtil.isEmpty(primary))
                {
                    separatedApo.setPrimaryExternalID(externalID);
                    primary = id;
                }
                else
                {
                    separatedApo.addExternalId(externalID);
                }
            }
        }
    }

    public static String makeExternalId(ExternalID externalID)
    {
        if (externalID == null)
            return "";

        StringBuilder sb = new StringBuilder();

        sb.append(makeString(externalID.getAssignAuthority())).append(makeString(externalID.getId()));

        return sb.toString();
    }

    public static String makeString(String st)
    {
        return (st != null ? st.trim() : "");
    }

    public static boolean isEmpty(String s)
    {
        return (s == null || s.trim().isEmpty());
    }

    @Deprecated
    /**
     * Use org.apache.commons.codec.binary.Hex.encodeHexString
     */
    public static String byteArray2Hex(byte[] hash)
    {
        try (Formatter formatter = new Formatter())
        {
            for (byte b : hash)
            {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    public static void addBatchIdToParsingDetails(Patient batchApo, Patient patient, Iterable<? extends BaseObject> codedBaseObjects)
    {
        String batchIdFromPatient = batchApo.getMetaTag(BATCHID);

        if (StringUtils.isNotEmpty(batchIdFromPatient))
        {
            addBatchIdToParsingDetails(patient, codedBaseObjects, batchIdFromPatient);
        }
        else
        {
            LogLog.warn(BATCHID + " was not found in the patient object's metadata - check to make sure it was not removed in pipeline...");
        }
    }

    private static void addBatchIdToParsingDetails(Patient patient, Iterable<? extends BaseObject> codedBaseObjects, String batchId)
    {
        if (codedBaseObjects == null)
        {
            return;
        }

        //
        // For each CodedBaseObject do the following:
        //
        //     1) get the parsing detail Id referenced in the codeBaseObject
        //     2) Place the batchId into the parsingDetails referenced by the codedBaseObject - creating a parsingDetails
        //        container if required.
        //
        for (BaseObject baseObject: codedBaseObjects)
        {
            if (baseObject.getParsingDetailsId() == null)
            {
                continue;
            }

            ParsingDetail parsingDetail = patient.getParsingDetailById(baseObject.getParsingDetailsId());

            //
            // If there isn't a parsing details with this id, we should create one, since it will be
            // required to have the batchId..
            //
            if (parsingDetail == null)
            {
//                LogLog.warn(batchId + ": parsing details with id [" + baseObject.getParsingDetailsId() + "] could not be found, creating a holder object");

                parsingDetail = new ParsingDetail();
                parsingDetail.setParsingDetailsId(baseObject.getParsingDetailsId());
                patient.addParsingDetail(parsingDetail);
            }

            if (parsingDetail.getSourceUploadBatch() == null)
            {
                parsingDetail.setSourceUploadBatch(batchId);
            }
        }
    }

    static public void addClinicalActors(CodedBaseObject codedBaseObject, Patient apo, Patient separatedApo)
    {
        addClinicalActors(codedBaseObject, apo, separatedApo, null);
    }

    static public void addClinicalActors(CodedBaseObject codedBaseObject, Patient apo, Patient separatedApo, Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap)
    {
        if (codedBaseObject == null)
            return;

        UUID primaryClinicalActorId = codedBaseObject.getPrimaryClinicalActorId();

        if (mergedBaseObjectUUIDToAuthoritativeUUIDMap != null)
        {
            primaryClinicalActorId = mergedBaseObjectUUIDToAuthoritativeUUIDMap.get(primaryClinicalActorId);
            codedBaseObject.setPrimaryClinicalActorId(primaryClinicalActorId);
        }

        ClinicalActor clinicalActor = apo.getClinicalActorById(primaryClinicalActorId);

        if (clinicalActor != null)
        {
            if (separatedApo.getClinicalActorById(primaryClinicalActorId) == null)
            {
                separatedApo.addClinicalActor(clinicalActor);
            }

            addParsingDetails(clinicalActor, apo, separatedApo);
            addSources(clinicalActor, apo, separatedApo);
        }

        //
        // Handle supplementaryClinicalActorId
        //
        Set<UUID> authSupplementaryClinicalActorIds = new HashSet<>();

        List<UUID> supplementaryClinicalActorIds = codedBaseObject.getSupplementaryClinicalActorIds();
        if (supplementaryClinicalActorIds != null)
        {
            for (UUID sClinicalId : supplementaryClinicalActorIds)
            {
                if (mergedBaseObjectUUIDToAuthoritativeUUIDMap != null)
                {
                    sClinicalId = mergedBaseObjectUUIDToAuthoritativeUUIDMap.get(sClinicalId);
                }

                clinicalActor = apo.getClinicalActorById(sClinicalId);
                if (clinicalActor != null)
                {
                    authSupplementaryClinicalActorIds.add(sClinicalId);

                    if (separatedApo.getClinicalActorById(sClinicalId) == null)
                    {
                        separatedApo.addClinicalActor(clinicalActor);
                    }

                    addParsingDetails(clinicalActor, apo, separatedApo);
                    addSources(clinicalActor, apo, separatedApo);
                }
            }
        }

        codedBaseObject.setSupplementaryClinicalActors(new ArrayList<UUID>(authSupplementaryClinicalActorIds));
    }

    static public void addEncounters(CodedBaseObject codedBaseObject, Patient apo, Patient separatedApo)
    {
        if (codedBaseObject == null)
            return;

        UUID sourceEncounterId = codedBaseObject.getSourceEncounter();

        // Get source encounter from original apo
        Encounter encounter = sourceEncounterId != null ? apo.getEncounterById(sourceEncounterId) : null;

        if (encounter != null)
        {
            // add the encounter to the separatedApo
            if (separatedApo.getEncounterById(sourceEncounterId) == null)
            {
                separatedApo.addEncounter(encounter);
            }

            //Encounters have a careSite
            addCareSite(encounter, apo, separatedApo);

            //Encounters have a link to parsing details
            addParsingDetails(encounter, apo, separatedApo);

            //Encounters have a link to a source
            addSources(encounter, apo, separatedApo);
        }
    }

    static public void addSources(BaseObject baseObject, Patient apo, Patient separatedApo)
    {
        if (baseObject == null)
            return;

        // Get source id from base object
        UUID sourceId = baseObject.getSourceId();

        // Get source from original apo using base source id
        Source source = sourceId != null ? apo.getSourceById(sourceId) : null;

        if (source != null)
        {
            // add the source to the separatedApo
            if (separatedApo.getSourceById(sourceId) == null)
            {
                separatedApo.addSource(source);
            }
            else if (separatedApo.getSources() != null && !separatedApo.getSources().iterator().hasNext())
            {
                source = AssemblerUtility.createSourceWithLatestSourceDate(apo);
                separatedApo.addSource(source);
            }

            // Sources have a ref to parsing details
            addParsingDetails(source, apo, separatedApo);
        }
    }

    static public void addParsingDetails(BaseObject baseObject, Patient apo, Patient separatedApo)
    {
        if (baseObject == null)
            return;

        UUID parsingDetailsId = baseObject.getParsingDetailsId();

        // Get source parsing details from original apo
        ParsingDetail parsingDetail = parsingDetailsId != null ? apo.getParsingDetailById(parsingDetailsId) : null;

        // add the encounter to the separatedApo
        if (parsingDetail!=null && separatedApo.getParsingDetailById(parsingDetailsId) == null)
        {
            separatedApo.addParsingDetail(parsingDetail);
        }

        //Parsing details is a WithMeta - so no references....
    }

    static public void addCareSite(Encounter encounter, Patient apo, Patient separatedApo)
    {
        if (encounter == null)
            return;

        CareSite careSite         = encounter.getSiteOfService();
        UUID     careSiteId       = careSite != null ? careSite.getCareSiteId() : null;
        UUID     parsingDetailsId = careSite != null ? careSite.getParsingDetailsId() : null;
        UUID     sourceId         = careSite != null ? careSite.getSourceId() : null;

        // add the encounter to the separatedApo
        if (careSite != null && careSiteId != null && separatedApo.getCareSiteById(careSiteId) == null)
        {
            separatedApo.addCareSite(careSite);
        }

        ParsingDetail parsingDetail = parsingDetailsId != null ? apo.getParsingDetailById(parsingDetailsId) : null;

        // add the encounter to the separatedApo
        if (parsingDetail != null && separatedApo.getParsingDetailById(parsingDetailsId) == null)
        {
            separatedApo.addParsingDetail(parsingDetail);
        }

        // Get source encounter from original apo
        Source source = sourceId != null ? apo.getSourceById(sourceId) : null;

        // add the encounter to the separatedApo
        if (source != null && separatedApo.getSourceById(sourceId) == null)
        {
            separatedApo.addSource(source);
        }
    }

    static public ClinicalActorMetaInfo dedupClinicalActors(Patient originalApo) {
        return dedupClinicalActors(originalApo, null);
    }

    static public ClinicalActorMetaInfo dedupClinicalActors(Patient originalApo, PatientSet patientSet)
    {
        Map<String, UUID> objectKeyToAuthoritativeUUIDMap = (patientSet == null || patientSet.getMergeContext(MERGE_DOWN_OBJECT_KEY_MAP) == null) ?
                new HashMap<>() : patientSet.getMergeContext(MERGE_DOWN_OBJECT_KEY_MAP) ;
        Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap = new HashMap<>();

        List<ClinicalActor> clinicalActors = (List<ClinicalActor>) originalApo.getClinicalActors();

        // case of no clinical actor - return empty list
        if (clinicalActors == null || clinicalActors.isEmpty())
            return new ClinicalActorMetaInfo(clinicalActors, mergedBaseObjectUUIDToAuthoritativeUUIDMap);

        List<ClinicalActor> dedupClinicalActorList = new ArrayList<>();

        for (ClinicalActor ca : clinicalActors)
        {
            UUID clinicalActorId = ca.getClinicalActorId();
            // Get the hash of the Clinical Actor
            String clinicalActorHash = getHash(ca);

            //Only process if the ca is not empty (i.e. it has value), and we've not already added
            //this to the set of details already.
            if (notEmpty(ca) && !objectKeyToAuthoritativeUUIDMap.containsKey(clinicalActorHash))
            {
                dedupClinicalActorList.add(ca);

                objectKeyToAuthoritativeUUIDMap.put(clinicalActorHash, clinicalActorId);
                mergedBaseObjectUUIDToAuthoritativeUUIDMap.put(clinicalActorId, clinicalActorId);
            }
            else
            {
                UUID authUUID = objectKeyToAuthoritativeUUIDMap.get(clinicalActorHash);
                if (authUUID!=null)
                {
                    mergedBaseObjectUUIDToAuthoritativeUUIDMap.put(clinicalActorId, authUUID);
                }
            }
        }

        if (patientSet != null) patientSet.addMergeContext(MERGE_DOWN_OBJECT_KEY_MAP, objectKeyToAuthoritativeUUIDMap);

        return new ClinicalActorMetaInfo(dedupClinicalActorList, mergedBaseObjectUUIDToAuthoritativeUUIDMap);
    }

    private static String getHash(ClinicalActor clinicalActor)
    {
        MessageDigest md = null;
        try
        {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e)
        {
            return null;
        }
        md.update(getIdentity(clinicalActor).getBytes(Charset.forName("UTF-8")));

        return Hex.encodeHexString(md.digest());
    }

    public static String getIdentity(ExternalID externalID)
    {
        if (externalID == null)
            return "";

        /*
        return new ToStringBuilder(instance, com.apixio.model.Constants.TO_STRING_STYLE)
                .append(externalID.getType())
                .append(externalID.getAssignAuthority())
                .append(externalID.getId())
                .append(externalID.getSource())
                .append(externalID.getMetadata()).toString();
                */

        // This is the same way we do it in Patient Utility without the null type
        StringBuffer buff = new StringBuffer("").append(externalID.getAssignAuthority()).append(externalID.getId());

        return buff.toString();
    }

    private static String getIdentityListExternalIds(List<ExternalID> externalIDs)
    {
        if (externalIDs == null)
            return "";

        //
        // Always sort the externalID list, so the identity string will always
        // be formed the same way, regardless of order..
        //
        Collections.sort(externalIDs, new Comparator<ExternalID>() {

            @Override
            public int compare(ExternalID o1, ExternalID o2)
            {
                String obj1 = getIdentity(o1);
                String obj2 = getIdentity(o2);

                if (obj1 == null) {
                    return -1;
                }
                if (obj2 == null) {
                    return 1;
                }
                if (obj1.equals( obj2 )) {
                    return 0;
                }
                return obj1.compareTo(obj2);
            }
        });
        StringBuilder stringBuilder = new StringBuilder();

        for(ExternalID externalID : externalIDs)
        {
            stringBuilder.append(new ToStringBuilder(instance, com.apixio.model.Constants.TO_STRING_STYLE)
                    .append(externalID.getType())
                    .append(externalID.getAssignAuthority())
                    .append(externalID.getId())
                    .append(externalID.getSource())
                    .append(externalID.getMetadata()).toString());
        }

        return stringBuilder.toString();
    }

    private static String getIdentity(ClinicalActor clinicalActor)
    {
        if (clinicalActor == null)
            return "";

        return new ToStringBuilder(instance, com.apixio.model.Constants.TO_STRING_STYLE)
                .append("role", clinicalActor.getRole())
                //.append("clinicalActorId", clinicalActor.getClinicalActorId())
                .append("contactDetails", clinicalActor.getContactDetails())
                .append("actorGivenName", clinicalActor.getActorGivenName())
                //.append("actorSupplementalNames", clinicalActor.getActorSupplementalNames())
                .append("title", clinicalActor.getTitle())
                .append("primaryId", getIdentity(clinicalActor.getPrimaryId()))
                //.append("alternateIds", getIdentityListExternalIds(clinicalActor.getAlternateIds()))
                .append("associatedOrg", clinicalActor.getAssociatedOrg())
                .append("originalId", getIdentity(clinicalActor.getOriginalId()))
                //.append("OtherOriginalIds", getIdentityListExternalIds(clinicalActor.getOtherOriginalIds()))
                .append("metadata", clinicalActor.getMetadata())
                .toString();
    }

    private static boolean notEmpty(ClinicalActor clinicalActor)
    {
        if (clinicalActor == null)
            return false;

        // merge key only has primary id and role..
        // so if too much partial data without primary id is added to a patient, then weird merging may occur

        // has any external id
        if (clinicalActor.getPrimaryId() != null
                && (clinicalActor.getPrimaryId().getId() != null
                || clinicalActor.getPrimaryId().getAssignAuthority() != null
                || clinicalActor.getPrimaryId().getSource() != null))
            return true;

        // has name or contact details
        if (clinicalActor.getActorGivenName() != null || clinicalActor.getContactDetails() != null)
            return true;

        // has role
        if (clinicalActor.getRole() != null)
            return true;

        // has metadata
        if (clinicalActor.getMetadata() != null && !clinicalActor.getMetadata().isEmpty())
            return true;

        return false;
    }
}
