package com.apixio.dao.patient2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apixio.dao.Constants;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.utility.DataSourceUtility;
import com.apixio.utility.HashCalculator;

class PatientSearchUtility
{
    private CqlCrud cqlCrud;

    PatientSearchUtility()
    {
    }

    public void setCqlCrud(CqlCrud cqlCrud)
    {
        this.cqlCrud = cqlCrud;
    }

    List<PatientUUIDWithTS> getPatientUUIDsByPartialPatientKey(String patientTable, String partialPatientKey, boolean hashKey)
            throws Exception
    {
        String partialPatientKeyHash;
        if (hashKey)
            partialPatientKeyHash = HashCalculator.getFileHash(PatientKeyUtility.getPartialPatientKey(partialPatientKey).getBytes("UTF-8"));
        else
            partialPatientKeyHash = PatientKeyUtility.getPartialPatientKey(partialPatientKey);

        String partialPatientRKey = PatientKeyUtility.preparePartialPatientRKey(PatientKeyUtility.getPartialPatientKey(partialPatientKeyHash));

        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, partialPatientRKey, Constants.patientUUIDPrefix, patientTable, Constants.RK_LIMIT);

        List<PatientUUIDWithTS> uuidWitTSs = new ArrayList<>();

        for (Map.Entry<String, ByteBuffer> element : results.entrySet())
        {
            PatientUUIDWithTS uuidWitTS = new PatientUUIDWithTS(UUID.fromString(PatientKeyUtility.getPatientUUIDKey(element.getKey())),
                                                                Long.valueOf(DataSourceUtility.getString(element.getValue())));
            uuidWitTSs.add(uuidWitTS);
        }

        return uuidWitTSs;
    }

    UUID getPatientUUIDByPartialPatientKey(String patientTable, String partialPatientKey, boolean hashKey)
            throws Exception
    {
        List<PatientUUIDWithTS> uuidWithTSs = getPatientUUIDsByPartialPatientKey(patientTable, partialPatientKey, hashKey);

        if (uuidWithTSs == null || uuidWithTSs.isEmpty())
            return null;

        // VK: if more than one, return the first recorded patient uuid. What a Cassandra screw up!!!

        UUID patientUUID = null;
        long smallest    = Long.MAX_VALUE;

        for (PatientUUIDWithTS uuidWithTS : uuidWithTSs)
        {
            if (uuidWithTS.TS < smallest)
            {
                patientUUID = uuidWithTS.patientUUID;
                smallest    = uuidWithTS.TS;
            }
        }

        return patientUUID;
    }

    UUID getPatientUUIDByDocumentUUID(String patientTable, String documentUUID)
            throws Exception
    {
        String docUUIDRKey = PatientKeyUtility.prepareDocumentUUIDRKey(documentUUID);

        return getPatientUUIDByRK(patientTable, docUUIDRKey);
    }

    String getPartialPatientKeyByDocumentUUID(String patientTable, String documentUUID)
            throws Exception
    {
        String docUUIDRKey = PatientKeyUtility.prepareDocumentUUIDRKey(documentUUID);
        PartialPatientInfo result = getPartialPatientInfoByRK(patientTable, docUUIDRKey);

        return  (result != null) ? result.partialPatientKeyHash : null;
    }

    List<PartialPatientInfo> getPartialPatientInfosByDocumentUUID(String patientTable, String documentUUID)
            throws Exception
    {
        String docUUIDRKey = PatientKeyUtility.prepareDocumentUUIDRKey(documentUUID);

        return getPartialPatientInfosByRK(patientTable, docUUIDRKey);
    }

    List<UUID> getDocUUIDsByPartialPatientKey(String patientTable, String partialPatientKey)
            throws Exception
    {
        String partialPatientKeyHash = HashCalculator.getFileHash(PatientKeyUtility.getPartialPatientKey(partialPatientKey).getBytes("UTF-8"));

        return getDocUUIDsByPartialPatientKeyByHash(patientTable, partialPatientKeyHash);
    }

    List<UUID> getDocUUIDsByPartialPatientKeyByHash(String patientTable, String partialPatientKeyHash)
            throws Exception
    {
        String partDocKey = PatientKeyUtility.preparePartialDocKey(PatientKeyUtility.getPartialPatientKey(partialPatientKeyHash));

        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, partDocKey, Constants.documentPrefix, patientTable);

        List<UUID> uuids = new ArrayList<>();
        if (results.isEmpty())
            return uuids;

        List<String> docColumnNames = new ArrayList<>(results.keySet());

        // Descending Order by timestamp
        Collections.sort(docColumnNames, new DocumentIdColumnComparator());

        String duplicateDocIdHash = null;

        for (String docColumnName : docColumnNames)
        {
            String docIdHash = PatientKeyUtility.getIdFromPartialDocIdColumnName(docColumnName);

            if (duplicateDocIdHash != null && duplicateDocIdHash.equalsIgnoreCase(docIdHash))
            {
                continue;
            }

            duplicateDocIdHash = docIdHash;

            uuids.add(UUID.fromString(DataSourceUtility.getString(results.get(docColumnName))));
        }

        return uuids;
    }

    private UUID getPatientUUIDByRK(String patientTable, String rKey)
            throws Exception
    {
        PartialPatientInfo result = getPartialPatientInfoByRK(patientTable, rKey);

        return  (result != null) ? getPatientUUIDByPartialPatientKey(patientTable, result.partialPatientKeyHash, false) : null;
    }

    private PartialPatientInfo getPartialPatientInfoByRK(String patientTable, String rKey)
            throws Exception
    {
        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, rKey, Constants.documentPrefix, patientTable, Constants.RK_LIMIT);

        String documentIdHash = null;
        ByteBuffer partialPatientKeyHash = null;
        long latest = 0;

        for (Map.Entry<String, ByteBuffer> element: results.entrySet())
        {
            String key  = element.getKey();
            long t = PatientKeyUtility.getTimeFromPartialColumnName(key);
            if (t > latest)
            {
                latest = t;
                documentIdHash = PatientKeyUtility.getIdFromPartialColumnName(key);
                partialPatientKeyHash = element.getValue();
            }
        }

        if (partialPatientKeyHash == null || documentIdHash == null)
            return null;

        return new PartialPatientInfo(DataSourceUtility.getString(partialPatientKeyHash), documentIdHash, latest);
    }

    private List<PartialPatientInfo> getPartialPatientInfosByRK(String patientTable, String rKey)
            throws Exception
    {
        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, rKey,  Constants.documentPrefix, patientTable, Constants.RK_LIMIT);

        List<PartialPatientInfo> all = new ArrayList<>();

        for (Map.Entry<String, ByteBuffer> element: results.entrySet())
        {
            String documentIdHash = PatientKeyUtility.getIdFromPartialColumnName(element.getKey());
            long time = PatientKeyUtility.getTimeFromPartialColumnName(element.getKey());
            String partialPatientKeyHash = DataSourceUtility.getString(element.getValue());

            all.add(new PartialPatientInfo(partialPatientKeyHash, documentIdHash, time));

        }

        return all;
    }

    private class DocumentIdColumnComparator implements Comparator<String>
    {
        @Override
        public int compare(String o1, String o2)
        {
            String docIdHash1 = PatientKeyUtility.getIdFromPartialDocIdColumnName(o1);
            String docIdHash2 = PatientKeyUtility.getIdFromPartialDocIdColumnName(o2);

            if (docIdHash1.equalsIgnoreCase(docIdHash2))
            {
                Long time1 = PatientKeyUtility.getTimeFromPartialDocIdColumnName(o1);
                Long time2 = PatientKeyUtility.getTimeFromPartialDocIdColumnName(o2);

                if (time1 < time2)
                    return 1;
                else if (time1 > time2)
                    return -1;
                else
                    return 0; // impossible case
            }
            else
            {
                return o1.compareTo(o2);
            }
        }
    }
}
