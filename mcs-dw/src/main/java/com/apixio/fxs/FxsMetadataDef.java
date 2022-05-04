package com.apixio.fxs;

import com.apixio.bms.MetadataDef;

/**
 * Central definition for fixed/static/known metadata on f(x) definitions
 */
public class FxsMetadataDef
{
    /**
     * Defined by science/engineering
     */
    public static final MetadataDef MD_NAME        = MetadataDef.stringAttribute("name").required();
    public static final MetadataDef MD_DESCRIPTION = MetadataDef.stringAttribute("description").required();

}
