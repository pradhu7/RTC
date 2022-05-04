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
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectInfoES implements NestedES {

    private ESStringValue projectId;
    private ESStringValue type;
    private ESStringValue productName;
    private ESDateValue date;
    private ESIntegerValue sweepYear;
    private ESIntegerValue sweepMonth;
    private ESStringValue market;
    private ESStringValue targetCodeSystem;
    private ESIntegerValue paymentYear;
    private ESBooleanValue analyzed;
    private ESStringValue sow;

    private ProjectInfoES(Builder builder) {
        if (builder.projectId != null) {
            projectId = new ESStringValue(builder.projectId);
        }
        if (builder.type != null) {
            type = new ESStringValue(builder.type);
        }
        if (builder.productName != null) {
            productName = new ESStringValue(builder.productName);
        }
        if (builder.date != null) {
            date = new ESDateValue(builder.date);
        }
        if (builder.sweepYear != null) {
            sweepYear = new ESIntegerValue(builder.sweepYear);
        }
        if (builder.sweepMonth != null) {
            sweepMonth = new ESIntegerValue(builder.sweepMonth);
        }
        if (builder.market != null) {
            market = new ESStringValue(builder.market);
        }
        if (builder.targetCodeSystem != null) {
            targetCodeSystem = new ESStringValue(builder.targetCodeSystem);
        }
        if (builder.paymentYear != null) {
            paymentYear = new ESIntegerValue(builder.paymentYear);
        }
        if (builder.analyzed != null) {
            analyzed = new ESBooleanValue(builder.analyzed);
        }
        if (builder.sow != null) {
            sow = new ESStringValue(builder.sow);
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> topLevel = new HashMap<>();

        Field[] projectInfoFields = DocumentInfoES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(projectInfoFields);

        topLevel.put("type", "nested");
        topLevel.put("properties", properties);
        return topLevel;
    }

    public static class Builder {
        private String projectId;
        private String type;
        private String productName;
        private DateTime date;
        private Integer sweepYear;
        private Integer sweepMonth;
        private String market;
        private String targetCodeSystem;
        private Integer paymentYear;
        private Boolean analyzed;
        private String sow;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder date(DateTime date) {
            this.date = date;
            return this;
        }

        public Builder sweepYear(Integer sweepYear) {
            this.sweepYear = sweepYear;
            return this;
        }

        public Builder sweepMonth(Integer sweepMonth) {
            this.sweepMonth = sweepMonth;
            return this;
        }

        public Builder market(String market) {
            this.market = market;
            return this;
        }

        public Builder targetCodeSystem(String targetCodeSystem) {
            this.targetCodeSystem = targetCodeSystem;
            return this;
        }

        public Builder paymentYear(Integer paymentYear) {
            this.paymentYear = paymentYear;
            return this;
        }

        public Builder analyzed(Boolean analyzed) {
            this.analyzed = analyzed;
            return this;
        }

        public Builder sow(String sow) {
            this.sow = sow;
            return this;
        }

        public ProjectInfoES build() {
            return new ProjectInfoES(this);
        }
    }
}
