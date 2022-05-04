package com.apixio.restbase.entity;

import java.util.List;

import com.apixio.ObjectTypes;
import com.apixio.utility.StringList;

/**
 * NAssoc is the base class for any "n-way" association, where the association
 * is represented as a tuple of values.  This class can be extended to allow
 * for extra fields to be stored with the association.
 *
 * These associations are normal objects in that they have an XUUID.
 */
public class NAssoc extends BaseEntity {

    public static final String OBJTYPE = ObjectTypes.ASSOCIATION;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_TYPE          = "type";            // or "namespace" (same thing)
    private final static String F_ELEMENTS      = "elements";

    /**
     * actual fields
     */
    private String       type;
    private List<String> elements;


    /**
     * Create a new association.
     */
    protected NAssoc(String type, List<String> elements)
    {
        super(OBJTYPE);

        this.type     = type;
        this.elements = elements;
    }


    /**
     * For restoring from persisted form only
     */
    public NAssoc(ParamSet fields)
    {
        super(fields);

        this.type     = fields.get(F_TYPE);
        this.elements = StringList.restoreList(fields.get(F_ELEMENTS));
    }

    /**
     * Getters
     */
    public String getType()
    {
        return type;
    }
    public List<String> getElements()
    {
        return elements;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_TYPE,     type);
        fields.put(F_ELEMENTS, StringList.flattenList(elements));
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[assoc: [" + super.toString() + "]" +
                "; type=" + type +
                "; elements=" + elements
            );
    }

}
