package com.apixio.sdk;

import java.util.List;

/**
 * A language binding maps from the symbolic type names to language-specific names.  Each typename
 * is defined by some f(x) definition and the type-specific language binding is defined as part
 * of the overall "base code" for that f(x) so these bindings "live" in the fxdef module.  Because
 * we can't have cross-module circular dependencies, the sdk core code will dynamically load a
 * class that implements this interface.  This class will be in the fxdef module and the
 * classname to use to set bindings can be overridden by the ECC.
 *
 * There can be more than one class used during bootup that implements this interface.
 */ 
public interface LanguageBinding
{

    /**
     * The actual class of this must reside outside SDK core code
     */
    public final static String DEFAULT_BINDING_CLASSNAME = "com.apixio.binding.DefaultJavaBindings";

    /**
     * Binding is just a mapping from symbolic type name to language-specific name.  Note that this
     * might include in the future multiple "target" names, which is why getJavaBindings returns
     * a list (vs a Map from symbolic name to Java classname).
     */
    public static class Binding
    {
        private String typename;
        private String javaClassname;

        public String getTypename()
        {
            return typename;
        }

        public String getJavaClassname()
        {
            return javaClassname;
        }

        public Binding(String typename, String javaClassname)
        {
            this.typename      = typename;
            this.javaClassname = javaClassname;
        }
    }

    /**
     * Get bindings for JVM-based languages
     */
    public List<Binding> getJavaBindings();

}
