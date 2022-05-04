package com.apixio.bms;

/**
 * Used as a specifier of part name and source of data when adding multiple parts
 * to a blob/model.
 *
 * The source of part data is given by a URI-like syntax, where the following protocols
 * are supported:
 *
 *  * md5:...
 *  * s3://...
 *
 * For example, to declare that the source of the part data is an already existing/controlled
 * blob that's identifiable by a specific MD5 hash value, the source value would be:
 *
 *    md5:e7d8a6ba2e891470d876c7b1af739b8e
 *
 * and to declare that the source is an S3 object:
 *
 *    s3://apixio-test-bucket/testdata/theobject
 */
public class PartSource
{
    /**
     * Name and source are required; mimeType is not required if source is md5
     */
    public String name;
    public String source;
    public String mimeType;

    /**
     * The supported source types; all values of the "source" field must start with one
     * of these strings in order to be recognized by the core code.
     */
    public static final String SOURCE_MD5 = "md5:";
    public static final String SOURCE_S3  = "s3://";

    /**
     *
     */ 
    public PartSource()
    {
    }

    @Override
    public String toString()
    {
        return "PartSource(" + name + ", " + source + ", " + mimeType + ")";
    }

}
