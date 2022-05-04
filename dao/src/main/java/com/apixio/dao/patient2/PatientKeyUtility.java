package com.apixio.dao.patient2;

import com.apixio.dao.Constants;

public class PatientKeyUtility
{
    public static String preparePartialPatientKey(String key)
    {
        if (key != null && !key.startsWith(Constants.partialPatientKeyPrefix))
            return Constants.partialPatientKeyPrefix + key;

        return key;
    }

    public static String getPartialPatientKey(String key)
    {
        if (key != null && key.startsWith(Constants.partialPatientKeyPrefix))
            return key.substring(Constants.partialPatientKeyPrefix.length());

        return key;
    }

    public static String prepareDocumentUUID(String key)
    {
        if (key != null && !key.startsWith(Constants.documentUUIDPrefix))
            return Constants.documentUUIDPrefix + key;

        return key;
    }

    public static String getDocumentUUID(String key)
    {
        if (key != null && key.startsWith(Constants.documentUUIDPrefix))
            return key.substring(Constants.documentUUIDPrefix.length());
        if (key != null && key.startsWith(Constants.documentUUIDRKeyPrefix))
            return key.substring(Constants.documentUUIDRKeyPrefix.length());
        return key;
    }

    public static String preparePartialDocKey(String key)
    {
        if (key != null && !key.startsWith(Constants.partialDocKeyPrefix))
            return Constants.partialDocKeyPrefix + key;

        return key;
    }

    public static String getPartialDocKey(String key)
    {
        if (key != null && key.startsWith(Constants.partialDocKeyPrefix))
            return key.substring(Constants.partialDocKeyPrefix.length());

        return key;
    }

    public static String prepareDocumentHashRKey(String key)
    {
        if (key != null && !key.startsWith(Constants.documentHashRKeyPrefix))
            return Constants.documentHashRKeyPrefix + key;

        return key;
    }

    public static String getDocumentHashRKey(String key)
    {
        if (key != null && key.startsWith(Constants.documentHashRKeyPrefix))
            return key.substring(Constants.documentHashRKeyPrefix.length());

        return key;
    }

    public static String prepareDocumentIdRKey(String key)
    {
        if (key != null && !key.startsWith(Constants.documentIdRKeyPrefix))
            return Constants.documentIdRKeyPrefix + key;

        return key;
    }

    public static String getDocumentIdRKey(String key)
    {
        if (key != null && key.startsWith(Constants.documentIdRKeyPrefix))
            return key.substring(Constants.documentIdRKeyPrefix.length());

        return key;
    }

    public static String prepareDocumentUUIDRKey(String key)
    {
        if (key != null && !key.startsWith(Constants.documentUUIDRKeyPrefix))
            return Constants.documentUUIDRKeyPrefix + key;

        return key;
    }

    public static String getDocumentUUIDRKey(String key)
    {
        if (key != null && key.startsWith(Constants.documentUUIDRKeyPrefix))
            return key.substring(Constants.documentUUIDRKeyPrefix.length());

        return key;
    }

    public static String preparePatientUUIDKey(String key)
    {
        if (key != null && !key.startsWith(Constants.patientUUIDPrefix))
            return Constants.patientUUIDPrefix + key;

        return key;
    }

    public static String getPatientUUIDKey(String key)
    {
        if (key != null && key.startsWith(Constants.patientUUIDPrefix))
            return key.substring(Constants.patientUUIDPrefix.length());

        return key;
    }

    public static String preparePartialPatientRKey(String key)
    {
        if (key != null && !key.startsWith(Constants.partialPatientRKeyPrefix))
            return Constants.partialPatientRKeyPrefix + key;
        return key;
    }

    public static String getPartialPatientRKey(String key)
    {
        if (key != null && key.startsWith(Constants.partialPatientRKeyPrefix))
            return key.substring(Constants.partialPatientRKeyPrefix.length());

        return key;
    }

    public static String preparePartialColumnName(String id, long TimeInMillis)
    {
        String column = id.startsWith(Constants.documentPrefix) ? id : Constants.documentPrefix + id;

        if (TimeInMillis != 0)
            return column + Constants.singleSeparator + TimeInMillis;
        else
            return column;
    }

    public static String getIdFromPartialColumnName(String partialPatientColumn)
    {
        String column;

        if (partialPatientColumn != null && partialPatientColumn.startsWith(Constants.documentPrefix))
            column = partialPatientColumn.substring(Constants.documentPrefix.length());
        else
            column = partialPatientColumn;

        int index = column.lastIndexOf(Constants.singleSeparator);
        if (index == -1)
            return null;

        return column.substring(0, index);
    }

    public static long getTimeFromPartialColumnName(String partialPatientColumn)
    {
        String column;

        if (partialPatientColumn != null && partialPatientColumn.startsWith(Constants.documentPrefix))
            column = partialPatientColumn.substring(Constants.documentPrefix.length());
        else
            column = partialPatientColumn;

        int index = column.lastIndexOf(Constants.singleSeparator);
        if (index == -1)
            return 0;

        return Long.valueOf(column.substring(index + 1));
    }

    public static String preparePartialDocIdColumnName(String id, long TimeInMillis)
    {
        String column = id.startsWith(Constants.documentPrefix) ? id : Constants.documentPrefix + id;

        if (TimeInMillis != 0)
            return column + Constants.singleSeparator + TimeInMillis;
        else
            return column;
    }

    public static String getIdFromPartialDocIdColumnName(String partialPatientColumn)
    {
        String column;

        if (partialPatientColumn != null && partialPatientColumn.startsWith(Constants.documentPrefix))
            column = partialPatientColumn.substring(Constants.documentPrefix.length());
        else
            column = partialPatientColumn;

        int index = column.lastIndexOf(Constants.singleSeparator);
        if (index == -1)
            return null;

        return column.substring(0, index);
    }

    public static long getTimeFromPartialDocIdColumnName(String partialPatientColumn)
    {
        String column;

        if (partialPatientColumn != null && partialPatientColumn.startsWith(Constants.documentPrefix))
            column = partialPatientColumn.substring(Constants.documentPrefix.length());
        else
            column = partialPatientColumn;

        int index = column.lastIndexOf(Constants.singleSeparator);
        if (index == -1)
            return 0;

        return Long.valueOf(column.substring(index + 1));
    }

    public static String preparePatientUUIDSharedKey(String key)
    {
        if (key != null && !key.startsWith(Constants.patientUUIDSharedPrefix))
            return Constants.patientUUIDSharedPrefix + key;

        return key;
    }

    // Add a separator between the prefix and batchname since prefix doesn't end with separator
    // Don't add a separator at the end since there is nothing else added to the key
    public static String prepareBatchDocumentOneToManyIndexKey(String batchName)
    {
        if (batchName != null && !batchName.startsWith(Constants.batchDocumentOneToManyIndexKeyPrefix))
            return Constants.batchDocumentOneToManyIndexKeyPrefix + Constants.singleSeparator + batchName;

        return batchName;
    }

    // Don't add a separator between the prefix and batchname since prefix ends with separator
    // Add a separator at the end since the key is augmented
    public static String prepareBatchDocumentOneToManyKey(String batchName)
    {
        if (batchName != null && !batchName.startsWith(Constants.batchDocumentOneToManyKeyPrefix))
            return Constants.batchDocumentOneToManyKeyPrefix + batchName + Constants.singleSeparator;

        return batchName;
    }
}
