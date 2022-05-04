package com.apixio.restbase;

import java.util.HashMap;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.restbase.DataServices;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.dao.CustomProperties;
import com.apixio.restbase.entity.CustomProperty;
import com.apixio.restbase.util.DateUtil;
import com.apixio.restbase.web.BaseException;

/**
 * CustomProperty utility class.  This is to be used by the business logic level
 * classes that want to expose to their clients the ability to add custom
 * properties to the entities they manage.  The use pattern is for the business
 * logic class to create an instance of this class using a unique scope name (which
 * must never change once released), and then for it to expose the desired methods
 * which just delegate to the corresponding methods in this class.
 */
public class PropertyUtil
{
    /**
     * The Custom Property model allows a set of uniquely named properties that are
     * then available to have values associated with those property names added to
     * an entity object.  Custom properties also have a type that limits the types
     * of values that can be added to customer objects.
     *
     * Custom Property definitions have a lifetime independent of entities and their
     * property values.  Actual custom property values on entities are tied to the
     * lifetime of Custom Properties as a deletion of the global Custom Property
     * definition will delete all properties of that type/name from all entities.
     *
     * A Custom Property has a unique name and a type, where the type can include
     * an arbitrary string after the (symbolic) Java type.  This arbitrary string
     * is meant to include finer-grained metadata about the type.
     */

    /**
     * The various types of custom property management failure.
     */
    public enum FailureType {
        /**
         * for modify
         */
        PROPDEF_ERROR,
        CANT_CONVERT_VALUE
    }

    /**
     * If property operations fail they will throw an exception of this class.
     */
    public static class PropertyException extends BaseException {

        public PropertyException(FailureType failureType, String details, Object... args)
        {
            super(failureType);
            super.description(details, args);
        }
    }

    /**
     * Instances need to hook into rest of system via sysServices.  Scope is the uniquely named
     * identifier of the scope/entity type (case-sensitive).
     */
    private DataServices sysServices;
    private String scope;

    public PropertyUtil(String scope, DataServices sysServices)
    {
        this.scope       = scope;
        this.sysServices = sysServices;
    }

    /* ################################################################ */
    /* Methods that deal with meta (definitional) level operations      */
    /* ################################################################ */

    /**
     * Add a new custom property to the global set of properties.  The name must be unique
     * when lowercased.
     */ 
    public void addPropertyDef(String name, PropertyType type)   // throws exception if name.trim.tolowercase is not unique
    {
        addPropertyDef(name, type, null);
    }

    public void addPropertyDef(String name, String typeMeta)   // throws exception if name.trim.tolowercase is not unique
    {
        int    col  = typeMeta.indexOf(':');
        String meta = null;

        if (col != -1)
        {
            if (col + 1 >= typeMeta.length())
                throw new PropertyException(FailureType.PROPDEF_ERROR,
                                            "CustomProperty [{}] is declaring a type-meta with invalid syntax [{}]:  metadata (after :) is empty.",
                                            name, typeMeta);

            meta     = typeMeta.substring(col + 1);
            typeMeta = typeMeta.substring(0, col);
        }

        addPropertyDef(name, PropertyType.valueOf(typeMeta.toUpperCase()), meta);
    }

    public void addPropertyDef(String name, PropertyType type, String meta)   // throws exception if name.trim.tolowercase is not unique
    {
        CustomProperties cp = sysServices.getCustomProperties();

        if (cp.findByName(name, scope) != null)
            throw PropertyException.badRequest("Property already exists {}", name);

        cp.create(new CustomProperty(name, type, scope, meta));
    }

    /**
     * Returns a map from unique property name to the declared PropertyType of that property.
     */
    public Map<String, String> getPropertyDefs(boolean includeMeta) // <propName, typeMeta>
    {
        CustomProperties    cps   = sysServices.getCustomProperties();
        Map<String, String> props = new HashMap<>();

        for (CustomProperty cp : cps.getAllCustomPropertiesByScope(scope))
            props.put(cp.getName(), (includeMeta) ? cp.getTypeMeta() : cp.getType().toString());

        return props;
    }

    /**
     * Removes the custom property definition.  This removal will cascade to a deletion of
     * property values on entity objects.
     */
    public void removePropertyDef(String name)
    {
        CustomProperties cps = sysServices.getCustomProperties();
        CustomProperty   cp  = cps.findByName(name, scope);

        if (cp != null)
        {
            cps.delete(cp);
            cps.deleteProperty(cp);
        }
    }

    /* ################################################################ */
    /* Methods that manage actual custom props on entity instances      */
    /* ################################################################ */

    /**
     * Add a property value to the given entit.  The actual value will be
     * converted--as possible--to the declared property type.
     */
    public void setEntityProperty(XUUID entity, String propertyName, String valueStr) // throws exception if name not known
    {
        CustomProperty cp = sysServices.getCustomProperties().findByName(propertyName, scope);

        if (cp == null)
            throw PropertyException.badRequest("CustomProperty name [{}] not found", propertyName);

        try
        {
            sysServices.getCustomProperties().setProperty(entity, cp, convertProperty(cp, valueStr));
        }
        catch (IllegalArgumentException x)
        {
            throw PropertyException.badRequest("Illegal argument {}", x.getMessage());
        }
        catch (PropertyException x)
        {
            throw PropertyException.badRequest("Fail to convert the input value", x.getDescription());
        }
    }

    /**
     * Remove a custom property value from the given entity.
     */
    public void removeEntityProperty(XUUID entity, String propertyName) // throws exception if name not known
    {
        CustomProperty cp = sysServices.getCustomProperties().findByName(propertyName, scope);

        if (cp == null)
            throw PropertyException.badRequest("CustomProperty name [{}] not found", propertyName);

        sysServices.getCustomProperties().removeProperty(entity, cp);
    }

    /**
     * Given an entity, return a map from property name to the property value for that entity.
     */
    public Map<String, Object> getEntityProperties(XUUID entityID)   // <propname, propvalue>
    {
        return sysServices.getCustomProperties().getAllCustomProperties(scope, entityID);
    }

    /**
     * Given an entity, return a map from property name to the property value for that entity.
     *
     * This method is replicated from method above to optimize getting properties
     * for single project by skipping loading all projects into local cache
     */
    public Map<String, Object> getEntityPropertiesForOneEntity(XUUID entityID)   // <propname, propvalue>
    {
        return sysServices.getCustomProperties().getAllCustomPropertiesForOneEntity(scope, entityID);
    }

    /* ################################################################ */
    /* Methods to query the aggregate entities                          */
    /* ################################################################ */

    /**
     * Returns a map from entit to a map that contains all the name=value pairs for each entity.
     */
    public Map<XUUID, Map<String, Object>> getAllCustomProperties()                 // <EntityID, <propname, propvalue>>
    {
        return sysServices.getCustomProperties().getAllCustomProperties(scope);
    }

    /**
     * Given a property name, return a map from entity to the property value for that entity.
     */
    public Map<XUUID, Object> getCustomProperty(String propName)     // <entityID, propvalue>
    {
        CustomProperty cp = sysServices.getCustomProperties().findByName(propName, scope);

        if (cp == null)
            throw PropertyException.badRequest("CustomProperty name [{}] not found", propName);

        return sysServices.getCustomProperties().getAllEntityProperties(cp);
    }

    /* ################################################################ */
    /* Utility methods                                                  */
    /* ################################################################ */

    /**
     * Converts a String value into the type of the given Property.  If a conversion is not
     * possible/legal an exception is thrown
     */
    private Object convertProperty(CustomProperty cp, String valueStr)
    {
        try
        {
            Object value = null;

            switch (cp.getType())
            {
                case STRING:
                {
                    value = valueStr;
                    break;
                }
                case BOOLEAN:
                {
                    value = Boolean.valueOf(valueStr);
                    break;
                }
                case INTEGER:
                {
                    value = Integer.valueOf(valueStr);
                    break;
                }
                case DOUBLE:
                {
                    value = Double.valueOf(valueStr);
                    break;
                }
                case DATE:
                {
                    value = DateUtil.validateIso8601(valueStr);
                    break;
                }
            }

            return value;
        }
        catch (Exception x)
        {
            throw new PropertyException(FailureType.CANT_CONVERT_VALUE,
                                        "Unable to convert value [{}] to required type of [{}]", valueStr, cp.getType());
        }
    }

}
