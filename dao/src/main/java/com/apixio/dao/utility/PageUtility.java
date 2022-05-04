package com.apixio.dao.utility;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.AmazonServiceException;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.model.blob.BlobType;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;

/**
 * Created by mramanna on 3/2/17.
 */
public class PageUtility {
    private static final Logger logger = LoggerFactory.getLogger(PageUtility.class);

    // WARN: do not change value.
    public static final String METADATA_TEXTEXTRACTED = "textextracted";
    public static final String NOT_IMPLEMENTED        = "NOT_IMPLEMENTED";

    // WARN: do not change value.
    public static final String ORIGINAL_CONTENT = "original_content";

    public static final String MD_KEY_S3_COPY_LEVEL     = "s3_ext.level";
    public static final String MD_VAL_S3_COPY_LEVEL_DOC = "doc";

    public static final String MD_KEY_PREFIX = "s3_ext.ts";

    public static final String TEXT_EXTRACTED            = "text_extracted";
    public static final String KEY_S3_EXT_TEXT_EXTRACTED = MD_KEY_PREFIX + "." + TEXT_EXTRACTED;

    public static final String STRING_CONTENT            = "string_content";
    public static final String KEY_S3_EXT_STRING_CONTENT = MD_KEY_PREFIX + "." + STRING_CONTENT;

    public static final int MAX_ATTEMPTS             = 10;
    public static final int SLEEP_MS_BETWEEN_RETRIES = 1000;

    public static BlobType getBlobTypeForLevel1StringContent(UUID documentUUID)
    {
        BlobType.Builder builder = new BlobType.Builder(documentUUID, STRING_CONTENT);
        return builder.build();
    }

    public static BlobType getBlobTypeForLevel1TextExtracted(UUID documentUUID)
    {
        BlobType.Builder builder = new BlobType.Builder(documentUUID, TEXT_EXTRACTED);
        return builder.build();
    }

    @Deprecated // By default, include both content and docCache for legacy APIs in downstream services
    public static void constructAPO(String pdsID, Patient patient, BlobDAO blobDao) throws Exception {
        constructAPO(pdsID, patient, blobDao, true, true);
    }

    public static void constructAPO(String pdsID, Patient patient, BlobDAO blobDao, boolean includeContent, boolean includeDocCache) throws Exception
    {
        if (patient == null || patient.getDocuments() == null)
            return;

        Iterator<Document> docIter = patient.getDocuments().iterator();
        if (docIter != null && docIter.hasNext())
        {
            Document d = docIter.next();

            if (includeContent) {
                constructDocument(pdsID, d, blobDao);
            }

            if (includeDocCache) {
                DocCacheElemUtility.reconstituteCleanTextInDocument(pdsID, d, blobDao);
                DocCacheElemUtility.updatePatientUuidInDocumentCleanText(patient.getPatientId(), d);
            }
        }
    }


    public static void constructDocument(String pdsId, Document d, DaoServices daoServices) throws Exception
    {
        BlobDAO blobDao = daoServices.getBlobDAO();
        constructDocument(pdsId, d, blobDao);
    }


    public static void constructDocument(String pdsId, Document d, BlobDAO blobDao) throws Exception
    {
        Map<String, String> metadata = d.getMetadata();

        // currently handles only document level s3 copy.
//        if (!metadata.containsKey(MD_KEY_S3_COPY_LEVEL) ||
//                !metadata.get(MD_KEY_S3_COPY_LEVEL).equals(MD_VAL_S3_COPY_LEVEL_DOC))
//        {
//            return;
//        }


        if (metadata.containsKey(KEY_S3_EXT_STRING_CONTENT))
        {
            boolean   success = false;
            int       attempt = 1;
            Exception lastError = null;

            for (; attempt < MAX_ATTEMPTS && !success; attempt++)
            {
                String      s;
                InputStream is = null;
                try
                {
                    BlobType blobType = getBlobTypeForLevel1StringContent(d.getInternalUUID());
                    is = blobDao.read(blobType, pdsId);
                    s = IOUtils.toString(is, StandardCharsets.UTF_8);
                    if (!isEmpty(s))
                    {
                        d.setStringContent(s);
                        success = true;
                    } else
                    {
                        logger.warn("unable to fetch stringContent from s3 for document: " + d.getInternalUUID());
                    }

                } catch (AmazonServiceException ex)
                {
                    if ("NoSuchKey".equalsIgnoreCase(ex.getErrorCode()) && ex.getStatusCode() == 404)
                    {
                        // key not found
                        lastError = ex;
                        break;
                    }

                    Thread.sleep(SLEEP_MS_BETWEEN_RETRIES);
                } catch (Exception ex)
                {
                    lastError = ex;
                    // sleep and retry
                    Thread.sleep(SLEEP_MS_BETWEEN_RETRIES);
                } finally
                {
                    if (is != null)
                    {
                        IOUtils.closeQuietly(is);
                    }
                }

            }

            if (!success)
            {
                if (lastError != null) {
                    throw new RuntimeException("unable to fetch StringContent for document uuid: " + d.getInternalUUID() + " from s3 after " + attempt + " attempts", lastError);
                } else {
                    throw new RuntimeException(
                        "unable to fetch StringContent for document uuid: " + d.getInternalUUID()
                            + " from s3 after " + attempt + " attempts");
                }
            }
        }

        if (metadata.containsKey(KEY_S3_EXT_TEXT_EXTRACTED))
        {
            boolean success = false;
            int     attempt = 1;

            for (; attempt < MAX_ATTEMPTS && !success; attempt++)
            {
                String      s;
                InputStream is = null;
                try
                {
                    BlobType blobType = getBlobTypeForLevel1TextExtracted(d.getInternalUUID());
                    is = blobDao.read(blobType, pdsId);
                    s = IOUtils.toString(is, StandardCharsets.UTF_8);
                    if (!isEmpty(s))
                    {
                        metadata.put(METADATA_TEXTEXTRACTED, s);
                        success = true;
                    } else
                    {
                        logger.warn("unable to fetch textExtracted from s3 for document: " + d.getInternalUUID());
                    }
                } catch (AmazonServiceException ex)
                {
                    if ("NoSuchKey".equalsIgnoreCase(ex.getErrorCode()) && ex.getStatusCode() == 404)
                    {
                        // key not found
                        break;
                    }

                    Thread.sleep(SLEEP_MS_BETWEEN_RETRIES);
                } catch (Exception ex)
                {
                    // sleep and retry
                    Thread.sleep(SLEEP_MS_BETWEEN_RETRIES);
                } finally
                {
                    if (is != null)
                    {
                        IOUtils.closeQuietly(is);
                    }
                }
            }

            if (!success)
            {
                throw new RuntimeException("unable to fetch textExtracted for document uuid: " + d.getInternalUUID() + " from s3 after " + attempt + " attempts");
            }
        }
    }

    private static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }
}
