package com.apixio.model.chart.elastic;

import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import com.apixio.model.chart.elastic.util.MappingUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderInfoES implements NestedES {

    private ESUUIDValue servicingProviderUUID;
    private ESUUIDValue signingProviderUUID;
    private ESStringValue placeOfService;

    private ProviderInfoES(Builder builder) {
        if (builder.servicingProviderUUID != null) {
            servicingProviderUUID = new ESUUIDValue(builder.servicingProviderUUID);
        }
        if (builder.signingProviderUUID != null) {
            signingProviderUUID = new ESUUIDValue(builder.signingProviderUUID);
        }
        if (builder.placeOfService != null) {
            placeOfService = new ESStringValue(builder.placeOfService);
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> topLevel = new HashMap<>();

        Field[] providerInfoFields = DocumentInfoES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(providerInfoFields);

        topLevel.put("type", "nested");
        topLevel.put("properties", properties);
        return topLevel;
    }

    public static class Builder {
        private UUID servicingProviderUUID;
        private UUID signingProviderUUID;
        private String placeOfService;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder servicingProviderUUID(UUID servicingProviderUUID) {
            this.servicingProviderUUID = servicingProviderUUID;
            return this;
        }

        public Builder signingProviderUUID(UUID signingProviderUUID) {
            this.signingProviderUUID = signingProviderUUID;
            return this;
        }

        public Builder placeOfService(String placeOfService) {
            this.placeOfService = placeOfService;
            return this;
        }

        public ProviderInfoES build() {
            return new ProviderInfoES(this);
        }
    }
}
