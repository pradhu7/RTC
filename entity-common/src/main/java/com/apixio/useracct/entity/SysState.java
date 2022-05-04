package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.BaseEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * SysState is a singleton persisted object (with a pre-defined ID) that is used
 * to keep global information about the system.
 */
public class SysState extends BaseEntity {

    public static final String OBJTYPE = ObjectTypes.SYS_STATE;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_STATE_SCHEMAVERSION       = "schemaVersion";

    /**
     * actual fields
     */
    private int   schemaVersion;  // must be monotonically increasing

    /**
     * Creates a new SysState with the given ID, bypassing the creation and assignment
     * of a real XUUID.
     */
    public SysState(XUUID forcedID)
    {
        this(forceCreateParams(forcedID));
    }

    /**
     * For restoring from persisted form only
     */
    public SysState(ParamSet fields)
    {
        super(fields);

        this.schemaVersion = Integer.parseInt(fields.get(F_STATE_SCHEMAVERSION));
    }

    /**
     * Getters
     */
    public int getSchemaVersion()
    {
        return schemaVersion;
    }

    /**
     * Setters
     */
    public void setSchemaVersion(int schemaVersion)
    {
        this.schemaVersion = schemaVersion;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_STATE_SCHEMAVERSION, Integer.toString(schemaVersion));
    }

    /**
     * Create a ParamSet that looks as though it was read in from persisted store but
     * is actually used to create the singleton with the given ID.
     */
    private static ParamSet forceCreateParams(XUUID forcedID)
    {
        ParamSet fake = new ParamSet();

        fake.put(BaseEntity.F_ID,        forcedID.toString());
        fake.put(BaseEntity.F_CREATEDAT, Long.toString(System.currentTimeMillis()));
        fake.put(F_STATE_SCHEMAVERSION,  "0");

        return fake;
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[SysState: [" + super.toString() + "]" +
                "; schemaVersion=" + schemaVersion
            );
    }

}
