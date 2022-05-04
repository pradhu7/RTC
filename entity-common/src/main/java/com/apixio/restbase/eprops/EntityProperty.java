package com.apixio.restbase.eprops;

public class EntityProperty {

    /**
     * The canonical name of the property, which is totally separate from its
     * JSON object field name and separate from the modify params POJO field
     * name and separate from the entity (bean) property name.
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
     * The name of the field in the incoming JSON object and the x-www-form-urlencoded
     * body.  Currently those two field names MUST be identical.
     */
    private String jsonObjectFieldName;

    /**
     * The name of the field in the Modify Java class that collects all the modifications
     */
    private String modifyPropertyName;

    /**
     * The name of the bean property (either publicly settable field or setXyz method name)
     * to actually set the value in the persisted entity.
     */
    private String entityPropertyName;

    // NOTE:  normally urlDetailName == jsonObjectFieldName, and ideally
    //  jsonObjectFieldName == modifyPropertyName.  We're getting really good if
    //  those all equal entityPropertyName

    /**
     *
     */
    public EntityProperty(String name)
    {
        this.propertyName        = name;

        //        this.urlDetailName       = name;
        this.jsonObjectFieldName = name;
        this.modifyPropertyName  = name;
        this.entityPropertyName  = name;
    }

    /**
     * Support method chaining just because it allows for neater initialization.
     */
    public EntityProperty jsonFieldName(String name)
    {
        this.jsonObjectFieldName = name;
        return this;
    }
    public EntityProperty modifyPropertyName(String name)
    {
        this.modifyPropertyName = name;
        return this;
    }
    public EntityProperty entityPropertyName(String name)
    {
        this.entityPropertyName = name;
        return this;
    }
    public EntityProperty readonly(boolean readonly)
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
    /*
    public String getUrlDetailName()
    {
        return urlDetailName;
    }
    */
    public String getJsonObjectFieldName()
    {
        return jsonObjectFieldName;
    }
    public String getModifyPropertyName()
    {
        return modifyPropertyName;
    }
    public String getEntityPropertyName()
    {
        return entityPropertyName;
    }

}
