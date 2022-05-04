package com.apixio.useracct.buslog;

import java.util.ArrayList;
import java.util.List;

import com.apixio.restbase.LogicBase;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.TextBlobs;
import com.apixio.useracct.entity.TextBlob;

/**
 */
public class TextBlobLogic extends LogicBase<SysServices> {

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType {
        /**
         * for modifyOrganization:
         */
        MISSING_NAME,
        MISSING_CONTENT
    }

    /**
     * If organization operations fail they will throw an exception of this class.
     */
    public static class TextBlobException extends BaseException {

        private FailureType failureType;

        public TextBlobException(FailureType failureType)
        {
            super(failureType);
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * System Services used
     */
    private TextBlobs     textBlobs;

    /**
     * Constructor.
     */
    public TextBlobLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    @Override
    public void postInit()
    {
        textBlobs = sysServices.getTextBlobs();
    }

    /**
     *
     */
    public TextBlob putTextBlob(String blobID, TextBlobDTO params)
    {
        TextBlob tb = textBlobs.getBlob(blobID);

        if (tb == null)
        {
            checkRequired(params);

            tb = new TextBlob(blobID, params.name);

            params.dtoToEntity(tb);
            textBlobs.create(blobID, tb);
        }
        else
        {
            params.dtoToEntity(tb);
            textBlobs.update(tb);
        }

        return tb;
    }

    /**
     */
    public TextBlob getTextBlob(String blobID)
    {
        return textBlobs.getBlob(blobID);
    }

    /**
     */
    public List<TextBlob> getAllTextBlobs()
    {
        List<TextBlob> all = new ArrayList<>();

        for (XUUID tbi : textBlobs.getAllEntityIDs())
            all.add(textBlobs.getBlobInCache(tbi));

        return all;
    }

    /**
     *
     */
    private void checkRequired(final TextBlobDTO params)
    {
        if (params.name    == null)             throw new TextBlobException(FailureType.MISSING_NAME);
        if (params.contents == null)            throw new TextBlobException(FailureType.MISSING_CONTENT);
    }

}
