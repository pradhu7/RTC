package com.apixio.sdk.ecc;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.builtin.EnvironmentAccessor;
import com.apixio.sdk.builtin.RequestAccessor;

/**
 * Accessors manages the list of Accessors within the EccSystem
 */
class Accessors
{
    /**
     * Map is from accessor ID (which is a unique and scoped name) to its Accessor
     */
    private Map<String, Accessor> accessors = new HashMap<>();
    
    /**
     * dynJars includes all accessor jars and f(x) implementation jar, in that order.  The
     * dynLoader is the single URLClassLoader that looks in all those jars.
     */
    private FxEnvironment  environment;

    /**
     * Sets up the accessors by loading the Class from its name and initializing.  Also adds the
     * two known/required accessors (environment and request)
     */
    Accessors(FxEnvironment fxEnv, ClassLoader dynLoader, List<String> classnames) throws Exception
    {
        environment = fxEnv;

        for (String classname : classnames)
        {
            Class<Accessor> clz  = (Class<Accessor>) dynLoader.loadClass(classname);

            addAccessor(clz.newInstance());
        }

        addAccessor(new EnvironmentAccessor());
        addAccessor(new RequestAccessor());
    }

    /**
     * 
     */
    public List<Accessor> getAccessors()
    {
        return new ArrayList<>(accessors.values());
    }

    private void addAccessor(Accessor acc)
    {
        String id = acc.getID();

        if (accessors.containsKey(id))
            throw new IllegalStateException("Attempt to add accessor with an ID that's already been registered.  ID: " + id +
                                            ", Accessor=" + acc);

        accessors.put(id, acc);
    }

}
