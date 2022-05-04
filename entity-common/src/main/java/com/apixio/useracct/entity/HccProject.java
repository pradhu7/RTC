package com.apixio.useracct.entity;

import java.util.Calendar;
import java.util.Date;

import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.util.DateUtil;

/**
 * HccProject
 */
public class HccProject extends Project {

    public static final String HCC_TYPE        = "HCC";
    public static final String OBJTYPE_PREFIX  = Project.OBJTYPE_PREFIX + HCC_TYPE;

    public static boolean isHccProjectType(String type)
    {
        return type.startsWith(OBJTYPE_PREFIX);
    }

    public enum Sweep {
        INITIAL, MID_YEAR, FINAL_RECONCILIATION
    }

    public enum PassType {
        FIRST_PASS, SECOND_PASS
    }

    public enum State {
        NEW, BUNDLED, COMPLETED
    }

    /**
     * enum identifiers MUST BE the exact same as what's in bootstrap-data.yaml (which defines
     * the role set names) as the code does the RoleSet lookup based on Type.toString().toLowerCase().
     */
    public enum Type {
        HCC(""),             // special case, otherwise we get "PRHCCHCC_"
        TEST("TEST"),
        SCIENCE("SCI"),
        MANUAL("MANUAL"),
        CODEVAL("CODEVAL");

        public String getPrefix()   { return this.prefix;   }

        private Type(String prefix) { this.prefix = prefix; }
        private String prefix;
    }

    /**
     * Fields
     */
    private final static String F_HCC_DOS_START     = "hcc-dos-start";
    private final static String F_HCC_DOS_END       = "hcc-dos-end";
    private final static String F_HCC_PAYMENT_YEAR  = "hcc-payment-year";
    private final static String F_HCC_SWEEP         = "hcc-sweep";
    private final static String F_HCC_PASSTYPE      = "hcc-passtype";
    private final static String F_HCC_RAWRAF        = "hcc-rawraf";
    private final static String F_HCC_RAF           = "hcc-raf";
    private final static String F_HCC_BUDGET        = "hcc-budget";
    private final static String F_HCC_DEADLINE      = "hcc-deadline";        // epoch ms
    private final static String F_HCC_DATASOURCE    = "hcc-datasource";
    private final static String F_HCC_STATE         = "hcc-state";
    private final static String F_HCC_PATIENTLIST   = "hcc-patientlist";
    private final static String F_HCC_DOCFILTERLIST = "hcc-docfilterlist";

    /**
     * Actual fields
     */
    private Date     dosStart;       // required; modifiable
    private Date     dosEnd;         // required; modifiable
    private String   paymentYear;    // required; modifiable; "YYYY"
    private Sweep    sweep;          // required; modifiable; Sweep.toString()
    private PassType passType;       // required; modifiable; PassType.toString()
    private Double   rawRaf;
    private Double   raf;
    private Double   budget;
    private Date     deadline;
    private String   datasource;
    private State    state;
    private String   patientList;
    private String   docFilterList;

    /**
     *
     */
    public HccProject(ParamSet fields)
    {
        super(fields);

        String val;

        this.paymentYear   = fields.get(F_HCC_PAYMENT_YEAR);
        this.datasource    = fields.get(F_HCC_DATASOURCE);
        this.patientList   = fields.get(F_HCC_PATIENTLIST);
        this.docFilterList = fields.get(F_HCC_DOCFILTERLIST);

        this.dosStart     = getOptDate(fields, F_HCC_DOS_START);
        this.dosEnd       = getOptDate(fields, F_HCC_DOS_END);
        this.rawRaf       = getOptDouble(fields, F_HCC_RAWRAF);
        this.raf          = getOptDouble(fields, F_HCC_RAF);
        this.budget       = getOptDouble(fields, F_HCC_BUDGET);
        this.deadline     = getOptDate(fields,   F_HCC_DEADLINE);

        if ((val = fields.get(F_HCC_SWEEP)) != null)
            this.sweep = Sweep.valueOf(val);

        if ((val = fields.get(F_HCC_PASSTYPE)) != null)
            this.passType = PassType.valueOf(val);

        if ((val = fields.get(F_HCC_STATE)) != null)
            this.state = State.valueOf(val);
    }



    /**
     * Create a new HccProject with the given XUUID.  This is a hack that is needed to
     * override the assignment of the entity's ID.
     */
    public HccProject(XUUID projectID, String name, String type, XUUID orgID, XUUID pdsID, Boolean status)  // to reconstruct Project
    {
        // this just fakes a reconstruction from persisted form...
        super(hackConstructWithID(projectID, name, ProjectClass.HCC, type, orgID, pdsID, status));
    }

    /**
     * A hack to create a "new" Project object with the given info.  Used hopefully
     * only during migration.
     */
    private static ParamSet hackConstructWithID(
        XUUID id,     // to reconstruct BaseEntity
        String name,  // to reconstruct NamedEntity
        ProjectClass projClass, String type, XUUID orgID, XUUID pdsID, Boolean status  // to reconstruct Project
        )
    {
        ParamSet ps = new ParamSet();

        // copied from BaseEntity(String objType) method.  It's either this or exposing a BaseEntity.setID() which is dangerous
        ps.put(F_ID,        id.toString());
        ps.put(F_CREATEDAT, Long.toString(System.currentTimeMillis()));

        // copied from NamedEntity(String objType) method.
        ps.put(F_NAME, normalizeName(name));

        // copied from Project ctor/toParamSet method.
        ps.put(F_PROJ_CLASS, projClass.toString());
        ps.put(F_PROJ_TYPE,  normalizeType(type.toString()));
        ps.put(F_PROJ_ORG,   orgID.toString());

        if (status != null)
            ps.put(F_PROJ_STATUS, Boolean.toString(status));

        if (pdsID != null)
            ps.put(F_PROJ_PDSID, pdsID.toString());

        return ps;
    }

    /**
     * ONLY and ALL required values must be declared as arguments:
     */
    public HccProject(String name, Type type, XUUID orgID, XUUID pdsID)
    {
        super(ProjectClass.HCC, name, type.toString(), HCC_TYPE + type.getPrefix(), orgID, pdsID);
    }

    /**
     * Testers
     */

    /**
     * Getters
     */
    public Date getDosStart()
    {
        return dosStart;
    }
    public Date getDosEnd()
    {
        return dosEnd;
    }
    public String getPaymentYear()
    {
        return paymentYear;
    }
    public Sweep getSweep()
    {
        return sweep;
    }
    public PassType getPassType()
    {
        return passType;
    }
    public Double getRawRaf()
    {
        return rawRaf;
    }
    public Double getRaf()
    {
        return raf;
    }
    public Double getBudget()
    {
        return budget;
    }
    public Date getDeadline()
    {
        return deadline;
    }
    public String getDatasource()
    {
        return datasource;
    }
    public State getState()
    {
        return state;
    }
    public String getPatientList()
    {
        return patientList;
    }
    public String getDocFilterList()
    {
        return docFilterList;
    }

    /**
     * Setters
     */
    /**
     * Sets the dates-of-service start and end.  The Date passed in is rounded down to 00:00:00 (hms) of the day
     */
    public void setDosStart(Date start)
    {
        dosStart = roundDown(start);
    }
    public void setDosEnd(Date end)
    {
        end = roundDown(end);

        if (end.getTime() < dosStart.getTime())
            throw new IllegalArgumentException("dosEnd [" + end + "] is before dosStart [" + dosStart + "]");

        dosEnd = end;
    }
    public void setPaymentYear(String year)
    {
        DateUtil.validateY(year);
        this.paymentYear = year;
    }
    public void setSweep(Sweep sweep)
    {
        this.sweep = sweep;
    }
    public void setPassType(PassType passType)
    {
        this.passType = passType;
    }
    public void setRawRaf(Double rawRaf)
    {
        this.rawRaf = rawRaf;
    }
    public void setRaf(Double raf)
    {
        this.raf = raf;
    }
    public void setBudget(Double budget)
    {
        this.budget = budget;
    }
    public void setDeadline(Date deadline)
    {
        this.deadline = deadline;
    }
    public void setDatasource(String datasource)
    {
        this.datasource = datasource;
    }
    public void setState(State state)
    {
        this.state = state;
    }
    public void setPatientList(String patientList)
    {
        this.patientList = patientList;
    }
    public void setDocFilterList(String docFilterList)
    {
        this.docFilterList = docFilterList;
    }

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_HCC_PAYMENT_YEAR,  paymentYear);
        fields.put(F_HCC_DATASOURCE,    datasource);
        fields.put(F_HCC_PATIENTLIST,   patientList);
        fields.put(F_HCC_DOCFILTERLIST, docFilterList);

        putOptional(fields, F_HCC_DOS_START, dosStart);
        putOptional(fields, F_HCC_DOS_END,   dosEnd);
        putOptional(fields, F_HCC_RAWRAF,    rawRaf);
        putOptional(fields, F_HCC_RAF,       raf);
        putOptional(fields, F_HCC_BUDGET,    budget);
        putOptional(fields, F_HCC_DEADLINE,  deadline);

        if (sweep != null)
            fields.put(F_HCC_SWEEP, sweep.toString());

        if (passType != null)
            fields.put(F_HCC_PASSTYPE,  passType.toString());

        if (state != null)
            fields.put(F_HCC_STATE, state.toString());
    }

    /**
     * Sets hour/min/sec to 0
     */
    private Date roundDown(Date dt)
    {
        Calendar cal = Calendar.getInstance();

        cal.setTime(dt);

        cal.set(Calendar.HOUR,   0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        return cal.getTime();
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[HccProject " + super.toString() +
                "; paymentYear=" + paymentYear +
                "; sweep=" + sweep +
                "; passType=" + passType +
                "]");
    }
}
