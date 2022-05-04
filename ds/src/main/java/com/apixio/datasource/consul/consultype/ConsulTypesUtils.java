package com.apixio.datasource.consul.consultype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.orbitz.consul.model.kv.Value;

import static com.apixio.datasource.consul.consultype.ConsulTypes.MainType;
import static com.apixio.datasource.consul.consultype.ConsulTypes.ScalarType;
import static com.apixio.datasource.consul.consultype.ConsulTypes.TypeAndValue;

public class ConsulTypesUtils
{
    // TODO: Handle escaping for patterns
    private final static String  LIST_SEPARATOR     = ",";
    private final static String  MAP_SEPARATOR      = ",";
    private final static String  MAP_ITEM_SEPARATOR = "=";
    private final static Pattern listPattern        = Pattern.compile(LIST_SEPARATOR);
    private final static Pattern mapPattern         = Pattern.compile(MAP_SEPARATOR);
    private final static Pattern mapItemPattern     = Pattern.compile(MAP_ITEM_SEPARATOR);

    public static Object convertConsulValueToObject(Optional<Value> value)
    {
        return value.map(val ->
        {
            try
            {
                byte     type = (byte) val.getFlags();
                MainType mt   = ConsulTypes.getMainType(type);
                if (mt == null) return null;

                Optional<String> v = val.getValueAsString();

                switch (mt)
                {
                    case ScalarType:
                        return v.isPresent() ? getScalar(v.get(), type) : "";
                    case ListType:
                        return v.isPresent() ? getList(v.get(), type) : new ArrayList<>();
                    case MapType:
                        return v.isPresent() ? getMap(v.get(), type) : new HashMap<>();
                    default:
                        return null;
                }
            }
            catch (Exception e)
            {
                return null;
            }
        }).orElse(null);
    }

    public static TypeAndValue convertObjectToConsulValue(Object object)
    {
        MainType mt = ConsulTypes.getMainType(object);
        if (mt == null) return null;

        switch (mt)
        {
            case ScalarType:
                return convertScalarToString(object);
            case ListType:
                return convertListToString((List<Object>) object);
            default:
                return null;
        }
    }

    public static boolean isSameObjectValue(Object object1, Object object2)
    {
        // null objects not accepted
        if (object1 == null || object2 == null)
            return false;

        MainType mt1 = ConsulTypes.getMainType(object1);
        if (mt1 == null) return false;

        MainType mt2 = ConsulTypes.getMainType(object2);
        if (mt2 == null) return false;

        if (!mt1.equals(mt2)) return false;

        switch (mt1)
        {
            case ScalarType:
                TypeAndValue tv1 = convertScalarToString(object1);
                TypeAndValue tv2 = convertScalarToString(object2);
                return tv1.equals(tv2);
            case ListType:
                TypeAndValue tvo1 = convertListToString((List<Object>) object1);
                TypeAndValue tvo2 = convertListToString((List<Object>) object2);
                return tvo1.equals(tvo2);
            default:
                return false;
        }
    }

    // format: a,b,c,d => [a,b,c,d]
    private static List<Object> getList(String value, byte type)
    {
        return listPattern.splitAsStream(value)
                .map(v -> getScalar(v, type))
                .collect(Collectors.toList());
    }

    // format: A=4,H=X,PO=87 => HashMap{A=4, H=X, PO=87}
    private static Map<String, Object> getMap(String value, byte type)
    {
        return mapPattern.splitAsStream(value)
                .map(s -> mapItemPattern.split(s))
                .collect(Collectors.toMap(
                        a -> a[0].trim(),           //key
                        a -> getScalar(a[1], type)  //value
                ));
    }

    private static Object getScalar(String value, byte type)
    {
        ScalarType st = ConsulTypes.getScalarType(type);
        if (st == null) return null;

        switch (st)
        {
            case StringType:
                return value.trim();
            case IntegerType:
                return Integer.valueOf(value);
            case LongType:
                return Long.valueOf(value);
            case DoubleType:
                return Double.valueOf(value);
            case BooleanType:
                return Boolean.valueOf(value);
            default:
                return null;
        }
    }

    private static TypeAndValue convertScalarToString(Object object)
    {
        if (object instanceof String) return new TypeAndValue(ConsulTypes.ConsulType.ConsulStringType, (String) object);
        else if (object instanceof Integer) return new TypeAndValue(ConsulTypes.ConsulType.ConsularIntegerType, Integer.toString( (Integer) object));
        else if (object instanceof Long) return new TypeAndValue(ConsulTypes.ConsulType.ConsularLongType, Long.toString( (Long) object));
        else if (object instanceof Double) return new TypeAndValue(ConsulTypes.ConsulType.ConsularDoubleType, Double.toString( (Double) object));
        else if (object instanceof Boolean) return new TypeAndValue(ConsulTypes.ConsulType.ConsularBooleanType, Boolean.toString( (Boolean) object));
        else return null;
    }

    private static TypeAndValue convertListToString(List<Object> objects)
    {
        ScalarType st = getListScalarType(objects);
        if (st == null) return null;

        Optional<ConsulTypes.ConsulType> consulType = ConsulTypes.ConsulType.fromScalarFlagMakeListType(st.scalarType);
        if (!consulType.isPresent()) return null;


        return new TypeAndValue(consulType.get(),
                                objects.stream().map(o -> convertScalarToString(o).getValue()).collect(Collectors.joining(LIST_SEPARATOR)));
    }

    private static ScalarType getListScalarType(List<Object> list)
    {
        ScalarType pst = null;

        for (Object element : list)
        {
            if (element == null) return null;

            ScalarType st = ConsulTypes.getScalarType(element);
            if (st == null) return null;

            if (pst == null) pst = st;
            else if (pst != st) return null;
        }

        return pst;
    }
}
