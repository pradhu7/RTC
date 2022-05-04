package com.apixio.restbase.eprops;

public class E2Property {

    /**
     * The canonical name of the property, which is totally separate from its
     * JSON object field name and separate from the entity (bean) property name.
     */
    private String propertyName;

    /**
     * ReadOnly is true if the property can't be set on the entity.
     */
    private boolean readOnly;

    /**
     * The name that is exposed in the URL; for example, it would be "password" in the URL
     *  http://server/users/me/password
     */
    //    private String urlDetailName;

    /**
     * The name of the bean property (either publicly settable field or setXyz method name)
     * to actually set the value in the persisted entity.
     */
    private String entityPropertyName;

    /**
     *
     */
    public E2Property(String name)
    {
        this.propertyName        = name;
        this.entityPropertyName  = name;
    }

    /**
     * Support method chaining just because it allows for neater initialization.
     */
    public E2Property entityPropertyName(String name)
    {
        this.entityPropertyName = name;
        return this;
    }
    public E2Property readonly(boolean readonly)
    {
        this.readOnly = readonly;
        return this;
    }

    /**
     * Getters
     */
    public String getPropertyName()
    {
        return propertyName;
    }
    public boolean getReadonly()
    {
        return readOnly;
    }
    public String getEntityPropertyName()
    {
        return entityPropertyName;
    }

}
