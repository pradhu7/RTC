package com.apixio.dao.trace1;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apixio.dao.Constants;
import com.apixio.dao.Constants.Trace;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.dao.utility.OneToManyStore;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.model.trace.DocumentEntry;
import com.apixio.model.trace.DocumentSummaryTrace;
import com.apixio.model.trace.DocumentTrace;
import com.apixio.model.trace.TraceJSONParser;
import com.apixio.utility.DataSourceUtility;

/**
 * @author nkrishna
 */
public class DocumentTraceDAO1
{
    private CqlCrud  cqlCrud;
    private CqlCache cqlCache;
    private Long ttlFromNowInSec;
    private String   linkCF;
    private CustomerProperties customerProperties;
    private OneToManyStore oneToManyStore;

    private static TraceJSONParser parser = new TraceJSONParser();

    public void setCqlCrud(CqlCrud cqlCrud)
    {
    	this.cqlCrud  = cqlCrud;
        this.cqlCache = cqlCrud.getCqlCache();
    }

    public void setTtlFromNowInSec(long ttlFromNowInSec)
    {
        this.ttlFromNowInSec = ttlFromNowInSec;
    }

    public void setLinkCF(String linkCF)
    {
        this.linkCF = linkCF;
    }

    public void setCustomerProperties(CustomerProperties customerProperties)
    {
    	this.customerProperties = customerProperties;
    }

    public void setOneToManyStore(OneToManyStore oneToManyStore)
    {
        this.oneToManyStore = oneToManyStore;
    }

    /**
     * This method is called to make an entry about the document, when it is received by Apixio successfully. Document
     * is received successfully, only after it is uploaded to file system of Apixio successfully.
     * To first entry, we need details of the document: documentHash, document uuId and source system from where it came.
     *
     * Important: Documents that don't have docId will not have a reverse key from docId to docUUID
     *
     * @param docEntry
     * @param orgId
     * @throws Exception
     */
    public void createDocumentEntry(DocumentEntry docEntry, String orgId)
            throws Exception
    {
        if (docEntry == null || docEntry.getDocumentUUID() == null)
        {
            throw new Exception("DocumentTraceDAO: docEntry, Document UUID cannot be null.");
        }

        String columnFamily    = customerProperties.getTraceColumnFamily2(orgId);
        String documentUUIDKey = prepareDocumentUUIDKey(docEntry.getDocumentUUID().toString());
        String docEntryColumn  = prepareDocEntryColumn(docEntry.getTimeInMillis());

        DataSourceUtility.saveRawData(cqlCache, documentUUIDKey, docEntryColumn, parser.entryParser.toBytes(docEntry), columnFamily);
    }

    /**
     * Given a documentUUID, it returns a document entry. If there are more than one document entry that
     * matches the query, it returns the latest document entry
     *
     * Note:
     * -- The reason that we might get more than one doc entry is that the doc receiver might by mistake create multiple
     * doc entries for the same document
     * -- There is an entry in the link table from docUUID to org that can be used to fetch from the appropriate trace table
     *
     * @param documentUUID
     * @return
     *
     * @throws Exception
     */
    public DocumentEntry getDocumentEntryByUUID(UUID documentUUID)
            throws Exception
    {
        String orgId = LinkDataUtility.getPdsIDByDocumentUUID(cqlCrud, documentUUID, linkCF);
        if (orgId == null)
            return null;

        return getDocumentEntryByUUID(documentUUID, orgId);
    }

    /**
     * Given a documentUUID and org, it returns a document entry. If there are more than one document entry that
     * matches the query, it returns the latest document entry
     *
     * Note: The reason that we might get more than one doc entry is that the doc receiver might by mistake create multiple
     * doc entries for the same document
     *
     * @param documentUUID
     * @param orgId
     * @return
     *
     * @throws Exception
     */
    public DocumentEntry getDocumentEntryByUUID(UUID documentUUID, String orgId)
            throws Exception
    {
        String columnFamily    = customerProperties.getTraceColumnFamily2(orgId);
        String documentUUIDKey = prepareDocumentUUIDKey(documentUUID.toString());

        Map<String, ByteBuffer> fromDocEntryColumnToDocEntry = DataSourceUtility.readColumns(cqlCrud, documentUUIDKey, Trace.docEntryPrefix, columnFamily, Constants.RK_LIMIT);

        ByteBuffer jsonBytes = null;
        long latest = 0;

        for (Map.Entry<String, ByteBuffer> element: fromDocEntryColumnToDocEntry.entrySet())
        {
            String key  = element.getKey();
            long t = getTimeFromDocEntryColumn(key);
            if (t > latest)
            {
                latest = t;
                jsonBytes = element.getValue();
            }
        }


        return (jsonBytes != null) ? parser.entryParser.parse(DataSourceUtility.getString(jsonBytes)) : null;
    }

    /**
     * This method is called to trace a received document. The document is in Apixio's file system.
     * Now it will go through various work flows within Apixio. These steps are captured through this method.
     *
     * @param docTrace
     * @param orgId
     * @throws Exception
     */
    public void createDocumentTrace(DocumentTrace docTrace, String orgId)
            throws Exception
    {
        if (docTrace == null || docTrace.getDocumentUUID() == null)
            throw new Exception("Document UUID cannot be null.");

        String columnFamily    = customerProperties.getTraceColumnFamily2(orgId);
        String documentUUIDKey = prepareDocumentUUIDKey(docTrace.getDocumentUUID().toString());
        String docTraceColumn  = prepareProcessNameColumn(docTrace.getProcessName(), docTrace.getTimeInMillis());

        if (ttlFromNowInSec != null)
            DataSourceUtility.saveRawData(cqlCache, documentUUIDKey, docTraceColumn, parser.traceParser.toBytes(docTrace), columnFamily, ttlFromNowInSec);
        else
            DataSourceUtility.saveRawData(cqlCache, documentUUIDKey, docTraceColumn, parser.traceParser.toBytes(docTrace), columnFamily);
    }

    /**
     * Given a documentUUID, it returns all document traces for that documentUUID.
     *
     * Note: There is an entry in the link table from docUUID to org that can be used to fetch from the appropriate trace table
     *
     * @param documentUUID
     * @return
     *
     * @throws Exception
     */
    public List<DocumentTrace> getDocumentTraceByUUID(UUID documentUUID)
            throws Exception
    {
        String orgId = LinkDataUtility.getPdsIDByDocumentUUID(cqlCrud, documentUUID, linkCF);
        if (orgId == null)
            return null;

        return getDocumentTraceByUUID(documentUUID, orgId);
    }

    /**
     * Given a documentUUID and org, it returns all document traces for that documentUUID
     *
     * @param documentUUID
     * @param orgId
     * @return
     *
     * @throws Exception
     */
    public List<DocumentTrace> getDocumentTraceByUUID(UUID documentUUID, String orgId) throws Exception
    {
        return getDocumentTraceByUUID(documentUUID, Trace.processNamePrefix, orgId);
    }

    /**
     * Given a documentUUID and org, it returns all document traces for that documentUUID
     * in json form.
     *
     * @param documentUUID
     * @param orgId
     * @return
     *
     * @throws Exception
     */
    public List<String> getDocumentTraceJsonByUUID(UUID documentUUID, String orgId) throws Exception
    {
        return getDocumentTraceJsonByUUID(documentUUID, Trace.processNamePrefix, orgId);
    }

    /**
     * Given a documentUUID and processName, it returns all document traces for that documentUUID that are of that
     * processName type
     *
     * Note: There is an entry in the link table from docUUID to org that can be used to fetch from the appropriate trace table
     *
     * @param documentUUID
     * @return
     *
     * @throws Exception
     */
    public List<DocumentTrace> getDocumentTraceByUUIDForProcess(UUID documentUUID, String processName)
            throws Exception
    {
        String orgId = LinkDataUtility.getPdsIDByDocumentUUID(cqlCrud, documentUUID, linkCF);
        if (orgId == null)
            return null;

        return getDocumentTraceByUUID(documentUUID, processName, orgId);
    }

    /**
     * Given a documentUUID, processName, and org, it returns all document traces for that documentUUID that are of that
     * processName type
     *
     * @param documentUUID
     * @param processName
     * @param orgId
     * @return
     *
     * @throws Exception
     */
    public List<DocumentTrace> getDocumentTraceByUUID(UUID documentUUID, String processName, String orgId) throws Exception
    {
        Map<String, ByteBuffer> results = readDocumentTraceByUUID(documentUUID, processName, orgId);

        List<DocumentTrace> traces = new ArrayList<DocumentTrace>();

        for (Map.Entry<String, ByteBuffer> entry : results.entrySet())
        {
            traces.add(parser.traceParser.parse(DataSourceUtility.getString(entry.getValue())));
        }

        return traces;
    }

    /**
     * Given a documentUUID, processName, and org, it returns all document traces for that documentUUID that are of that
     * processName type in json.
     *
     * @param documentUUID
     * @param processName
     * @param orgId
     * @return
     *
     * @throws Exception
     */
    public List<String> getDocumentTraceJsonByUUID(UUID documentUUID, String processName, String orgId) throws Exception
    {
        Map<String, ByteBuffer> results = readDocumentTraceByUUID(documentUUID, processName, orgId);

        List<String> traces = new ArrayList<String>();

        for (Map.Entry<String, ByteBuffer> entry : results.entrySet())
        {
            traces.add(new String(DataSourceUtility.getBytes(entry.getValue())));
        }

        return traces;
    }

    private Map<String, ByteBuffer> readDocumentTraceByUUID(UUID documentUUID, String processName, String orgId) throws Exception
    {
        if (processName == null)
            throw new Exception("Process name not submitted.");

        String columnFamily    = customerProperties.getTraceColumnFamily2(orgId);
        String documentUUIDKey = prepareDocumentUUIDKey(documentUUID.toString());
        String docTraceColumn  = prepareProcessNameColumn(processName, 0);

        Map<String, ByteBuffer> results = DataSourceUtility.readColumns(cqlCrud, documentUUIDKey, docTraceColumn, columnFamily);

        return results;
    }

    public void createDocumentSummaryTrace(DocumentSummaryTrace summaryTrace, String orgId)
            throws Exception
    {
        String columnFamily       = customerProperties.getTraceColumnFamily2(orgId);
        String oneToManyIndexKey  = prepareDocumentSummaryTraceOneToManyIndexKey(summaryTrace.getBatchId());
        String oneToManyKey       = prepareDocumentSummaryTraceOneToManyKey(summaryTrace.getBatchId());

        oneToManyStore.put(summaryTrace.getDocumentUUID().toString(), parser.summaryParser.toBytes(summaryTrace),
                           oneToManyIndexKey, oneToManyKey, Trace.traceDocumentSummaryNumberOfBuckets, columnFamily);
    }

    public Iterator<UUID> getDocumentSummaryTraceUUIDs(String batchName, String orgId)
            throws Exception
    {
        return new DocumentSummaryTraceUUIDIterator(batchName, orgId);
    }

    private class DocumentSummaryTraceUUIDIterator implements Iterator<UUID>
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

        private DocumentSummaryTraceUUIDIterator(String batchName, String orgId) throws Exception
        {
            String columnFamily       = customerProperties.getTraceColumnFamily2(orgId);
            String oneToManyIndexKey  = prepareDocumentSummaryTraceOneToManyIndexKey(batchName);
            String oneToManyKey       = prepareDocumentSummaryTraceOneToManyKey(batchName);

            keys = oneToManyStore.getKeys(oneToManyIndexKey, oneToManyKey, columnFamily);
        }
    }

    public Iterator<DocumentSummaryTrace> getDocumentSummaryTraces(String batchName, String orgId)
            throws Exception
    {
        return new DocumentSummaryTraceIterator(batchName, orgId);
    }

    private class DocumentSummaryTraceIterator implements Iterator<DocumentSummaryTrace>
    {
        Iterator<OneToManyStore.KeyAndValue> keyAndValues;

        /**
         * hasNext returns true if and only if there should be another column left to return.
         */
        public boolean hasNext()
        {
            return keyAndValues.hasNext();
        }

        public DocumentSummaryTrace next()
        {
            if (!hasNext())
                return null;

            try
            {
                OneToManyStore.KeyAndValue keyAndValue = keyAndValues.next();
                return keyAndValue == null ? null : parser.summaryParser.parse(DataSourceUtility.getString(keyAndValue.value));
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

        private DocumentSummaryTraceIterator(String batchName, String orgId) throws Exception
        {
            String columnFamily       = customerProperties.getTraceColumnFamily2(orgId);
            String oneToManyIndexKey  = prepareDocumentSummaryTraceOneToManyIndexKey(batchName);
            String oneToManyKey       = prepareDocumentSummaryTraceOneToManyKey(batchName);

            keyAndValues = oneToManyStore.getKeysAndValues(oneToManyIndexKey, oneToManyKey, columnFamily);
        }
    }

    // This is the key of the entry/trace row for a docUUID - This key allows us to see the entire
    // entry/trace history of a document
    private String prepareDocumentUUIDKey(String key)
    {
        if (key != null && !key.startsWith(Trace.documentUUIDPrefix))
            return Trace.documentUUIDPrefix + key;

        return key;
    }

    // This is the column name for doc entry
    private String prepareDocEntryColumn(long timeInMillis)
    {
        return Trace.docEntryPrefix + timeInMillis;
    }

    // Get the time when the doc entry was entered
    private long getTimeFromDocEntryColumn(String docEntryColumn)
    {
        if (docEntryColumn != null && docEntryColumn.startsWith(Trace.docEntryPrefix))
            return Long.valueOf(docEntryColumn.substring(Trace.docEntryPrefix.length()));

        return 0;
    }

    // This is the column name for doc trace
    private String prepareProcessNameColumn(String processName, long timeInMillis)
    {
        String column = processName.startsWith(Trace.processNamePrefix) ? processName : Trace.processNamePrefix + processName;

        if (timeInMillis != 0)
            return column + Constants.singleSeparator + timeInMillis;
        else
            return column;
    }

    // Add a separator between the prefix and batchname since prefix doesn't end with separator
    // Don't add a separator at the end since there is nothing else added to the key
    static String prepareDocumentSummaryTraceOneToManyIndexKey(String batchName)
    {
        if (batchName != null && !batchName.startsWith(Trace.traceDocumentSummaryOneToManyIndexKeyPrefix))
            return Trace.traceDocumentSummaryOneToManyIndexKeyPrefix + Constants.singleSeparator + batchName;

        return batchName;
    }

    // Don't add a separator between the prefix and batchname since prefix ends with separator
    // Add a separator at the end since the key is augmented
    static String prepareDocumentSummaryTraceOneToManyKey(String batchName)
    {
        if (batchName != null && !batchName.startsWith(Trace.traceDocumentSummaryOneToManyKeyPrefix))
            return Trace.traceDocumentSummaryOneToManyKeyPrefix + batchName + Constants.singleSeparator;

        return batchName;
    }
}
