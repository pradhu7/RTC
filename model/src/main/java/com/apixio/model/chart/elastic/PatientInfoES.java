package com.apixio.model.chart.elastic;

import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import com.apixio.model.chart.elastic.util.MappingUtil;
import com.apixio.model.patient.Gender;
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
import java.util.UUID;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientInfoES implements NestedES {

    private ESUUIDValue patientUUID;

    //TODO: can we combine all memberIds, externalIds (hcin, mbi, aca) into a list with types?
    private ESEncryptedStringValue memberId;
    private ESEncryptedStringValue hcin;
    private ESEncryptedStringValue mbi;
    private ESEncryptedStringValue aca;
    private ESEncryptedStringValue firstName;
    private ESEncryptedStringValue lastName;
    private ESEncryptedDateValue dob;
    private ESEncryptedDateValue dod;
    private ESStringValue gender; //just treat enums as strings in ES
    private ESStringValue state;
    private List<ESEncryptedStringValue> phones;
    private ESStringValue eligibility;
    private ESStringValue healthPlanName;
    private ESStringValue healthPlanId;
    private ESStringValue subscriberId;
    private ESStringValue contractNumber;
    private ESEncryptedStringValue beneficiaryId;

    private PatientInfoES(Builder builder) {
        if (builder.patientUUID != null) {
            patientUUID = new ESUUIDValue(builder.patientUUID);
        }
        if (builder.memberId != null) {
            memberId = new ESEncryptedStringValue(builder.memberId);
        }
        if (builder.hcin != null) {
            hcin = new ESEncryptedStringValue(builder.hcin);
        }
        if (builder.mbi != null) {
            mbi = new ESEncryptedStringValue(builder.mbi);
        }
        if (builder.aca != null) {
            aca = new ESEncryptedStringValue(builder.aca);
        }
        if (builder.firstName != null) {
            firstName = new ESEncryptedStringValue(builder.firstName);
        }
        if (builder.lastName != null) {
            lastName = new ESEncryptedStringValue(builder.lastName);
        }
        if (builder.dob != null) {
            dob = new ESEncryptedDateValue(builder.dob);
        }
        if (builder.dod != null) {
            dod = new ESEncryptedDateValue(builder.dod);
        }
        if (builder.gender != null) {
            gender = new ESStringValue(builder.gender.name());
        }
        if (builder.state != null) {
            state = new ESStringValue(builder.state);
        }
        if (builder.phones != null && !builder.phones.isEmpty()) {
            phones = new ArrayList<>();
            for (String phone : builder.phones) {
                phones.add(new ESEncryptedStringValue(phone));
            }
        }
        if (builder.eligibility != null) {
            eligibility = new ESStringValue(builder.eligibility);
        }
        if (builder.healthPlanName != null) {
            healthPlanName = new ESStringValue(builder.healthPlanName);
        }
        if (builder.healthPlanId != null) {
            healthPlanId = new ESStringValue(builder.healthPlanId);
        }
        if (builder.subscriberId != null) {
            subscriberId = new ESStringValue(builder.subscriberId);
        }
        if (builder.contractNumber != null) {
            contractNumber = new ESStringValue(builder.contractNumber);
        }
        if (builder.beneficiaryId != null) {
            beneficiaryId = new ESEncryptedStringValue(builder.beneficiaryId);
        }
    }

    public static Map<String, Object> getElasticMapping() throws ElasticMappingDefinitionException {
        Map<String, Object> topLevel = new HashMap<>();

        Field[] patientInfoFields = DocumentInfoES.class.getDeclaredFields();
        Map<String, Object> properties = MappingUtil.extractESFieldProperties(patientInfoFields);

        topLevel.put("type", "nested");
        topLevel.put("properties", properties);
        return topLevel;
    }

    public static class Builder {

        private UUID patientUUID;
        private String memberId;
        private String hcin;
        private String mbi;
        private String aca;
        private String firstName;
        private String lastName;
        private DateTime dob;
        private DateTime dod;
        private Gender gender;
        private String state;
        private List<String> phones;
        private String eligibility;
        private String healthPlanName;
        private String healthPlanId;
        private String subscriberId;
        private String contractNumber;
        private String beneficiaryId; //should this be encrypted??

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder patientUUID(UUID patientUUID) {
            this.patientUUID = patientUUID;
            return this;
        }

        public Builder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder hcin(String hcin) {
            this.hcin = hcin;
            return this;
        }

        public Builder mbi(String mbi) {
            this.mbi = mbi;
            return this;
        }

        public Builder aca(String aca) {
            this.aca = aca;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder dob(DateTime dob) {
            this.dob = dob;
            return this;
        }

        public Builder dod(DateTime dod) {
            this.dod = dod;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder phones(List<String> phones) {
            this.phones = phones;
            return this;
        }

        public Builder eligibility(String eligibility) {
            this.eligibility = eligibility;
            return this;
        }

        public Builder healthPlanName(String healthPlanName) {
            this.healthPlanName = healthPlanName;
            return this;
        }

        public Builder healthPlanId(String healthPlanId) {
            this.healthPlanId = healthPlanId;
            return this;
        }

        public Builder subscriberId(String subscriberId) {
            this.subscriberId = subscriberId;
            return this;
        }

        public Builder contractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
            return this;
        }

        public Builder beneficiaryId(String beneficiaryId) {
            this.beneficiaryId = beneficiaryId;
            return this;
        }

        public PatientInfoES build() {
            return new PatientInfoES(this);
        }
    }
}
