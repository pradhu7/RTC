package com.apixio.bms;

/**
 * Easy & quick definition of one bit of metadata for a blob.  This definition
 * consists of type (string, boolean, etc.), field name, and a flag that indicates
 * if a value is required for a blob.
 *
 * Use model for this is to use a factory method to create a new def of a
 * particular type and then mark it as optional or required (default is for it to
 * be optional).
 *
 * MetadataDefs can be grouped into distinct groups by assigning the group name to
 * each def.  This allows higher levels to apply different semantics/behavior to
 * each group.  The default group name is null, meaning no explicit group.
 */
public class MetadataDef
{

    /**
     * Supported types.  This is overkill currently but the enum is filled out
     * to show the pattern/possibilities
     */
    public enum Type {
        STRING  (String.class),
        BOOLEAN (Boolean.class),
        DOUBLE  (Double.class),
        INTEGER (Integer.class);

        public Class<?> getTypeClass() { return typeClass;     }

        Type(Class<?> clz)             { this.typeClass = clz; }

        private Class<?> typeClass;
    }

    /**
     *
     */
    private String  group;
    private String  keyName;
    private Type    valueType;
    private Boolean required;    // null => not specified

    /**
     * Factory methods to create the given type of MetadataDef
     */
    public static MetadataDef stringAttribute(String keyName)
    {
        return checkAndCreate(keyName, Type.STRING);
    }

    public static MetadataDef integerAttribute(String keyName)
    {
        return checkAndCreate(keyName, Type.INTEGER);
    }

    public static MetadataDef doubleAttribute(String keyName)
    {
        return checkAndCreate(keyName, Type.DOUBLE);
    }

    public static MetadataDef booleanAttribute(String keyName)
    {
        return checkAndCreate(keyName, Type.BOOLEAN);
    }

    private static MetadataDef checkAndCreate(String keyName, Type type)
    {
        if ((keyName == null) || ((keyName = keyName.trim()).length() == 0))
            throw new IllegalArgumentException("MetadataDef keyname can't be empty:  " + keyName);

        MetadataDef mdd = new MetadataDef();

        mdd.keyName   = keyName;
        mdd.valueType = type;

        return mdd;
    }

    /**
     *
     */
    public MetadataDef group(String group)
    {
        this.group = group;

        return this;
    }

    /**
     * Mark it optional or required
     */
    public MetadataDef required()
    {
        if (required != null)
            throw new IllegalStateException("Already specified");

        required = Boolean.TRUE;

        return this;
    }

    public MetadataDef optional()
    {
        if (required != null)
            throw new IllegalStateException("Already specified");

        required = Boolean.FALSE;

        return this;
    }

    /**
     * Getters
     */
    public String getGroup()
    {
        return group;
    }

    public Type getType()
    {
        return valueType;
    }

    public String getKeyName()
    {
        return keyName;
    }

    public boolean isRequired()
    {
        // the default is for the value to be optional

        return Boolean.TRUE.equals(required);
    }

    /**
     * Validates that the given value conforms to the declared type and throws an exception
     * if it doesn't.  This includes a test for 'required'
     */
    public void checkValue(Object val)
    {
        if (val == null)
        {
            if (isRequired())
                throw new IllegalStateException("Metadata field for '" + keyName + "' is required by schema but not present");
        }
        else if (!valueType.getTypeClass().isAssignableFrom(val.getClass()))
        {
            throw new IllegalArgumentException("Metadata field '" + keyName +
                                               "' is required to be of type " + valueType + " but actual value [" + val + "] is of type " +
                                               val.getClass().getName());
        }
    }

    @Override
    public String toString()
    {
        return "(MetadataDef group=" + group + "; key=" + keyName + "; type=" + valueType + "; req=" + required + ")";
    }
}
