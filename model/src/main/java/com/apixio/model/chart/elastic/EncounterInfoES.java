package com.apixio.model.chart.elastic;

import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import com.apixio.model.chart.elastic.util.MappingUtil;
import com.apixio.model.patient.EncounterType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterInfoES implements NestedES {

    private ESStringValue encounterId;
    private ESDateValue date;
    private ESStringValue type; // see { @see com.apixio.model.patient.EncounterType }

    private EncounterInfoES(Builder builder) {
        if (builder.encounterId != null) {
            encounterId = new ESStringValue(builder.encounterId);
        }
        if (builder.date != null) {
            date = new ESDateValue(builder.date);
        }
        if (builder.type != null) {
            type = new ESStringValue(builder.type.name());
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> topLevel = new HashMap<>();

        Field[] encounterInfoFields = DocumentInfoES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(encounterInfoFields);

        topLevel.put("type", "nested");
        topLevel.put("properties", properties);
        return topLevel;
    }

    public static class Builder {

        private String encounterId;
        private DateTime date;
        private EncounterType type;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder encounterId(String encounterId) {
            this.encounterId = encounterId;
            return this;
        }

        public Builder date(DateTime date) {
            this.date = date;
            return this;
        }

        public Builder type(EncounterType type) {
            this.type = type;
            return this;
        }

        public EncounterInfoES build() {
            return new EncounterInfoES(this);
        }
    }
}
