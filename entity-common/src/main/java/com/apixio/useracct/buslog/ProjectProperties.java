package com.apixio.useracct.buslog;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.restbase.util.DateUtil;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.entity.HccProject.PassType;
import com.apixio.useracct.entity.HccProject.State;
import com.apixio.useracct.entity.HccProject.Sweep;

/**
 * ProjectProperties handles the outbound (back to REST client) conversion
 * of Project entity.  It also exposes some support functions for inbound
 * conversion from String to enums.
 */
public class ProjectProperties {

    /**
     * External string values for Sweep and PassType
     */
    private final static String SWEEP_INITIAL = "initial";
    private final static String SWEEP_MIDYEAR = "midYear";
    private final static String SWEEP_FINAL   = "finalReconciliation";

    private final static String PASS_FIRST    = "firstPass";
    private final static String PASS_SECOND   = "secondPass";

    private final static String STATE_NEW       = "New";
    private final static String STATE_BUNDLED   = "Bundled";
    private final static String STATE_STARTED   = "Started";
    private final static String STATE_COMPLETED = "Completed";

    /**
     * The list of automatically handled properties of a Project
     */
    private final static E2Properties<Project> baseProperties = new E2Properties<>(
            Project.class,
            (new E2Property("createdAt")).readonly(true),
            new E2Property("name"),
            new E2Property("description"),
            (new E2Property("type")).readonly(true),
            (new E2Property("organizationID")).readonly(true),
            (new E2Property("patientDataSetID")).readonly(true),
            (new E2Property("status")).readonly(true)
    );

    // just add delta from general properties
    private final static E2Properties<HccProject> hccProperties = new E2Properties<>(
            HccProject.class,
            new E2Property("paymentYear"),
            new E2Property("rawRaf"),
            new E2Property("raf"),
            new E2Property("budget"),
            new E2Property("datasource"),
            new E2Property("patientList"),
            new E2Property("docFilterList")
    );

    private final static E2Properties<QualityProject> qualityProperties = new E2Properties<>(
            QualityProject.class,
            new E2Property("measurementYear"),
            new E2Property("measureList"),
            new E2Property("lineOfBusiness"),
            new E2Property("measureDateRangeOverride"),
            new E2Property("measureEligibilityFiltering"),
            new E2Property("eligibleMeasurePrograms"),
            new E2Property("eligibleComplianceStatus"),
            new E2Property("budget"),
            new E2Property("datasource")
    );

    private final static E2Properties<ProspectiveProject> prospectiveProperties = new E2Properties<>(
            ProspectiveProject.class,
            new E2Property("historicalYears"),
            new E2Property("patientList"),
            new E2Property("paymentYear")
    );

    /**
     * Mappings between internal and external for Sweep and PassType
     */
    private static Map<String, Sweep>    sweepTo     = new HashMap<>();
    private static Map<Sweep, String>    sweepBk     = new HashMap<>();

    private static Map<String, PassType> passTypesTo = new HashMap<>();
    private static Map<PassType, String> passTypesBk = new HashMap<>();

    private static Map<String, State>    statesTo = new HashMap<>();
    private static Map<State,  String>   statesBk = new HashMap<>();

    private static Map<String, QualityProject.State> qualityStatesTo = new HashMap<>();
    private static Map<QualityProject.State, String> qualityStatesBk = new HashMap<>();

    private static Map<String, ProspectiveProject.State> prospectiveStatesTo = new HashMap<>();
    private static Map<ProspectiveProject.State, String> prospectiveStatesBk = new HashMap<>();

    private static Map<String, LabelProject.State> labelStatesTo = new HashMap<>();
    private static Map<LabelProject.State, String> labelStatesBk = new HashMap<>();

    static
    {
        sweepTo.put(SWEEP_INITIAL,  Sweep.INITIAL);
        sweepTo.put(SWEEP_MIDYEAR,  Sweep.MID_YEAR);
        sweepTo.put(SWEEP_FINAL,    Sweep.FINAL_RECONCILIATION);

        sweepBk.put(Sweep.INITIAL,              SWEEP_INITIAL);
        sweepBk.put(Sweep.MID_YEAR,             SWEEP_MIDYEAR);
        sweepBk.put(Sweep.FINAL_RECONCILIATION, SWEEP_FINAL);

        passTypesTo.put(PASS_FIRST,  PassType.FIRST_PASS);
        passTypesTo.put(PASS_SECOND, PassType.SECOND_PASS);

        passTypesBk.put(PassType.FIRST_PASS,  PASS_FIRST);
        passTypesBk.put(PassType.SECOND_PASS, PASS_SECOND);

        statesTo.put(STATE_NEW,       State.NEW);
        statesTo.put(STATE_BUNDLED,   State.BUNDLED);
        statesTo.put(STATE_COMPLETED, State.COMPLETED);

        statesBk.put(State.NEW,       STATE_NEW);
        statesBk.put(State.BUNDLED,   STATE_BUNDLED);
        statesBk.put(State.COMPLETED, STATE_COMPLETED);

        qualityStatesTo.put(STATE_NEW,       QualityProject.State.NEW);
        qualityStatesTo.put(STATE_STARTED,   QualityProject.State.STARTED);
        qualityStatesTo.put(STATE_COMPLETED, QualityProject.State.COMPLETED);

        qualityStatesBk.put(QualityProject.State.NEW,       STATE_NEW);
        qualityStatesBk.put(QualityProject.State.STARTED,   STATE_STARTED);
        qualityStatesBk.put(QualityProject.State.COMPLETED, STATE_COMPLETED);

        prospectiveStatesTo.put(STATE_NEW,       ProspectiveProject.State.NEW);
        prospectiveStatesTo.put(STATE_STARTED,   ProspectiveProject.State.STARTED);
        prospectiveStatesTo.put(STATE_COMPLETED, ProspectiveProject.State.COMPLETED);

        prospectiveStatesBk.put(ProspectiveProject.State.NEW,       STATE_NEW);
        prospectiveStatesBk.put(ProspectiveProject.State.STARTED,   STATE_STARTED);
        prospectiveStatesBk.put(ProspectiveProject.State.COMPLETED, STATE_COMPLETED);

        labelStatesTo.put(STATE_NEW,         LabelProject.State.NEW);
        labelStatesTo.put(STATE_STARTED,     LabelProject.State.STARTED);
        labelStatesTo.put(STATE_COMPLETED,   LabelProject.State.COMPLETED);

        labelStatesBk.put(LabelProject.State.NEW,         STATE_NEW);
        labelStatesBk.put(LabelProject.State.STARTED,     STATE_STARTED);
        labelStatesBk.put(LabelProject.State.COMPLETED,   STATE_COMPLETED);
    }

    /**
     * Conversions from external strings to various enums.
     */
    public static Sweep toSweep(String name)
    {
        return (name != null) ? sweepTo.get(name) : null;
    }
    public static PassType toPassType(String name)
    {
        return (name != null) ? passTypesTo.get(name) : null;
    }
    public static State toState(String name)
    {
        return (name != null) ? statesTo.get(name) : null;
    }

    public static QualityProject.State toQualityState(String name)
    {
        return (name != null) ? qualityStatesTo.get(name) : null;
    }

    public static ProspectiveProject.State toProspectiveState(String name)
    {
        return (name != null) ? prospectiveStatesTo.get(name) : null;
    }

    public static LabelProject.State toLabelState(String name)
    {
        return (name != null) ? labelStatesTo.get(name) : null;
    }
    /**
     * Convenience method to create a Map that can be easily translated to, say, a JSON
     * object. 
     */
    public static Map<String, Object> toMap(Project proj)
    {
        return toMap(proj, null, null);
    }

    public static Map<String, Object> toMap(Project proj, Organization org, PatientDataSet pds)
    {
        if (proj.getProjectClass() == Project.ProjectClass.HCC)
            return hccToMap((HccProject) proj, org, pds);
        if (proj.getProjectClass() == Project.ProjectClass.QUALITY)
            return qualityToMap((QualityProject) proj, org, pds);
        if (proj.getProjectClass() == Project.ProjectClass.PROSPECTIVE)
            return prospectiveToMap((ProspectiveProject) proj, org, pds);
        if (proj.getProjectClass() == Project.ProjectClass.LABEL)
            return labelToMap((LabelProject) proj, org, pds);
        else
            return baseToMap(proj, org, pds);
    }

    private static Map<String, Object> baseToMap(Project proj, Organization org, PatientDataSet pds)
    {
        Map<String, Object> map =  baseProperties.getFromEntity(proj);

        map.put("id", proj.getID().toString());

        if (org != null)
            map.put("organizationName", org.getName());

        if (pds != null)
        {
            map.put("pdsName",       pds.getName());
            map.put("pdsExternalID", pds.getCOID());
        }

        return map;
    }

    private static Map<String, Object> prospectiveToMap(ProspectiveProject proj, Organization org, PatientDataSet pds)
    {
        Map <String, Object> map = baseToMap(proj, org, pds);
        ProspectiveProject.State st;
        Date                dt;

        map.putAll(prospectiveProperties.getFromEntity(proj));

        if ((st = proj.getState()) != null)
            map.put("state", prospectiveStatesBk.get(st));

        if ((dt = proj.getDeadline()) != null)
            map.put("deadline", DateUtil.dateToIso8601(dt));

        return map;
    }

    private static Map<String, Object> hccToMap(HccProject proj, Organization org, PatientDataSet pds)
    {
        Map<String, Object> map = baseToMap(proj, org, pds);
        Date                dt;
        State               st;

        map.putAll(hccProperties.getFromEntity(proj));

        map.put("sweep",    sweepBk.get(proj.getSweep()));
        map.put("passType", passTypesBk.get(proj.getPassType()));

        if ((dt = proj.getDosStart()) != null)
            map.put("dosStart", DateUtil.dateToIso8601(dt));      // reasonable enough to use iso8601

        if ((dt = proj.getDosEnd()) != null)
            map.put("dosEnd",   DateUtil.dateToIso8601(dt));

        if ((st = proj.getState()) != null)
            map.put("state", statesBk.get(st));

        if ((dt = proj.getDeadline()) != null)
            map.put("deadline", DateUtil.dateToIso8601(dt));

        return map;
    }

    private static Map<String, Object> qualityToMap(QualityProject proj, Organization org, PatientDataSet pds)
    {
        Map<String, Object>  map = baseToMap(proj, org, pds);
        Date                 dt;
        QualityProject.State st;

        map.putAll(qualityProperties.getFromEntity(proj));

        if ((st = proj.getState()) != null)
            map.put("state", qualityStatesBk.get(st));

        if ((dt = proj.getDeadline()) != null)
            map.put("deadline", DateUtil.dateToIso8601(dt));

        return map;
    }

    private static Map<String, Object> labelToMap(LabelProject proj, Organization org, PatientDataSet pds)
    {
        Map<String, Object>  map = baseToMap(proj, org, pds);
        LabelProject.State st;

        if ((st = proj.getState()) != null)
            map.put("state", labelStatesBk.get(st));

        return map;
    }
}
