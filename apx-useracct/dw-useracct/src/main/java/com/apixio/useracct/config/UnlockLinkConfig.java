package com.apixio.useracct.config;

import com.apixio.useracct.entity.VerifyLink;

/**
 * Created by dnguyen on 2/7/17.
 */
public class UnlockLinkConfig extends VerifyLinkConfig {
    private String clientCareEmail;

    public String getClientCareEmail() {
        return this.clientCareEmail;
    }
    public void setClientCareEmail(String clientCareEmail) {
        this.clientCareEmail = clientCareEmail;
    }
}
