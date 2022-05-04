package com.apixio.useracct.entity;

import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.restbase.util.DateUtil;
import java.util.Date;

/**
 * Quality ID Project
 */
public class QualityProject extends Project {

    public static final String QUALITY_TYPE   = "QUALITY";
    public static final String OBJTYPE_PREFIX = Project.OBJTYPE_PREFIX + QUALITY_TYPE;

    /**
     * Fields
     */
    private final static String F_QUALITY_MEASUREMENT_YEAR              = "quality-measurement-year";
    private final static String F_QUALITY_MEASURE_LIST                  = "quality-measure-list";
    private final static String F_QUALITY_LINE_OF_BUSINESS              = "quality-line-of-business";
    private final static String F_QUALITY_MEASURE_DATE_RANGE_OVERRIDE   = "quality-measure-date-range-override";
    private final static String F_QUALITY_MEASURE_ELIGIBILITY_FILTERING = "quality-measure-eligibility-filtering";
    private final static String F_QUALITY_ELIGIBLE_MEASURE_PROGRAMS     = "quality-eligible-measure-programs";
    private final static String F_QUALITY_ELIGIBLE_COMPLIANCE_STATUS    = "quality-eligible-compliance-status";

    private final static String F_QUALITY_BUDGET     = "quality-budget";
    private final static String F_QUALITY_DEADLINE   = "quality-deadline"; // epoch ms
    private final static String F_QUALITY_DATASOURCE = "quality-datasource";
    private final static String F_QUALITY_STATE      = "quality-state";


    /**
     * Actual fields
     */
    private String measurementYear;           // required; modifiable?; "YYYY"
    private String measureList;               // required
    private String lineOfBusiness;            // required

    private String measureDateRangeOverride;  // optional
    // something like...
    // COL: 2014-01-01 to 2018-01-01
    // ABA: 2017-06-01 to 2018-01-01
    // ... but parsable

    private Boolean measureEligibilityFiltering; // required; default true
    private String eligibleMeasurePrograms;      // required; default to "ALL"
    private String eligibleComplianceStatus;     // required; default to "NON_COMPLIANT"


    private Double budget;                    // optional
    private Date   deadline;                  // optional
    private String datasource;                // optional
    private State  state;                     // required; default NEW

    public enum State {
        NEW, STARTED, COMPLETED
    }

    public static boolean isQualityProjectType(String type)
    {
        return type.startsWith(OBJTYPE_PREFIX);
    }

    /**
     *
     */
    public QualityProject(ParamSet fields)
    {
        super(fields);

        String val;

        this.measurementYear             = fields.get(F_QUALITY_MEASUREMENT_YEAR);
        this.measureList                 = fields.get(F_QUALITY_MEASURE_LIST);
        this.lineOfBusiness              = fields.get(F_QUALITY_LINE_OF_BUSINESS);
        this.measureDateRangeOverride    = fields.get(F_QUALITY_MEASURE_DATE_RANGE_OVERRIDE);
        this.measureEligibilityFiltering = getOptBool(fields, F_QUALITY_MEASURE_ELIGIBILITY_FILTERING);
        this.eligibleMeasurePrograms     = fields.get(F_QUALITY_ELIGIBLE_MEASURE_PROGRAMS);
        this.eligibleComplianceStatus    = fields.get(F_QUALITY_ELIGIBLE_COMPLIANCE_STATUS);

        this.budget       = getOptDouble(fields, F_QUALITY_BUDGET);
        this.deadline     = getOptDate(fields, F_QUALITY_DEADLINE);
        this.datasource   = fields.get(F_QUALITY_DATASOURCE);

        if ((val = fields.get(F_QUALITY_STATE)) != null)
            this.state = State.valueOf(val);
    }

    /**
     * ONLY and ALL required values must be declared as arguments:
     */
    public QualityProject(String name, XUUID orgID, XUUID pdsID)
    {
        super(ProjectClass.QUALITY, name, QUALITY_TYPE, QUALITY_TYPE, orgID, pdsID);
    }

    /**
     * Testers
     */

    /**
     * Getters
     */

    public String getMeasurementYear()
    {
        return measurementYear;
    }

    public String getMeasureList()
    {
        return measureList;
    }

    public String getLineOfBusiness()
    {
        return lineOfBusiness;
    }

    public String getMeasureDateRangeOverride()
    {
        return measureDateRangeOverride;
    }

    public Boolean getMeasureEligibilityFiltering()
    {
        return measureEligibilityFiltering;
    }

    public String getEligibleMeasurePrograms()
    {
        return eligibleMeasurePrograms;
    }

    public String getEligibleComplianceStatus()
    {
        return eligibleComplianceStatus;
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

    /**
     * Setters
     */

    public void setMeasurementYear(String year)
    {
        DateUtil.validateY(year);
        this.measurementYear = year;
    }

    /**
     * Should be a string with 0 or more comma separated measure ids
     * @param measureList
     */
    public void setMeasureList(String measureList)
    {
        this.measureList = measureList;
    }

    public void setLineOfBusiness(String lineOfBusiness)
    {
        this.lineOfBusiness = lineOfBusiness;
    }

    public void setMeasureDateRangeOverride(String measureDateRangeOverride)
    {
        // todo: format validation?
        this.measureDateRangeOverride = measureDateRangeOverride;
    }

    public void setMeasureEligibilityFiltering(Boolean measureEligibilityFiltering)
    {
        this.measureEligibilityFiltering = measureEligibilityFiltering;
    }

    public void setEligibleMeasurePrograms(String eligibleMeasurePrograms)
    {
        this.eligibleMeasurePrograms = eligibleMeasurePrograms;
    }

    public void setEligibleComplianceStatus(String eligibleComplianceStatus)
    {
        this.eligibleComplianceStatus = eligibleComplianceStatus;
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

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_QUALITY_MEASUREMENT_YEAR, measurementYear);
        fields.put(F_QUALITY_MEASURE_LIST,     measureList);
        fields.put(F_QUALITY_LINE_OF_BUSINESS, lineOfBusiness);
        fields.put(F_QUALITY_MEASURE_DATE_RANGE_OVERRIDE,  measureDateRangeOverride);
        putOptional(fields, F_QUALITY_MEASURE_ELIGIBILITY_FILTERING, measureEligibilityFiltering);
        fields.put(F_QUALITY_ELIGIBLE_MEASURE_PROGRAMS, eligibleMeasurePrograms);
        fields.put(F_QUALITY_ELIGIBLE_COMPLIANCE_STATUS, eligibleComplianceStatus);

        putOptional(fields, F_QUALITY_BUDGET,    budget);
        putOptional(fields, F_QUALITY_DEADLINE,  deadline);
        fields.put(F_QUALITY_DATASOURCE,         datasource);

        if (state != null)
            fields.put(F_QUALITY_STATE, state.toString());
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[QualityProject " + super.toString() +
                "; measurementYear=" + measurementYear +
                "; measureList=" + measureList +
                "; lineOfBusiness=" + lineOfBusiness +
                "; measureDateRangeOverride" + measureDateRangeOverride +
                "; measureEligibilityFiltering" + measureEligibilityFiltering +
                "; eligibleMeasurePrograms" + eligibleMeasurePrograms +
                "; eligibleComplianceStatus" + eligibleComplianceStatus +
                "]");
    }
}
