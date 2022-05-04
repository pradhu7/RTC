package com.apixio.dao.seqstore.utility;

import java.util.UUID;

import com.apixio.dao.Constants;
import com.apixio.dao.Constants.SeqStore;

public class SeqStoreKeyUtility
{
    public static String prepareSubjectPeriodKey(String subjectId)
    {
        return SeqStore.subjectPeriodKeyPrefix + subjectId;
    }

    public static String prepareSubjectPeriodColumn(String period)
    {
        return period;
    }

    public static String prepareSubjectKey(String subjectId, String period)
    {
        return SeqStore.subjectKeyPrefix + subjectId + Constants.singleSeparator + period;
    }

    public static String prepareSubjectColumn(String endTimePeriodSt, String insertionTime)
    {
        return endTimePeriodSt + Constants.singleSeparator + insertionTime + Constants.singleSeparator + UUID.randomUUID().toString();
    }

    public static String preparePathValueKey(String subjectId)
    {
        return SeqStore.pathValueKeyPrefix + subjectId;
    }

    public static String preparePathValueColumn(String pathValue)
    {
        return pathValue;
    }

    public static String prepareSubjectPathValuePeriodKey(String subjectId, String pathValue)
    {
        return SeqStore.subjectPathValuePeriodKeyPrefix + subjectId + Constants.singleSeparator + pathValue;
    }

    public static String prepareSubjectPathValuePeriodColumn(String period)
    {
        return period;
    }

    public static String prepareSubjectPathValueKey(String subjectId, String pathValue, String period)
    {
        return SeqStore.subjectPathValueKeyPrefix + subjectId + Constants.singleSeparator + pathValue + Constants.singleSeparator + period;
    }

    public static String prepareSubjectPathValueColumn(String endTimePeriodSt, String insertionTime)
    {
        return endTimePeriodSt + Constants.singleSeparator + insertionTime + Constants.singleSeparator + UUID.randomUUID().toString();
    }

    public static String prepareSubjectPathPeriodKey(String subjectId, String path)
    {
        return SeqStore.subjectPathPeriodKeyPrefix + subjectId + Constants.singleSeparator + path;
    }

    public static String prepareSubjectPathPeriodColumn(String period)
    {
        return period;
    }

    public static String prepareSubjectPathKey(String subjectId, String path, String period)
    {
        return SeqStore.subjectPathKeyPrefix + subjectId + Constants.singleSeparator + path + Constants.singleSeparator + period;
    }

    public static String prepareSubjectPathColumn(String endTimePeriodSt, String insertionTime)
    {
        return endTimePeriodSt + Constants.singleSeparator + insertionTime + Constants.singleSeparator + UUID.randomUUID().toString();
    }

    public static String prepareAddressKey(String addressId)
    {
        return SeqStore.addressKeyPrefix + addressId;
    }

    public static String prepareAddressColumn(String insertionTime)
    {
        return insertionTime + Constants.singleSeparator + UUID.randomUUID().toString();
    }
}
