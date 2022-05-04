package com.apixio.dao.utility;

import com.amazonaws.AmazonServiceException;
import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.model.blob.BlobType;
import com.apixio.model.patient.Document;
import com.apixio.protobuf.Doccacheelements.DocCacheElementListProto;
import com.apixio.protobuf.Doccacheelements.DocCacheElementProto;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by mramanna on 6/15/17.
 */
public class DocCacheElemUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocCacheElemUtility.class);

    public static final String MD_KEY_DCE_COPY_LEVEL     = "doccacheelem_s3_ext.level";
    public static final String MD_VAL_DCE_COPY_LEVEL_DOC = "doc";

    static final        String MD_KEY_DCE_PREFIX          = "doccacheelem_s3_ext";
    public static final String MD_KEY_DCE_TS              = MD_KEY_DCE_PREFIX + ".ts";
    public static final String MD_KEY_DCE_FORMAT          = MD_KEY_DCE_PREFIX + ".format";
    public static final String MD_VAL_DCE_FORMAT_PROTOBUF = "protobuf_base64";
    public static final String MD_KEY_DEC_DATA            = MD_KEY_DCE_PREFIX + ".data";

    public static final String BLOB_TYPE_CLEANTEXT_PROTOBUF_B64 = "cleantext_protobuf_b64";

    public static final int MAX_ATTEMPTS             = 10;
    public static final int SLEEP_MS_BETWEEN_RETRIES = 1000;

    public static BlobType getBlobTypeForCleanTextProtobufBase64(UUID documentUUID)
    {
        BlobType.Builder builder = new BlobType.Builder(documentUUID, BLOB_TYPE_CLEANTEXT_PROTOBUF_B64);
        return builder.build();
    }


    public static void reconstituteCleanTextInDocument(String pdsId, Document d, BlobDAO blobDao) throws Exception
    {
        Map<String, String> metadata = d.getMetadata();

        // currently handles only document level s3 copy.
        if (!metadata.containsKey(MD_KEY_DCE_COPY_LEVEL) ||
                !metadata.get(MD_KEY_DCE_COPY_LEVEL).equalsIgnoreCase(MD_VAL_DCE_COPY_LEVEL_DOC) ||
                !metadata.containsKey(MD_KEY_DCE_FORMAT) ||
                !metadata.get(MD_KEY_DCE_FORMAT).equalsIgnoreCase(MD_VAL_DCE_FORMAT_PROTOBUF))
        {
            return;
        }


        boolean success = false;
        int     attempt = 1;

        for (; attempt < MAX_ATTEMPTS && !success; attempt++)
        {
            String      s;
            InputStream is = null;
            try
            {
                BlobType blobType = getBlobTypeForCleanTextProtobufBase64(d.getInternalUUID());
                is = blobDao.read(blobType, pdsId);
                s = IOUtils.toString(is, StandardCharsets.UTF_8);
                if (!isEmpty(s))
                {
                    // just set the data element in document. Should not update other metadata.
                    setSerializedDocCacheElemData(d, s);
                    success = true;
                } else
                {
                    LOGGER.warn("unable to fetch clean text from s3 for document: " + d.getInternalUUID());
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
            throw new RuntimeException("unable to fetch clean text for document uuid: " + d.getInternalUUID() + " from s3 after " + attempt + " attempts");
        }
    }

    /**
     * updates the patient uuid in existing serialized clean text. If clean text does not exist, will not create.
     *
     * @param patientUUID
     * @param d
     */
    public static void updatePatientUuidInDocumentCleanText(UUID patientUUID, Document d)
    {

        if (!hasCleanText(d))
        {
            return;
        }

        List<DocCacheElementProto> list  = extractDocCacheElementProtoList(d);
        List<DocCacheElementProto> list2 = new ArrayList<>();

        if (list != null)
        {
            for (DocCacheElementProto proto : list)
            {
                DocCacheElementProto.Builder builder = DocCacheElementProto.newBuilder(proto);
                builder.setPatientUUID(patientUUID.toString());
                list2.add(builder.build());
            }
        }

        if (!list2.isEmpty())
        {
            LOGGER.info("Updating cleantext in document with updated list");
            updateDocumentWithDocCacheElementProtoList(d, list2);
        }

    }


    /**
     * updates the document with serialized DocCacheElementProto list.
     *
     * @param d
     * @param protoList
     * @return serialized Data of list on success
     */
    public static String updateDocumentWithDocCacheElementProtoList(Document d, List<DocCacheElementProto> protoList)
    {

        if (d == null || protoList == null && protoList.isEmpty())
        {
            return null;
        }

        String bytesEncoded = serializeDocCacheElementProtoList(protoList);
        if (isEmpty(bytesEncoded))
        {
            throw new RuntimeException("unable to serialize DocCacheElementProto list");
        }

        return updateDocumentWithSerializedCleanTextData(d, bytesEncoded);
    }

    /**
     * updates the document with serialized text if possible and returns serialized data.
     *
     * @param d
     * @param data
     * @return
     */
    public static String updateDocumentWithSerializedCleanTextData(Document d, String data)
    {
        if (d == null || isEmpty(data))
        {
            return null;
        }

        final Map<String, String> md = d.getMetadata();

        // meta data
        // document level data
        md.put(MD_KEY_DCE_COPY_LEVEL, MD_VAL_DCE_COPY_LEVEL_DOC);
        // format of data
        md.put(MD_KEY_DCE_FORMAT, MD_VAL_DCE_FORMAT_PROTOBUF);
        // ts
        md.put(MD_KEY_DCE_TS, Long.toString(System.currentTimeMillis()));

        // actual data
        setSerializedDocCacheElemData(d, data);

        return getSerializedDocCacheElemData(d);
    }

    public static String getSerializedDocCacheElemData(Document d)
    {

        if (d == null || d.getMetadata() == null)
        {
            return null;
        }

        return d.getMetadata().get(MD_KEY_DEC_DATA);
    }

    /**
     * This method is expected to just set
     *
     * @param d
     * @param s
     */
    public static void setSerializedDocCacheElemData(Document d, String s)
    {
        if (d == null || d.getMetadata() == null || s == null || s.isEmpty())
        {
            return;
        }

        d.getMetadata().put(MD_KEY_DEC_DATA, s);
    }

    public static String deleteSerializedDocCacheElemData(Document d)
    {
        if (d != null || d.getMetadata() != null)
        {
            return d.getMetadata().remove(MD_KEY_DEC_DATA);
        }
        return null;
    }

    public static String serializeDocCacheElementProtoList(List<DocCacheElementProto> protoList)
    {

        if (protoList == null || protoList.isEmpty())
        {
            return null;
        }

        DocCacheElementListProto.Builder builder = DocCacheElementListProto.newBuilder();
        for (DocCacheElementProto proto : protoList)
        {
            builder.addData(proto);
        }

        DocCacheElementListProto listProto = builder.build();
        byte[]                   bytes     = listProto.toByteArray();
        return DatatypeConverter.printBase64Binary(bytes);
    }

    public static List<DocCacheElementProto> deserializeToDocCacheElementProtoList(final String ser)
    {

        if (isEmpty(ser))
        {
            return null;
        }

        byte[] bytes = DatatypeConverter.parseBase64Binary(ser);
        try
        {
            DocCacheElementListProto listProto = DocCacheElementListProto.parseFrom(bytes);
            if (listProto != null)
            {
                return listProto.getDataList();
            }
        } catch (InvalidProtocolBufferException e)
        {
            LOGGER.warn("Unable to deserialize using protobuf: {}", e);
        }

        return null;
    }

    /**
     * inspect document to figure out if clean text has been extracted in the past. Should not have side effects.
     */
    public static boolean hasCleanText(Document d)
    {

        if (d == null)
        {
            return false;
        }

        final Map<String, String> md = d.getMetadata();

        if (!md.containsKey(MD_KEY_DCE_COPY_LEVEL) || !md.get(MD_KEY_DCE_COPY_LEVEL).equalsIgnoreCase(MD_VAL_DCE_COPY_LEVEL_DOC))
        {
            return false;
        }

        if (!md.containsKey(MD_KEY_DCE_FORMAT) || !md.get(MD_KEY_DCE_FORMAT).equalsIgnoreCase(MD_VAL_DCE_FORMAT_PROTOBUF))
        {
            return false;
        }

        return true;
    }

    /**
     * must not throw. May return null.
     *
     * @param d
     * @return
     */
    public static Long getCleanTextTimestamp(Document d)
    {

        if (!hasCleanText(d))
        {
            return null;
        }

        final Map<String, String> md = d.getMetadata();
        if (!md.containsKey(MD_KEY_DCE_TS))
        {
            return null;
        }

        try
        {
            return Long.parseLong(md.get(MD_KEY_DCE_TS));
        } catch (Exception ex)
        {
            return null;
        }
    }

    public static List<DocCacheElementProto> extractDocCacheElementProtoList(Document d)
    {
        if (d == null || !hasCleanText(d))
        {
            return null;
        }

        String base64Encoded = getSerializedDocCacheElemData(d);
        if (isEmpty(base64Encoded))
        {
            return null;
        }

        return deserializeToDocCacheElementProtoList(base64Encoded);
    }

    public static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

}
