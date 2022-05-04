package com.apixio.model.assembly;

/**
 * Part represents an extracted piece of an assembly.  There is one instance of
 * a Part (subclass) for each extracted piece and these instances are wrappers
 * of the piece.  Each part has a category (type) which is taken from the initial
 * request to separate a piece from the assembly.
 *
 * Each subclass is responsible for the mechanics of serializing and deserializing
 * the wrapped piece.  The subclass is NOT responsible for keeping track of the
 * ID (as it's assigned externally).
 *
 * There are two ways of constructing a new Part instance:  either from an existing
 * POJO or from byte[] (which will be pulled from Cassandra storage).
 */
public class Part<P>
{
    /**
     * Create a Part from a runtime POJO
     */
    public Part(String category, String id, P partObject)
    {
        this.category   = category;
        this.id         = id;
        this.partObject = partObject;
    }

    /**
     * Create a Part from a runtime POJO
     */
    public Part(String category, P partObject)
    {
        this.category   = category;
        this.partObject = partObject;
    }

    /**
     * Get the wrapped object
     */
    public P getPart()
    {
        return partObject;
    }

    /**
     * What category was used to separate this part out from the assembly.
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Return the externally assigned/defined ID
     */
    public String getID()
    {
        return id;
    }

    /**
     *
     */
    private String  category;
    private String  id;
    private P       partObject;
}
