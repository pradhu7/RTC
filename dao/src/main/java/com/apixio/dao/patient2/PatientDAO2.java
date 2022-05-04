package com.apixio.dao.patient2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.dao.Constants;
import com.apixio.dao.Constants.KeyType;
import com.apixio.dao.utility.OneToManyStore;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.utility.DataSourceUtility;
import com.apixio.utility.HashCalculator;

public class PatientDAO2
{
    private static final Logger logger = LoggerFactory.getLogger(PatientDAO2.class);

    private PatientSearchUtility searchUtility = new PatientSearchUtility();

    private CqlCrud        cqlCrud;
    private CqlCache       cqlCache;
    private OneToManyStore oneToManyStore;

    public void setCqlCrud(CqlCrud cqlCrud)
    {
        this.cqlCrud  = cqlCrud;
        this.cqlCache = cqlCrud.getCqlCache();
        searchUtility.setCqlCrud(cqlCrud);
    }

    public void setOneToManyStore(OneToManyStore oneToManyStore)
    {
        this.oneToManyStore = oneToManyStore;
    }

    /**
     * This method creates a number of links/connections:
     * -- from partial patient to (col = doc id, value = document uuid)
     * -- from the doc uuid to the partial patient (for now, in both the patient table and the link table)
     *
     * @param partialPatientKey
     * @param documentId
     * @param documentUUID
     * @param partialPatientKey
     * @throws Exception
     */
    public String createPartialPatientLinks(String patientTable, String partialPatientKey, String documentId, UUID documentUUID)
            throws Exception
    {
        long   currentTime           = System.currentTimeMillis();
        String partialPatientKeyHash = HashCalculator.getFileHash(PatientKeyUtility.getPartialPatientKey(partialPatientKey).getBytes("UTF-8"));
        String documentIdHash        = HashCalculator.getFileHash(documentId.getBytes("UTF-8"));

        // link from partial patient key to doc id
        String partDocKey         = PatientKeyUtility.preparePartialDocKey(partialPatientKeyHash);
        String docIdColumnName    = PatientKeyUtility.preparePartialDocIdColumnName(documentIdHash, currentTime);
        DataSourceUtility.saveRawData(cqlCache, partDocKey, docIdColumnName, documentUUID.toString().getBytes("UTF-8"), patientTable);

        // given the docUUID, allows you to get the partial patient doc id
        String docUUIDRKey = PatientKeyUtility.prepareDocumentUUIDRKey(documentUUID.toString());
        String docColumnName  = PatientKeyUtility.preparePartialColumnName(documentIdHash, currentTime);
        String partPatientKey = PatientKeyUtility.preparePartialPatientKey(partialPatientKeyHash);
        DataSourceUtility.saveRawData(cqlCache, docUUIDRKey, docColumnName, partPatientKey.getBytes("UTF-8"), patientTable);

        return partPatientKey;
    }

    /**
     * This method links partial patients together through the patient UUID.
     * - It creates a link from patient uuid to partial patient
     * - It creates a link from partial patient to patient uuid
     *
     * @param patientTable
     * @param partialPatientKeyHash
     * @throws Exception
     */
    public void createPatientLinks(String patientTable, UUID patientUUID, String partialPatientKeyHash)
            throws Exception
    {
        long   currentTime           = System.currentTimeMillis();
        String currentTimeSt         = currentTime + "";

        // clean the key before using it!!!
        partialPatientKeyHash = PatientKeyUtility.getPartialPatientKey(partialPatientKeyHash);

        // link the patientUUID to partial patient
        String patientUUIDKey    = PatientKeyUtility.preparePatientUUIDKey(patientUUID.toString());
        String patientColumnName = PatientKeyUtility.preparePartialPatientKey(partialPatientKeyHash);
        DataSourceUtility.saveRawData(cqlCache, patientUUIDKey, patientColumnName, currentTimeSt.getBytes("UTF-8"), patientTable);

        // link the partial patient to patientUUID
        String partialPatientRKey       = PatientKeyUtility.preparePartialPatientRKey(partialPatientKeyHash);
        String partialPatientColumnName = PatientKeyUtility.preparePatientUUIDKey(patientUUID.toString());
        DataSourceUtility.saveRawData(cqlCache, partialPatientRKey, partialPatientColumnName, currentTimeSt.getBytes("UTF-8"), patientTable);

        oneToManyStore.put(patientUUID.toString(), Constants.patientOneToManyIndexKeyPrefix, Constants.patientOneToManyKeyPrefix, Constants.patientNumberOfBuckets, patientTable);
    }

    public void addPatientUUIDToIndex(String patientTable, UUID patientUUID) throws Exception
    {
        oneToManyStore.put(patientUUID.toString(), Constants.patientOneToManyIndexKeyPrefix, Constants.patientOneToManyKeyPrefix, Constants.patientNumberOfBuckets, patientTable);
    }

    public boolean checkIfPatientUUIDIsIndexed(String patientTable, UUID patientUUID)
            throws Exception
    {
        return oneToManyStore.checkIfKeyExists(patientUUID.toString(), Constants.patientOneToManyIndexKeyPrefix, Constants.patientOneToManyKeyPrefix, Constants.patientNumberOfBuckets, patientTable);
    }

    /**
     * This method returns all the patient uuids together with TS associated with a partial patient key. If more than one is returned,
     * it means that there is an issue with the algorithm that generates patient uuids.
     *
     * Note: This method might be helpful for QC programs.
     *
     * @param patientTable
     * @param keyType
     * @throws Exception
     */
    // Admin
    public List<PatientUUIDWithTS> getPatientUUIDsByPartialPatientKey(String patientTable, String partialPatientKey, KeyType keyType)
            throws Exception
    {
        switch (keyType)
        {
            case partialPatientKey:
                return searchUtility.getPatientUUIDsByPartialPatientKey(patientTable, partialPatientKey, true);
            case partialPatientKeyHash:
                return searchUtility.getPatientUUIDsByPartialPatientKey(patientTable, partialPatientKey, false);

            default:
                return null;
        }
    }

    /**
     * This method returns the patient uuid given any key and its type. If there is more than one patient uuid,
     * then it returns null (which indicates an error).
     *
     * @param patientTable
     * @param key
     * @param keyType
     * @throws Exception
     */
    public UUID getPatientUUID(String patientTable, String key, KeyType keyType)
            throws Exception
    {
        switch (keyType)
        {
            case partialPatientKey:
                return searchUtility.getPatientUUIDByPartialPatientKey(patientTable, key, true);
            case partialPatientKeyHash:
                return searchUtility.getPatientUUIDByPartialPatientKey(patientTable, key, false);
            case documentUUID:
                return searchUtility.getPatientUUIDByDocumentUUID(patientTable, key);

            default:
                return null;
        }
    }

    /**
     * Given a document uuid, this method returns the partial patient key hash. If there is more than one partial patient key,
     * then it returns the latest one.
     *
     * Note: The org is fetched from the link table
     *
     * @param documentUUID
     * @throws Exception
     */
    // Admin
    public String getPartialPatientKeyByDocumentUUID(String patientTable, UUID documentUUID)
            throws Exception
    {
        return searchUtility.getPartialPatientKeyByDocumentUUID(patientTable, documentUUID.toString());
    }

    /**
     * Given a patientUUID and org, this method returns the list of partial patient key hash.
     *
     * @param patientTable
     * @param patientUUID
     * @throws Exception
     */
    // Admin
    public List<String> getPartialPatientKeysByPatientUUID(String patientTable, UUID patientUUID)
            throws Exception
    {
        String patientUUIDKey = PatientKeyUtility.preparePatientUUIDKey(patientUUID.toString());

        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, patientUUIDKey, Constants.partialPatientKeyPrefix, patientTable, Constants.RK_LIMIT);

        List<String> partPatKeyHashes = new ArrayList<>();
        if (results.isEmpty())
            return partPatKeyHashes;

        for (Map.Entry<String, ByteBuffer> entry : results.entrySet())
        {
            partPatKeyHashes.add(entry.getKey());
        }

        return partPatKeyHashes;
    }

    /**
     * Given a documentUUID key and org, this method returns a list of partial patient infos (partial patient key,
     * document id, and time). It returns duplicates also.
     *
     * Note: This method might be helpful for QC programs.
     *
     * @param patientTable
     * @param documentUUID
     * @throws Exception
     */
    // Admin
    public List<PartialPatientInfo> getPartialPatientInfosByDocumentUUID(String patientTable, UUID documentUUID)
            throws Exception
    {
        return searchUtility.getPartialPatientInfosByDocumentUUID(patientTable, documentUUID.toString());
    }

    /**
     * Given a key, its type, and org, this method returns the list of all partial patient doc uuids
     *
     * Note: duplicate docs uuids are removed.
     *
     * @param patientTable
     * @param key
     * @param keyType
     * @throws Exception
     */
    // Admin
    public List<UUID> getDocUUIDs(String patientTable, String key, KeyType keyType)
            throws Exception
    {
        switch (keyType)
        {
            case partialPatientKey:
                return searchUtility.getDocUUIDsByPartialPatientKey(patientTable, key);
            case partialPatientKeyHash:
                return searchUtility.getDocUUIDsByPartialPatientKeyByHash(patientTable, key);

            default:
                return null;
        }
    }

    /**
     * Given a patientUUID and org, this method returns the list of all patient doc uuids
     *
     * Note: duplicate docs uuids are removed.
     *
     * @param patientTable
     * @param patientUUID
     * @throws Exception
     */
    public List<UUID> getDocUUIDs(String patientTable, UUID patientUUID)
            throws Exception
    {
        String patientUUIDKey = PatientKeyUtility.preparePatientUUIDKey(patientUUID.toString());

        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, patientUUIDKey, Constants.partialPatientKeyPrefix, patientTable, Constants.RK_LIMIT);

        if (results.isEmpty())
            return null;

        List<UUID> uuids = new ArrayList<>();

        for (Map.Entry<String, ByteBuffer> entry : results.entrySet())
        {
            uuids.addAll(searchUtility.getDocUUIDsByPartialPatientKeyByHash(patientTable, PatientKeyUtility.getPartialPatientKey(entry.getKey())));
        }

        return uuids;
    }

    // Admin
    public Iterator<UUID> getPatientKeys(String patientTable) throws Exception
    {
        return new PatientKeysIterator(patientTable);
    }

    private class PatientKeysIterator implements Iterator<UUID>
    {
        Iterator<String> keys;

        /**
         * hasNext returns true if and only if there should be another column left to return.
         */
        public boolean hasNext()
        {
            return keys.hasNext();
        }

        public UUID next()
        {
            if (!hasNext())
                return null;

            try
            {
                String key = keys.next();
                return key == null ? null : UUID.fromString(key);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        private PatientKeysIterator(String patientTable) throws Exception
        {
            keys =  oneToManyStore.getKeys(Constants.patientOneToManyIndexKeyPrefix, Constants.patientOneToManyKeyPrefix, patientTable);
        }
    }

    public void addDocumentUUIDToBatchIndex(String patientTable, UUID documentUUID, String batchName)
            throws Exception
    {
        String batchOneToManyIndexKey  = PatientKeyUtility.prepareBatchDocumentOneToManyIndexKey(batchName);
        String batchOneToManyKey       = PatientKeyUtility.prepareBatchDocumentOneToManyKey(batchName);

        oneToManyStore.put(documentUUID.toString(), batchOneToManyIndexKey, batchOneToManyKey, Constants.batchDocumentNumberOfBuckets, patientTable);
    }

    // VK: We never delete the index (i.e., one to many index). Only one-to-many. That will be dangerous
    public void deleteDocumentUUIDToBatchIndex(String patientTable, UUID documentUUID, String batchName)
            throws Exception
    {
        String batchOneToManyKey = PatientKeyUtility.prepareBatchDocumentOneToManyKey(batchName);

        oneToManyStore.delete(documentUUID.toString(), batchOneToManyKey, Constants.batchDocumentNumberOfBuckets, patientTable);
    }

    public boolean checkIfBatchDocumentUUIDIsIndexed(String patientTable, UUID documentUUID, String batchName)
            throws Exception
    {
        String batchOneToManyIndexKey  = PatientKeyUtility.prepareBatchDocumentOneToManyIndexKey(batchName);
        String batchOneToManyKey       = PatientKeyUtility.prepareBatchDocumentOneToManyKey(batchName);

        return oneToManyStore.checkIfKeyExists(documentUUID.toString(), batchOneToManyIndexKey, batchOneToManyKey, Constants.batchDocumentNumberOfBuckets, patientTable);
    }

    // Admin
    public Iterator<UUID> getBatchDocumentUUIDs(String patientTable, String batchName)
            throws Exception
    {
        return new BatchDocumentIterator(patientTable, batchName);
    }

    private class BatchDocumentIterator implements Iterator<UUID>
    {
        Iterator<String> keys;

        /**
         * hasNext returns true if and only if there should be another column left to return.
         */
        public boolean hasNext()
        {
            return keys.hasNext();
        }

        public UUID next()
        {
            if (!hasNext())
                return null;

            try
            {
                String key = keys.next();
                return key == null ? null : UUID.fromString(key);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        private BatchDocumentIterator(String patientTable, String batchName) throws Exception
        {
            String batchOneToManyIndexKey  = PatientKeyUtility.prepareBatchDocumentOneToManyIndexKey(batchName);
            String batchOneToManyKey       = PatientKeyUtility.prepareBatchDocumentOneToManyKey(batchName);

            keys = oneToManyStore.getKeys(batchOneToManyIndexKey, batchOneToManyKey, patientTable);
        }
    }
}
