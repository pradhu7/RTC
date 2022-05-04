package com.apixio.sdk.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.apixio.mcs.RestClient;
import com.apixio.restbase.config.ConfigSet;

/**
 * Centralized utility for MCS client operations
 */
public class McsUtil
{
    /**
     * "Standard" config points via ConfigSet via .yaml.  Expected structure of yaml is
     *
     *  mcs:
     *    serverUrl:  ""
     *    connectTimeoutMs:             nn        # default is 10 seconds
     *    connectionRequestTimeoutMs:   nn        # default is  5 seconds
     *    socketTimeoutMs:              nn        # default is 10 seconds
     *
     */
    public static final String MCS_SERVERURL         = "mcs.serverUrl";
    public static final String MCS_TO_CONNECT        = "mcs.connectTimeoutMs";
    public static final String MCS_TO_SOCKET         = "mcs.socketTimeoutMs";
    public static final String MCS_TO_CONNECTREQUEST = "mcs.connectionRequestTimeoutMs";
    
    /**
     * MCS server info
     */
    public static class McsServerInfo
    {
        public URL mcsServerUrl;        // base
        public int socketTimeoutMs;
        public int connectTimeoutMs;
        public int connectionRequestTimeoutMs;

        /**
         * Create from non-standard config
         */
        public McsServerInfo(URL serverUrl, int socketTimeout, int connectTimeout, int connRequestTimeout)
        {
            this.mcsServerUrl               = serverUrl;
            this.socketTimeoutMs            = socketTimeout;
            this.connectTimeoutMs           = connectTimeout;
            this.connectionRequestTimeoutMs = connRequestTimeout;
        }

        /**
         * Create from standard config with optional override for server for convenience.  The "root" of the
         * config set must be at the parent of the "mcs" key so that a config.getConfigString("mcs") would
         * return a non-null ConfigSet
         */
        public McsServerInfo(ConfigSet config, String serverUrlOverride) throws MalformedURLException
        {
            this(new URL(config.getString(MCS_SERVERURL, serverUrlOverride)),
                 config.getInteger(MCS_TO_CONNECT,       10 * 1000),
                 config.getInteger(MCS_TO_SOCKET,        10 * 1000),
                 config.getInteger(MCS_TO_CONNECTREQUEST, 5 * 1000)
                );
        }
    }

    /**
     * Create a RestClient from the server info
     */
    public static RestClient makeMcsUtil(McsServerInfo serverInfo) throws Exception
    {
        return new RestClient(serverInfo.mcsServerUrl.toURI(),
                              serverInfo.socketTimeoutMs, serverInfo.connectTimeoutMs, serverInfo.connectionRequestTimeoutMs);
    }

}
