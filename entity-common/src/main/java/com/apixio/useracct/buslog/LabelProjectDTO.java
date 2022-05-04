package com.apixio.useracct.buslog;

import com.apixio.useracct.entity.LabelProject;
import com.apixio.useracct.entity.Project;

public class LabelProjectDTO extends ProjectDTO
{

    public LabelProject.State state;                        // optional

    /**
     * Convenient and central method to transfer fields that are set in the DTO
     * over to the entity itself.  Only modifiable fields can be added here.
     */
    protected void dtoToEntity(LabelProject project)
    {
        super.dtoToEntity(project);
        if (state != null) project.setState(state);
    }

    @Override
    public void legacyFill(Project.ProjectParamsLegacy reqParams, Boolean forCreate)
    {
        super.legacyFill(reqParams, forCreate);
        state = ProjectProperties.toLabelState(reqParams.state);
    }

}
