package com.apixio.useracct.entity;

import com.apixio.XUUID;
import com.apixio.restbase.entity.ParamSet;

public class LabelProject extends Project
{
    public static final String LABEL_TYPE = "LABEL";
    public static final String OBJTYPE_PREFIX = Project.OBJTYPE_PREFIX + LABEL_TYPE;

    private final static String F_LABEL_STATE = "label-state";

    private State state;

    public enum State
    {
        NEW, STARTED, COMPLETED
    }

    public LabelProject(ParamSet fields)
    {
        super(fields);
        String val;

        if ((val = fields.get(F_LABEL_STATE)) != null)
            this.state = State.valueOf(val);
    }

    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        if (state != null)
            fields.put(F_LABEL_STATE, state.toString());
    }

    public LabelProject(String name, XUUID ordID, XUUID pdsID)
    {
        super(ProjectClass.LABEL, name, LABEL_TYPE, LABEL_TYPE, ordID, pdsID);

    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }

    @Override
    public String toString()
    {
        return ("[LabelProject " + super.toString() +
                "]");
    }

}
