package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;

/**
 * PatientDataSets are owned by at most 1 Organization (they can dangle); they don't have to
 * be owned by one (and, in fact, aren't owned initially).
 */
public class PatientDataSet extends NamedEntity {

    public static final String OBJTYPE = ObjectTypes.PATIENT_DATASET;

    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    private final static String F_CO_ID       = "co-id";
    private final static String F_ACTIVE      = "active";
    private final static String F_OWNING_ORG  = "owner-org";  // optional


    /**
     * actual fields
     */
    private String  coID;
    private boolean isActive;
    private XUUID   owningOrgID;

    /**
     * Create a new PatientDataSet from the given information
     */
    public PatientDataSet(String name)
    {
        super(OBJTYPE, name);
    }

    /**
     * Create a new PatientDataSet with the name and XUUID.  This is a hack that is needed to
     * override the assignment of the entity's ID.
     */
    public PatientDataSet(String name, XUUID id)
    {
        // this just fakes a reconstruction from persisted form...
        super(hackConstructWithID(name, id));
    }

    /**
     * A hack to create a "new" PatientDataset object with the given info.  Used hopefully
     * only during migration.
     */
    private static ParamSet hackConstructWithID(String name, XUUID id)
    {
        String idType = id.getType();

        if ((idType == null) || !idType.equals(OBJTYPE))
            throw new IllegalArgumentException("PatientDataSet entity creation with forced XUUID requires XUUID of type " + OBJTYPE + " but is " + id);

        ParamSet ps = new ParamSet();

        // copied from BaseEntity(String objType) method.  It's either this or exposing a BaseEntity.setID() which is dangerous
        ps.put(F_ID,        id.toString());
        ps.put(F_CREATEDAT, Long.toString(System.currentTimeMillis()));

        // copied from NamedEntity(String objType) method.
        ps.put(F_NAME, normalizeName(name));

        return ps;
    }

    /**
     * For restoring from persisted form only
     */
    public PatientDataSet(ParamSet fields)
    {
        super(fields);

        this.coID        = fields.get(F_CO_ID);
        this.isActive    = Boolean.valueOf(fields.get(F_ACTIVE));
        this.owningOrgID = XUUID.fromString(fields.get(F_OWNING_ORG));
    }

    /**
     * Getters
     */
    public String getCOID()      // externally assigned;  "raw" id (MySQL BIGINT, like "10000...300")
    {
        return coID;
    }
    public boolean getActive()    // "inactive" => old customer
    {
        return isActive;
    }
    public XUUID getOwningOrganization()
    {
        return owningOrgID;
    }

    /**
     * Setters
     */
    public void setCOID(String coid)
    {
        this.coID = coid;
    }
    public void setActive(boolean active)
    {
        this.isActive = active;
    }
    public void setOwningOrganization(XUUID orgID)
    {
        this.owningOrgID = orgID;
    }

    /**
     * Return a Map of field=value for persisting the object.
     */
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ACTIVE, Boolean.toString(isActive));
        fields.put(F_CO_ID, coID);

        if (owningOrgID != null)
            fields.put(F_OWNING_ORG, owningOrgID.toString());
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[PDS: [" + super.toString() + "]" +
                ";coID=" + coID +
                ";isActive=" + isActive +
                ";owningOrg=" + owningOrgID +
                "]"
            );
    }

}
