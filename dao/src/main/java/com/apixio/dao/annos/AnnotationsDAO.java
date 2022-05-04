package com.apixio.dao.annos;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import com.apixio.datasource.s3.S3Ops;
import com.apixio.model.event.EventType;
import com.apixio.model.event.transformer.EventTypeJSONParser;
import com.apixio.restbase.config.ConfigSet;

/**
 * AnnotationsDAO provides access to patient annotations that have been pushed into the
 * "feedback" loop for the application side of the system.  These annotations are all in
 * the EventType (JSON) format and have been been added to a kafka queue and processed by
 * *something* (not sure what).  Because they've been in a kakfa queue, these events have
 * been backed up by kafka-connect and have been stored in an S3 bucket.  This DAO reads
 * these backed up JSON events from S3 and creates (for now) a by-patient index on top of
 * these source-of-truth files so that a query for all annotations for a patient ends up
 * with a list of these S3 objects to look through and filter for just that patient.
 *
 * While this is intended to be a short-term solution on the implementation side, there
 * is a general need to be able to pull patient annotations so this implementation could
 * change (hopefully will, as it's inefficient and inflexible) the API hopefully doesn't.
 */
public class AnnotationsDAO
{
    /**
     * Config points; these are all under the "patientAnnoConfigV1" yaml key
     */
    private static final String CFG_TOPICS_BUCKET      = "kafkaTopicBackup.bucket";
    private static final String CFG_TOPICS_FOLDER      = "kafkaTopicBackup.folder";
    private static final String CFG_TOPICS_OBJECT      = "kafkaTopicBackup.object";
    private static final String CFG_INDEX_BUCKET       = "index.bucket";
    private static final String CFG_INDEX_PREFIX       = "index.prefix";
    private static final String CFG_LOCAL_INDEXDIR     = "localIndexDirectory";
    private static final String CFG_LOCAL_LOGFILE      = "localLogFile";

    /**
     * Kafka topic-backup files that are older than ~Dec 2020 have JSON EventType data
     * that was serialized with different jackson options and aren't readable by the
     * newer EventType parser, so we have to use the older serialization options.
     */
    private EventTypeJSONParser oldParser = EventTypeJSONParser.getDeprecatedParser();
    private EventTypeJSONParser newParser = EventTypeJSONParser.getStandardParser();

    private IndexerConfig             idxConfig;
    private S3Ops                     s3Ops;
    private UUIDToAnnotationFileIndex index;

    /**
     * Create a new AnnotationsDAO instance
     */
    public AnnotationsDAO(ConfigSet config, S3Ops s3Ops)
    {
        this.s3Ops = s3Ops;

        setConfiguration(config);
    }

    /**
     * Set configuration for the DAO; all yaml keys are required and live under "patientAnnoConfigV1"
     * top-level key.
     */
    private void setConfiguration(ConfigSet config)
    {
        // LRU_MAX_SIZE?

        if (config == null)
            throw new IllegalArgumentException("No configuration for Patient Annotations DAO - check for yaml key 'patientAnnoConfigV1'");

        idxConfig = new IndexerConfig(config.getString(CFG_TOPICS_BUCKET), config.getString(CFG_TOPICS_FOLDER), config.getString(CFG_TOPICS_OBJECT),
                                      config.getString(CFG_INDEX_BUCKET), config.getString(CFG_INDEX_PREFIX),
                                      config.getString(CFG_LOCAL_INDEXDIR), config.getString(CFG_LOCAL_LOGFILE));

        index = new UUIDToAnnotationFileIndex(idxConfig, s3Ops.getS3Connector().getAmazonS3());
    }

    /**
     * Getting annotations for a patient consists of first getting all S3 object paths
     * that contain data for that patient, then reading each one in and keeping only
     * those EventType instances that have the patient as the subject.
     */
    public List<EventType> getAnnotationsForPatient(UUID patUUID) throws IOException
    {
        String[]         s3Paths  = index.getFilesForId(patUUID);
        String           patAsStr = patUUID.toString();
        List<EventType>  annos    = new ArrayList<>();

        //!! TODO:  cache by s3path or by patUUID

        for (String s3Path : s3Paths)
        {
            try (InputStream is = s3Ops.getObject(idxConfig.getKafkaTopicsBucket(), s3Path))
            {
                for (String jsonline : IndexUtil.readLinesFromGzStream(is))
                {
                    EventType event = safeParseEventTypeJSON(jsonline);

                    if (event.getSubject().getUri().equals(patAsStr))
                        annos.add(event);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return annos;
    }

    /**
     * Adds the EventType JSON data in the backup files for the given day to the indexes.
     */
    public void addS3KafkaTopicBackupToIndex(LocalDate ld) throws Exception
    {
        UUIDToAnnotationFileIndexer indexer = new UUIDToAnnotationFileIndexer(idxConfig, s3Ops.getS3Connector().getAmazonS3());

        //        System.out.println("SFMSFM AnnotationsDAO.addS3KafkaTopicBackupToIndex(" + ld + ")");

        indexer.addS3AnnotationFolderToS3Index(ld, new File(idxConfig.getProcessedLogFilePath()));
    }

    /**
     * Return the local log file that has the dates that have been processed
     */
    public File getLogFile()
    {
        return new File(idxConfig.getProcessedLogFilePath());
    }

    /**
     * Parse the EventType JSON in a way that supports both the old format and the new format.
     */
    private EventType safeParseEventTypeJSON(String json) throws JsonParseException, JsonMappingException, IOException
    {
        // new (current) parser shouldn't throw an exception on valid old and new JSON
        EventType event = newParser.parseEventTypeData(json);

        // old JSON parsed with new parser will not have most fields, so choose subject as a marker
        if (event.getSubject() == null)
            event = oldParser.parseEventTypeData(json);

        return event;
    }

}
