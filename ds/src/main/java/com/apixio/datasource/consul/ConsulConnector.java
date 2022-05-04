package com.apixio.datasource.consul;

import java.util.List;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;

public class ConsulConnector
{
    private Consul consul;

    public ConsulConnector(ConsulConfig consulCfg)
    {
        Consul.Builder builder = Consul.builder()
            .withPing(true)
            .withUrl(consulCfg.getUrl())
            .withConnectTimeoutMillis(consulCfg.getConnTimeout())
            .withReadTimeoutMillis(consulCfg.getReadTimeout());

        if (consulCfg.getToken() != null && consulCfg.getToken().trim().length() != 0)
            builder.withAclToken(consulCfg.getToken().trim());

        consul = builder.build();

        List<String> dcs = consul
                .catalogClient()
                .getDatacenters();

        if ( (consulCfg.getDataCenter() != null && consulCfg.getDataCenter().trim().length() != 0) && !dcs.contains(consulCfg.getDataCenter().trim()))
            throw new RuntimeException("Consul Data center " + consulCfg.getDataCenter() + " not in list of available Data centers: " + dcs);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> consul.destroy()));
    }

    public Consul getConsul()
    {
        return consul;
    }

    public KeyValueClient getClient()
    {
        return consul.keyValueClient();
    }
}
