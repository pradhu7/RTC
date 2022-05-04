package com.apixio.useracct.buslog;

import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.useracct.entity.TextBlob;

public class TextBlobProperties {

    /**
     * Convenience method to create a Map that can be easily translated to a JSON
     * object. 
     */
    public static Map<String, Object> toJson(TextBlob tb, boolean includeContent)
    {
        Map<String, Object> json =  autoProperties.getFromEntity(tb);

        if (includeContent)
            json.put("contents", tb.getBlobContents());

        return json;
    }

    /**
     * The list of automatically handled properties of a TextBlob
     */
    private final static E2Properties<TextBlob> autoProperties = new E2Properties<>(
        TextBlob.class,
        new E2Property("name"),
        new E2Property("description"),
        (new E2Property("blobID")).readonly(true)
        );
}
