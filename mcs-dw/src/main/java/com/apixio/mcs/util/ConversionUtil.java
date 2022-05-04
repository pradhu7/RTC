package com.apixio.mcs.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.apixio.bms.Blob;
import com.apixio.bms.Metadata;
import com.apixio.mcs.FullModelMeta;
import com.apixio.mcs.Lifecycle;
import com.apixio.mcs.McsMetadataDef;
import com.apixio.mcs.ModelMeta;
import com.apixio.mcs.PartMeta;

/**
 * Methods to convert from Blobs to external forms of the same data.
 */
public class ConversionUtil
{
    /**
     * PartMeta is DTO
     */
    public static PartMeta blobToPartMeta(String modelId, Blob part)
    {
        return new PartMeta(
            modelId,
            part.getPartName(),
            part.getStoragePath(),
            part.getMd5Digest(),
            part.getCreatedBy(),
            part.getCreatedAt(),
            part.getMimeType(),
            jsonToMap(part.getExtra1()),
            jsonToMap(part.getExtra2())
            );
    }

    /**
     * ModelMeta is DTO
     */
    public static ModelMeta blobToModelMeta(Blob blob)
    {
        Metadata md = blob.getMetadata();

        return new ModelMeta(
            blob.getUuid().getID(),
            blob.getCreatedAt(),
            blob.getSoftDeleted(),
            blob.getCreatedBy(),
            md.getString(McsMetadataDef.MD_EXECUTOR),
            md.getString(McsMetadataDef.MD_NAME),
            md.getString(McsMetadataDef.MD_OUTPUT_TYPE),
            md.getString(McsMetadataDef.MD_PRODUCT),
            md.getString(McsMetadataDef.MD_VERSION),
            Lifecycle.valueOf(md.getString(McsMetadataDef.MD_STATE)),
            md.getString(McsMetadataDef.MD_PDSID),
            jsonToMap(blob.getExtra1()),
            jsonToMap(blob.getExtra2())
            );
    }

    public static FullModelMeta blobToFullModelMeta(Blob blob, List<PartMeta> parts)
    {
        Metadata md = blob.getMetadata();

        // unfortunately repeated code as there's no good way to fully separate
        // this mini business logic from the pure DTOs in common artifact

        return new FullModelMeta(
            blob.getUuid().getID(),
            blob.getCreatedAt(),
            blob.getSoftDeleted(),
            blob.getCreatedBy(),
            md.getString(McsMetadataDef.MD_EXECUTOR),
            md.getString(McsMetadataDef.MD_NAME),
            md.getString(McsMetadataDef.MD_OUTPUT_TYPE),
            md.getString(McsMetadataDef.MD_PRODUCT),
            md.getString(McsMetadataDef.MD_VERSION),
            Lifecycle.valueOf(md.getString(McsMetadataDef.MD_STATE)),
            md.getString(McsMetadataDef.MD_PDSID),
            jsonToMap(blob.getExtra1()),
            jsonToMap(blob.getExtra2()),
            parts);
    }

    /**
     * Protects against null json input by returning empty map for it
     */
    public static Map<String, Object> jsonToJson(JSONObject json)
    {
        return (json != null) ? jsonToMap(json) : Collections.EMPTY_MAP;
    }

    /**
     * Recursive method to create a Map<String,Object> from an arbitrary
     * JSONObject.
     */
    private static Map<String, Object> jsonToMap(JSONObject json)
    {
        if (json == null)
            return null;

        Map<String, Object> map = new HashMap<>();

        for (String key : json.keySet())
        {
            Object val = json.get(key);

            if (val instanceof JSONObject)
                val = jsonToMap((JSONObject) val);
            else if (val instanceof JSONArray)
                val = jsonToList((JSONArray) val);

            map.put(key, val);
        }

        return map;
    }

    private static List<Object> jsonToList(JSONArray array)
    {
        List<Object> list = new ArrayList<>();
        int          len  = array.length();

        for (int i = 0; i < len; i++)
        {
            Object val = array.get(i);

            if (val instanceof JSONObject)
                val = jsonToMap((JSONObject) val);
            else if (val instanceof JSONArray)
                val = jsonToList((JSONArray) val);

            list.add(val);                    
        }

        return list;
    }

}
