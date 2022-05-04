package com.apixio.bizlogic.patient.assembly.merge;

import static com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge.MERGE_DOWN_MAP;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.patient.Address;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.CodedBaseObject;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.Patient;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.helpers.LogLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dyee on 1/12/18.
 */

// TODO(twang): Recommend this class be private or package private.
// Note: All the merges should have occurred before the reconcile
public abstract class MergeBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MergeBase.class);
    private Map<String,EditType>  inactiveBaseObjectKeys = new HashMap<>();

    // if this should be proccessed it will return identity hash otherwise empty string
    // hack so that getIdentityHash is not computed twice with no reason
    protected String shouldProcess(BaseObject baseObject)
    {
        if (baseObject == null)
        {
            return "";
        }

        String key = getIdentityHash(baseObject);

        if (key.isEmpty())
        {
            return "";
        }

        EditType editType = baseObject.getEditType();
        if (editType != EditType.ACTIVE)
        {
            // if not active, we need to retain this and wipe out anything matching this key
            // but we only need to add it once (most recent wins)
            if (!inactiveBaseObjectKeys.containsKey(key))
            {
                inactiveBaseObjectKeys.put(key, editType);
            }
            return "";
        }
        else
        {
            if (inactiveBaseObjectKeys.containsKey(key))
            {
                // is it as simple as just skipping this item entirely?
                // TODO: no, I think we have to consider the source dates
                //       unless we can guarantee objects added in chronological order...
                return "";
            }
        }
        return key;
    }

    protected String getIdentityHash(BaseObject baseObject)
    {
        if (null == baseObject)
        {
            return "";
        }
        try
        {
            final String identity = getIdentity(baseObject);
            if (null == identity)
            {
                final String className = baseObject.getClass().getName();
                LOG.warn("Null identity for baseObject with UUID " + baseObject.getInternalUUID()
                    + " and className : " + className);
                return "";
            }

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(identity.getBytes(Charset.forName("UTF-8")));
            return Hex.encodeHexString(md.digest());
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("Could not obtain partId", ex);
        }
    }

    protected String getIdentity(BaseObject baseOBject)
    {
        throw new UnsupportedOperationException("Default Implementation not provided");
    }

    static protected void reconcileReferenceClinicalActorId(CodedBaseObject codedBaseObject, PatientSet patientSet)
    {
        if (codedBaseObject == null)
            return;

        Map<UUID, UUID> clinicalActorMergeDownMap = patientSet.getMergeContext(ClinicalActorMerge.MERGE_DOWN_MAP);

        //
        // Checked the merged down map, so we replace all primary clinical actor ids that were removed
        // as part of the de-dup process, with the auth one.
        //
        // Primary Clinical Actor ID is the same as internal UUID

        if (clinicalActorMergeDownMap!=null)
        {
            if (clinicalActorMergeDownMap.containsKey(codedBaseObject.getPrimaryClinicalActorId()))
            {
                UUID authClinicalActorId = clinicalActorMergeDownMap.get(codedBaseObject.getPrimaryClinicalActorId());

                if (authClinicalActorId != null)
                {
                    codedBaseObject.setPrimaryClinicalActorId(authClinicalActorId);
                } else
                {
                    LogLog.error("Error: Could not reconcileReferenceClinicalActorId with the auth uuid");
                }
            }

            Set<UUID> reconciledClinicalActors = new HashSet<>();
            for (UUID uuid : codedBaseObject.getSupplementaryClinicalActorIds())
            {
                if (clinicalActorMergeDownMap.containsKey(uuid))
                {
                    UUID authClinicalActorId = clinicalActorMergeDownMap.get(uuid);

                    if (authClinicalActorId != null)
                    {
                        reconciledClinicalActors.add(authClinicalActorId);
                    } else
                    {
                        LogLog.error("Error: Could not reconcileReferenceClinicalActorId with the auth uuid");
                    }
                } else {
                    LogLog.error("Error: Could not find supplemental clinical actor id in reconcileReferenceClinicalActorId");
                }
            }

            codedBaseObject.setSupplementaryClinicalActors(new ArrayList<UUID>(reconciledClinicalActors));
        }


    }

    static protected void reconcileReferenceEncounterId(CodedBaseObject codedBaseObject, PatientSet patientSet)
    {
        if (codedBaseObject == null)
            return;

        Map<UUID, UUID> encounterMergeDownMap = patientSet.getMergeContext(EncounterMerge.MERGE_DOWN_MAP);

        //
        // Checked the merged down map, so we replace all reference encounter ids that were removed
        // as part of the de-dup process, with the auth one.
        //
        // Source Encounter is the same as internal UUID

        if(encounterMergeDownMap != null && encounterMergeDownMap.containsKey(codedBaseObject.getSourceEncounter()))
        {
            UUID authId = encounterMergeDownMap.get(codedBaseObject.getSourceEncounter());

            if(authId != null)
            {
                codedBaseObject.setSourceEncounter(authId);
            }
            else
            {
                LogLog.error("Error: Could not reconcileReferenceEncounterId with the auth uuid");
            }
        }
    }

    static protected void reconcileReferenceSourceId(BaseObject baseObject, PatientSet patientSet)
    {

        if (baseObject == null)
            return;

        Map<UUID, UUID> sourceMergeDownMap = patientSet.getMergeContext(SourceMerge.MERGE_DOWN_MAP);

        //
        // Checked the merged down map, so we replace all source ids that were removed
        // as part of the de-dup process, with the auth one.
        //
        // Source id is the same as internal UUID

        if(sourceMergeDownMap!=null && sourceMergeDownMap.containsKey(baseObject.getSourceId()))
        {
            UUID authId = sourceMergeDownMap.get(baseObject.getSourceId());

            if(authId != null)
            {
                baseObject.setSourceId(authId);
            }
            else
            {
                LogLog.error("Error: Could not reconcileReferenceSourceId with the auth uuid");
            }
        }
    }

    static protected void reconcileContainedCareSiteReferences(Encounter encounter, PatientSet patientSet)
    {
        //
        // Encounter contains a full care site - not a reference - we need to loop through all
        // caresites, and make sure that we reconcile there sources and parsing details. Be aware,
        // that this is very brittle (the model doesn't enforce anything) - and requires the merge to
        // be done before this is called.
        //

        if (encounter != null)
        {
            CareSite careSite = encounter.getSiteOfService();
            if (careSite != null)
            {
                reconcileReferenceParsingDetailId(careSite, patientSet);
                reconcileReferenceSourceId(careSite, patientSet);
            }
        }
    }

    // NO need to reconcile. It points to meta data only
    static protected void reconcileReferenceParsingDetailId(BaseObject baseObject, PatientSet patientSet) {
    }


    /**
     * Handles the deduplication of clinicalActors, and merging of the mergeDownMap to account
     * for any uuid --> auth uuid - that will be used to reconcile..
     *
     * @param apo
     * @param patientSet
     */
    static protected void dedupClinicalActors(Patient apo, PatientSet patientSet)
    {
        AssemblerUtility.ClinicalActorMetaInfo clinicalActorMetaInfo = AssemblerUtility.dedupClinicalActors(apo, patientSet);

        //
        // The merge down might have already been set, so merge the ids... with the current result
        // overriding the current older one if there are dups..
        //
        Map<UUID, UUID> mergeDownMap = patientSet.getMergeContext(MERGE_DOWN_MAP);

        if(mergeDownMap == null)
        {
            mergeDownMap = new HashMap<>();
        }

        mergeDownMap.putAll(clinicalActorMetaInfo.mergedBaseObjectUUIDToAuthoritativeUUIDMap);

        patientSet.addMergeContext(MERGE_DOWN_MAP, mergeDownMap);
    }

    static String clinicalCodeToStringValue(ClinicalCode code)
    {
        StringBuilder codeString = new StringBuilder();
        codeString.append("code_" + code.getCode());
        codeString.append("codingSystem_" + code.getCodingSystem());
        codeString.append("codingSystemOID_" +  code.getCodingSystemOID());
        codeString.append("codingSystemVersion_" + code.getCodingSystemVersions());
        codeString.append("displayName_" +  code.getDisplayName());
        return codeString.toString();
    }

    static String getConsistentOrderedMetaData(Map<String, String> metadata)
    {
        char[] sortedChars = (metadata.toString().toCharArray());

        //No matter what order the metadata is in, even if out of order, when sorted
        //will always be the same.
        Arrays.sort(sortedChars);

        return new String(sortedChars);
    }

    // can't use getConsistentOrderedList because ClinicalCode doesn't implement comparable..
    // @TODO: fix it one day.
    static String getConsistentOrderedClinicalCodes(List<ClinicalCode> clinicalCodes)
    {
        StringBuilder stringBuffer = new StringBuilder();
        for(ClinicalCode clinicalCode : clinicalCodes)
        {
            stringBuffer.append(clinicalCodeToStringValue(clinicalCode));
        }

        char[] sortedChars = stringBuffer.toString().toCharArray();

        //No matter what order the metadata is in, even if out of order, when sorted
        //will always be the same.
        Arrays.sort(sortedChars);

        return new String(sortedChars);
    }

    static String getConsistentOrderAddress(Address address)
    {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("streetAddresses_" + address.getStreetAddresses());
        stringBuffer.append("city_" + address.getCity());
        stringBuffer.append("state+" + address.getState());
        stringBuffer.append("zip_" + address.getZip());
        stringBuffer.append("country_" + address.getCountry());
        stringBuffer.append("county_" +  address.getCounty());
        if (address.getAddressType() != null) {
            stringBuffer.append("addressType_" +  address.getAddressType().name());
        }

        char[] sortedChars = stringBuffer.toString().toCharArray();
        Arrays.sort(sortedChars);

        return new String(sortedChars);
    }


    static String getConsistentSupportingDiagnosis(List<ClinicalCode> supportingDiagnosis)
    {
        StringBuilder supportingDiagnosisStringBuffer = new StringBuilder();
        if(supportingDiagnosis!=null && !supportingDiagnosis.isEmpty())
        {
            for (ClinicalCode clinicalCode : supportingDiagnosis)
            {
                supportingDiagnosisStringBuffer.append(clinicalCodeToStringValue(clinicalCode));
            }

            char[] sortedSupportingDiag = supportingDiagnosisStringBuffer.toString().toCharArray();

            //No matter what order, any list of the same support diagnosis, even if out of order, when sorted
            //will always be the same.
            Arrays.sort(sortedSupportingDiag);
            return new String(sortedSupportingDiag);
        }
        return null;
    }

    static <T extends Comparable<? super T>> List<T> getConsistentOrderedList(List<T> list)
    {
        List<T> consistentOrderedList = new ArrayList<>(list);
        Collections.sort(consistentOrderedList);
        return consistentOrderedList;
    }
}
