package com.apixio.sdk.util;

import java.io.File;
import java.net.URI;

import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecEnvironment;

public class Util
{

    /**
     *
     */
    public static FxExecEnvironment checkExecEnvironment(FxEnvironment env)
    {
        if (!(env instanceof FxExecEnvironment))
            throw new IllegalStateException("FxEnvironment argument must be of class FxExecEnvironment:  " + env.getClass());

        return (FxExecEnvironment) env;
    }

    /**
     *
     */
    public static URI ensureUri(String pathOrUri)
    {
        System.out.println("##### ensureUri(" + pathOrUri + ")...");

        try
        {
            URI uri = new URI(pathOrUri);

            if (uri.getScheme() == null)
                throw new IllegalArgumentException("pathOrUri must specify a scheme  " + pathOrUri);
            else if (uri.getPath() == null)
                throw new IllegalArgumentException("pathOrUri must specify a path  " + pathOrUri);

            return uri;
        }
        catch (Exception x)
        {
            try
            {
                return (new File(pathOrUri)).toURI();
            }
            catch (Exception xx)
            {
                throw new RuntimeException("Failed to form URI from " + pathOrUri, xx);
            }
        }
    }

}
