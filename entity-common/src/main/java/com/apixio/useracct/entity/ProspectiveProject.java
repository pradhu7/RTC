package com.apixio.useracct.entity;

import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;
import java.util.Date;

public class ProspectiveProject extends Project {

    public static final String PROSPECTIVE_TYPE   = "PROSPECTIVE";
    public static final String OBJTYPE_PREFIX = Project.OBJTYPE_PREFIX + PROSPECTIVE_TYPE;

    /**
     * Fields
     */
    private final static String F_HISTORICAL_YEARS              = "prospective-historical-years";
    private final static String F_PATIENT_LIST                  = "prospective-patient-list";
    private final static String F_PAYMENT_YEAR                  = "prospective-payment-year";

    private final static String F_PROSPECTIVE_BUDGET     = "prospective-budget";
    private final static String F_PROSPECTIVE_DEADLINE   = "prospective-deadline"; // epoch ms
    private final static String F_PROSPECTIVE_DATASOURCE = "prospective-datasource";
    private final static String F_PROSPECTIVE_STATE      = "prospective-state";

    /**
     * Actual fields
     */
    private Double historicalYears; // required
    private String patientList; // required
    private String paymentYear; // required

    private Double budget;                    // optional
    private Date deadline;                  // optional
    private String datasource;                // optional
    private State state;                     // required; default NEW


    public enum State {
        NEW, STARTED, COMPLETED
    }

    public ProspectiveProject(ParamSet fields)
    {
        super(fields);
        String val;

        this.historicalYears    = getOptDouble(fields, F_HISTORICAL_YEARS);
        this.patientList        = fields.get(F_PATIENT_LIST);
        this.paymentYear        = fields.get(F_PAYMENT_YEAR);

        this.budget       = getOptDouble(fields, F_PROSPECTIVE_BUDGET);
        this.deadline     = getOptDate(fields, F_PROSPECTIVE_DEADLINE);
        this.datasource   = fields.get(F_PROSPECTIVE_DATASOURCE);

        if ((val = fields.get(F_PROSPECTIVE_STATE)) != null)
            this.state = ProspectiveProject.State.valueOf(val);
    }

    public ProspectiveProject(String name, XUUID orgID, XUUID pdsID) {
        super(ProjectClass.PROSPECTIVE, name, PROSPECTIVE_TYPE, PROSPECTIVE_TYPE, orgID, pdsID);
    }

    public static boolean isProspectiveProjectType(String type)
    {
        return type.startsWith(OBJTYPE_PREFIX);
    }


    /**
     * Getters
     */

    public Double getHistoricalYears()
    {
        return historicalYears;
    }

    public String getPatientList()
    {
        return patientList;
    }

    public String getPaymentYear()
    {
        return paymentYear;
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

    public void setHistoricalYears(Double year)
    {
        this.historicalYears = year;
    }

    public void setPatientList(String patients)
    {
        this.patientList = patients;
    }

    public void setPaymentYear(String year){
        this.paymentYear = year;
    }

    public void setBudget(Double budget) { this.budget = budget; }

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

        putOptional(fields, F_HISTORICAL_YEARS, historicalYears);
        fields.put(F_PATIENT_LIST, patientList);
        fields.put(F_PAYMENT_YEAR, paymentYear);

        putOptional(fields, F_PROSPECTIVE_BUDGET,    budget);
        putOptional(fields, F_PROSPECTIVE_DEADLINE,  deadline);
        fields.put(F_PROSPECTIVE_DATASOURCE,         datasource);
        if (state != null)
            fields.put(F_PROSPECTIVE_STATE, state.toString());
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[ProspectiveProject " + super.toString() +
                "; historicalYear=" + historicalYears +
                "; patientList=" + patientList +
                "; paymentYear=" + paymentYear +
                "]");
    }
}
