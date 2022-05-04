package com.apixio.model.event.transformer;

import org.joda.time.DateTime;

import com.apixio.model.event.AttributeType;
import com.apixio.model.event.AttributesType;
import com.apixio.model.event.CodeType;
import com.apixio.model.event.EvidenceType;
import com.apixio.model.event.FactType;
import com.apixio.model.event.ReferenceType;
import com.apixio.model.event.TimeRangeType;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.ClinicalCode;

import java.util.UUID;

public class EventTransformerUtils {

    public static ReferenceType reference(String type, String uri) {
        ReferenceType rt = new ReferenceType();
        rt.setType(type);
        rt.setUri(uri);
        return rt;
    }

    public static ReferenceType patient(UUID patientUUID) {
        return reference("patient",patientUUID.toString());
    }

    public static ReferenceType document(UUID documentUUID) {
        return reference("document", documentUUID.toString());
    }



    public static EvidenceType createConcreteEvidence() {
		EvidenceType evidenceType = new EvidenceType();
		evidenceType.setInferred(false);
		return evidenceType;
	}

	public static ReferenceType createReference(BaseObject baseObject) {
		ReferenceType referenceType = new ReferenceType();
		referenceType.setUri(baseObject.getInternalUUID().toString());
		referenceType.setType(baseObject.getClass().getSimpleName());
		return referenceType;
	}

	public static ReferenceType createReference(Object uri, String type) {
		ReferenceType referenceType = null;
		if (uri != null) {
			referenceType = new ReferenceType();
			referenceType.setUri(uri.toString());
			referenceType.setType(type);
		}
		return referenceType;
	}
	
	public static FactType createFact(ClinicalCode code, DateTime startDate, DateTime endDate, AttributesType attributes) {
		FactType fact = new FactType();
		fact.setCode(convertCodeToCodeType(code));
		fact.setTime(getTimeRange(startDate, endDate));
		fact.setValues(attributes);
		return fact;
	}

	public static TimeRangeType getTimeRange(DateTime startDate, DateTime endDate) {
		TimeRangeType timeRangeType = new TimeRangeType();
		if (startDate != null)
			timeRangeType.setStartTime(startDate.toDate());//getGregorianDate(startDate));
		if (endDate != null)
			timeRangeType.setEndTime(endDate.toDate());//getGregorianDate(endDate));
		return timeRangeType;
	}

	public static CodeType convertCodeToCodeType(ClinicalCode code) {
		CodeType codeType = new CodeType();
		if (code != null) {
			codeType.setCode(code.getCode());
			codeType.setCodeSystem(code.getCodingSystemOID());
			codeType.setCodeSystemName(code.getCodingSystem());
			codeType.setCodeSystemVersion(code.getCodingSystemVersions());
			codeType.setDisplayName(code.getDisplayName());
		}
		return codeType;
	}

	public static AttributeType createAttributeType(String name, Object value) {
		AttributeType attribute = new AttributeType();
		attribute.setName(name);
		attribute.setValue(value != null ? value.toString() : "null");
		return attribute;
	}

	// TODO: not used. what is this for?
	public static AttributeType createAttributeType(String name, Object value, BaseObject base) {
		AttributeType attribute = new AttributeType();
		attribute.setName(name);
		attribute.setValue(value != null ? value.toString() : "null");
		return attribute;
	}
}
