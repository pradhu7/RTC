package com.apixio.useracct.buslog;

import com.apixio.useracct.entity.TextBlob;

/**
 * Properties that can be supplied when creating or modifying a text blob.
 */
public class TextBlobDTO {
    public String   name;                  // required
    public String   description;           // optional
    public String   contents;              // required

    /**
     * Convenient and central method to transfer fields that are set in the DTO
     * over to the entity itself.  Only modifiable fields can be added here.
     */
    void dtoToEntity(TextBlob blob)
    {
        if (name != null)         blob.setName(name);
        if (description != null)  blob.setDescription(description);
        if (contents != null)     blob.setBlobContents(contents);
    }

}
