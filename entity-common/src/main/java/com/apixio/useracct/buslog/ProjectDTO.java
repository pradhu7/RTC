package com.apixio.useracct.buslog;

import com.apixio.XUUID;
import com.apixio.useracct.entity.Project;

/**
 * Properties that can be supplied when creating or modifying a project.
 */
public class ProjectDTO {
    public String   name;                  // required
    public String   description;           // optional
    public Boolean  status;                // optional
    public XUUID    organizationID;        // required
    public XUUID    patientDataSetID;      // required

    /**
     * Convenient and central method to transfer fields that are set in the DTO
     * over to the entity itself.  Only modifiable fields can be added here.
     */
    protected void dtoToEntity(Project project)
    {
        if (name != null)            project.setName(name);
        if (description != null)     project.setDescription(description);
        if (status != null)          project.setStatus(status);
    }

    public void legacyFill(Project.ProjectParamsLegacy reqParams, Boolean forCreate) {
        name             = reqParams.name;
        description      = reqParams.description;
        status           = reqParams.status;
        if (forCreate) {
            // update Project method NEVER NEVER NEVER can change its orgId and pdsId
            organizationID   = XUUID.fromString(reqParams.organizationID);
            patientDataSetID = XUUID.fromString(reqParams.patientDataSetID);
        }
    }

    public String toString()
    {
        return ("[ProjectDto " +
                "; name=" + name +
                "; description=" + description +
                "; organizationID=" + organizationID +
                "; patientDataSetID=" + patientDataSetID +
                "]");
    }

}
