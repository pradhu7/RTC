package com.apixio.accessors;

import java.util.UUID;
import java.util.List;

import com.apixio.bizlogic.patient.logic.PatientLogic;
import com.apixio.model.patient.Patient;
import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;

/* START block of TESTING CODE */
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.Medication;
import com.apixio.model.patient.Prescription;
import java.util.ArrayList;
import com.apixio.model.patient.Procedure;

import org.joda.time.DateTime;
/* END block of TESTING CODE */

public class PatientAccessor extends BaseAccessor
{

    private PatientLogic patientLogic;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        super.setEnvironment(env);

        patientLogic = new PatientLogic(fxEnv.getDaoServices());
    }

    /**
     * Signature:  com.apixio.model.patient.Patient patient(String uuid)
     */
    @Override
    public String getID()
    {
        return "patient";
    }

    @Override
    public Object eval(AccessorContext context, List<Object> args) throws Exception
    {
        String patientuuid = (String) args.get(0);
        /* START block of TESTING CODE */
        if (((String) args.get(0)).contains("1ef9b330-937e-42da-97bd-81758a37e25b")) {
            patientuuid = "bb5af146-ff88-453d-bc21-1fb0a52f696d";
        }
        /* END block of TESTING CODE */

        Patient apo = fullPatient(UUID.fromString(patientuuid));

        /* START block of TESTING CODE */
        if (!((String) args.get(0)).equals(patientuuid)) {
            apo.setPatientId(UUID.fromString((String) args.get(0)));
            String[] probcodes = new String[]{
                                            "M069"/* v24_40, 2.16.840.1.113883.6.90*/,
                                            "7140"/* v24_40, 2.16.840.1.113883.6.103*/,
                                            "E119"/* V24_19 2.16.840.1.113883.6.90*/};
            for (String probcode: probcodes) {
                Procedure p = new Procedure();
                ClinicalCode code = new ClinicalCode();
                code.setCode(probcode);
                code.setCodingSystem("2.16.840.1.113883.6.90");
                code.setCodingSystemOID("2.16.840.1.113883.6.90");
                p.setCode(code);
                p.setEndDate(DateTime.now());
                List<ClinicalCode> supportingDiagnosis = new ArrayList<>();
                supportingDiagnosis.add(code);
                p.setSupportingDiagnosis(supportingDiagnosis);
                apo.addProcedure(p);            
            }

            String[] ndccodes = new String[]{"50111048203" /*v24_111*/,
                                            "68382013101"/* v24_40*/,
                                            "54458092610"/* v24_161*/};
            for (String ndccode: ndccodes) {
                Prescription p = new Prescription();
                ClinicalCode ndc = new ClinicalCode();
                ndc.setCode(ndccode);
                ndc.setCodingSystem("NDC");
                ndc.setCodingSystemOID("NDC");
                p.setCode(ndc);
                Medication associatedMedication = new Medication();
                associatedMedication.setCode(ndc);
                p.setAssociatedMedication(associatedMedication);
                p.setEndDate(DateTime.now());
                apo.addPrescription(p);            
            }
        }
        /* END block of TESTING CODE */
        return apo;
    }

    private Patient fullPatient(UUID uuid) throws Exception
    {
        return patientLogic.getPatient(uuid);
    }

}
