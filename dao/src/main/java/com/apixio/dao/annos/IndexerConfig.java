package com.apixio.dao.annos;

class IndexerConfig
{

    /**
     * S3 bucket name that holds the kafka-connect backups of kafka queue/topic messages.  This bucket
     * will have data read FROM it
     */
    private String kafkaTopicsBucket;       // like "apixio-backups-kafka-topics"

    /**
     * The expected object key/path for backup topic files under kafkaTopicsBucket.  This string is used to
     * construct the actual object key given month/day/year.  The folder MUST be a prefix of the object
     * template!
     */
    private String topicFolderTemplate;     // like "prod/topics/annotationscience_prd/year=%s/month=%s/day=%s"
    private String topicObjectTemplate;     // like "prod/topics/annotationscience_prd/year=%s/month=%s/day=%s/annotationscience_prd+%s+%010d.json.gz"

    /**
     * S3 bucket and key prefix that will store the indexes files produced by this code.
     */
    private String indexFilesBucket;        // like "apixio-science-data"
    private String indexFilesKeyPrefix;     // like "david/fxannotations/patientindex/v1"; a trailing "/" is ensured


    /**
     * Path to local directory that will hold the constructed/merged .index.gz files prior
     * to being pushed to S3 (at indexFilesBucket/indexFilesKeyprefix)
     */
    private String localIndexDirectory;     // like "/tmp/patientindex/v1"

    /**
     * Path to local log file that contains history of dates processed 
     */
    private String processedLogfilePath;    // like "/tmp/PatientIndex.log.csv"
    

    /**
     *
     */
    public IndexerConfig(String topicsBucket, String topicFolderTemplate, String topicObjectTemplate,
                         String indexBucket, String indexKeyPrefix,
                         String localIndexDir, String localLogfile)
    {
        this.kafkaTopicsBucket    = topicsBucket;
        this.topicFolderTemplate  = topicFolderTemplate;
        this.topicObjectTemplate  = topicObjectTemplate;
        this.indexFilesBucket     = indexBucket;
        this.indexFilesKeyPrefix  = ensureTrailingSlash(indexKeyPrefix);
        this.localIndexDirectory  = localIndexDir;
        this.processedLogfilePath = localLogfile;
    }

    /**
     * Getters
     */
    public String getKafkaTopicsBucket()
    {
        return kafkaTopicsBucket;
    }
    public String getTopicFolderTemplate()
    {
        return topicFolderTemplate;
    }
    public String getTopicObjectTemplate()
    {
        return topicObjectTemplate;
    }
    public String getIndexFilesBucket()
    {
        return indexFilesBucket;
    }
    public String getIndexFilesKeyPrefix()
    {
        return indexFilesKeyPrefix;
    }
    public String getLocalIndexDirectory()
    {
        return localIndexDirectory;
    }
    public String getProcessedLogFilePath()
    {
        return processedLogfilePath;
    }

    /**
     *
     */
    private String ensureTrailingSlash(String s)
    {
        if (!s.endsWith("/"))
            return s + "/";
        else
            return s;
    }

}
