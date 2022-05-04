package com.apixio.bizlogic.patient.logic;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.dao.patient2.PatientUtility;
import com.apixio.dao.patient2.PatientWithTS;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.Constants.KeyType;
import com.apixio.dao.patient2.PartialPatientInfo;
import com.apixio.dao.patient2.PatientUUIDWithTS;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.model.nassembly.CPersistable;
import com.apixio.nassembly.AssemblyLogic;
import com.apixio.nassembly.locator.AssemblyLocator;
import com.apixio.model.nassembly.Exchange;
import com.apixio.model.patient.Patient;
import com.apixio.nassembly.patientcategory.PatientCategoryTranslator;
import com.apixio.useracct.buslog.PatientDataSetLogic;

public class PatientAdminLogic extends PatientLogicBase
{

    public PatientAdminLogic(final DaoServices daoServices)
    {
        super(daoServices);
    }

    ////////////////////////////////////////////////////// get patient table keys

    public Iterator<UUID> getPatientKeys(String pdsID) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.getPatientKeys(table);
    }

    ////////////////////////////////////////////////////// get patient uuid

    public List<PatientUUIDWithTS> getPatientUUIDsByPartialPatientKey(String pdsID, String partialPatientKey, KeyType keyType) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.getPatientUUIDsByPartialPatientKey(table, partialPatientKey, keyType);
    }

    ////////////////////////////////////////////////////// get partial patient key

    public String getPartialPatientKeyByDocumentUUID(String pdsID, UUID documentUUID) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.getPartialPatientKeyByDocumentUUID(table, documentUUID);
    }

    public List<String> getPartialPatientKeysByPatientUUID(String pdsID, UUID patientUUID) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return patientDAO2.getPartialPatientKeysByPatientUUID(table, pdsIDAndPatientUUID.patientUUID);
    }

    public List<String> getPartialPatientKeysByPatientUUID(UUID patientUUID)
            throws Exception
    {
        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        return getPartialPatientKeysByPatientUUID(pdsIDAndPatientUUID.pdsID, pdsIDAndPatientUUID.patientUUID);
    }

    public List<PartialPatientInfo> getPartialPatientInfosByDocumentUUID(String pdsID, UUID documentUUID) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.getPartialPatientInfosByDocumentUUID(table, documentUUID);
    }

    ////////////////////////////////////////////////////// get doc uuids

    public List<UUID> getDocUUIDs(String pdsID, String key, KeyType keyType) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.getDocUUIDs(table, key, keyType);
    }

    public Iterator<UUID> getBatchDocumentUUIDs(String pdsID, String batchName) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        String table = translator.translate(pdsID);

        return patientDAO2.getBatchDocumentUUIDs(table, batchName);
    }

    ////////////////////////////////////////////////////// Get all single partial patients

    @Deprecated
    public List<Patient> getSinglePartialPatients(String pdsID, UUID patientUUID, boolean duplicates) throws Exception
    {
        pdsID =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        List<PatientWithTS> singlePartialPatientsWithTS = getSinglePartialPatientsWithTimeStamp(pdsID, patientUUID, duplicates);

        List<Patient> apos = new ArrayList<>();

        for (PatientWithTS partWithTS: singlePartialPatientsWithTS)
        {
            apos.add(partWithTS.patient);
        }

        return apos;
    }

    @Deprecated
    public List<PatientWithTS> getSinglePartialPatientsWithTimeStamp(String pdsID, UUID patientUUID, boolean duplicates) throws Exception
    {
        List<PatientWithTS> apos = getAllUnmergedPatientsForCategoryWithTS(pdsID, patientUUID, PatientAssembly.PatientCategory.ALL.getCategory());

        if (duplicates)
            return apos;

        //
        // Sort in desc order, so it's in time order. - we do this to keep the last one, and not have it deduped out...
        //
        //  300,200,150,100,50 <=== latest is the first element
        //
        Collections.sort(apos, new Comparator<PatientWithTS>()
        {
            @Override
            public int compare(PatientWithTS o1, PatientWithTS o2)
            {
                if(o1.TS == o2.TS)
                    return 0;
                else if(o1.TS < o2.TS)
                    return 1;
                else
                    return -1;
            }
        });

        Map<UUID, PatientWithTS> dedupApoMap = new HashMap<>();

        for (Iterator<PatientWithTS> iterator = apos.iterator(); iterator.hasNext();)
        {
            PatientWithTS partWithTS = iterator.next();
            Patient apo = partWithTS.patient;
            UUID docUUID = PatientUtility.getSourceFileArchiveUUID(apo);

            if(!dedupApoMap.containsKey(docUUID))
            {
                dedupApoMap.put(docUUID, partWithTS);
            }
        }

        List<PatientWithTS> orderedDedupPartWithTs = new ArrayList<>(dedupApoMap.values());

        // Sort in ascending order - clients expect this in desc order.

        // Sort in desc order, so it's in time order.
        //
        //  50,100,150,200,300 <=== earliest is the first element
        //
        // the earlier contract states this, so we need to comply.
        Collections.sort(orderedDedupPartWithTs, new Comparator<PatientWithTS>()
        {
            @Override
            public int compare(PatientWithTS o1, PatientWithTS o2)
            {
                if(o1.TS == o2.TS)
                    return 0;
                else if(o1.TS > o2.TS)
                    return 1;
                else
                    return -1;
            }
        });

        return orderedDedupPartWithTs;
    }
    ////////////////////////////////////////////////////// Get assemblies

    @Deprecated
    public List<Patient> getAllUnmergedPatientsForCategory(String pdsID, UUID patientUUID, String category, String partID) throws Exception
    {
        return getAllUnmergedPatientsForCategoryGuts(pdsID, patientUUID, category, partID);
    }

    @Deprecated
    public List<Patient> getAllUnmergedPatientsForCategory(String pdsID, UUID patientUUID, String category) throws Exception
    {
        return getAllUnmergedPatientsForCategoryGuts(pdsID, patientUUID, category, null);
    }

    private List<Patient> getAllUnmergedPatientsForCategoryGuts(String pdsID, UUID patientUUID, String category, String partID) throws Exception
    {
        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        PatientAssembly.PatientCategory patientCategory = PatientAssembly.PatientCategory.fromCategory(category);

        return getAssemblyForDataType(pdsID, patientUUID, patientCategory.getTranslator(), partID);
    }

    private List<Patient> getAssemblyForDataType(String pdsID, UUID patientUUID, PatientCategoryTranslator translator, String partID) throws Exception
    {
        List<com.apixio.nassembly.AssemblyLogic.AssemblyQueryResult> assemblyQueryResults = partID != null ?
                PatientLogicUtils.getAssemblyForDataType(this, pdsID, patientUUID, translator, partID) :
                PatientLogicUtils.getAssemblyForDataType(this, pdsID, patientUUID, translator);


        Exchange exchange = translator.getExchange(partID);
        // We can make 1 Patient because this is backwards compatible THROWAWAY code and it's far more performant
        exchange.fromProtoStream(assemblyQueryResults.stream().map(AssemblyLogic.AssemblyQueryResult::getProtobuf).collect(Collectors.toList()));
        return StreamSupport.stream(exchange.toApo().spliterator(), false).collect(Collectors.toList());
    }

    @Deprecated
    public List<PatientWithTS> getAllUnmergedPatientsForCategoryWithTS(String pdsID, UUID patientUUID, String category) throws Exception
    {
        return getAllUnmergedPatientsForCategoryWithTSGuts(pdsID, patientUUID, category, null);
    }

    @Deprecated
    public List<PatientWithTS> getAllUnmergedPatientsForCategoryWithTS(String pdsID, UUID patientUUID, String category, String partID) throws Exception
    {
        return getAllUnmergedPatientsForCategoryWithTSGuts(pdsID, patientUUID, category, partID);
    }

    private List<PatientWithTS> getAllUnmergedPatientsForCategoryWithTSGuts(String pdsID, UUID patientUUID, String category, String partID) throws Exception {
        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;


        PatientAssembly.PatientCategory patientCategory = PatientAssembly.PatientCategory.fromCategory(category);

        return getAssemblyForDataTypeWithTS(pdsID, patientUUID, patientCategory.getTranslator(), partID);

    }

    private List<PatientWithTS> getAssemblyForDataTypeWithTS(String pdsID, UUID patientUUID, PatientCategoryTranslator translator, String partID) throws Exception
    {
        List<com.apixio.nassembly.AssemblyLogic.AssemblyQueryResult> assemblyQueryResults = partID != null ?
                PatientLogicUtils.getAssemblyForDataType(this, pdsID, patientUUID, translator, partID) :
                PatientLogicUtils.getAssemblyForDataType(this, pdsID, patientUUID, translator);


        // Hack because it's guaranteed that table name and exchange is same for all translator datatype names [THIS IS BACKWARDS COMPATIBLE THROWAWAY CODE]
        Exchange exchange = translator.getExchange(partID);
        CPersistable cPersistable = AssemblyLocator.getPersistable(exchange.getDataTypeName());
        return assemblyQueryResults.stream().map(aqr ->
        {
            exchange.fromProtoStream(Arrays.asList(aqr.getProtobuf()));
            return new PatientWithTS(exchange.toApo().iterator().next(), aqr.getTimeInMicroseconds(), cPersistable.getSchema().getTableName(), aqr.getRowKeyValue(), aqr.getColFieldNamesToValues().get(partID).toString());
        }).collect(Collectors.toList());
    }
}
