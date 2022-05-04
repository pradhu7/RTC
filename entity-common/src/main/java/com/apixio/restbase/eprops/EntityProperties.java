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
public class EntityProperties<M, E> {

    private Class<M> modifyClass;
    private Class<E> entityClass;
    private EntityProperty[] properties;
    private Map<String, Integer> propToIndex = new HashMap<String, Integer>();

    /**
     * Not all Java types can be properties.  This lists what can be.  If classes/types are
     * added the code needs to be added (just search for int.class and use as template)
     */
    private static Class<?>[] supportedClasses = new Class<?>[] {
        int.class, long.class, boolean.class, java.lang.String.class, XUUID.class, Date.class
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
        void setInt(Object o, int v) throws IllegalAccessException, InvocationTargetException
        {
            validate(true, type, int.class, "getInt", "non-int");

            if (field != null)
                field.setInt(o, v);
            else
                setter.invoke(o, Integer.valueOf(v));
        }

        long getLong(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, long.class, "getLong", "non-long");

            return (field != null) ? field.getLong(o) : ((Long) getter.invoke(o)).longValue();
        }
        void setLong(Object o, long v) throws IllegalAccessException, InvocationTargetException
        {
            validate(true, type, long.class, "getLong", "non-long");

            if (field != null)
                field.setLong(o, v);
            else
                setter.invoke(o, Long.valueOf(v));
        }

        boolean getBoolean(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, boolean.class, "getBoolean", "non-boolean");

            return (field != null) ? field.getBoolean(o) : ((Boolean) getter.invoke(o)).booleanValue();
        }
        void setBoolean(Object o, boolean v) throws IllegalAccessException, InvocationTargetException
        {
            validate(true, type, boolean.class, "getBoolean", "non-boolean");

            if (field != null)
                field.setBoolean(o, v);
            else
                setter.invoke(o, Boolean.valueOf(v));
        }

        String getString(Object o) throws IllegalAccessException, InvocationTargetException
        {
            validate(false, type, java.lang.String.class, "getString", "non-String");

            return (field != null) ? ((String) field.get(o)) : ((String) getter.invoke(o));
        }
        void setString(Object o, String v) throws IllegalAccessException, InvocationTargetException
        {
            validate(true, type, java.lang.String.class, "getString", "non-String");

            if (field != null)
                field.set(o, v);
            else
                setter.invoke(o, v);
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
     * modifyAccessors is one-to-one with properties[] and each array element
     * holds the mechanism to get to the property on the "modify" DTO.  It's
     * expected that these will generally be Fields.
     */
    private FieldOrMethod[] modifyAccessors;

    /**
     * entityAccessors is one-to-one with properties[] and each array element
     * holds the mechanism to get to the property on the actual entity object.  It's
     * expected that these will generally be Methods.
     */
    private FieldOrMethod[] entityAccessors;

    public EntityProperties(Class<M> modifyClass, Class<E> entityClass, EntityProperty... props)
    {
        this.modifyClass = modifyClass;
        this.entityClass = entityClass;
        this.properties  = props;

        setupReflection();
    }

    /**
     * Called by *RS code to move fields/values from the JSON or URLENCODED input
     * into the Java "modify params" object that's passed to the business logic level.
     */
    public void copyToModifyParams(Map<String, String> requestProps, M modifyParams)
    {
        try
        {
            for (Map.Entry<String, String> entry : requestProps.entrySet())
            {
                String  key = entry.getKey();
                Integer idx = propToIndex.get(key);

                if (idx != null)
                {
                    String        value = entry.getValue();
                    FieldOrMethod fom   = modifyAccessors[idx.intValue()];

                    if (fom.type == int.class)
                        fom.setInt(modifyParams, Integer.parseInt(value));
                    else if (fom.type == long.class)
                        fom.setLong(modifyParams, Long.parseLong(value));
                    else if (fom.type == boolean.class)
                        fom.setBoolean(modifyParams, Boolean.valueOf(value).booleanValue());
                    else if (fom.type == java.lang.String.class)
                        fom.setString(modifyParams, value);
                }
            }
        }
        catch (Exception ix)
        {
            ix.printStackTrace();
        }
    }

    /**
     * Called by business logic code to actually push from the modify
     * params into the actual entity object.
     */
    public void copyToEntity(M modifyParams, E entity)
    {
        try
        {
            for (int idx = 0; idx < properties.length; idx++)
            {
                FieldOrMethod modifyAccessor = modifyAccessors[idx];
                FieldOrMethod entityAccessor = entityAccessors[idx];

                if (!properties[idx].getReadonly())
                    copyProperty(modifyParams, entity, modifyAccessor, entityAccessor);
            }
        }
        catch (Exception ix)
        {
            ix.printStackTrace();
        }
    }

    /**
     * Retrieves prop values from entity and puts them into the modify params POJO.
     */
    public void copyFromEntity(E entity, M modifyParams)
    {
        try
        {
            for (int idx = 0; idx < properties.length; idx++)
            {
                FieldOrMethod modifyAccessor = modifyAccessors[idx];
                FieldOrMethod entityAccessor = entityAccessors[idx];

                copyProperty(entity, modifyParams, entityAccessor, modifyAccessor);
            }
        }
        catch (Exception ix)
        {
            ix.printStackTrace();
        }
    }

    /**
     * Retrieves prop values from entity and puts them directly into request-level
     * properties.
     */
    public void copyFromEntity(E entity, Map<String, String> requestProps)
    {
        try
        {
            for (int idx = 0; idx < properties.length; idx++)
            {
                FieldOrMethod entityAccessor = entityAccessors[idx];
                String        value          = getPropertyAsString(entity, entityAccessor);

                if (value != null)
                    requestProps.put(properties[idx].getPropertyName(), value);
            }
        }
        catch (Exception ix)
        {
            ix.printStackTrace();
        }
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
        }

        return props;
    }

    /**
     * Retrieves the single prop value from entity and puts it directly into request-level
     * properties.
     */
    public void copyFromEntity(E entity, Map<String, String> requestProps, String propName)
    {
        try
        {
            Integer iidx = propToIndex.get(propName);

            if (iidx != null)
            {
                int           idx            = iidx.intValue();
                FieldOrMethod entityAccessor = entityAccessors[idx];
                String        value          = getPropertyAsString(entity, entityAccessor);

                if (value != null)
                    requestProps.put(properties[idx].getPropertyName(), value);
            }
        }
        catch (Exception ix)
        {
            ix.printStackTrace();
        }
    }

    /**
     * Creates the modify and entity accessors from the configured property list.  Once
     * this returns the EntityProperty is usable.
     */
    private void setupReflection()
    {
        // for each property, use reflection to get either Methods or Fields to
        // allow updating, etc.

        modifyAccessors = new FieldOrMethod[properties.length];
        entityAccessors = new FieldOrMethod[properties.length];

        for (int idx = 0; idx < properties.length; idx++)
        {
            EntityProperty ep  = properties[idx];
            boolean        ro  = ep.getReadonly();

            modifyAccessors[idx] = getFieldOrMethod(modifyClass, ep.getModifyPropertyName(), ro);
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
     * Copy a property value from the source to the destination using the required accessor.
     */
    private void copyProperty(Object srcObj, Object dstObj, FieldOrMethod src, FieldOrMethod dst) throws IllegalAccessException, InvocationTargetException
    {
        if (dst.type == int.class)
            dst.setInt(dstObj, src.getInt(srcObj));
        else if (dst.type == long.class)
            dst.setLong(dstObj, src.getLong(srcObj));
        else if (dst.type == boolean.class)
            dst.setBoolean(dstObj, src.getBoolean(srcObj));
        else if (dst.type == java.lang.String.class)
            dst.setString(dstObj, src.getString(srcObj));
    }

    /**
     * Copy a property value from the source to the Map<String, String>
     */
    private String getPropertyAsString(Object srcObj, FieldOrMethod src) throws IllegalAccessException, InvocationTargetException
    {
        if (src.type == int.class)
            return Integer.toString(src.getInt(srcObj));
        else if (src.type == long.class)
            return Long.toString(src.getLong(srcObj));
        else if (src.type == boolean.class)
            return Boolean.toString(src.getBoolean(srcObj));
        else if (src.type == java.lang.String.class)
            return src.getString(srcObj);
        else if (src.type == XUUID.class)
            return src.getXuuid(srcObj);
        else if (src.type == Date.class)
            return src.getDate(srcObj);
        else
            throw new IllegalStateException("Attempting to get string of unsupported type");
    }

    /**
     * Copy a property value from the source to the Map<String, Object>.  Native
     * types are converted to JSON native, if possible.
     */
    private Object getPropertyAsObject(Object srcObj, FieldOrMethod src) throws IllegalAccessException, InvocationTargetException
    {
        if (src.type == int.class)
            return Integer.valueOf(src.getInt(srcObj));
        else if (src.type == long.class)
            return Long.valueOf(src.getLong(srcObj));
        else if (src.type == boolean.class)
            return Boolean.valueOf(src.getBoolean(srcObj));
        else if (src.type == java.lang.String.class)
            return src.getString(srcObj);
        else if (src.type == XUUID.class)
            return src.getXuuid(srcObj);
        else
            throw new IllegalStateException("Attempting to get string of unsupported type");
    }

    // ################################################################ test

    /*
    public static class TestEntity {
        private int myInt;
        private long myLong;
        private boolean myBoolean;
        private String myString;

        public TestEntity()
        {
        }

        public void setMyInt(int i)
        {
            myInt = i;
        }
        public int getMyInt()
        {
            return myInt;
        }
        public void setMyLong(long l)
        {
            myLong = l;
        }
        public long getMyLong()
        {
            return myLong;
        }
        public void setMyBoolean(boolean b)
        {
            myBoolean = b;
        }
        public boolean getMyBoolean()
        {
            return myBoolean;
        }
        public void setMyString(String s)
        {
            myString = s;
        }
        public String getMyString()
        {
            return myString;
        }

        public String toString()
        {
            return "TestEntity(" + myInt + ", " + myLong + "L, " + myBoolean + ", \"" + myString + "\")";
        }
    }

    public static class TestModify {
        public int myInt;
        public long myLong;
        public boolean myBoolean;
        public String myString;

        public String toString()
        {
            return "TestModify(" + myInt + ", " + myLong + "L, " + myBoolean + ", \"" + myString + "\")";
        }
    }

    private static EntityProperties<TestModify, TestEntity> testProps = new EntityProperties<TestModify, TestEntity>(
        TestModify.class, TestEntity.class,
        new EntityProperty("myInt"),
        new EntityProperty("myLong"),
        new EntityProperty("myBoolean"),
        new EntityProperty("myString")
        );

    public static void main(String[] args)
    {
        TestModify           tm = new TestModify();
        TestEntity           te = new TestEntity();
        Map<String, String>  gp = new HashMap<String, String>();

        gp.put("myInt",     "22");
        gp.put("myLong",    "-1");
        gp.put("myBoolean", "true");
        gp.put("myString",  "hello");

        testProps.copyToModifyParams(gp, tm);

        System.out.println("TestModify:  " + tm);
        System.out.println("TestEntity:  " + te);

        testProps.copyToEntity(tm, te);
        System.out.println("TestEntity:  " + te);
    }
    */

}
