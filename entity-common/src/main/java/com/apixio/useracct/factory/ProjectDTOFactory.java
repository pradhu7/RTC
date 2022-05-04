package com.apixio.useracct.factory;

import com.apixio.useracct.buslog.*;
import com.apixio.useracct.entity.Project;

public class ProjectDTOFactory {
    public static ProjectDTO getDTO(Project.ProjectClass projectClass)
    {
        switch (projectClass) {
            case HCC:
                return new HccProjectDTO();
            case QUALITY:
                return new QualityProjectDTO();
            case PROSPECTIVE:
                return new ProspectiveProjectDTO();
            case LABEL:
                return new LabelProjectDTO();
            default:
                throw new IllegalArgumentException(String.format("Input projectclass: %s is not valid to generate DTO", projectClass));
        }
    }
}
