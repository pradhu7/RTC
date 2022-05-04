package com.apixio.datasource.consul.consultype;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Consul kv client only supports very simple kv type: Key as string and value as a String. Consul types is our attempt
 * to extend the type system supported by Consul by using the flags associated with a key value pair to indicate the
 * type of the value. So, we have made some assumptions what those flags indicate.
 * STRING_TYPE       = 1
 * INT_TYPE          = 2
 * LONG_TYPE         = 3
 * DOUBLE_TYPE       = 4
 * BOOLEAN_TYPE      = 5
 * LIST_STRING_TYPE  = 11
 * LIST_INT_TYPE     = 12
 * LIST_LONG_TYPE    = 13
 * LIST_DOUBLE_TYPE  = 14
 * LIST_BOOLEAN_TYPE = 15
 * MAP_STRING_TYPE   = 21
 * MAP_INT_TYPE      = 22
 * MAP_LONG_TYPE     = 23
 * MAP_DOUBLE_TYPE   = 24
 * MAP_BOOLEAN_TYPE  = 25
 *
 * The list is represented as string such as: a,b,c,d
 * The map is represented as a string such as: A=4,H=X,PO=87
 */
public class ConsulTypes
{
    final static byte STRING_FLAG       = 1;
    final static byte INTEGER_FLAG      = 2;
    final static byte LONG_FLAG         = 3;
    final static byte DOUBLE_FLAG       = 4;
    final static byte BOOLEAN_FLAG      = 5;

    final static byte LIST_INCREMENT    = 10;
    final static byte LIST_STRING_FLAG  = LIST_INCREMENT + STRING_FLAG;
    final static byte LIST_INTEGER_FLAG = LIST_INCREMENT + INTEGER_FLAG;
    final static byte LIST_LONG_FLAG    = LIST_INCREMENT + LONG_FLAG;
    final static byte LIST_DOUBLE_FLAG  = LIST_INCREMENT + DOUBLE_FLAG;
    final static byte LIST_BOOLEAN_FLAG = LIST_INCREMENT + BOOLEAN_FLAG;

    final static byte MAP_INCREMENT     = 20;
    final static byte MAP_STRING_FLAG   = MAP_INCREMENT + STRING_FLAG;
    final static byte MAP_INTEGER_FLAG  = MAP_INCREMENT + INTEGER_FLAG;
    final static byte MAP_LONG_FLAG     = MAP_INCREMENT + LONG_FLAG;
    final static byte MAP_DOUBLE_FLAG   = MAP_INCREMENT + DOUBLE_FLAG;
    final static byte MAP_BOOLEAN_FLAG  = MAP_INCREMENT + BOOLEAN_FLAG;

    public enum ConsulType
    {
        ConsulStringType(STRING_FLAG, "STRING_FLAG"),
        ConsularIntegerType(INTEGER_FLAG, "INTEGER_FLAG"),
        ConsularLongType(LONG_FLAG, "LONG_FLAG"),
        ConsularDoubleType(DOUBLE_FLAG, "DOUBLE_FLAG"),
        ConsularBooleanType(BOOLEAN_FLAG, "BOOLEAN_FLAG"),

        ConsularListStringType(LIST_STRING_FLAG, "LIST_STRING_FLAG"),
        ConsularListIntegerType(LIST_INTEGER_FLAG, "LIST_INTEGER_FLAG"),
        ConsularListLongType(LIST_LONG_FLAG, "LIST_LONG_FLAG"),
        ConsularListDoubleType(LIST_DOUBLE_FLAG, "LIST_DOUBLE_FLAG"),
        ConsularListBooleanType(LIST_BOOLEAN_FLAG, "LIST_BOOLEAN_FLAG"),

        ConsularMapStringType(MAP_STRING_FLAG, "MAP_STRING_FLAG"),
        ConsularMapIntegerType(MAP_INTEGER_FLAG, "MAP_INTEGER_FLAG"),
        ConsularMapLongType(MAP_LONG_FLAG, "MAP_LONG_FLAG"),
        ConsularMapDoubleType(MAP_DOUBLE_FLAG, "MAP_DOUBLE_FLAG"),
        ConsularMapBooleanType(MAP_BOOLEAN_FLAG, "MAP_BOOLEAN_FLAG");

        ConsulType(byte flag, String description)
        {
            this.flag        = flag;
            this.description = description;
        }

        public byte getFlag()
        {
            return flag;
        }

        public String getDescription()
        {
            return description;
        }

        private byte   flag;
        private String description;

        public static Optional<ConsulType> fromFlag(byte flag)
        {
            return Arrays.stream(values()).filter(v -> v.getFlag() == flag).findFirst();
        }

        public static Optional<ConsulType> fromScalarFlagMakeListType(byte scalarFlag)
        {
            byte listFlag = (byte) (LIST_INCREMENT + scalarFlag);
            return Arrays.stream(values()).filter(v -> v.getFlag() == listFlag).findFirst();
        }

        public static Optional<ConsulType> fromScalarFlagMakeMapType(byte scalarFlag)
        {
            byte mapFlag = (byte) (MAP_INCREMENT + scalarFlag);
            return Arrays.stream(values()).filter(v -> v.getFlag() == mapFlag).findFirst();
        }

        public static Optional<ConsulType> fromDescription(String description)
        {
            return Arrays.stream(values()).filter(v -> v.getDescription().equalsIgnoreCase(description)).findFirst();
        }

        @Override
        public String toString()
        {
            return "ConsulType{" +
                    "name=" + name() +
                    ", flag=" + flag +
                    ", description=" + description +
                    "}";
        }
    }

    public static class TypeAndValue
    {
        public TypeAndValue(ConsulType type, String value)
        {
            this.consulType = type;
            this.value      = value;
        }

        public ConsulType getConsulType()
        {
            return consulType;
        }

        public String getValue()
        {
            return value;
        }

        private ConsulType consulType;
        private String     value;

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;

            if (other == null) return false;

            if (getClass() != other.getClass()) return false;

            TypeAndValue tv = (TypeAndValue) other;

            return (consulType == tv.consulType ||  value.equals(value));
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(consulType, value);
        }

        @Override
        public String toString()
        {
            return "TypeAndValue{" +
                    "consulType=" + consulType +
                    ", value=" + value +
                    "}";
        }
    }

    public enum MainType
    {
        ScalarType, ListType, MapType;
    }

    public enum ScalarType
    {
        StringType(STRING_FLAG),
        IntegerType(INTEGER_FLAG),
        LongType(LONG_FLAG),
        DoubleType(DOUBLE_FLAG),
        BooleanType(BOOLEAN_FLAG);

        ScalarType(byte st)
        {
            scalarType = st;
        }

        public byte getScalarType()
        {
            return scalarType;
        }

        byte scalarType;
    }

    public static MainType getMainType(byte type)
    {
        switch (type)
        {
            case STRING_FLAG:
            case INTEGER_FLAG:
            case LONG_FLAG:
            case DOUBLE_FLAG:
            case BOOLEAN_FLAG:
                return MainType.ScalarType;

            case LIST_STRING_FLAG:
            case LIST_INTEGER_FLAG:
            case LIST_LONG_FLAG:
            case LIST_DOUBLE_FLAG:
            case LIST_BOOLEAN_FLAG:
                return MainType.ListType;

            case MAP_STRING_FLAG:
            case MAP_INTEGER_FLAG:
            case MAP_LONG_FLAG:
            case MAP_DOUBLE_FLAG:
            case MAP_BOOLEAN_FLAG:
                return MainType.MapType;

            default:
                return null;
        }
    }

    public static MainType getMainType(Object object)
    {
        if (getScalarType(object) != null) return MainType.ScalarType;
        else if (object instanceof List) return MainType.ListType;
        else return null;
    }

    public static ScalarType getScalarType(byte type)
    {
        switch (type)
        {
            case STRING_FLAG:
            case LIST_STRING_FLAG:
            case MAP_STRING_FLAG:
                return ScalarType.StringType;

            case INTEGER_FLAG:
            case LIST_INTEGER_FLAG:
            case MAP_INTEGER_FLAG:
                return ScalarType.IntegerType;

            case LONG_FLAG:
            case LIST_LONG_FLAG:
            case MAP_LONG_FLAG:
                return ScalarType.LongType;

            case DOUBLE_FLAG:
            case LIST_DOUBLE_FLAG:
            case MAP_DOUBLE_FLAG:
                return ScalarType.DoubleType;

            case BOOLEAN_FLAG:
            case LIST_BOOLEAN_FLAG:
            case MAP_BOOLEAN_FLAG:
                return ScalarType.BooleanType;

            default:
                return null;
        }
    }

    public static ScalarType getScalarType(Object o)
    {
        if (o instanceof String)        return ScalarType.StringType;
        else if (o instanceof Integer)  return ScalarType.IntegerType;
        else if (o instanceof Long)     return ScalarType.LongType;
        else if (o instanceof Double)   return ScalarType.DoubleType;
        else if (o instanceof Boolean)  return ScalarType.BooleanType;
        else return null;
    }
}
