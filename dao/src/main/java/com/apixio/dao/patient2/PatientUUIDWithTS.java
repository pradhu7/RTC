package com.apixio.dao.patient2;

import java.util.UUID;

public class PatientUUIDWithTS
{
    public PatientUUIDWithTS(UUID patientUUID, long TS)
    {
        this.patientUUID = patientUUID;
        this.TS = TS;
    }

    public UUID patientUUID;
    public long TS;

    @Override
    public String toString()
    {
        return ("[PatientUUIDWithTS: "+
                "patientUUID=" + patientUUID +
                "; TS=" + TS +
                "]");
    }
}
