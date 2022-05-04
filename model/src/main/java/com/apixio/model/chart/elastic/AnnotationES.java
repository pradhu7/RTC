package com.apixio.model.chart.elastic;

import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import com.apixio.model.chart.elastic.util.MappingUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationES implements NestedES {

    private ESStringValue id;
    private ESStringValue type;
    private ESDateValue timestamp;
    private FindingES finding;
    private List<ESKeyValuePair> evidence;
    private List<ESKeyValuePair> fact;
    private List<ESKeyValuePair> attributes; //TODO: flattening attributes that are common across all annotations

    private AnnotationES(Builder builder) {
        if (builder.id != null) {
            id = new ESStringValue(builder.id);
        }
        if (builder.type != null) {
            type = new ESStringValue(builder.type);
        }
        if (builder.timestamp != null) {
            timestamp = new ESDateValue(builder.timestamp);
        }
        if (builder.finding != null) {
            finding = builder.finding;
        }
        if (builder.evidence != null && !builder.evidence.isEmpty()) {
            evidence = new ArrayList<>();
            for (Map.Entry<String, String> entry : builder.evidence.entrySet()) {
                evidence.add(new ESKeyValuePair(entry.getKey(), new ESStringValue(entry.getValue())));
            }
        }
        if (builder.fact != null && !builder.fact.isEmpty()) {
            fact = new ArrayList<>();
            for (Map.Entry<String, String> entry : builder.fact.entrySet()) {
                fact.add(new ESKeyValuePair(entry.getKey(), new ESStringValue(entry.getValue())));
            }
        }
        if (builder.attributes != null && !builder.attributes.isEmpty()) {
            attributes = new ArrayList<>();
            for (Map.Entry<String, String> entry : builder.attributes.entrySet()) {
                attributes.add(new ESKeyValuePair(entry.getKey(), new ESStringValue(entry.getValue())));
            }
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> topLevel = new HashMap<>();

        Field[] annotationFields = AnnotationES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(annotationFields);

        topLevel.put("type", "nested");
        topLevel.put("properties", properties);
        return topLevel;
    }

    public static class Builder {
        private String id;
        private String type;
        private DateTime timestamp;
        private FindingES finding;
        private Map<String, String> evidence;
        private Map<String, String> fact;
        private Map<String, String> attributes;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(DateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder finding(FindingES finding) {
            this.finding = finding;
            return this;
        }

        public Builder evidence(Map<String, String> evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder fact(Map<String, String> fact) {
            this.fact = fact;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public AnnotationES build() {
            return new AnnotationES(this);
        }
    }

    @Getter
    @NoArgsConstructor
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FindingES implements NestedES {

        private ESStringValue code;
        private List<ESKeyValuePair> attributes;

        private FindingES(FindingBuilder builder) {
            if (builder.code != null) {
                code = new ESStringValue(builder.code);
            }
            if (builder.attributes != null && !builder.attributes.isEmpty()) {
                attributes = new ArrayList<>();
                for (Map.Entry<String, String> entry : builder.attributes.entrySet()) {
                    attributes.add(new ESKeyValuePair(entry.getKey(), new ESStringValue(entry.getValue())));
                }
            }
        }

        public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
            Map<String, Object> topLevel = new HashMap<>();

            Field[] findingFields = FindingES.class.getDeclaredFields();
            Map<String, Object> properties = MappingUtil.extractESFieldProperties(findingFields);

            topLevel.put("type", "object");
            topLevel.put("properties", properties);
            return topLevel;
        }

        public static class FindingBuilder {
            private String code;
            private Map<String, String> attributes;

            private FindingBuilder() {
            }

            public static FindingBuilder newBuilder() {
                return new FindingBuilder();
            }

            public FindingBuilder code(String code) {
                this.code = code;
                return this;
            }

            public FindingBuilder attributes(Map<String, String> attributes) {
                this.attributes = attributes;
                return this;
            }

            public FindingES build() {
                return new FindingES(this);
            }
        }

    }
}
