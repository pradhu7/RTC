package com.apixio.model.external;

import java.util.*;

public class AxmCodeOrName {

    private final String name;
    private final AxmClinicalCode code;


    public AxmCodeOrName(AxmClinicalCode code) {
        this(null, code);
    }

    public AxmCodeOrName(String name) {
        this(name, null);
    }

    private AxmCodeOrName(String name, AxmClinicalCode code) {
        this.name = name;
        this.code = code;
    }

    /**
     * Only used by Jackson to support 'JsonSerialize.Inclusion.NON_DEFAULT' feature.
     */
    private AxmCodeOrName() {
        this(null, null);
    }

    public boolean isCode() {
        return code != null;
    }

    public boolean isName() {
        return !isCode();
    }

    public AxmClinicalCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, code);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmCodeOrName that = (AxmCodeOrName) obj;
        return Objects.equals(this.name, that.name)
            && Objects.equals(this.code, that.code);
    }

}
