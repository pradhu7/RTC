package com.apixio.util.nassembly

import com.apixio.datacatalog.ClinicalCodeOuterClass.{ClinicalCode => ClinicalCodeProto}
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.model.external.{AxmClinicalCode, AxmExternalId}
import com.google.common.base.Strings.nullToEmpty

object AxmToProtoConverters {

    def convertAxmClinicalCode(code: AxmClinicalCode): ClinicalCodeProto = {
        val builder = ClinicalCodeProto.newBuilder()
        if (code == null) return builder.build()

        if (!nullToEmpty(code.getCode).trim.isEmpty) builder.setCode(code.getCode)
        if (!nullToEmpty(code.getName).trim.isEmpty) builder.setDisplayName(code.getName)
        if (!nullToEmpty(code.getSystem).trim.isEmpty) builder.setSystem(code.getSystem)
        if (!nullToEmpty(code.getSystemOid).trim.isEmpty) builder.setSystemOid(code.getSystemOid)
        if (!nullToEmpty(code.getSystemVersion).trim.isEmpty) builder.setSystemVersion(code.getSystemVersion)
        builder.build()
    }

    def convertAxmExternalId(eid: AxmExternalId): ExternalId = {
        val builder = ExternalId.newBuilder()
        if (eid == null) return builder.build()

        if (!nullToEmpty(eid.getId).trim.isEmpty) builder.setId(eid.getId)
        if (!nullToEmpty(eid.getAssignAuthority).trim.isEmpty) builder.setAssignAuthority(eid.getAssignAuthority)
        builder.build()
    }
}
