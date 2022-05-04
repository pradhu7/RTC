package com.apixio.restbase.apiacl.acctrole;

import java.util.List;
import java.util.Map;

import com.apixio.SysServices;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.ApiCheck;
import com.apixio.restbase.apiacl.ProtectedApi;
import com.apixio.restbase.apiacl.RestEnforcer;

public class RestAccountRoleEnforcer implements RestEnforcer {

    @Override
    public void init(SysServices sysServices)
    {
    }

    @Override
    public ApiCheck createApiCheck(InitInfo initInfo, ProtectedApi api, BooleanOp op, List<Map<String, String>> configs)
    {
        if (configs.size() > 1)
            throw new IllegalArgumentException("RestAccountRoleEnforcer supports testing only 1 item");

        Map<String, String> config = configs.get(0);

        // config should consist of a single key=value, where the key is a CSV list of state names
        // and value is either "allow" or "deny"

        if (config.size() != 1)
            throw new IllegalArgumentException("state-config object MUST have exactly 1 key, but has:  " + config);

        for (Map.Entry<String, String> ele : config.entrySet())
            return new ApiCheckRole(api, ele.getKey(), ele.getValue());

        return null;
    }

}
