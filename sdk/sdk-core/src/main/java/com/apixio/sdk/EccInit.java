package com.apixio.sdk;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.apixio.sdk.protos.EvalProtos.ArgList;
import com.apixio.sdk.protos.FxProtos.FxImpl;
import com.apixio.sdk.util.Util;

/**
 * Captures all data needed to initialize EccSystem
 */
public class EccInit
{
    /**
     * This declares how to form the actual arg values for f(x)impl invocation
     */
    private ArgList evalArgs;

    /**
     * Lists of classnames to dynamically load.  All of these classes must be loadable from
     * the classpath or they must exist in the jars as given in pluginJars
     */
    private List<String> accessorClassnames;        // Class.forName() must return Class<?> that implements Accessor
    private List<String> converterClassnames;       // Class.forName() must return Class<?> that implements Converter
    private String       umCreatorClassname;        // Class.forName() must return Class<?> that implements UmCreator

    /**
     * (Java) language bindings.  Default binding class will be loaded if nothing is specified here
     */
    private List<String> bindingClassnames;

    /**
     * This is a superset of jars that cover the classes above.  This might be empty if the JVM classpath
     * includes jars that contain the above classes.  The type is URI (instead of URL) as we want to be able to
     * specify a jar that's actually in the FxImpl's asset list and in order to specify this (since the final
     * location is not known beforehand) the URI will be "asset://something/assetname" where the "something" is
     * ignored (it's required for URI syntax--it can event be empty, like "asset:///assetname") and the asset
     * name is the full name as declared in the .publish file.
     */
    private List<URI> pluginJars;

    /**
     * Lists the implementation(s) to load.  Multiple impls can be loaded if and only if they all have the same impl.jar
     * URI.  This list must not be empty.
     */
    private List<FxImpl> fxImpls;

    /**
     * Builder pattern for construction
     */
    public static class Builder
    {
        private ArgList      evalArgs;
        private List<String> accessorClassnames;        // Class.forName() must return Class<?> that implements Accessor
        private List<String> converterClassnames;       // Class.forName() must return Class<?> that implements Converter
        private String       umCreatorClassname;        // Class.forName() must return Class<?> that implements UmCreator
        private List<String> bindingClassnames;
        private List<URI>    pluginJars;
        private List<FxImpl> fxImpls;

        public Builder()
        {
        }

        public Builder evalArgs(ArgList argList)
        {
            evalArgs = argList;
            return this;
        }

        public Builder accessors(List<String> classnames)
        {
            accessorClassnames = classnames;
            return this;
        }

        public Builder converters(List<String> classnames)
        {
            converterClassnames = classnames;
            return this;
        }

        public Builder uriManagerCreator(String classname)
        {
            umCreatorClassname = classname;
            return this;
        }

        public Builder bindings(List<String> classnames)
        {
            bindingClassnames = classnames;
            return this;
        }

        /**
         * Assume caller has made sure each URI has scheme, host, and path
         */
        public Builder jars(List<URI> pluginJars)
        {
            this.pluginJars = pluginJars;
            return this;
        }

        /**
         * Validate/convert refs to files and check for scheme, host, and path.  This will also convert frm
         * a local file ref (no file:///) to a valid URI, totally for convenience
         */
        public Builder rawJars(List<String> pluginJars)
        {
            this.pluginJars = (pluginJars != null) ? pluginJars.stream().map(j -> Util.ensureUri(j)).collect(Collectors.toList()) : null;
            return this;
        }

        public Builder fxImpls(List<FxImpl> fxImpls)
        {
            this.fxImpls = fxImpls;
            return this;
        }

        public EccInit build()
        {
            return new EccInit(evalArgs, accessorClassnames, converterClassnames, umCreatorClassname,
                               pluginJars, fxImpls, bindingClassnames);
        }

    }

    /**
     *
     */
    private EccInit(ArgList evalArgs,
                   List<String> accessorClassnames, List<String> converterClassnames, String umCreatorClassname,
                   List<URI> pluginJars, List<FxImpl> fxImpls,
                   List<String> languageBindings
        )
    {
        // notNull("EvalArgs", evalArgs);
        // notNull("FxImpls", fxImpls);

        this.evalArgs = evalArgs;

        // if (fxImpls.size() == 0)
        //     throw new IllegalArgumentException("Empty FxImpl list is not allowed");

        this.fxImpls  = fxImpls;

        this.accessorClassnames  = cleanList(accessorClassnames);
        this.converterClassnames = cleanList(converterClassnames);
        this.umCreatorClassname  = umCreatorClassname;
        this.bindingClassnames   = cleanList(languageBindings);

        this.pluginJars = (pluginJars != null) ? pluginJars : new ArrayList<>();
    }

    /**
     * Getters
     */
    public ArgList getEvalArgs()
    {
        return evalArgs;
    }
    public List<String> getAccessorClassnames()
    {
        return accessorClassnames;
    }
    public List<String> getConverterClassnames()
    {
        return converterClassnames;
    }
    public String getUmCreatorClassname()
    {
        return umCreatorClassname;
    }
    public List<URI> getPluginJars()
    {
        return pluginJars;
    }
    public List<FxImpl> getFxImpls()
    {
        return fxImpls;
    }
    public List<String> getLanguageBindings()
    {
        return bindingClassnames;
    }

    /**
     * implementation
     */
    private static void notNull(String msg, Object o)
    {
        if (o == null)
            throw new IllegalArgumentException("Param " + msg + " can't be null but is");
    }

    private static List<String> cleanList(List<String> ss)
    {
        if (ss == null)
            return new ArrayList<>();
        else
            return ss.stream().map(c -> c.trim()).filter(c -> c.length() > 0).collect(Collectors.toList());
    }
    
}
