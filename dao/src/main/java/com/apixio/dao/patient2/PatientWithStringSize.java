package com.apixio.dao.patient2;

import com.apixio.model.patient.Patient;

public class PatientWithStringSize
{
    private Patient patient;
    private int stringSize;

    public Patient getPatient()
    {
        return patient;
    }

    public void setPatient(Patient patient)
    {
        this.patient = patient;
    }

    public int getStringSize()
    {
        return stringSize;
    }

    public void setStringSize(int stringSize)
    {
        this.stringSize = stringSize;
    }
}
