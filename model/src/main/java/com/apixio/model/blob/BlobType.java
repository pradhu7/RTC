package com.apixio.model.blob;

import java.util.UUID;

import com.google.common.base.Strings;

/**
 *  A blobType describes a blob. It has two required properties and two optional properties.
 *  1. Required properties: docUUID and imageType
 *  2. Optional properties: resolution and pageNumber
 *  3. Page numbers start from zero. If no page number property is set, the value of the page number is set to -1 (meaning no page).
 *
 *  A blob has a logical id which is basically a concatenation of its properties.
 *
 */

public class BlobType
{
    private static final String blobIdPrefix = "bl";
    private static final String seperator = "::";

    private final String docUUID;
    private final String imageType;
    private final String resolution;
    private final int pageNumber;

    public static class Builder
    {
        private final String docUUID;
        private final String imageType;
        private String resolution;
        private int pageNumber = -1;

        public Builder(UUID docUUID, String imageType)
        {
            this(docUUID.toString(), imageType);
        }

        public Builder(String docUUID, String imageType)
        {
            if (Strings.isNullOrEmpty(docUUID) || Strings.isNullOrEmpty(imageType))
                throw new IllegalStateException();

            this.docUUID = docUUID;
            this.imageType = imageType;
        }

        public Builder resolution(String resolution)
        {
            this.resolution = resolution;
            return this;
        }

        public Builder pageNumber(int pageNumber)
        {
            this.pageNumber = pageNumber;
            return this;
        }

        public BlobType build()
        {
            return new BlobType(this);
        }
    }

    private BlobType(Builder builder)
    {
        this.docUUID = builder.docUUID;
        this.imageType = builder.imageType;
        this.resolution = builder.resolution;
        this.pageNumber = builder.pageNumber;
    }

    /**
     * Returns a logical id that represents the blob. Used to access the Blob.
     *
     * @return
     */
    public String getID()
    {
        final StringBuilder sb = new StringBuilder(blobIdPrefix).append(docUUID).append(seperator).append(imageType);

        if (resolution != null)
        {
            sb.append(seperator).append(resolution);
            sb.append(seperator).append(pageNumber);
        }
        else
        {
            sb.append(seperator).append(seperator).append(pageNumber);
        }

        return sb.toString();
    }

    /**
     * Given an blob ID, it returns a BlobType
     *
     * @return
     */
    public static BlobType getBlobData(String ID)
    {
        return new Builder(getDocUUID(ID), getImageType(ID)).resolution(getResolution(ID)).pageNumber(getPageNumber(ID)).build();
    }

    /**
     * Get docUUID
     *
     * @return
     */
    public String getDocUUID()
    {
        return docUUID;
    }

    /**
     * Get docUUID given a blob ID
     *
     * @return
     */
    public static String getDocUUID(String ID)
    {
        String[] split = ID.split(seperator);
        return (split.length >= 1) ? split[0].substring(blobIdPrefix.length()) : null;
    }

    /**
     * Get imageType
     *
     * @return
     */
    public String getImageType()
    {
        return imageType;
    }

    /**
     * Get imageType given a blob ID
     *
     * @return
     */
    public static String getImageType(String ID)
    {
        String[] split = ID.split(seperator);
        return (split.length >= 2) ? split[1] : null;
    }

    /**
     * Get Resolution (might be null)
     *
     * @return
     */
    public String getResolution()
    {
        return resolution;
    }

    /**
     * Get Resolution given a blob ID (might be null)
     *
     * @return
     */
    public static String getResolution(String ID)
    {
        String[] split = ID.split(seperator);
        String resolution = (split.length >= 3) ? split[2] : null;
        if (Strings.isNullOrEmpty(resolution)) resolution = null;

        return resolution;
    }

    /**
     * Get pageNumber (if not set, returns -1)
     *
     * @return
     */
    public int getPageNumber()
    {
        return pageNumber;
    }

    /**
     * Get pageNumber given a blob ID (if not set, returns -1)
     *
     * @return
     */
    public static int getPageNumber(String ID)
    {
        String[] split = ID.split(seperator);
        return (split.length >= 4) ? Integer.valueOf(split[3]) : -1;
    }

    /**
     * ModelID should be used only for filtering/searching BlobTypes and not for accessing blobs.
     *
     * @return
     */
    public String getModelID()
    {
        final StringBuilder sb = new StringBuilder(blobIdPrefix).append(docUUID).append(seperator).append(imageType);

        if (resolution != null)
        {
            sb.append(seperator).append(resolution);
            if (pageNumber != -1) sb.append(seperator).append(pageNumber);
        }
        else if (pageNumber != -1)
        {
            sb.append(seperator).append(seperator).append(pageNumber);
        }

        return sb.toString();
    }

    /**
     * Used only for filtering/searching BlobTypes.
     *
     * @return
     */
    public static String makeModelDocUUID(String docUUID)
    {
        final StringBuilder sb = new StringBuilder(blobIdPrefix).append(docUUID);

        return sb.toString();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("BlobType{");

        sb.append("docUUID=").append(docUUID);
        sb.append(", imageType=").append(imageType);
        sb.append(", resolution=").append(resolution);
        sb.append(", pageNumber=").append(pageNumber);
        sb.append('}');

        return sb.toString();
    }
}
