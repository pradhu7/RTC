package com.apixio.model.chart.elastic;

import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import com.apixio.model.chart.elastic.util.MappingUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DocumentInfoES implements NestedES {

    private List<ESEncryptedStringValue> externalIds;
    private ESEncryptedStringValue name;
    private ESStringValue source;
    private ESDateValue date; //original date - TODO: not sure if we have this in pipeline yet
    private ESDateValue dateReceived;
    private ESDateValue dateTransferred;
    private ESStringValue packageId;
    private ESStringValue destination; //transfer destination
    private ESStringValue mimeType;

    private DocumentInfoES(Builder builder) {
        if (builder.externalIds != null && !builder.externalIds.isEmpty()) {
            externalIds = new ArrayList<>();
            for (String externalId : builder.externalIds) {
                externalIds.add(new ESEncryptedStringValue(externalId));
            }
        }
        if (builder.name != null) {
            name = new ESEncryptedStringValue(builder.name);
        }
        if (builder.source != null) {
            source = new ESStringValue(builder.source);
        }
        if (builder.originalDate != null) {
            date = new ESDateValue(builder.originalDate);
        }
        if (builder.dateReceived != null) {
            dateReceived = new ESDateValue(builder.dateReceived);
        }
        if (builder.packageId != null) {
            packageId = new ESStringValue(builder.packageId);
        }
        if (builder.destination != null) {
            destination = new ESStringValue(builder.destination);
        }
        if (builder.dateTransferred != null) {
            dateTransferred = new ESDateValue(builder.dateTransferred);
        }
        if (builder.mimeType != null) {
            mimeType = new ESStringValue(builder.mimeType);
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> topLevel = new HashMap<>();

        Field[] documentInfoFields = DocumentInfoES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(documentInfoFields);

        topLevel.put("type", "nested");
        topLevel.put("properties", properties);
        return topLevel;
    }

    public static class Builder {

        private List<String> externalIds;
        private String name;
        private String source;
        private DateTime originalDate;
        private DateTime dateReceived;
        private String packageId;
        private String destination;
        private DateTime dateTransferred;
        private String mimeType;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder externalIds(List<String> externalIds) {
            this.externalIds = externalIds;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder originalDate(DateTime originalDate) {
            this.originalDate = originalDate;
            return this;
        }

        public Builder dateReceived(DateTime dateReceived) {
            this.dateReceived = dateReceived;
            return this;
        }

        public Builder packageId(String packageId) {
            this.packageId = packageId;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder dateTransferred(DateTime dateTransferred) {
            this.dateTransferred = dateTransferred;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public DocumentInfoES build() {
            return new DocumentInfoES(this);
        }
    }
}
