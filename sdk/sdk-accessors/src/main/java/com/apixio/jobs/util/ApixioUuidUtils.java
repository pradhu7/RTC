package com.apixio.jobs.util;

import com.apixio.XUUID;
import com.apixio.XUUIDPrefixes;
import com.apixio.ensemble.impl.common.SmartXUUID;
import com.apixio.useracct.buslog.PatientDataSetLogic;

import java.util.Optional;
import java.util.UUID;

public class ApixioUuidUtils {

    public static XUUID toPatientXUuid(UUID uuid)
    {
        return toPatientSmartXUuid(uuid).toXUUID();
    }

    public static XUUID toDocumentXUuid(UUID uuid)
    {
        return toDocumentSmartXUuid(uuid).toXUUID();
    }

    public static XUUID toSourceXUuid(UUID uuid)
    {
        return XUUID.fromString("SRC_" + uuid.toString());
    }

    public static SmartXUUID toPatientSmartXUuid(UUID uuid)
    {
        return new SmartXUUID(SmartXUUID.PatientIdentifier(), uuid.toString());
    }

    public static SmartXUUID toDocumentSmartXUuid(UUID uuid)
    {
        return new SmartXUUID(SmartXUUID.DocumentIdentifier(), uuid.toString());
    }

    public static XUUID toPdsXUuid(String pdsId)
    {
        return PatientDataSetLogic.patientDataSetIDFromLong(Long.parseLong(pdsId));
    }


    public static XUUID toDocumentSetXUuid(String docSetId)
    {
        return XUUID.fromString(XUUIDPrefixes.DOCSET_TYPE + "_00000000-0000-0000-0000-" + zeroPad12(Long.parseLong(docSetId)), XUUIDPrefixes.DOCSET_TYPE);
    }

    public static String zeroPad12(long value)
    {
        String vStr = Long.toString(value);
        return "000000000000".substring(0, 12 - vStr.length()) + vStr;
    }


}
