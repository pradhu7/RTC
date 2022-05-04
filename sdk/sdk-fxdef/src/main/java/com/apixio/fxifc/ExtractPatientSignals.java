package com.apixio.fxifc;

import java.util.List;

import com.apixio.ensemble.ifc.Signal;
import com.apixio.model.patient.Patient;

public interface ExtractPatientSignals
{
    public List<Signal> extractPatientSignals(Patient p) throws Exception;
    public void setup() throws Exception;
}
