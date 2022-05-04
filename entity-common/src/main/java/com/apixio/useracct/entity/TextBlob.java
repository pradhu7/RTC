package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 *
 */
public class TextBlob extends NamedEntity {

    public static final String OBJTYPE = ObjectTypes.TEXT_BLOB;

    /**
     * Fields
     */
    private final static String F_BLOB_ID    = "bid";       // bid is the externally supplied unique id/name
    private final static String F_BLOB_TEXT  = "blob";      // actual text contents
    private final static String F_BLOB_MOD   = "modified";  // last modified time

    /**
     * Actual fields
     */
    private String blobID;
    private String contents;
    private long   modified;

    /**
     *
     */
    public TextBlob(ParamSet paramSet)
    {
        super(paramSet);

        this.blobID   = paramSet.get(F_BLOB_ID);
        this.contents = paramSet.get(F_BLOB_TEXT);
        this.modified = Long.parseLong(paramSet.get(F_BLOB_MOD));
    }

    public TextBlob(String bid, String name)
    {
        super(OBJTYPE, name);

        this.blobID   = bid;
        this.modified = System.currentTimeMillis();
    }

    /**
     *
     */
    public String getBlobID()
    {
        return blobID;
    }
    public String getBlobContents()
    {
        return contents;
    }
    public long getBlobModified()
    {
        return modified;
    }

    public void setBlobContents(String contents)
    {
        this.contents = contents;
        this.modified = System.currentTimeMillis();
    }

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_BLOB_ID,   blobID);
        fields.put(F_BLOB_TEXT, contents);
        fields.put(F_BLOB_MOD,  Long.toString(modified));
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[TextBlob " + super.toString() +
                "; blobID=" + blobID +
                "; contents=" + contents +
                "; modified=" + modified +
                "]");
    }
}
