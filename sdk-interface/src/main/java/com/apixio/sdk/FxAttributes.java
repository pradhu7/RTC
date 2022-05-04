package com.apixio.sdk;

import java.util.Map;

/**
 * FxAttributes is a base interface that declares a set of string attributes that are
 * gettable by some aspect of the SDK system.  These attributes are readonly and all
 * are string values for simplicity at this level.
 */
public interface FxAttributes
{
    /**
     * Look up and return a single attribute.  Null is returned if there is no attribute
     * with the given ID.
     */
    public String getAttribute(String id);

    /**
     * Unmodifiable map
     */
    public Map<String,String> getAttributes();
}
