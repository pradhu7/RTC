package com.apixio.s3lambda;

/**
 * Class that has a copy of some PatientDataSetLogic code that canonicalizes the PDS ID as the
 * AWS S3 Lambda function needs to be able to extract the PDS ID from an S3 path.
 */
public class LambdaUtil
{
    /**
     * The consts and methods here have been taken from PatientDataSetLogic.java
     */
    private static final String UUID_GRP1TO4     = "00000000-0000-0000-0000-";   // 8-4-4-4-
    private static final String ZERO12           = "000000000000";
    private static final String PDS_OBJTYPE      = "O";

    /**
     * For production S3 documents, the PDS ID (which is the v2 security scope) is part of the S3 URL.  As of 2/2021
     * there are two URL patterns to handle:
     *
     *  * s3://apixio-document-oregon/document10001014/0004/7988/-c2d/8-46/8e-8/c4d-/ae0c/8da5/7b45
     *  * s3://apixio-document-oregon/buck10000357/0004/7988/-c2d/8-46/8e-8/c4d-/ae0c/8da5/7b46
     *
     * the only difference being "document" vs "buck".  This method locates the orgID (e.g., 10001014) and forms the
     * canonical scope, which is like "pds-O_00000000-0000-0000-0000-000010000330".  Note that the canonicalization
     * logic/constants are copied from PatientDataSetLogic.java
     */
    public static String getScope(String key)
    {
        if (!key.startsWith("s3://"))
            throw new IllegalArgumentException("Expected S3 key to begin with s3:// but didn't:  " + key);

        String[] elements = key.substring("s3://".length()).split("/");

        if (elements.length > 2)
        {
            String pdsID = grabPdsID(elements[1], "document");

            if (pdsID == null)
                pdsID = grabPdsID(elements[1], "buck");

            if (pdsID != null)
                return pdsID;
        }

        throw new IllegalArgumentException("Unable to convert S3 key '" + key + "' to its PDS ID");
    }

    /**
     * Convert a long value into an XUUID by converting the long to a zero-padded string
     * and putting that string in the lower part of a UUID.
     */
    private static String patientDataSetIDFromLong(long id)
    {
        return PDS_OBJTYPE + "_" + UUID_GRP1TO4 + zeroPad12(id);
    }
    private static String zeroPad12(long value)
    {
        String vStr = Long.toString(value);
        return ZERO12.substring(0, 12 - vStr.length()) + vStr;
    }

    private static String grabPdsID(String ele, String prefix)
    {
        if (ele.startsWith(prefix))
            return patientDataSetIDFromLong(Long.parseLong(ele.substring(prefix.length())));
        else
            return null;
    }

}
