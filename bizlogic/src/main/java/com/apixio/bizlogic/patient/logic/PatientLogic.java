package com.apixio.bizlogic.patient.logic;

import com.apixio.bizlogic.patient.assembly.PatientAssembly.PatientCategory;
import com.apixio.bizlogic.patient.assembly.merge.AllMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.dao.Constants;
import com.apixio.dao.patient2.PatientUtility;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.dao.utility.LinkDataUtility.PdsIDAndPatientUUID;
import com.apixio.dao.utility.LinkDataUtility.PdsIDAndPatientUUIDs;
import com.apixio.dao.utility.PageUtility;
import com.apixio.model.nassembly.Exchange;
import com.apixio.model.patient.Patient;
import com.apixio.nassembly.patientcategory.PatientCategoryTranslator;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.utility.HashCalculator;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PatientLogic extends PatientLogicBase
{

    public PatientLogic(final DaoServices daoServices) throws Exception
    {
        super(daoServices);
    }

    public UUID reservePatientUUID(String pdsID, String patientKey, boolean hashed) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        UUID patientUUID;

        String redisKey     = makePatientUUIDKey(pdsID, patientKey, hashed);
        String redisLockKey = "patientuuid-lock-" + pdsID + "-" + patientKey;

        String st = redisOps.get(redisKey);
        if (st != null)
        {
            patientUUID = UUID.fromString(st);
            return patientUUID;
        }

        String lock = null;
        try
        {
            lock = lockUtil.getLock(redisLockKey, true);

            // double guard
            st = redisOps.get(redisKey);
            if (st != null)
            {
                patientUUID = UUID.fromString(st);
                return patientUUID;
            }

            patientUUID = UUID.randomUUID();
            redisOps.set(redisKey, patientUUID.toString());

            return patientUUID;
        }
        finally
        {
            if (lock != null)
                lockUtil.unlock(redisLockKey, lock);
        }
    }

    public void patientUUIDReservationCompleted(String pdsID, String patientKey, boolean hashed) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // five days!!!
        redisOps.expire(makePatientUUIDKey(pdsID, patientKey, hashed), 5 * 86400);
    }

    //
    // Make sure that reservePatientUUID and patientUUIDReservationCompleted, always use the same logic to construct the key.
    //
    private String makePatientUUIDKey(String pdsID, String patientKey, boolean hashed) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        if (!hashed)
            patientKey = HashCalculator.getFileHash(patientKey.getBytes("UTF-8"));

        return "patientuuid-reserve-" + pdsID + "-" + patientKey;
    }


    public String createPartialPatientLinks(String pdsID, Patient apo) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        String patientKey = PatientUtility.getPatientKey(apo);
        if (patientKey == null)
            throw new Exception("Could not compute patient key. Cannot persist.");

        String documentKey = PatientUtility.getDocumentKey(apo);
        if (documentKey == null)
            throw new Exception("Document Key is null. Cannot persist.");

        UUID uuid = PatientUtility.getSourceFileArchiveUUID(apo);
        if (uuid == null)
            throw new Exception("Document UUID is null. Cannot persist.");

        return patientDAO2.createPartialPatientLinks(table, patientKey, documentKey, uuid);
    }

    public String createPartialPatientLinks(String pdsID, String patientKey, String documentKey, UUID uuid) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);
        String table = translator.translate(pdsID);

        return patientDAO2.createPartialPatientLinks(table, patientKey, documentKey, uuid);
    }

    public void createPatientLinks(String pdsID, UUID patientUUID, String partialPatientKeyHash) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        patientDAO2.createPatientLinks(table, patientUUID, partialPatientKeyHash);
    }

    public void addPatientToLinkTable(String pdsID, UUID patientUUID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String savedPdsID = LinkDataUtility.getPdsIDByPatientUUID(applicationCqlCrud, patientUUID, linkCF);
        if (pdsID.equals(savedPdsID)) return;

        // Let's put it in the link CF - patientUUID to pdsID
        LinkDataUtility.savePdsIDForPatient(applicationCqlCache, patientUUID, pdsID, linkCF);
    }

    public void addDocUUIDsToLinkTable(String pdsID, UUID patientUUID, List<UUID> docUUIDs) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // Let's put it in the link CF - docUUID to patientUUID and pdsID
        for (UUID docUUID : docUUIDs)
        {
            PdsIDAndPatientUUID pdsIDAndPatientUUID = LinkDataUtility.getPdsIDAndPatientUUIDByDocumentUUID(applicationCqlCrud, docUUID, linkCF);
            if (pdsIDAndPatientUUID != null &&
                    pdsID.equals(pdsIDAndPatientUUID.pdsID) && patientUUID.equals(pdsIDAndPatientUUID.patientUUID))
                continue;

            LinkDataUtility.savePdsIDAndPatientUUIDForDocument(applicationCqlCache, docUUID, patientUUID, pdsID, linkCF);
        }
    }

    public void addAuthoritativePatientUUID(String pdsID, UUID patientUUID, UUID authPatientUUID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        PdsIDAndPatientUUID pdsIDAndPatientUUID = LinkDataUtility.getPdsIDAndAuthPatientUUIDdByPatientUUID(applicationCqlCrud, patientUUID, linkCF);
        if (pdsIDAndPatientUUID != null &&
                pdsID.equals(pdsIDAndPatientUUID.pdsID) && authPatientUUID.equals(pdsIDAndPatientUUID.patientUUID))
            return;

        // Let's put it in the link CF - patientUUID to authoritative patientUUID
        LinkDataUtility.saveAuthoritativePatientUUID(applicationCqlCache, patientUUID, authPatientUUID, pdsID, linkCF);
    }

    public void addPatientUUIDList(String pdsID, UUID patientUUID, List<UUID> patientUUIDs) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        PdsIDAndPatientUUIDs pdsIDAndPatientUUIDs = LinkDataUtility.getPdsIDAndPatientUUIDsByPatientUUID(applicationCqlCrud, patientUUID, linkCF);
        if (pdsIDAndPatientUUIDs != null &&
                pdsID.equals(pdsIDAndPatientUUIDs.pdsID) && patientUUIDs.containsAll(pdsIDAndPatientUUIDs.patientUUIDs))
            return;

        // Let's put it in the link CF - patientUUID to pdsID
        LinkDataUtility.savePatientUUIDList(applicationCqlCache, patientUUID, patientUUIDs, pdsID, linkCF);
    }

////////////////////////////////////////////////////// get UUIDs

    // assumes externalID is for the primary assign authority
    public UUID getPatientUUIDFromExternalID(String pdsID, String externalID) throws Exception
    {
        String patientKey;

        // the "null" at the end is for backwards compatibility on the data.  see PatientUtility.getPatientKey() for
        // where it shows up (an "append(type)" is done where 'type' is set to null).
        pdsID      = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);
        patientKey = customerProperties.getPrimaryAssignAuthority(pdsID) + externalID + "null";

        return getPatientUUID(pdsID, patientKey, Constants.KeyType.partialPatientKey);
    }

    // It will guarantee to return authoritative patient uuid
    public UUID getPatientUUID(String pdsID, String key, Constants.KeyType keyType) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table       = translator.translate(pdsID);
        UUID   patientUUID = null;

        if (keyType == Constants.KeyType.documentUUID)
        {
            patientUUID = LinkDataUtility.getPatientUUIDByDocumentUUID(applicationCqlCrud, UUID.fromString(key), linkCF);
        }

        if (patientUUID == null)
        {
            patientUUID = patientDAO2.getPatientUUID(table, key, keyType);
        }

        return patientUUID;
    }

    public List<UUID> getDocUUIDs(String pdsID, UUID patientUUID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return getDocUUIDsGut(pdsID, pdsIDAndPatientUUID.patientUUID);
    }

    public List<UUID> getDocUUIDs(UUID patientUUID) throws Exception
    {
        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return getDocUUIDsGut(pdsIDAndPatientUUID.pdsID, pdsIDAndPatientUUID.patientUUID);
    }

    private List<UUID> getDocUUIDsGut(String pdsID, UUID patientUUID) throws Exception
    {
        String table = translator.translate(pdsID);

        return patientDAO2.getDocUUIDs(table, patientUUID);
    }

////////////////////////////////////////////////////// get APO - from patient table


    @Deprecated // By default, include doc cache elements for legacy APIs in downstream services
    public Patient getSinglePartialPatient(String pdsID, UUID patientUUID, UUID docUUID, boolean includeContent) throws Exception
    {
        return getSinglePartialPatient(pdsID, patientUUID, docUUID, includeContent, true);
    }

    public Patient getSinglePartialPatient(String pdsID, UUID patientUUID, UUID docUUID, boolean includeContent, boolean includeDocCache) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return getSinglePartialPatientGut(pdsID, pdsIDAndPatientUUID.patientUUID, docUUID, includeContent, includeDocCache);
    }

    @Deprecated // By default, include doc cache elements for legacy APIs in downstream services
    public Patient getSinglePartialPatient(UUID docUUID, boolean includeContent) throws Exception
    {
        return getSinglePartialPatient(docUUID, includeContent, true);
    }

    public Patient getSinglePartialPatient(UUID docUUID, boolean includeContent, boolean includeDocCache) throws Exception
    {
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndPatientUUIDByDocumentUUID(docUUID);

        return pdsIDAndPatientUUID != null ? getSinglePartialPatientGut(pdsIDAndPatientUUID.pdsID, pdsIDAndPatientUUID.patientUUID, docUUID, includeContent, includeDocCache) : null;
    }

    private Patient getSinglePartialPatientGut(String pdsID, UUID patientUUID, UUID docUUID, boolean includeContent, boolean includeDocCache) throws Exception
    {
        Patient patient = getAssemblyForDataType(pdsID, patientUUID, PatientCategory.ALL.getTranslator(), docUUID.toString());


        if (patient != null && includeContent)
        {
            PageUtility.constructAPO(pdsID, patient, blobDAO, includeContent, includeDocCache);
        }

        return patient;
    }

    public Patient getPatient(String pdsID, UUID patientUUID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return getPatientGut(pdsID, pdsIDAndPatientUUID.patientUUID);
    }

    public Patient getPatient(UUID patientUUID) throws Exception
    {
        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return getPatientGut(pdsIDAndPatientUUID.pdsID, pdsIDAndPatientUUID.patientUUID);
    }

    private Patient getPatientGut(String pdsID, UUID patientUUID) throws Exception
    {
        return getAssemblyForDataType(pdsID, patientUUID, PatientCategory.ALL.getTranslator(), null);
    }

////////////////////////////////////////////////////// from Assembly Table

    public Patient getMergedPatientSummaryForCategory(String pdsID, UUID patientUUID, String category, String partID) throws Exception
    {
        PatientCategory patientCategory = PatientCategory.fromCategory(category);

        return getAssemblyForDataType(pdsID, patientUUID, patientCategory.getTranslator(), partID);

    }

    private Patient getAssemblyForDataType(String pdsID, UUID patientUUID, PatientCategoryTranslator translator, String partID) throws Exception
    {
        return getAssemblyForDataTypeGut(pdsID, patientUUID, translator, partID);
    }

    public Patient getMergedPatientSummaryForCategory(String pdsID, UUID patientUUID, String category) throws Exception
    {
        PatientCategory patientCategory = PatientCategory.fromCategory(category);

        return getAssemblyForDataType(pdsID, patientUUID, patientCategory.getTranslator());
    }

    private Patient getAssemblyForDataType(String pdsID, UUID patientUUID, PatientCategoryTranslator translator) throws Exception
    {
        return getAssemblyForDataTypeGut(pdsID, patientUUID, translator, null);
    }

    private Patient getAssemblyForDataTypeGut(String pdsID, UUID patientUUID, PatientCategoryTranslator translator, String partID) throws Exception
    {

        List<com.apixio.nassembly.AssemblyLogic.AssemblyQueryResult> assemblyQueryResults = partID != null ?
                PatientLogicUtils.getAssemblyForDataType(this, pdsID, patientUUID, translator, partID) :
                PatientLogicUtils.getAssemblyForDataType(this, pdsID, patientUUID, translator);

        Exchange exchange = translator.getExchange(partID);
        exchange.fromProtoStream(assemblyQueryResults.stream().map(aqr -> aqr.getProtobuf()).collect(Collectors.toList()));

        Iterator<Patient> iterator = exchange.toApo().iterator();
        Patient merged = null;
        while (iterator.hasNext())
        {
            Patient patient = iterator.next();
            if (merged == null)
            {
                merged = patient;
            }
            else {
                PatientSet patientSet = new PatientSet(merged, patient);
                AllMerge.merge(patientSet);
                merged = patientSet.mergedPatient;
            }
        }
        return merged;
    }

////////////////////////////////////////////////////// one to many indices

    public void addPatientUUIDToIndex(String pdsID, UUID patientUUID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        patientDAO2.addPatientUUIDToIndex(table, patientUUID);
    }

    public boolean checkIfPatientUUIDIsIndexed(String pdsID, UUID patientUUID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.checkIfPatientUUIDIsIndexed(table, patientUUID);
    }

    public void addDocumentUUIDToBatchIndex(String pdsID, UUID documentUUID, String batchName) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        patientDAO2.addDocumentUUIDToBatchIndex(table, documentUUID, batchName);
    }

    // VK: We never delete the index (i.e., one to many index). Only one-to-many. That will be dangerous
    public void deleteDocumentUUIDToBatchIndex(String pdsID, UUID documentUUID, String batchName) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        patientDAO2.deleteDocumentUUIDToBatchIndex(table, documentUUID, batchName);
    }

    public boolean checkIfBatchDocumentUUIDIsIndexed(String pdsID, UUID documentUUID, String batchName) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.checkIfBatchDocumentUUIDIsIndexed(table, documentUUID, batchName);
    }
}