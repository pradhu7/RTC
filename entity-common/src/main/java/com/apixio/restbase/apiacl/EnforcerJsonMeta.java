package com.apixio.restbase.apiacl;

/**
 * Enforcer metadata for JSON specification of protected APIs.
 */
public class EnforcerJsonMeta {

    public String jsonConfigFieldName;
    public String jsonEnforcerFieldClsName;
    public Class<? extends RestEnforcer> enforcerClass;

    public EnforcerJsonMeta(String configField, String enforcerClassname, Class<? extends RestEnforcer> cls)
    {
        this.jsonConfigFieldName      = configField;
        this.jsonEnforcerFieldClsName = enforcerClassname;
        this.enforcerClass            = cls;
    }

}
