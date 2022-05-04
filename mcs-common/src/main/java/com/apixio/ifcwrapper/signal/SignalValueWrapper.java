package com.apixio.ifcwrapper.signal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SignalValueWrapper {

    @JsonIgnore
    public Object unwrap() {
        if (isNumber()) {
            return floatVal();
        } else {
            return stringVal();
        }
    }

    @JsonIgnore
    public abstract Boolean isNumber();

    @JsonIgnore
    @Nullable
    public abstract String stringVal();

    @JsonIgnore
    @Nullable
    public abstract Float floatVal();

    public static Builder builder() {
        return new AutoValue_SignalValueWrapper.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonIgnore
        public abstract Builder isNumber(Boolean isNumber);

        @JsonIgnore
        @Nullable
        public abstract Builder stringVal(String stringVal);

        @JsonIgnore
        @Nullable
        public abstract Builder floatVal(Float floatVal);

        public abstract SignalValueWrapper autoBuild();

        public SignalValueWrapper build() {
            return autoBuild();
        }
    }
}
