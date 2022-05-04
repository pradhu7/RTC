package com.apixio.useracct.buslog;

import com.apixio.useracct.entity.ProspectiveProject;
import com.apixio.useracct.entity.Project.ProjectParamsLegacy;
import com.apixio.useracct.entity.ProspectiveProject.State;
import java.util.Date;
import static com.apixio.CommonUtil.iso8601;

public class ProspectiveProjectDTO extends ProjectDTO {
    public Double historicalYears; // required
    public String patientList; // required
    public String paymentYear; // required; "YYYY"
    public State state;                        // optional
    public Double budget;                       // optional
    public Date   deadline;                     // optional
    public String datasource;                   // optional

    /**
     * Convenient and central method to transfer fields that are set in the DTO
     * over to the entity itself.  Only modifiable fields can be added here.
     */
    protected void dtoToEntity(ProspectiveProject project)
    {
        super.dtoToEntity(project);
        if (historicalYears != null)           project.setHistoricalYears(historicalYears);
        if (paymentYear != null)               project.setPaymentYear(paymentYear);
        if (patientList != null)               project.setPatientList(patientList);
        if (state != null)                     project.setState(state);
        if (budget != null)                    project.setBudget(budget);
        if (deadline != null)                  project.setDeadline(deadline);
        if (datasource != null)                project.setDatasource(datasource);
    }

    @Override
    public void legacyFill(ProjectParamsLegacy reqParams, Boolean forCreate)
    {
        super.legacyFill(reqParams, forCreate);
        historicalYears  = reqParams.historicalYears;
        patientList      = reqParams.patientList;
        paymentYear      = reqParams.paymentYear;
        state            = ProjectProperties.toProspectiveState(reqParams.state);
        budget           = reqParams.budget;
        deadline         = iso8601("deadline", reqParams.deadline);
        datasource       = reqParams.datasource;
    }

    public String toString()
    {
        return ("[ProspectiveDto " +
                "; name=" + name +
                "; description=" + description +
                "; organizationID=" + organizationID +
                "; patientDataSetID=" + patientDataSetID +
                "; historicalYears=" + historicalYears +
                "; paymentYear=" + paymentYear +
                "; patientList" + patientList +
                "]");
    }
}
