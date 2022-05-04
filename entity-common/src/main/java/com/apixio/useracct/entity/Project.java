package com.apixio.useracct.entity;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.buslog.ProjectProperties;

import java.util.Date;

import static com.apixio.CommonUtil.iso8601;
/**
 * Project
 */
public abstract class Project extends NamedEntity {

    public static final String OBJTYPE_PREFIX  = ObjectTypes.PROJECT_BASEPFX;

    public static boolean isProjectType(String type)
    {
        return type.startsWith(OBJTYPE_PREFIX) || type.startsWith("CP");  //!! "CP" is historical xuuid prefix ("Customer Project").  Prefix clash!!
    }

    /**
     * ProjectClass is a hacked attempt at future-proofing to a limited degree the
     * projects.  The idea is that this base class of Project could be extended to
     * more than a single type (Java class) and that for each type there must be an
     * enum key so that we can do the right thing when restoring, etc.
     */
    public enum ProjectClass {
        HCC,
        QUALITY,
        PROSPECTIVE,
        LABEL
    }

    /**
     * Fields
     */
    protected final static String F_PROJ_CLASS  = "class";   // ProjectClass.toString()
    protected final static String F_PROJ_TYPE   = "type";    // as declared by client
    protected final static String F_PROJ_ORG    = "org-id";  // owning organization
    protected final static String F_PROJ_PDSID  = "pds-id";  // associated patientDataSet
    protected final static String F_PROJ_STATUS = "proj-status";  // boolean

    /**
     * Actual fields
     */
    private ProjectClass projClass; // required; so we can properly reconstruct it
    private String       type;      // required; semantics of type string defined outside
    private XUUID        orgID;     // required; projects are owned by exactly 1 Organization
    private XUUID        pdsID;     // required; projects are associated with exactly 1 PatientDataSet.
    private boolean      status;    // defaults to true; modifiable

    /**
     *
     */
    public static ProjectClass getProjectClass(ParamSet fields)
    {
        return ProjectClass.valueOf(fields.get(F_PROJ_CLASS));
    }

    /**
     *
     */
    protected Project(ParamSet fields)
    {
        super(fields);

        this.projClass = ProjectClass.valueOf(fields.get(F_PROJ_CLASS));
        this.type      = fields.get(F_PROJ_TYPE);
        this.orgID     = XUUID.fromString(fields.get(F_PROJ_ORG));
        this.pdsID     = XUUID.fromString(fields.get(F_PROJ_PDSID));
        this.status    = Boolean.valueOf(fields.get(F_PROJ_STATUS)).booleanValue();
    }

    /**
     * ALL and ONLY truly required values must be declared as arguments:
     */
    protected Project(ProjectClass projClass, String name, String type, String prefix, XUUID orgID, XUUID pdsID)
    {
        super(validatePrefix(prefix), name);

        this.projClass = projClass;
        this.type      = normalizeType(type);
        this.orgID     = orgID;
        this.pdsID     = pdsID;
    }

    private static String validatePrefix(String prefix)
    {
        if (prefix.indexOf('_') != -1)   //    !!! use XUUID.SEP_AS_STRING !!
            throw new IllegalArgumentException("Project prefixes cannot have an _ character in them.  Found one in [" + prefix + "]");

        return OBJTYPE_PREFIX + prefix.toUpperCase();
    }

    /**
     * Testers
     */
    public static boolean eqOwningOrg(String ownerID, ParamSet ps)
    {
        return ownerID.equals(ps.get(F_PROJ_ORG));
    }
    public static boolean eqPatientDataSet(String pdsID, ParamSet ps)
    {
        return pdsID.equals(ps.get(F_PROJ_PDSID));
    }

    /**
     * Getters
     */
    public ProjectClass getProjectClass()
    {
        return projClass;
    }
    public String getType()
    {
        return type;
    }
    public XUUID getOrganizationID()
    {
        return orgID;
    }
    public XUUID getPatientDataSetID()
    {
        return pdsID;
    }
    public boolean getStatus()
    {
        return status;
    }

    /**
     * Setters
     */
    public void setStatus(boolean status)
    {
        this.status = status;
    }

    public static String normalizeType(String type)
    {
        return (type == null) ? null : type.trim().toLowerCase();
    }

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_PROJ_CLASS,  projClass.toString());
        fields.put(F_PROJ_TYPE,   type.toString());
        fields.put(F_PROJ_ORG,    orgID.toString());
        fields.put(F_PROJ_STATUS, Boolean.toString(status));

        if (pdsID != null)
            fields.put(F_PROJ_PDSID, pdsID.toString());
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[Project " + super.toString() +
                "; prjClass=" + projClass +
                "; type=" + type +
                "; orgID=" + orgID +
                "; pdsID=" + pdsID +
                "; status=" + status +
                "]");
    }


    /**
     * Moved from useraccount used to update the project there.
     * We have a new ProjectParam in projectadmin service where is the new place for project-based administration.
     * Since we still want to use some current useraccount methods and migrate to projectadmin, we need to follow the old way
     * that have been proved robust and trust-worthy to people.
     *
     * Currently ProjectParamsLegacy will be used only for two update basic/custom project property methods in ProjectAdmin
     */
    public static class ProjectParamsLegacy {

        // all projects
        public String   createdAt;          // ISO8601 format; accepted in JSON but ignored as it's readonly
        public String   name;               //
        public String   description;        //
        public String   type;               //
        public String   organizationID;     // XUUID
        public String   patientDataSetID;   // XUUID
        public Boolean  status;             //
        public String   state;              // ["new", "bundled", "started", "completed"]
        public Double   budget;
        public String   deadline;           // ISO8601 format:  "yyyy-MM-dd'T'HH:mm:ss'Z'"
        public String   datasource;

        // hcc projects
        public String   dosStart;           // ISO8601 format:  "yyyy-MM-dd'T'HH:mm:ss'Z'"
        public String   dosEnd;             // ISO8601 format:  "yyyy-MM-dd'T'HH:mm:ss'Z'"
        public String   paymentYear;        // YYYY
        public String   sweep;              // ["initial", "midYear", "finalReconciliation"]
        public String   passType;           // ["First Pass", "Second Pass"]
        public Double   rawRaf;
        public Double   raf;
        public String   patientList;
        public String   docFilterList;

        // quality projects
        public String  measurementYear;       // YYYY
        public String  measureList;
        public String  lineOfBusiness;
        public String  measureDateRangeOverride;
        public Boolean measureEligibilityFiltering;
        public String  eligibleMeasurePrograms;
        public String  eligibleComplianceStatus;


        //prospective projects
        public Double historicalYears; // required

        public static Date computeDosStart(String dosStart) { return iso8601("dosStart", dosStart);}
        public static Date computeDosEnd(String dosEnd) { return iso8601("dosEnd", dosEnd);}
        public static Date computeDeadline(String deadline) { return iso8601("deadline", deadline);}
    }
}
