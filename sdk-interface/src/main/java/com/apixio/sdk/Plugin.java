package com.apixio.sdk;

/**
 * A Plugin in a runtime component defined and needed by the SDK system.  Its defining
 * feature is that it has an ID that is used in various ways, depending on the needs
 * of that type of plugin (e.g., the ID of an accessor is used as the name of it within
 * a string that defines how to create the arg values).
 */
public interface Plugin extends FxComponent
{
    /**
     * Within a particular type of plugin, all IDs must be unique.  Generally speaking
     * these IDs should also be just alphanumeric due to some parsing constraints and
     * expectations of usability.
     */
    public String getID();

}
