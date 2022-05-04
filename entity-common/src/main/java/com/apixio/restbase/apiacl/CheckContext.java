package com.apixio.restbase.apiacl;

import com.apixio.restbase.apiacl.ApiAcls.LogLevel;
import com.apixio.utility.StringUtil;

/**
 * CheckContext is a per-request object that can be used to pass information
 * up and down the call chain (e.g., for debugging/logging).
 */
public class CheckContext {

    private LogLevel      logLevel;
    private String        disallowedReason = "";
    private StringBuilder detail           = new StringBuilder();
    private AccessInfo    access;

    static class AccessInfo {
        /*
         * These are non-null only for permission based checks
         */
        String  subject;
        String  operation;
        String  object;

        /*
         * Reason should be non-null only if !allowed
         */
        String  reason;
        boolean allowed;

        AccessInfo(boolean allowed, String reason)
        {
            this.allowed = allowed;
            this.reason  = reason;
        }
    }

    CheckContext(LogLevel logLevel)
    {
        this.logLevel = logLevel;

        detail = new StringBuilder();
    }

    public void recordAccess(boolean allowed, String reason, String subject, String operation, String object)
    {
        access = new AccessInfo(allowed, reason);

        access.subject   = subject;
        access.operation = operation;
        access.object    = object;
    }

    public void recordAccess(boolean allowed, String reason)
    {
        access = new AccessInfo(allowed, reason);
    }

    public void recordDetail(String fmt, Object... args)
    {
        if (logLevel != LogLevel.NONE)
        {
            detail.append("APIACL DETAIL:  ");
            detail.append(StringUtil.subargsPos(fmt, args));
            detail.append("\n");
        }
    }

    public void recordWarn(String fmt, Object... args)
    {
        detail.append("APIACL WARN:  ");
        detail.append(StringUtil.subargsPos(fmt, args));
        detail.append("\n");
    }

    public void recordDisallowed(String reason, Object... args)
    {
        disallowedReason = StringUtil.subargsPos(reason, args);
    }

    public AccessInfo getAccess()
    {
        return access;
    }

    public String getDetails()
    {
        if (disallowedReason.length() > 0)
        {
            detail.append("APIACL DETAIL:  Request was DISALLOWED in the end due to:  ");
            detail.append(disallowedReason);
        }

        return detail.toString();
    }

}
