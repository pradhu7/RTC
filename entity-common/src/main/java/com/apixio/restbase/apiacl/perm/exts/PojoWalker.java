package com.apixio.restbase.apiacl.perm.exts;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.apixio.restbase.util.ReflectionUtil;

/**
 * This class will extract a single field value from a possibly (chained set of)
 * Java object(s).  The extraction supports both public fields and "getter" methods
 * to retrieve the field.  Additionally, java.util.Map objects can be used as the
 * source of data.
 *
 * The specification of the path to the field is a dotted path String, like:
 *
 *   obj.field1.field2
 *
 * where "obj" MUST have a field named "field1" (where "have" means either a public field
 * with that exact name or a public method with the name getField1().  The field value
 * MUST be an object that has a field/getter "field2".  If the terminal field is a
 * scalar (int/log, boolean, float/double) then the boxed object is returned, otherwise
 * field value as an object is returned.
 *
 */
public class PojoWalker {

    private static Map<Class<?>, Map<String, FieldOrMethod>> reflectionCache = new HashMap<Class<?>, Map<String, FieldOrMethod>>();

    /**
     * Attempts to retrieve the field value defined by the dotted path from the
     * (chain of) object(s).  As the dotted path is assumed to match the actual
     * structure of the object (tree), any mismatch will cause an error.  In order
     * to try to be useful, the details of the exception thrown in an error case
     * will identify the underlying cause.  All errors have the same base
     * exception--only the message will differ, as it's assumed that errors will
     * be logged and read by a human.  Errors:
     *
     *   * null "obj" argument
     *   * null/empty path
     *   * bad path syntax
     *   * obj chain had interior null
     *   * no such field on object
     */
    public static Object getObjectField(Object obj, String dottedPath)
    {
        if (obj == null)
            throw new IllegalArgumentException("Root object is null");
        else if ((dottedPath == null) || ((dottedPath = dottedPath.trim()).length() == 0))
            throw new IllegalArgumentException("Dotted path is null or empty");

        String origDottedPath = dottedPath;

        while (true)
        {
            int     dot   = dottedPath.indexOf('.');
            boolean isMap = (obj instanceof Map);

            if (dot == -1)
            {
                try
                {
                    if (isMap)
                        obj = ((Map) obj).get(dottedPath);
                    else
                        obj = getFieldValue(obj, dottedPath);
                }
                catch (Exception x)
                {
                    throw new IllegalArgumentException("Failed to get field \"" + dottedPath + "\" in dottedPath \"" + origDottedPath + "\"");
                }

                break;
            }
            else if ((dot > 0) && (dot + 1 < dottedPath.length()))
            {
                String ele = dottedPath.substring(0, dot);

                try
                {
                    if (isMap)
                        obj = ((Map) obj).get(ele);
                    else
                        obj = getFieldValue(obj, ele);
                }
                catch (Exception x)
                {
                    throw new IllegalArgumentException("Failed to get field \"" + ele + "\" in dottedPath \"" + origDottedPath + "\"");
                }

                if (obj == null)
                    throw new IllegalArgumentException("Object chain had interior null field value.  dottedPath=\"" + origDottedPath +
                                                           "\"; null field value at \"" + dottedPath + "\"");

                dottedPath = dottedPath.substring(dot + 1);
            }
            else
            {
                throw new IllegalArgumentException("Invalid dotted path:  leading '.' or final '.' or '..' encountered:  " + origDottedPath);
            }
        }

        return obj;
    }

    /**
     * Gets the given logical field value from the object. Does NOT descend into
     * subobjects!
     */
    private static Object getFieldValue(Object obj, String field) throws IllegalAccessException, InvocationTargetException
    {
        FieldOrMethod fom = getCachedFieldOrMethod(obj.getClass(), field);

        if (fom != null)
            return fom.get(obj);
        else
            throw new IllegalAccessException("No such field " + field);
    }

    /**
     * Gets the cached FieldOrMethod based on class/fieldname, retrieving it via reflection if
     * it's not in the cache.  An exception is thrown (from reflection code) if the field
     * and getter method don't exist.
     */
    private static FieldOrMethod getCachedFieldOrMethod(Class<?> clazz, String field)
    {
        FieldOrMethod fom = null;

        synchronized (reflectionCache)
        {
            Map<String, FieldOrMethod> methodCache = reflectionCache.get(clazz);

            if (methodCache == null)
            {
                methodCache = new HashMap<String, FieldOrMethod>();
                reflectionCache.put(clazz, methodCache);
            }

            fom = methodCache.get(field);
        }

        if (fom == null)
        {
            fom = getFieldOrMethod(clazz, field);

            if (fom != null)
            {
                synchronized (reflectionCache)
                {
                    reflectionCache.get(clazz).put(field, fom);
                }
            }
        }

        return fom;
    }

    /**
     * Performs Java reflection to look up the Field or Method that can be used to retrieve
     * the value denoted by the fieldName.  Preference is given to a public Field over a
     * getter method.
     */
    private static FieldOrMethod getFieldOrMethod(Class<?> clazz, String fieldName)
    {
        try
        {
            Field field = ReflectionUtil.safeGetField(clazz, fieldName);

            // give preference to public fields

            if (field != null)
            {
                return new FieldOrMethod(clazz, field);
            }
            else
            {
                Method  getter = ReflectionUtil.safeGetMethod(clazz, ReflectionUtil.makeMethodName("get", fieldName));

                if (getter != null)
                    return new FieldOrMethod(clazz, getter);
                else
                    throw new IllegalArgumentException("No field or method '" + fieldName + "' on class " + clazz.getName());
            }
        }
        catch (Exception x)
        {
            throw new IllegalStateException("Failure in getting field or method", x);
        }
    }

    /**
     * Small class to encapsulate the actual runtime reflection-based retrieval of a logical field
     * from an object.
     */
    private static class FieldOrMethod {
        Field     field;
        Method    getter;
        Class<?>  type;

        FieldOrMethod(Class<?> clz, Field field)
        {
            this.field = field;
            this.type  = clz;
        }

        FieldOrMethod(Class<?> clz, Method getter)
        {
            this.getter = getter;
            this.type   = clz;
        }

        Object get(Object o) throws IllegalAccessException, InvocationTargetException
        {
            if (o == null)
                return null;
            else if (o.getClass() != type)
                throw new IllegalArgumentException("Attempt to get field value from unexpected type of object.  " +
                                                   "This FieldOrMethod set up against class " + type + " and object " +
                                                   "passed to get() is of class " + o.getClass());

            return (field != null) ? field.get(o) : getter.invoke(o);
        }
    }

    /* test code
    private static class TestA {
        public int anInteger;
        private TestB theB;

        public TestB getB() { return theB; }

        TestA(int x)   { anInteger = x; }
        TestA(TestB b) { theB = b;      }
    }

    private static class TestB {
        public String foober;

        TestB(String v) { foober = v; }
    }

    public static void main(String[] args) throws Exception
    {
        TestB b = new TestB("yellow");
        TestA a = new TestA(2);
        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> map2 = new HashMap<String, Object>();
        Map<String, Object> map3 = new HashMap<String, Object>();

        map1.put("m2", map2);
        map2.put("m3", map3);
        map3.put("a", a);
        map3.put("b", b);

        for (String p : args)
            System.out.println(getObjectField(map1, p));
    }
    */

}
