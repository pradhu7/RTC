package com.apixio.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Interface to fetch data from a URI.  The set of URI schemes supported is determined by the
 * implementer of this interface (including "easy" ones such as file:///)
 */
public interface UriFetcher
{
    /**
     * Locate the object given the URI and return an InputStream that can be used to read
     * the contents.  If the object can't be found or opened, an exception must be thrown.
     * The caller of this method owns the stream and must close it when done.
     */
    public InputStream fetchUri(URI uri) throws IOException;

}
