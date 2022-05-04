package com.apixio.model.event.transformer;
import java.util.Collection;

import com.apixio.model.event.EventType;
import com.apixio.model.patient.Patient;


public abstract class PatientEventTransformer {

	public abstract Collection<EventType> transformPatientToEvents(Collection<Patient> patients);
}
