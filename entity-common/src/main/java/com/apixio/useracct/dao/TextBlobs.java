package com.apixio.useracct.dao;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.dao.UniqueID;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.TextBlob;
import com.apixio.restbase.DaoBase;

/**
 * TextBlobs are just named text blobs kept in redis.
 */
public final class TextBlobs extends CachingBase<TextBlob> {

    /**
     *
     */
    private UniqueID uniqueID;

    /**
     * Constructor
     */
    public TextBlobs(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.TEXT_BLOBS, TextBlob.OBJTYPE);

        uniqueID = new UniqueID(this, "textblob");
    }

    /**
     *
     */
    public void create(String id, TextBlob blob)
    {
        if (uniqueID.getUniqueID(id) != null)
            throw new IllegalArgumentException("Text blob with id [" + id + "] already exists");

        uniqueID.addUniqueID(id, blob.getID());

        super.create(blob);
    }

    /**
     *
     */
    public TextBlob getBlobInCache(XUUID blobID)
    {
        ParamSet fields = findByID(blobID);

        if (blobID != null)
            return new TextBlob(fields);
        else
            return null;
    }

    /**
     *
     */
    public TextBlob getBlob(String id)
    {
        XUUID blobID = uniqueID.getUniqueID(id);

        if (blobID != null)
            return new TextBlob(findByID(blobID));
        else
            return null;
    }

}
