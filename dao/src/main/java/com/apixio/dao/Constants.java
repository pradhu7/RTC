package com.apixio.dao;

public class Constants
{
    public static enum KeyType
    {
        partialPatientKey,
        partialPatientKeyHash,
        patientUUID,
        documentHash,
        documentId,
        documentUUID
    }

    public static final int RK_LIMIT = 1000; // VK: We are badly screwed - that's why we increased the threshold to 1000 from 5!!!!

    public static final String patientUUIDSharedPrefix     = "patShared_";   // used as a prefix of a key. In WorkArea table

    // prefixes used in the patient table
    public static final String documentPrefix              = "doc_";         // used as a prefix of a column name
    public static final String documentUUIDRKeyPrefix      = "rk_docuuid_";  // used as a reverseKey
    public static final String partialDocKeyPrefix         = "partdoc_";     // used as a Key
    public static final String patientUUIDPrefix           = "pat_";         // used as a Key
    public static final String partialPatientRKeyPrefix    = "rk_part_";     // used as a reverseKey

    // prefixes used in the patient table and periodic table
    public static final String partialPatientKeyPrefix     = "part_";        // used as a Key and a prefix of a column name

    // prefixes used in the patient table and trace table
    public static final String documentHashRKeyPrefix      = "rk_dochash_";  // used as a reverseKey
    public static final String documentIdRKeyPrefix        = "rk_docid_";    // used as a reverseKey

    // prefixes used in the trace table
    public static final String processNamePrefix           = "proc_";        // used as a prefix of a column name
    public static final String docEntryPrefix              = "docentry_";    // used as a prefix of a column name

    // global separators
    public static final String singleSeparator             = "_";
    public static final String doubleSeparator             = "__";

    // prefixes used in the patient table, trace table and link table
    public static final String documentUUIDPrefix          = "docuuid_";    // used as a key and a prefix of a column name

    public static final int patientNumberOfBuckets            = 100;      // don't change it!!!!
    public static final String patientOneToManyIndexKeyPrefix = "potmi";  // used as a Key
    public static final String patientOneToManyKeyPrefix      = "potm_";  // used as a Key

    public static final int batchDocumentNumberOfBuckets            = 100;      // don't change it!!!!
    public static final String batchDocumentOneToManyIndexKeyPrefix = "bdotmi"; // used as a Key
    public static final String batchDocumentOneToManyKeyPrefix      = "bdotm_"; // used as a Key

    public static final int traceDocumentSummaryNumberOfBuckets             = 100;       // don't change it!!!!
    public static final String traceDocumentSummaryOneToManyIndexKeyPrefix  = "tdsotmi"; // used as a Key
    public static final String traceDocumentSummaryOneToManyKeyPrefix       = "tdsotm_"; // used as a Key

    public static class Link
    {
        // Link table

        public static final String patientLinkPrefix               = "patuuid_";    // used as a Key
        public static final String documentLinkPrefix              = "docuuid_";    // used as a Key
        public static final String authoritativePatientLinkPrefix  = "apatuuid_";   // used as a Key

        public static final String orgLink                         = "org";                // used as a column name
        public static final String orgAndPatientUUIDLink           = "organdpatientuuid";  // used as a column name
        public static final String patientUUIDList                 = "patientuuidList";    // used as a column name
    }

    public static class Trace
    {
        // trace data - in patient table

        public static final String documentUUIDPrefix                           = "t_docuuid_";    // used as a key and a prefix of a column name

        public static final String traceDocumentSummaryOneToManyIndexKeyPrefix  = "tdsotmi"; // used as a Key
        public static final String traceDocumentSummaryOneToManyKeyPrefix       = "tdsotm_"; // used as a Key

        public static final String processNamePrefix                            = "proc_";        // used as a prefix of a column name
        public static final String docEntryPrefix                               = "docentry_";    // used as a prefix of a column name

        public static final int traceDocumentSummaryNumberOfBuckets             = 100;       // don't change it!!!!
    }

    public static class Event
    {
        public static final String MODEL_FILE        = "$modelFile";
        public static final String PROPERTY_VERSION  = "$version";
        public static final String NEW_INFERRED_CF   = "$newInferredCF";
        public static final String Model_NAME        = "model_filename";
    }

    public static class SeqStore
    {
        public static final String subjectPeriodKeyPrefix           = "sp_";      // used as a key
        public static final String subjectKeyPrefix                 = "s_";       // used as a key

        public static final String pathValueKeyPrefix               = "pv_";      // used as a key

        public static final String subjectPathValuePeriodKeyPrefix  = "spvp_";    // used as a key
        public static final String subjectPathValueKeyPrefix        = "spv_";     // used as a key

        public static final String subjectPathPeriodKeyPrefix       = "spp_";     // used as a key
        public static final String subjectPathKeyPrefix             = "sk_";      // used as a key (wrong prefix so that i doesn't conflict!!!)

        public static final String addressKeyPrefix                 = "a_";       // used as a key
    }
}
