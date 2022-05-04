package com.apixio.dao.apxdata;

import com.apixio.XUUID;
import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;

/**
 * This class provides naming conventions on the typical and anticipated query key
 * fields.
 */
public class StandardQueryKeys
{
    /**
     * The toString() form of these enum values is used as the persisted key name.
     */
    public enum KEYNAME
    {
        PDS_UUID,
        PROJECT_UUID,
        DOCSET_UUID,
        SOURCE_UUID,
        SOURCE_TYPE,
        PATIENT_UUID
    }

    private QueryKeys qk = new QueryKeys();

    public StandardQueryKeys()
    {
    }

    public StandardQueryKeys withPdsID(XUUID pdsID)
    {
        addKey(pdsID.toString(), KEYNAME.PDS_UUID, "PDS ID");

        return this;
    }

    public StandardQueryKeys withProjectID(XUUID projectID)
    {
        addKey(projectID.toString(), KEYNAME.PROJECT_UUID, "Project ID");

        return this;
    }

    public StandardQueryKeys withDocsetID(XUUID docsetID)
    {
        addKey(docsetID.toString(), KEYNAME.DOCSET_UUID, "Docset ID");

        return this;
    }

    public StandardQueryKeys withSourceID(String sourceID, String sourceType)
    {
        addKey(sourceID,   KEYNAME.SOURCE_UUID, "Source ID");
        addKey(sourceType, KEYNAME.SOURCE_TYPE, "Source Type");

        return this;
    }

    public StandardQueryKeys withPatientUuid(XUUID patientUuid)
    {
        addKey(patientUuid.toString(), KEYNAME.PATIENT_UUID, "Patient UUID");

        return this;
    }

    public QueryKeys build()
    {
        return qk;
    }

    private void addKey(Object val, KEYNAME key, String desc)
    {
        if (val == null)
            throw new IllegalArgumentException("Value for " + desc + " can't be null");

        qk.with(key.toString(), val);
    }
}

