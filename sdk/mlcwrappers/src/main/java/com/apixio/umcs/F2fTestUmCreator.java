package com.apixio.umcs;

import java.util.Arrays;

import com.apixio.sdk.DataUriManager;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.UmCreator;
import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.sdk.util.FxEvalParser;

/**
 */
public class F2fTestUmCreator implements UmCreator
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
        dum.setPrimaryKeyArgs(FxEvalParser.parse("'f2f',request('docuuid')"),   // "docuuid" is set up by TestLoadAndRun
                              Arrays.asList(new String[] {"algo", "docuuid"}));     // this "docuuid" doesn't need to match above; it's put in returned URI

        return dum;
    }

}
