package com.apixio.umcs;

import com.apixio.sdk.DataUriManager;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.UmCreator;
import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.sdk.util.FxEvalParser;

import java.util.Arrays;

/**
 */
public class AlgoPatientPatsetUmCreator implements UmCreator
{
    private FxEnvironment env;

    @Override
    public void setEnvironment(FxEnvironment env)
    {
        this.env = env;
    }

    @Override
    public DataUriManager createUriManager() throws Exception
    {
        ApxQueryDataUriManager dum = new ApxQueryDataUriManager();

        dum.setEnvironment(env);

        // this is pretty fake as the list<person> returned from f(x)impl doesn't
        // depend on any of this stuff.
        // also, the # of args in the parse() results must match the # in the Arrays.asList()
        dum.setPrimaryKeyArgs(FxEvalParser.parse("request('algo'),request('patientuuid'),request('patsetid')"),
                              Arrays.asList(new String[] {"algo", "patientuuid", "patsetid}"}));

        return dum;
    }

}
