package com.apixio.signals;
import com.apixio.ensemble.impl.source.PatientSource;
import com.apixio.ensemble.ifc.Source;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.ifc.SignalType;
import com.apixio.ensemble.impl.common.SmartXUUID;
import com.apixio.ensemble.impl.common.Generator;

public class AnnotationSignal implements Signal {
    final String AnnotationSignalName = "Annotation";

    PatientSource patientSource = null;
    String value = null;
    String code = null;
    String sourceName = null;
    float score = 1.0f;
    String date = null;

    public AnnotationSignal(String patientId, String cde, String acceptOrReject, String val, String modelPath, String dt) {
        patientSource = new PatientSource(new SmartXUUID(SmartXUUID.PatientIdentifier(), patientId), null);
        sourceName = acceptOrReject;
        code = cde;
        value = val;
        date = dt;
    }
    @Override
    public int compareTo(Signal o) {
        return 0;
    }

    @Override
    public Generator getGenerator() {
        return new Generator(AnnotationSignalName, "1.0", 
                             "com.apixio.accessors.S3PatientAnnotationsAccessor", 
                             "1.0", "fx-accessors.jar");
    }

    @Override
    public String getName() {
        return sourceName + "-" + code;
    }

    @Override
    public Source getSource() {
        return patientSource;
    }

    @Override
    public SignalType getType() {
        return SignalType.CATEGORY;
    }

    @Override
    public Object getValue() {
        return value + ":" + date;
    }

    @Override
    public float getWeight() {
        return score;
    }
    
}
