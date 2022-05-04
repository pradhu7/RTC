package com.apixio.useracct.dao;

import java.util.ArrayList;
import java.util.List;

import com.apixio.Datanames;
import com.apixio.XUUID;
import com.apixio.restbase.dao.OneToMany;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.entity.Project.ProjectClass;
import com.apixio.useracct.entity.QualityProject;

/**
 *
 */
public final class Projects extends CachingBase<Project>
{
    /**
     * Unique "one to many" prefix for recording organization-to-PDS and PDS-to-project
     * relationships.  This MUST NOT CHANGE and MUST BE UNIQUE across all OneToMany uses.
     */
    private static final String PDS_TO_PROJECT_1TOMANY = "pds-to-prj";

    private static OneToMany.OneToManyConfig pdsToProjConfig = new OneToMany.OneToManyConfig(
        PDS_TO_PROJECT_1TOMANY, PatientDataSet.OBJTYPE, null
        );

    /**
     * Embedded OneToMany that manages a project referencing at most one PDS.  This
     * is modeled as a PDS "owning" multiple Projects and a project being "owned"
     * by at most one PDS.
     */
    private OneToMany pdsToProject;

    /**
     * Creates a new Projects DAO instance.
     */
    public Projects(DaoBase seed, DataVersions dv)
    {
        super(seed, dv, Datanames.PROJECTS,
              Project.OBJTYPE_PREFIX + "*",       // "*" indicates to test XUUID with .startsWith() rather than .equals()
              "CP");                              // "CP" is historical for "Customer Project" (ugh).

        pdsToProject = new OneToMany(this.redisOps, makeKey(""), pdsToProjConfig);
    }

    /**
     * Intercept the creation so we can keep track of unassociated PDSs
     */
    @Override
    public void create(Project proj)
    {
        super.create(proj);

        // initial state of project is unowned by anything.
        pdsToProject.recordChild(proj.getID());
    }

    /**
     *
     */
    public List<Project> getAllProjects()
    {
        List<Project> all = new ArrayList<Project>();

        for (XUUID id : super.getAllEntityIDs())
        {
            ParamSet fields = findInCacheByID(id);

            if (fields != null)
                all.add(createByClass(fields));
        }

        return all;
    }

    /**
     * Bulk translate from XUUID to Project, throwing exception if any ID can't be found
     */
    public List<Project> findProjectsByIDs(List<XUUID> ids)
    {
        List<Project> projList = new ArrayList<>();

        cacheLatest();

        for (XUUID id : ids)
        {
            Project proj = findCachedProjectByID(id);

            if (proj == null)
                throw new IllegalArgumentException("Unknown Project id [" + id + "]");

            projList.add(proj);
        }

        return projList;
    }

    /**
     *
     */
    public List<Project> getProjectsByOwningOrg(XUUID orgID)
    {
        List<Project> projs  = new ArrayList<>();
        String        orgStr = orgID.toString();

        for (XUUID id : getAllEntityIDs())
        {
            ParamSet ps = findInCacheByID(id);

            if (Project.eqOwningOrg(orgStr, ps))
                projs.add(createByClass(ps));
        }

        return projs;
    }

    /**
     *
     */
    public List<Project> getProjectsByPatientDataSet(XUUID pdsID)
    {
        List<Project> projs  = new ArrayList<>();
        String        pdsStr = pdsID.toString();

        for (XUUID id : getAllEntityIDs())
        {
            ParamSet ps = findInCacheByID(id);

            if (Project.eqPatientDataSet(pdsStr, ps))
                projs.add(createByClass(ps));
        }

        return projs;
    }

    /**
     *
     */
    @Override
    public void delete(Project orgProject)
    {
        super.delete(orgProject);

        pdsToProject.unrecordChild(orgProject.getID());
    }

    /**
     * Looks for an Project instance with the given ID in the cache.
     */
    public Project findCachedProjectByID(XUUID id)
    {
        return fromParamSet(findInCacheByID(id));
    }

    /**
     * Looks for an Project instance with the given ID in Redis and if found returns
     * a restored Project instance.  Null is returned if the ID is not found.
     */
    public Project findProjectByID(XUUID id)
    {
        return fromParamSet(findByID(id));
    }

    /**
     * Return a list of Project XUUIDs that aren't owned by anything
     */
    public List<XUUID> getUnassociatedProjects()
    {
        return pdsToProject.getDanglingChildren();
    }

    /**
     * Associate the given Proj ID with the otherID, returning false if the PDS ID
     * is already associated.  In that case, the client must first unassociate
     * before reassociating.
     */
    public boolean associateProject(XUUID projID, XUUID pdsID)
    {
        pdsToProject.addChildToParent(pdsID, projID);

        return true;
    }

    /**
     * Remove the association between the PDS ID and whatever it is already associated
     * with.
     */
    public void unassociateProject(XUUID projID)
    {
        pdsToProject.removeChildFromParent(projID);
    }

    /**
     * Get the list of PDS IDs that are associated with the given otherID.
     */
    public List<XUUID> getProjectsAssociatedWithPds(XUUID pdsID)
    {
        return pdsToProject.getChildren(pdsID);
    }

    /**
     * Restore an Project from persisted form.
     */
    private Project fromParamSet(ParamSet fields)
    {
        if (fields != null)
            return createByClass(fields);
        else
            return null;
    }

    /**
     * Use the persisted name of the project class to determine what actual
     * runtime POJO to create.
     */
    private Project createByClass(ParamSet fields)
    {
        ProjectClass projClass = Project.getProjectClass(fields);

        if (projClass == ProjectClass.HCC)
            return new HccProject(fields);
        if (projClass == ProjectClass.QUALITY)
            return new QualityProject(fields);
        if (projClass == ProjectClass.PROSPECTIVE)
            return new ProspectiveProject(fields);
        if (projClass == ProjectClass.LABEL)
            return new LabelProject(fields);
        else
            return null;
    }

}
