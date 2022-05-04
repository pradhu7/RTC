package com.apixio.model.external;

import java.util.*;

public class AxmClinicalCode {
    private String name;
    private String system;
    private String systemOid;
    private String code;
    private String systemVersion;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSystemOid() {
        return systemOid;
    }

    public void setSystemOid(String systemOid) {
        this.systemOid = systemOid;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public void setSystemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, system, systemOid, code, systemVersion);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmClinicalCode that = (AxmClinicalCode) obj;
        return Objects.equals(this.name, that.name)
            && Objects.equals(this.system, that.system)
            && Objects.equals(this.systemOid, that.systemOid)
            && Objects.equals(this.code, that.code)
            && Objects.equals(this.systemVersion, that.systemVersion);
    }
}
