package com.apixio.useracct.buslog;

import java.util.Date;

import com.apixio.useracct.entity.HccProject;
import com.apixio.useracct.entity.HccProject.PassType;
import com.apixio.useracct.entity.HccProject.State;
import com.apixio.useracct.entity.HccProject.Sweep;
import com.apixio.useracct.entity.HccProject.Type;
import com.apixio.useracct.entity.Project.ProjectParamsLegacy;
import static com.apixio.CommonUtil.iso8601;

/**
 * Properties that can be supplied when creating or modifying a project.
 */
public class HccProjectDTO extends ProjectDTO {
    public Type     type;                  // required
    public Date     dosStart;              // required
    public Date     dosEnd;                // required
    public String   paymentYear;           // required; "YYYY"
    public Sweep    sweep;                 // required
    public PassType passType;              // required
    public State    state;                 // optional
    public Double   rawRaf;                // optional
    public Double   raf;                   // optional
    public Double   budget;                // optional
    public Date     deadline;              // optional
    public String   datasource;            // optional
    public String   patientList;           // optional
    public String   docFilterList;         // optional

    /**
     * Convenient and central method to transfer fields that are set in the DTO
     * over to the entity itself.  Only modifiable fields can be added here.
     */
    protected void dtoToEntity(HccProject project)
    {
        super.dtoToEntity(project);
        if (dosStart != null)        project.setDosStart(dosStart);
        if (dosEnd != null)          project.setDosEnd(dosEnd);
        if (paymentYear != null)     project.setPaymentYear(paymentYear);
        if (sweep != null)           project.setSweep(sweep);
        if (passType != null)        project.setPassType(passType);
        if (state != null)           project.setState(state);
        if (rawRaf != null)          project.setRawRaf(rawRaf);
        if (raf != null)             project.setRaf(raf);
        if (budget != null)          project.setBudget(budget);
        if (deadline != null)        project.setDeadline(deadline);
        if (datasource != null)      project.setDatasource(datasource);
        if (patientList != null)     project.setPatientList(patientList);
        if (docFilterList != null)   project.setDocFilterList(docFilterList);
    }

    @Override
    public void legacyFill(ProjectParamsLegacy reqParams, Boolean forCreate)
    {
        super.legacyFill(reqParams, forCreate);
        dosStart      = iso8601("dosStart", reqParams.dosStart);
        dosEnd        = iso8601("dosEnd", reqParams.dosEnd);
        paymentYear   = reqParams.paymentYear;
        sweep         = ProjectProperties.toSweep(reqParams.sweep);
        passType      = ProjectProperties.toPassType(reqParams.passType);
        state         = ProjectProperties.toState(reqParams.state);
        rawRaf        = reqParams.rawRaf;
        raf           = reqParams.raf;
        budget        = reqParams.budget;
        deadline      = iso8601("deadline", reqParams.deadline);
        datasource    = reqParams.datasource;
        patientList   = reqParams.patientList;
        docFilterList = reqParams.docFilterList;
    }

    public String toString()
    {
        return ("[HccDto " +
                "; name=" + name +
                "; description=" + description +
                "; type=" + type +
                "; organizationID=" + organizationID +
                "; patientDataSetID=" + patientDataSetID +
                "; dosStart=" + dosStart +
                "; dosEnd=" + dosEnd +
                "; paymentYear=" + paymentYear +
                "; sweep=" + sweep +
                "; passType=" + passType +
                "; status=" + status +
                "; state=" + state +
                "; rawRaf=" + rawRaf +
                "; raf=" + raf +
                "; budget=" + budget +
                "; deadline=" + deadline +
                "; datasource=" + datasource +
                "; patientList=" + patientList +
                "; docFilterList=" + docFilterList +
                "]");
    }
}
