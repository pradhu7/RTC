package com.apixio.dao.utility;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.dao.Constants;
import com.apixio.utility.DataSourceUtility;

public class LinkDataUtility
{
    private static final Logger logger = LoggerFactory.getLogger(LinkDataUtility.class);

    public static void savePdsIDForPatient(CqlCache cqlCache, UUID patientUUID, String pdsID, String linkCF)
            throws Exception
    {
        String patientUUIDKey = preparePatientUUIDKey(patientUUID.toString());
        String linkColumn     = preparePdsLinkColumn();

        // now, let's put it in the link CF
        DataSourceUtility.saveRawData(cqlCache, patientUUIDKey, linkColumn, pdsID.getBytes("UTF-8"), linkCF);
    }


    public static String getPdsIDByPatientUUID(CqlCrud cqlCrud, UUID patientUUID, String linkCF)
            throws Exception
    {
        String patientUUIDKey = preparePatientUUIDKey(patientUUID.toString());
        String linkColumn = preparePdsLinkColumn();

        ByteBuffer result = DataSourceUtility.readColumnValue(cqlCrud, patientUUIDKey, linkColumn, linkCF);

        if (result == null)
            return null;

        return DataSourceUtility.getString(result);
    }

    public static PdsIDAndPatientUUID getPdsIDAndAuthPatientUUIDdByPatientUUID(CqlCrud cqlCrud, UUID patientUUID, String linkCF)
            throws Exception
    {
        String patientUUIDKey = prepareAuthoritativePatientUUIDKey(patientUUID.toString());

        return getPdsIDAndPatientUUIDByKey(cqlCrud, patientUUIDKey, linkCF);
    }

    public static void savePdsIDAndPatientUUIDForDocument(CqlCache cqlCache, UUID documentUUID, UUID patientUUID, String pdsID, String linkCF)
            throws Exception
    {
        String documentUUIDKey = prepareDocumentUUIDKey(documentUUID.toString());
        String linkColumn      = preparePdsAndPatientUUIDLinkColumn();
        String value           = combinePdsIDAndPatientUUID(pdsID, patientUUID.toString());

        // now, let's put it in the link CF
        DataSourceUtility.saveRawData(cqlCache, documentUUIDKey, linkColumn, value.getBytes("UTF-8"), linkCF);
    }

    public static String getPdsIDByDocumentUUID(CqlCrud cqlCrud, UUID documentUUID, String linkCF)
            throws Exception
    {
        PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndPatientUUIDByDocumentUUID(cqlCrud, documentUUID, linkCF);

        return (pdsIDAndPatientUUID != null ? pdsIDAndPatientUUID.pdsID : null);
    }

    // We are sure to get the authoritative patient UUID
    public static UUID getPatientUUIDByDocumentUUID(CqlCrud cqlCrud, UUID documentUUID, String linkCF)
            throws Exception
    {
        PdsIDAndPatientUUID pdsIDAndPatientUUID = getPdsIDAndPatientUUIDByDocumentUUID(cqlCrud, documentUUID, linkCF);

        return (pdsIDAndPatientUUID != null ? pdsIDAndPatientUUID.patientUUID : null);
    }

    // We are sure to get the authoritative patient UUID
    public static PdsIDAndPatientUUID getPdsIDAndPatientUUIDByDocumentUUID(CqlCrud cqlCrud, UUID documentUUID, String linkCF)
            throws Exception
    {
        String documentUUIDKey = prepareDocumentUUIDKey(documentUUID.toString());

        return getPdsIDAndPatientUUIDByKey(cqlCrud, documentUUIDKey, linkCF);
    }

    public static PdsIDAndPatientUUIDs getPdsIDAndPatientUUIDsByPatientUUID(CqlCrud cqlCrud, UUID patientUUID, String linkCF)
            throws Exception
    {
        String patientUUIDKey = prepareAuthoritativePatientUUIDKey(patientUUID.toString());
        String linkColumn     = preparePatientUUIDListColumn();

        ByteBuffer result = DataSourceUtility.readColumnValue(cqlCrud, patientUUIDKey, linkColumn, linkCF);
        if (result == null)
            return null;

        String stringResult = DataSourceUtility.getString(result);

        String[] splits = stringResult.split(Constants.doubleSeparator);

        if (splits.length < 2)
            return null;

        String      pdsID        = splits[0];
        List<UUID>  patientUUIDS = new ArrayList<>();

        for (int index = 1; index < splits.length; index++)
        {
            patientUUIDS.add(UUID.fromString(splits[index]));
        }

        return new PdsIDAndPatientUUIDs(pdsID, patientUUIDS);
    }

    private static PdsIDAndPatientUUID getPdsIDAndPatientUUIDByKey(CqlCrud cqlCrud, String key, String linkCF)
            throws Exception
    {
        String linkColumn = preparePdsAndPatientUUIDLinkColumn();

        ByteBuffer result = DataSourceUtility.readColumnValue(cqlCrud, key, linkColumn, linkCF);
        if (result == null)
            return null;

        String stringResult = DataSourceUtility.getString(result);

        return new PdsIDAndPatientUUID(getPdsID(stringResult), UUID.fromString(getPatientUUID(stringResult)));
    }

    public static void saveAuthoritativePatientUUID(CqlCache cqlCache, UUID patientUUID, UUID authPatientUUID, String pdsID, String linkCF)
            throws Exception
    {
        String patientUUIDKey = prepareAuthoritativePatientUUIDKey(patientUUID.toString());
        String linkColumn     = preparePdsAndPatientUUIDLinkColumn();
        String value          = combinePdsIDAndPatientUUID(pdsID, authPatientUUID.toString());

        // now, let's put it in the link CF
        DataSourceUtility.saveRawData(cqlCache, patientUUIDKey, linkColumn, value.getBytes("UTF-8"), linkCF);
    }

    public static void savePatientUUIDList(CqlCache cqlCache, UUID patientUUID, List<UUID> patientUUIDs, String pdsID, String linkCF)
            throws Exception
    {
        String patientUUIDKey = prepareAuthoritativePatientUUIDKey(patientUUID.toString());
        String linkColumn     = preparePatientUUIDListColumn();
        String value          = combinePdsIDAndPatientUUIDList(pdsID, patientUUIDs);

        // now, let's put it in the link CF
        DataSourceUtility.saveRawData(cqlCache, patientUUIDKey, linkColumn, value.getBytes("UTF-8"), linkCF);
    }

    private static String preparePatientUUIDKey(String key)
    {
        if (key != null && !key.startsWith(Constants.Link.patientLinkPrefix))
            return Constants.Link.patientLinkPrefix + key;

        return key;
    }

    private static String prepareDocumentUUIDKey(String key)
    {
        if (key != null && !key.startsWith(Constants.Link.documentLinkPrefix))
            return Constants.Link.documentLinkPrefix + key;

        return key;
    }

    private static String prepareAuthoritativePatientUUIDKey(String key)
    {
        if (key != null && !key.startsWith(Constants.Link.authoritativePatientLinkPrefix))
            return Constants.Link.authoritativePatientLinkPrefix + key;

        return key;
    }

    private static String preparePdsLinkColumn()
    {
        return Constants.Link.orgLink; // DON'T CHANGE IT. THINGS WILL STOP WORKING!!!
    }

    private static String preparePdsAndPatientUUIDLinkColumn()
    {
        return Constants.Link.orgAndPatientUUIDLink;  // DON'T CHANGE IT. THINGS WILL STOP WORKING!!!
    }

    private static String preparePatientUUIDListColumn()
    {
        return Constants.Link.patientUUIDList;
    }

    private static String combinePdsIDAndPatientUUID(String pdsID, String patientUUID)
    {
        return pdsID + Constants.doubleSeparator + patientUUID;
    }

    private static String combinePdsIDAndPatientUUIDList(String pdsID, List<UUID> patientUUIDs)
    {
        StringBuffer sb = new StringBuffer(pdsID);

        for (UUID patientUUID: patientUUIDs)
        {
            sb.append(Constants.doubleSeparator);
            sb.append(patientUUID);
        }

        return sb.toString();
    }

    private static String getPdsID(String value)
    {
        String[] splits = value.split(Constants.doubleSeparator);

        if (splits.length != 2)
            return null;

        return splits[0];
    }

    private static String getPatientUUID(String value)
    {
        String[] splits = value.split(Constants.doubleSeparator);

        if (splits.length != 2)
            return null;


        return splits[1];
    }

    public static class PdsIDAndPatientUUID
    {
        public PdsIDAndPatientUUID(String pdsID, UUID patientUUID)
        {
            this.pdsID       = pdsID;
            this.patientUUID = patientUUID;
        }

        public String pdsID;
        public UUID   patientUUID;

        public String getPatientUUIDAsString()
        {
            return patientUUID.toString();
        }

        @Override
        public String toString()
        {
            return ("[PdsIDAndPatientUUID: "+
                    "pdsID=" + pdsID +
                    "; patientUUID=" + patientUUID +
                    "]");
        }
    }

    public static class PdsIDAndPatientUUIDs
    {
        public PdsIDAndPatientUUIDs(String pdsID, List<UUID> patientUUIDs)
        {
            this.pdsID        = pdsID;
            this.patientUUIDs = patientUUIDs;
        }

        public String     pdsID;
        public List<UUID> patientUUIDs;

        @Override
        public String toString()
        {
            return ("[PdsIDAndPatientUUIDs: "+
                    "pdsID=" + pdsID +
                    "; patientUUIDs=" + patientUUIDs +
                    "]");
        }
    }
}
