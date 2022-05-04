package com.apixio.accessors;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxRequest;

public class JSONPatientAccessor implements Accessor
{

    private PatientJSONParser patientJSONParser;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        patientJSONParser = new PatientJSONParser();
    }

    /**
     * Signature:  com.apixio.model.patient.Patient patient(String uuid)
     */
    @Override
    public String getID()
    {
        return "jsonpatient";
    }

    @Override
    public Object eval(AccessorContext req, List<Object> args) throws Exception
    {
        String jsonpath = (String) args.get(0);
        Patient apo = patientJSONParser.parsePatientData(new FileInputStream(new File(jsonpath)));
        return apo;
    }

}
