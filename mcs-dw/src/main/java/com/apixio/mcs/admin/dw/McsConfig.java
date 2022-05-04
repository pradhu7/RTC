package com.apixio.mcs.admin.dw;

import java.io.IOException;
import java.util.Map;

import com.apixio.restbase.config.BootConfig;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.MicroserviceConfig;

public class McsConfig extends MicroserviceConfig
{
    private ConfigSet  mcsConfig;

    public void setMcsConfig(Map<String, Object> cfg) throws IOException
    {
        mcsConfig = ConfigSet.fromMap(cfg);

        // Ugly (currently) as we need to create BootConfig from props only because snakeyaml
        // doesn't call setters in any predictable order so we can't be guaranteed that
        // super.getBootProps will return anything useful
        mcsConfig.applyBootProperties(BootConfig.fromSystemProps(mcsConfig).getBootProps());
    }
    public ConfigSet getMcsConfig()
    {
        return mcsConfig;
    }

    public String toString()
    {
        return ("McsConfig: " + super.toString() + "; mcsConfig: " + mcsConfig);
    }
}
