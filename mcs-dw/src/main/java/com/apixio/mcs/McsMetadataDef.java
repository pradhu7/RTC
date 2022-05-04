package com.apixio.mcs;

import com.apixio.bms.MetadataDef;

/**
 * Central definition for fixed/static/known metadata on Model Combinations
 */
public class McsMetadataDef
{
    /**
     * Defined by science/engineering
     */
    public static final MetadataDef MD_EXECUTOR    = MetadataDef.stringAttribute("executor").required();
    public static final MetadataDef MD_NAME        = MetadataDef.stringAttribute("name").required();
    public static final MetadataDef MD_OUTPUT_TYPE = MetadataDef.stringAttribute("outputType").required();
    public static final MetadataDef MD_PDSID       = MetadataDef.stringAttribute("pdsId").optional();
    public static final MetadataDef MD_PRODUCT     = MetadataDef.stringAttribute("product").required();
    public static final MetadataDef MD_STATE       = MetadataDef.stringAttribute("state").required();
    public static final MetadataDef MD_VERSION     = MetadataDef.stringAttribute("version").optional();

    // [experimental] custops-defined and -owned metadata
    public static final MetadataDef MD_STUFF = MetadataDef.stringAttribute("stuff").optional().group("custops");

}
