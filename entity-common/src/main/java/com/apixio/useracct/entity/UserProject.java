package com.apixio.useracct.entity;

import java.util.ArrayList;
import java.util.List;

import com.apixio.XUUID;
import com.apixio.restbase.entity.NAssoc;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.utility.StringList;

/**
 * UserProject records an association between a UserID and a ProjectID and
 * adds an "active" flag and a list of "phases".
 */
public class UserProject extends NAssoc {

    public final static String ASSOC_TYPE = "userproj";

    /**
     * Field names (used to store in Redis)
     */
    private final static String F_UPA_ACTIVE   = "active";
    private final static String F_UPA_PHASES   = "phases";   // csv list of strings

    /**
     * Actual fields
     */
    private boolean      active;
    private XUUID        userID;  // repeated/cached (it's also in super.elements)
    private XUUID        projID;  // repeated/cached
    private List<String> phases;

    /**
     * Construct a new UserProject with the given userID and projectID.
     */
    public UserProject(XUUID userID, XUUID projID)
    {
        super(ASSOC_TYPE, makeElementList(userID, projID));
        
        this.userID = userID;
        this.projID = projID;
        this.phases = new ArrayList<String>();
    }

    /**
     * Reconstruct a UserProject from persisted form.
     */
    public UserProject(ParamSet fields)
    {
        super(fields);

        this.active = Boolean.valueOf(fields.get(F_UPA_ACTIVE));
        this.phases = StringList.restoreList(fields.get(F_UPA_PHASES));

        userID = XUUID.fromString(getElements().get(0), User.OBJTYPE);  // 0 must match what's done in makeElementList
        projID = XUUID.fromString(getElements().get(1));                // 1 must match what's done in makeElementList
    }

    /**
     *
     */
    public static List<String> makeElementList(XUUID userID, XUUID projID)
    {
        List<String> elements = new ArrayList<>(2);

        elements.add(userID.toString());
        elements.add(projID.toString());

        return elements;
    }

    /**
     * Getters
     */
    public boolean isActive()
    {
        return active;
    }
    public XUUID getUserID()
    {
        return userID;
    }
    public XUUID getProjectID()
    {
        return projID;
    }
    public List<String> getPhases()
    {
        return phases;
    }

    /**
     * Setters
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }
    public void setPhases(List<String> phases)
    {
        this.phases = (phases == null) ? (new ArrayList<String>()) : phases;
    }

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_UPA_ACTIVE, Boolean.toString(active));
        fields.put(F_UPA_PHASES, StringList.flattenList(phases));
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[UserProject " + super.toString() +
                ";userID=" + userID +
                ";projID=" + projID +
                ";active=" + active +
                ";phases=" + phases +
                "]");
    }
}
