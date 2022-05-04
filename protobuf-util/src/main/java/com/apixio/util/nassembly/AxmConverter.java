package com.apixio.util.nassembly;

import com.apixio.datacatalog.ClinicalCodeOuterClass;
import com.apixio.datacatalog.ExternalIdOuterClass;
import com.apixio.model.external.AxmClinicalCode;
import com.apixio.model.external.AxmExternalId;

public class AxmConverter {

    public static AxmExternalId covertToAxmExternalId(ExternalIdOuterClass.ExternalId idProto){
        if (idProto == null) return null;

        AxmExternalId eid = new AxmExternalId();
        eid.setId(idProto.getId());
        eid.setAssignAuthority(idProto.getAssignAuthority());
        return eid;
    }

    public static AxmClinicalCode convertToAxmClinicalCode(ClinicalCodeOuterClass.ClinicalCode codeProto){
        if (codeProto == null) return null;

        AxmClinicalCode clinicalCode = new AxmClinicalCode();
        clinicalCode.setCode(codeProto.getCode());
        clinicalCode.setSystemVersion(codeProto.getSystemVersion());
        clinicalCode.setName(codeProto.getDisplayName());
        clinicalCode.setSystem(codeProto.getSystem());
        clinicalCode.setSystemOid(codeProto.getSystemOid());
        return clinicalCode;
    }


}
