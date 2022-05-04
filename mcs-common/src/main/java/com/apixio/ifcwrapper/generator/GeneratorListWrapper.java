package com.apixio.ifcwrapper.generator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = GeneratorListDeserializer.class)
public abstract class GeneratorListWrapper {

    @JsonProperty("generators")
    public abstract List<GeneratorWrapper> generators();

    public static Builder builder() {
        return new AutoValue_GeneratorListWrapper.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("generators")
        public abstract Builder generators(List<GeneratorWrapper> generators);

        public abstract GeneratorListWrapper build();
    }
}
