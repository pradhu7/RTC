package com.apixio.useracct.dw;

import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.useracct.config.*;

public class ServiceConfiguration extends MicroserviceConfig
{
    private EmailConfig               emailConfig;
    private ResetLinkConfig           resetLinkConfig;
    private VerifyLinkConfig          verifyLinkConfig;
    private UnlockLinkConfig          unlockLinkConfig;
    private AuthConfig                authConfig;
    private MessengerConfig           messengerConfig;

    public void setEmailConfig(EmailConfig emailConfig)
    {
        this.emailConfig = emailConfig;
    }
    public EmailConfig getEmailConfig()
    {
        return emailConfig;
    }

    public void setResetLinkConfig(ResetLinkConfig rlConfig)
    {
        this.resetLinkConfig = rlConfig;
    }
    public ResetLinkConfig getResetLinkConfig()
    {
        return resetLinkConfig;
    }

    public void setVerifyLinkConfig(VerifyLinkConfig vlConfig)
    {
        this.verifyLinkConfig = vlConfig;
    }
    public VerifyLinkConfig getVerifyLinkConfig()
    {
        return verifyLinkConfig;
    }

    public void setUnlockLinkConfig(UnlockLinkConfig rlConfig)
    {
        this.unlockLinkConfig = rlConfig;
    }
    public UnlockLinkConfig getUnlockLinkConfig()
    {
        return unlockLinkConfig;
    }

    public void setAuthConfig(AuthConfig authConfig)
    {
        this.authConfig = authConfig;
    }
    public AuthConfig getAuthConfig()
    {
        return authConfig;
    }

    public void setMessengerConfig(MessengerConfig messengerConfig) { this.messengerConfig = messengerConfig; }
    public MessengerConfig getMessengerConfig() { return messengerConfig; }

    @Override
    public String toString()
    {
        return ("Config: " + super.toString());
    }
}
