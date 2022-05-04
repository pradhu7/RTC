package com.apixio.restbase.entity;

import com.apixio.Datanames;
import com.apixio.ObjectTypes;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 *
 *
 * A note on "scope".  Scope really is the (unique) entity type (e.g., Customer, UserOrg, etc.)
 * which is represented as a String because we don't want an enum that "reaches out" beyond this
 * package...
 */
public class CustomProperty extends BaseEntity {

    public static final String OBJTYPE = ObjectTypes.CUSTOM_PROPERTY;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_NAME    = "name";   // trimmed, lowercased
    /**
     * Type is the String form of restbase.PropertyType.  Meta is an optional "{string}", the
     * semantics of which are defined by the client.  Example uses include constraints on the field,
     * such as "immutable" or an allowed range, etc.
     */
    private final static String F_TYPE    = "type";
    private final static String F_META    = "meta";
    /**
     * For "scope" if it's null then that is interpreted to refer to Customer entities as
     * the CustomProperty concept was first introduced for Customers and later generalized
     * to other things.
     */
    private final static String F_SCOPE   = "scope";  // can be null

    /**
     * actual fields
     */
    private String        name;
    private PropertyType  type;
    private String        meta;     // this is appended to type; null means no ":" or meta appended
    private String        scope;

    /**
     * Create a new CustomProperty from the given information
     */
    public CustomProperty(String name, PropertyType type, String scope, String meta)
    {
        super(OBJTYPE);

        if (name == null)  throw new IllegalArgumentException("'name'  must not be null in new CustomProperty");
        if (type == null)  throw new IllegalArgumentException("'type'  must not be null in new CustomProperty");
        if (scope == null) throw new IllegalArgumentException("'scope' must not be null in new CustomProperty");

        if ((meta != null) && (meta.length() == 0))
            meta = null;

        this.name  = normalizeName(name);
        this.type  = type;
        this.meta  = meta;
        this.scope = scope;
    }

    /**
     * For restoring from persisted form only
     */
    public CustomProperty(ParamSet fields)
    {
        super(fields);

        this.type  = PropertyType.valueOf(fields.get(F_TYPE));
        this.meta  = fields.get(F_META);
        this.name  = fields.get(F_NAME);
        this.scope = fields.get(F_SCOPE);

        if (this.scope == null)
            this.scope = Datanames.SCOPE_CUSTOMER;   //!! really gross but is needed to support original use of CustomProperties
    }

    /**
     * Getters
     */
    public String getName()      // not unique right now; need to constrain to be unique in the future
    {
        return name;
    }
    public PropertyType getType()
    {
        return type;
    }
    public String getMeta()
    {
        return meta;
    }
    public String getTypeMeta()
    {
        String types = type.toString();

        return (meta != null) ? (types + ":" + meta) : types;
    }
    public String getScope()
    {
        return scope;
    }

    /**
     * Returns true if the two scopes are equal, where that means that both are null or
     * that they are the same string (case-sensitive).
     */
    public boolean sameScope(String scope2)
    {
        return ((scope == null) && (scope2 == null)) || ((scope != null) && scope.equals(scope2));
    }

    /**
     * No setters because other data are linked via the name.
     */

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_NAME, name);
        fields.put(F_TYPE, type.toString());
        fields.put(F_META, meta);

        if (scope != null)
            fields.put(F_SCOPE, scope);
    }

    /**
     * Canonical name
     */
    public static String normalizeName(String name)
    {
        return name.trim().toLowerCase();
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[CustomProperty: [" + super.toString() + "]" +
                ";name=" + name +
                ";type=" + type +
                ";meta=" + meta +
                ";scope=" + scope +
                "]"
            );
    }

}
