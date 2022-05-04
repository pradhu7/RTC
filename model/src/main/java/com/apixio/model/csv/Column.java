package com.apixio.model.csv;

import java.util.Set;
import java.util.HashSet;

public class Column {

    private final ColumnName name;

    private final Set<Constraint> constraints =  new HashSet<>();

    public Column(ColumnName name, Constraint... constraints) {
        this.name = name;
        for (Constraint constraint : constraints) {
            this.constraints.add(constraint);
        }
    }

    public ColumnName getName() {
        return name;
    }

    public boolean containsDate() {
        return name.toString().contains("DATE");
    }

    public boolean isRequired() {
        return constraints.contains(Constraint.REQUIRED);
    }

    public boolean isIdWithOptionalAuthority() {
        return constraints.contains(Constraint.ID);
    }

    public boolean isIdWithRequiredAuthority() {
        return constraints.contains(Constraint.ID_WITH_AUTHORITY);
    }

    public boolean isEnum() {
        return getEnumType() != null;
    }

    public <T extends Enum<T>> Class<T> getEnumType() {
        for (Constraint constraint : constraints) {
            if (constraint.getType() == Constraint.Type.IN_ENUM) {
                return constraint.getEnumType();
            }
        }
        return null;
    }

    public static Column column(ColumnName name, Constraint... constraints) {
        return new Column(name, constraints);
    }

    @Override
    public String toString() {
        return getName().toString();
    }
}
