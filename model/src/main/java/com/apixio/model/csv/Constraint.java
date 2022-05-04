package com.apixio.model.csv;

public class Constraint<T extends Enum<T>> {

    public enum Type {
        REQUIRED,
        IN_ENUM,
        ID,
        ID_WITH_AUTHORITY
    }

    public static final Constraint REQUIRED = new Constraint(Type.REQUIRED);
    public static final Constraint ID = new Constraint(Type.ID);
    public static final Constraint ID_WITH_AUTHORITY = new Constraint(Type.ID_WITH_AUTHORITY);

    public static <T extends Enum<T>> Constraint<T> IN_ENUM(Class<T> enumClass) {
        return new Constraint<>(Type.IN_ENUM, enumClass);
    }

    private final Type type;
    private final Class<T> enumClass;

    private Constraint(Type type) {
        this(type, null);
    }

    private Constraint(Type type, Class<T> enumClass) {
        this.type = type;
        this.enumClass = enumClass;
    }

    public Type getType() {
        return type;
    }

    public Class<T> getEnumType() {
        return enumClass;
    }
}
