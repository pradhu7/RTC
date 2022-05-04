package com.apixio.dao.patient2;

import com.apixio.model.patient.Patient;

public class PatientWithTS
{
    public PatientWithTS(Patient patient, long TS)
    {
        this.patient = patient;
        this.TS      = TS;
    }

    public PatientWithTS(Patient patient, long TS, String table, String rk, String cn)
    {
        this.patient = patient;
        this.TS      = TS;
        this.table   = table;
        this.rk      = rk;
        this.cn      = cn;
    }

    public Patient patient;
    public long    TS;
    public String  table;
    public String  rk;
    public String  cn;

    @Override
    public String toString()
    {
        return ("[PatientWithTS: "+
                "patient=" + patient +
                "; TS=" + TS +
                "; table=" + table +
                "; rk=" + rk +
                "; cn=" + cn +
                "]");
    }
}
