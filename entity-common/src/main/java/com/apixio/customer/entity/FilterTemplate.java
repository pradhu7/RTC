package com.apixio.customer.entity;

import com.apixio.ObjectTypes;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 *
 */
public class FilterTemplate extends NamedEntity {

    public static final String OBJTYPE = ObjectTypes.FILTER_TEMPLATE;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_XML     = "xml";

    /**
     * actual fields
     */
    private String xml;

    /**
     * Construct a new FilterTemplate
     */
    public FilterTemplate(String name, String xml)
    {
        super(OBJTYPE, name);

//        if ((xml == null) || (xml.trim().length() == 0))
//            throw new IllegalArgumentException("XML of a filter template can't be null/empty");
        if (xml == null)
            throw new IllegalArgumentException("XML of a filter template can't be null");

        this.xml = xml;
    }

    /**
     * Allow construction of a new FilterTemplate from a subclass
     */
    protected FilterTemplate(String objType, String name, String xml)
    {
        super(objType, name);

//        if ((xml == null) || (xml.trim().length() == 0))
//            throw new IllegalArgumentException("XML of a filter template can't be null/empty");
        if (xml == null)
            throw new IllegalArgumentException("XML of a filter template can't be null");

        this.xml = xml;
    }

    /**
     * For restoring from persisted form only
     */
    public FilterTemplate(ParamSet fields)
    {
        super(fields);

        this.xml = fields.get(F_XML);
    }

    /**
     * Getters
     */
    public String getXml()
    {
        return xml;
    }

    /**
     * Setters
     */
    public void setXml(String xml)
    {
        // assumed to have {name} placeholders
        this.xml = xml;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_XML, xml);
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[FilterTemplate: [" + super.toString() + "]" +
                ";xml=" + xml +
                "]"
            );
    }

}

