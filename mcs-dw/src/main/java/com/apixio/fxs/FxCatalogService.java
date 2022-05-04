package com.apixio.fxs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import com.google.protobuf.util.JsonFormat;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.apixio.XUUID;
import com.apixio.bms.Blob;
import com.apixio.bms.BlobManager;
import com.apixio.bms.Metadata;
import com.apixio.bms.Schema;
import com.apixio.dao.utility.DaoServices;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.util.ConversionUtil;
import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.util.Md5DigestInputStream;

/**
 * FxCatalogService is the interface from MCS code to Blob Management Service
 * code.  It contains state-based business logic.
 */
public class FxCatalogService
{
    private static Logger LOGGER = Logger.getLogger(FxCatalogService.class);

    /**
     * Type prefix for XUUIDs
     */
    public static final String TYPE_PREFIX = "FX";

    /**
     * We store FxDefs using JSON format so we need protobuf converter
     */
    private final static JsonFormat.Printer formatter = JsonFormat.printer().omittingInsignificantWhitespace();

    /**
     * Schema is just a bunch of metadata definitions.
     */
    private static Schema schema = new Schema(
        FxsMetadataDef.MD_NAME,
        FxsMetadataDef.MD_DESCRIPTION
        );

    /**
     * Does all the hard work
     */
    private BlobManager blobManager;

    /**
     *
     */
    public FxCatalogService(DaoServices daos, ConfigSet config)
    {
        String loggingConfig = config.getString("log4jConfiguration", null);

        blobManager = new BlobManager(daos, config, schema);

        if (loggingConfig != null)
        {
            System.out.println("Setting log4j configuration from file " + loggingConfig);
            PropertyConfigurator.configure(loggingConfig);
        }
    }

    /**
     * Finds an f(x) def by serializing the def to protobuf JSON and searching for a blob with the
     * same md5 hash, returning the blob if it's found, and creating one if it isn't found.
     */
    public Blob findOrCreateFxDef(FxDef fxDef, String createdBy, String name, String description) throws Exception
    {
        String      pbJson   = formatter.print(fxDef);
        String      md5      = getMd5Hash(pbJson);
        List<Blob>  existing = blobManager.getBlobsWithMd5Hash(md5);

        if (existing.size() > 0)
        {
            if (existing.size() > 1)
                LOGGER.warn("More than one FxDef found for the MD5 hash: " + md5);

            return existing.get(0);
        }
        else
        {
            Metadata md = Metadata.create();

            md.add(FxsMetadataDef.MD_NAME,        name);
            md.add(FxsMetadataDef.MD_DESCRIPTION, description);

            // we store the json format of FxDef as extra_1
            return blobManager.createBlobWithMd5Hash(TYPE_PREFIX, createdBy, md,
                                                     (new JSONObject(pbJson)), null,
                                                     md5);
        }
    }

    /**
     * deletes a blob
     */
    public void deleteFxDef(Blob blob)
    {
        blobManager.deleteBlob(blob);
    }

    /**
     * Loads and returns the given blob by ID
     */
    public Blob getFxDef(XUUID id)
    {
        //!! results of blobManager.getBlobMeta can be cached if lifecycle state != DRAFT

        return blobManager.getBlobMeta(id);
    }

    /**
     * Calculate md5 hash for the given string, using UTF8 as the encoding
     */
    private String getMd5Hash(String str)
    {
        MessageDigest digest = DigestUtils.getDigest(MessageDigestAlgorithms.MD5);
        byte[]        bytes  = str.getBytes(StandardCharsets.UTF_8);

        digest.update(bytes, 0, bytes.length);

        return ConversionUtil.toHex(digest.digest()).toUpperCase();
    }

}
