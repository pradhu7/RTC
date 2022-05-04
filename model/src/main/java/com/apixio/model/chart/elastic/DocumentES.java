package com.apixio.model.chart.elastic;

import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import com.apixio.model.chart.elastic.util.MappingUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentES {

    private ESUUIDValue documentUUID;
    private DocumentInfoES documentInfo;
    private PatientInfoES patientInfo;
    private EncounterInfoES encounterInfo;
    private ProviderInfoES providerInfo;
    private List<ProjectInfoES> projectInfo;
    private List<AnnotationES> annotations; //winning & losing annotations

    private DocumentES(Builder builder) {
        if (builder.documentUUID != null) {
            documentUUID = new ESUUIDValue(builder.documentUUID); //TODO: should we throw an exception if uuid is missing?
        }
        if (builder.documentInfo != null) {
            documentInfo = builder.documentInfo;
        }
        if (builder.patientInfo != null) {
            patientInfo = builder.patientInfo;
        }
        if (builder.encounterInfo != null) {
            encounterInfo = builder.encounterInfo;
        }
        if (builder.providerInfo != null) {
            providerInfo = builder.providerInfo;
        }
        if (builder.projectInfo != null) {
            projectInfo = builder.projectInfo;
        }
        if (builder.annotations != null && !builder.annotations.isEmpty()) {
            annotations = builder.annotations;
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> mappings = new HashMap<>();
        Map<String, Object> document = new HashMap<>();
        //we should call each classes own 'getElasticMapping' function
        //for top level classes, call getElasticMapping
        //for low level type classes, call getElasticTypeMapping which just gives the types (not field names)

        //loop over class fields
        Field[] documentFields = DocumentES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(documentFields);

        document.put("properties", properties);
        mappings.put("document", document);
        return ImmutableMap.of("mappings", mappings);
    }

    public static class Builder {
        private UUID documentUUID;
        private DocumentInfoES documentInfo;
        private PatientInfoES patientInfo;
        private EncounterInfoES encounterInfo;
        private ProviderInfoES providerInfo;
        private List<ProjectInfoES> projectInfo;
        private List<AnnotationES> annotations; //winning annotations

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder documentUUID(UUID documentUUID) {
            this.documentUUID = documentUUID;
            return this;
        }

        public Builder documentInfo(DocumentInfoES documentInfo) {
            this.documentInfo = documentInfo;
            return this;
        }

        public Builder patientInfo(PatientInfoES patientInfo) {
            this.patientInfo = patientInfo;
            return this;
        }

        public Builder encounterInfo(EncounterInfoES encounterInfo) {
            this.encounterInfo = encounterInfo;
            return this;
        }

        public Builder providerInfo(ProviderInfoES providerInfo) {
            this.providerInfo = providerInfo;
            return this;
        }

        public Builder projectInfo(List<ProjectInfoES> projectInfo) {
            this.projectInfo = projectInfo;
            return this;
        }

        public Builder annotations(List<AnnotationES> annotations) {
            this.annotations = annotations;
            return this;
        }

        public DocumentES build() {
            return new DocumentES(this);
        }
    }

}
