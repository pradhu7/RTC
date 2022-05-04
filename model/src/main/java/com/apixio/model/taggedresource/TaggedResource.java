package com.apixio.model.taggedresource;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by vvyas on 3/28/18.
 */
public interface TaggedResource {
    String getName();
    String getId();
    long getTimestamp();
    Map<String,String> getTags();
    InputStream getResource();

    void setName(String name);
    void setTags(Map<String,String> tags);
}
