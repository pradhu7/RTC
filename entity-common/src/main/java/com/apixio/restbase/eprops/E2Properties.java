package com.apixio.restbase.eprops;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.restbase.util.DateUtil;
import com.apixio.restbase.util.ReflectionUtil;

/**
 * EntityProperties allows for a more declarative approach to defining entity (e.g., User
 * or Org) properties that are accessible via RESTful calls.  It is intended to bridge between
 * the RESTful level (with its data defined in JSON objects or URLENCODED n=v pairs), the
 * business logic level (with its data interface defined via Data Transfer POJOs), and
 * the actual Redis entity objects (with its data interface defined by getter/setters).
 *
 * One could easily argue:
 *
 *  1.  to keep all data in Map<String, String> from top to bottom
 *  2.  to reduce the number of layers
 *
 * but following #1 leads to non-declarative code and lack of type-checking, etc., while
 * following #2 leads to less reusable code due to no abstraction layers.
 *
 * Yeah, this class could be partially implemented via Spring's
 * BeanUtils, but those appear to require getters/setters and don't handle fields.
 */
public class E2Properties<E> {

    private Class<E> entityClass;
    private E2Property[] properties;
    private Map<String, Integer> propToIndex = new HashMap<String, Integer>();

    /**
     * Not all Java types can be properties.  This lists what can be.  If classes/types are
     * added the code needs to be added (just search for int.class and use as template)
     */
    private static Class<?>[] supportedClasses = new Class<?>[] {
        int.class, long.class, boolean.class, double.class,
        java.lang.String.class, java.lang.Double.class,
        XUUID.class, Date.class, Boolean.class, Integer.class
        };

    /**
     * FieldOrMethod encapsulates the access to a value by supporting both direct object
     * field access or access through a setter/getter.
     */
    static class FieldOrMethod {
        String   propertyName;
        Field    field;
        Method   setter;  // will be null for readonly property
        Method   getter;
        Class<?> type;    // both setter arg and getter return type must be of this type

        FieldOrMethod(String propertyName, Field field)
        {
            this.propertyName = propertyName;
            this.field        = field;
            this.type         = field.getType();

            checkSupportedType(this.type);
        }

        FieldOrMethod(String propertyName, Method getter, Method setter)
        {
            this.propertyName = propertyName;
            this.getter       = getter;
            this.setter       = setter;
            this.type         = getter.getReturnType();

            checkSupportedType(this.type);
        }

        /**
         * Getter/setter for each support class type.
         */
        int getInt(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, int.class, "getInt", "non-int");

            return (field != null) ? field.getInt(o) : ((Integer) getter.invoke(o)).intValue();
        }

        Integer getInteger(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, Integer.class, "getInteger", "non-Integer");

            if(field != null)
                return field.getInt(o);

            Object val = getter != null ? getter.invoke(o) : null;
            return val != null ?  (Integer) val : null;
        }

        long getLong(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, long.class, "getLong", "non-long");

            return (field != null) ? field.getLong(o) : ((Long) getter.invoke(o)).longValue();
        }

        double getDouble(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, double.class, "getDouble", "non-double");

            return (field != null) ? field.getDouble(o) : ((Double) getter.invoke(o)).doubleValue();
        }

        Double getDoubleObj(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, java.lang.Double.class, "getDoubleObj", "non-doubleobj");

            return (Double) ((field != null) ? field.get(o) : getter.invoke(o));
        }

        boolean getBoolean(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, boolean.class, "getBoolean", "non-boolean");

            return (field != null) ? field.getBoolean(o) : ((Boolean) getter.invoke(o)).booleanValue();
        }

        Boolean getBooleanObj(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, java.lang.Boolean.class, "getBooleanObj", "non-booleanobj");

            return (Boolean) ((field != null) ? field.get(o) : getter.invoke(o));
        }

        String getString(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, java.lang.String.class, "getString", "non-String");

            return (field != null) ? ((String) field.get(o)) : ((String) getter.invoke(o));
        }

        String getXuuid(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, XUUID.class, "getXuuid", "non-XUUID");

            XUUID xuuid = (field != null) ? ((XUUID) field.get(o)) : ((XUUID) getter.invoke(o));

            return (xuuid != null) ? xuuid.toString() : null;
        }

        String getDate(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, Date.class, "getDate", "non-Date");

            Date date = (field != null) ? ((Date) field.get(o)) : ((Date) getter.invoke(o));

            return (date != null) ? DateUtil.dateToIso8601(date) : null;
        }

        private void validate(boolean forWrite, Class<?> actual, Class<?> required, String methodName, String msg)
        {
            if (actual != required)
                throw new IllegalStateException("Attempt to " + methodName + " on a " + msg + " type '" + actual + "'");
            else if (forWrite && (field == null) && (setter == null))
                throw new IllegalStateException("Attempt to " + methodName + " on a readonly property '" + propertyName + "'");
        }

        private void checkSupportedType(Class<?> type)
        {
            boolean found = false;

            for (Class<?> clz : supportedClasses)
            {
                if (clz == type)
                {
                    found = true;
                    break;
                }
            }

            if (!found)
                throw new IllegalArgumentException("Class '" + type + "' not supported");
        }
    }

    /**
     * entityAccessors is one-to-one with properties[] and each array element
     * holds the mechanism to get to the property on the actual entity object.  It's
     * expected that these will generally be Methods.
     */
    private FieldOrMethod[] entityAccessors;

    public E2Properties(Class<E> entityClass, E2Property... props)
    {
        this.entityClass = entityClass;
        this.properties  = props;

        setupReflection();
    }

    /**
     * Simpler model to pull properties from an entity.  While the declaration of
     * the returned Map is <String,Object> all values will be String; this just
     * allows the caller to add other simple non-String values (e.g., lists or
     * booleans).
     */
    public Map<String, Object> getFromEntity(E entity)
    {
        Map<String, Object> props = new HashMap<>();

        try
        {
            for (int idx = 0; idx < properties.length; idx++)
            {
                FieldOrMethod entityAccessor = entityAccessors[idx];
                Object        value          = getPropertyAsObject(entity, entityAccessor);

                if (value != null)
                    props.put(properties[idx].getPropertyName(), value);
            }
        }
        catch (Exception ix)
        {
            ix.printStackTrace();
            // todo: maybe this exception shouldn't be swallowed?
        }

        return props;
    }

    /**
     * Creates the entity accessors from the configured property list.  Once
     * this returns the E2Property is usable.
     */
    private void setupReflection()
    {
        // for each property, use reflection to get either Methods or Fields to
        // allow updating, etc.

        entityAccessors = new FieldOrMethod[properties.length];

        for (int idx = 0; idx < properties.length; idx++)
        {
            E2Property ep  = properties[idx];
            boolean        ro  = ep.getReadonly();

            entityAccessors[idx] = getFieldOrMethod(entityClass, ep.getEntityPropertyName(), ro);

            propToIndex.put(ep.getPropertyName(), Integer.valueOf(idx));
        }
    }

    /**
     * Given a class and a property name, create, if possible, the accessor for it.
     */
    private FieldOrMethod getFieldOrMethod(Class<?> clz, String propName, boolean readonly)
    {
        try
        {
            Field   field  = ReflectionUtil.safeGetField(clz, propName);

            // give preference to public fields

            if (field != null)
            {
                return new FieldOrMethod(propName, field);
            }
            else
            {
                Method  getter = ReflectionUtil.safeGetMethod(clz, ReflectionUtil.makeMethodName("get", propName));
                Method  setter = (!readonly) ? ReflectionUtil.safeGetMethod(clz, ReflectionUtil.makeMethodName("set", propName), getter.getReturnType()) : null;

                if ((getter != null) && ((setter != null) || readonly))
                    return new FieldOrMethod(propName, getter, setter);
                else
                    throw new IllegalArgumentException("No field or method '" + propName + "' on class " + clz.getName());
            }
        }
        catch (Exception x)
        {
            throw new IllegalStateException("Failure in getting field or method", x);
        }
    }

    /**
     * Copy a property value from the source to the Map<String, Object>.  Native
     * types are converted to JSON native, if possible.
     */
    private Object getPropertyAsObject(Object srcObj, FieldOrMethod src) throws IllegalAccessException, InvocationTargetException
    {
        if (src.type == int.class)
            return Integer.valueOf(src.getInt(srcObj));
        else if(src.type == Integer.class)
            return srcObj != null ? src.getInteger(srcObj) : null;
        else if (src.type == long.class)
            return Long.valueOf(src.getLong(srcObj));
        else if (src.type == boolean.class)
            return Boolean.valueOf(src.getBoolean(srcObj));
        else if (src.type == Boolean.class)
            return src.getBooleanObj(srcObj);
        else if (src.type == java.lang.String.class)
            return src.getString(srcObj);
        else if (src.type == XUUID.class)
            return src.getXuuid(srcObj);
        else if (src.type == double.class)
            return Double.valueOf(src.getDouble(srcObj));
        else if (src.type == Double.class)
            return src.getDoubleObj(srcObj);
        else if (src.type == Date.class)
            return src.getDate(srcObj);
        else
            throw new IllegalStateException("Attempting to get string of unsupported type");
    }

}
