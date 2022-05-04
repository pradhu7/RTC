package com.apixio.accessors;

import java.util.UUID;
import java.util.List;

import com.apixio.bizlogic.patient.logic.PatientLogic;
import com.apixio.model.patient.Patient;
import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxLogger;

public class SinglePartialPatientAccessor extends BaseAccessor
{

    private PatientLogic patientLogic;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        super.setEnvironment(env);

        patientLogic = new PatientLogic(fxEnv.getDaoServices());
    }

    /**
     * Signature:  com.apixio.model.patient.Patient singlePartialPatient(String uuid)
     */
    @Override
    public String getID()
    {
        return "singlePartialPatient";
    }

    @Override
    public Object eval(AccessorContext context, List<Object> args) throws Exception
    {
        Patient apo = singlePartialPatient(UUID.fromString((String) args.get(0)));

        return apo;
    }

    private Patient singlePartialPatient(UUID uuid) throws Exception
    {
        return patientLogic.getSinglePartialPatient(uuid, true);
    }

}
