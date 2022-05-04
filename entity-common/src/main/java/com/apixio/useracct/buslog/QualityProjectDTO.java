package com.apixio.useracct.buslog;

import com.apixio.useracct.entity.Project;
import com.apixio.useracct.entity.QualityProject;
import com.apixio.useracct.entity.QualityProject.State;
import java.util.Date;

import static com.apixio.CommonUtil.iso8601;

/**
 * Properties that can be supplied when creating or modifying a project.
 */
public class QualityProjectDTO extends ProjectDTO {
    public String measurementYear;              // required; "YYYY"
    public String measureList;                  // required
    public String lineOfBusiness;               // required
    public String measureDateRangeOverride;     // optional
    public Boolean measureEligibilityFiltering; // required; default true
    public String eligibleMeasurePrograms;      // required; default to "ALL"
    public String eligibleComplianceStatus;     // required; default to "NOT_COMPLIANT"
    public State  state;                        // optional
    public Double budget;                       // optional
    public Date   deadline;                     // optional
    public String datasource;                   // optional

    /**
     * Convenient and central method to transfer fields that are set in the DTO
     * over to the entity itself.  Only modifiable fields can be added here.
     */
    protected void dtoToEntity(QualityProject project)
    {
        super.dtoToEntity(project);
        if (measurementYear != null)             project.setMeasurementYear(measurementYear);
        if (measureList != null)                 project.setMeasureList(measureList);
        if (lineOfBusiness != null)              project.setLineOfBusiness(lineOfBusiness);
        if (measureDateRangeOverride != null)    project.setMeasureDateRangeOverride(measureDateRangeOverride);
        if (measureEligibilityFiltering != null) project.setMeasureEligibilityFiltering(measureEligibilityFiltering);
        if (eligibleMeasurePrograms != null)     project.setEligibleMeasurePrograms(eligibleMeasurePrograms);
        if (eligibleComplianceStatus != null)    project.setEligibleComplianceStatus(eligibleComplianceStatus);
        if (state != null)                       project.setState(state);
        if (budget != null)                      project.setBudget(budget);
        if (deadline != null)                    project.setDeadline(deadline);
        if (datasource != null)                  project.setDatasource(datasource);
    }

    public void legacyFill(Project.ProjectParamsLegacy reqParams, Boolean forCreate)
    {
        super.legacyFill(reqParams, forCreate);
        state                       = ProjectProperties.toQualityState(reqParams.state);
        budget                      = reqParams.budget;
        deadline                    = iso8601("deadline", reqParams.deadline);
        datasource                  = reqParams.datasource;
        measurementYear             = reqParams.measurementYear;
        measureList                 = reqParams.measureList;
        lineOfBusiness              = reqParams.lineOfBusiness;
        measureDateRangeOverride    = reqParams.measureDateRangeOverride;
        measureEligibilityFiltering = reqParams.measureEligibilityFiltering;
        eligibleMeasurePrograms     = reqParams.eligibleMeasurePrograms;
        eligibleComplianceStatus    = reqParams.eligibleComplianceStatus;
    }

    public String toString()
    {
        return ("[QualityDto " +
                "; name=" + name +
                "; description=" + description +
                "; organizationID=" + organizationID +
                "; patientDataSetID=" + patientDataSetID +
                "; measurementYear=" + measurementYear +
                "; measureList=" + measureList +
                "; lineOfBusiness" + lineOfBusiness +
                "; measureDateRangeOverride=" + measureDateRangeOverride +
                "; measureEligibilityFiltering=" + measureEligibilityFiltering +
                "; eligibleMeasurePrograms=" + eligibleMeasurePrograms +
                "; eligibleComplianceStatus=" + eligibleComplianceStatus +
                "; status=" + status +
                "; state=" + state +
                "; budget=" + budget +
                "; deadline=" + deadline +
                "; datasource=" + datasource +
                "]");
    }
}
