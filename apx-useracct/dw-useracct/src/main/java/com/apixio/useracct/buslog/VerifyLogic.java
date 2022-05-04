package com.apixio.useracct.buslog;

import java.util.HashSet;
import java.util.Set;

import com.apixio.restbase.LogicBase;
import com.apixio.XUUID;
import com.apixio.restbase.entity.AuthState;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.User;
import com.apixio.useracct.entity.VerifyLink;
import com.apixio.useracct.entity.VerifyType;
import com.apixio.useracct.util.NonceUtil;
import com.apixio.SysServices;

/**
 * Contains reusable verification level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 */
public class VerifyLogic extends LogicBase<SysServices> {

    private PrivSysServices privSysServices;

    private final static Set<AccountState> VERIFY_NEW_STATES   = new HashSet<>();
    private final static Set<AccountState> VERIFY_RESET_STATES = new HashSet<>();

    static {
        VERIFY_NEW_STATES.add(AccountState.NEW);
        VERIFY_RESET_STATES.add(AccountState.ACTIVE);
        VERIFY_RESET_STATES.add(AccountState.EXPIRED_PASSWORD);
    }

    /**
     * The various types of verification failure.
     */
    public enum FailureType {
        /**
         *
         */ 
        ALREADY_VERIFIED,
        MUST_BE_ACTIVE
    }

    /**
     * If verification operations fail they will throw an exception of this class.
     */
    public static class VerifyException extends BaseException {

        private FailureType failureType;

        public VerifyException(FailureType failureType)
        {
            super(failureType);
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    public static class Verification {
        public Token         partialAuthToken;
        public long          nonce;
        public AccountState  status;
        public boolean       linkUsed;
        public String        emailAddress;
    }

    public static class VerifyInfo {
        public String       emailAddress;
    }

    private static class CommonReturn {
        User         u;
        VerifyLink   l;
        Verification v;
    }

    public VerifyLogic(PrivSysServices sysServices, ServiceConfiguration configuration)
    {
        super(sysServices);

        this.privSysServices = sysServices;
    }

    /**
     * Verify the link by looking up the link and checking the account.
     */
    public Verification verify(
        Token  verifier,  // could be null
        String ipAddress,
        String userAgent,
        XUUID  linkID
        ) throws VerifyException
    {
        CommonReturn cr;

        cr = verifyCommon(verifier, VerifyType.NEW_ACCOUNT, ipAddress, userAgent, linkID,
                         VERIFY_NEW_STATES, FailureType.ALREADY_VERIFIED);

        // No longer updated the account state here anymore this logic is moved after you have set
        // the password successfully and the nounce matched correctly

        return cr.v;
    }

    /**
     * Verify the link by looking up the link and checking the account.
     */
    public Verification verifyResetLink(
        Token  verifier,  // could be null
        String ipAddress,
        String userAgent,
        XUUID  linkID
        ) throws VerifyException
    {
        CommonReturn cr;

        cr = verifyCommon(verifier, VerifyType.RESET_PASSWORD, ipAddress, userAgent, linkID,
                          VERIFY_RESET_STATES, FailureType.MUST_BE_ACTIVE);

        return cr.v;
    }

    /**
     * Verify the link by looking up the link and checking the account.
     */
    public VerifyInfo getVerifyLinkInfo(
        String ipAddress,
        String userAgent,
        XUUID  linkID
        ) throws VerifyException
    {
        VerifyLink   vl = privSysServices.getVerifyLinks().findVerifyLinkByID(linkID);

        // need to log ipAddress, userAgent

        if ((vl != null) && (vl.getVerifyType() == VerifyType.RESET_PASSWORD))
        {
            User  user = privSysServices.getPrivUsers().findUserByID(vl.getUserID());

            if (user != null)
            {
                VerifyInfo vi = new VerifyInfo();

                vi.emailAddress = user.getEmailAddress().toString();

                return vi;
            }
        }

        return null;
    }

    /**
     */
    private CommonReturn verifyCommon(
        Token             verifier,  // could be null
        VerifyType        reqType,
        String            ipAddress,
        String            userAgent,
        XUUID             linkID,
        Set<AccountState> requiredState,
        FailureType       badStateType
        ) throws VerifyException
    {
        VerifyLink   vl = privSysServices.getVerifyLinks().findVerifyLinkByID(linkID);
        CommonReturn cr = new CommonReturn();

        if ((vl != null) && (vl.getVerifyType() == reqType))
        {
            PrivUsers  privUsers = privSysServices.getPrivUsers();
            Token      partial;

            cr.u = privUsers.findUserByID(vl.getUserID());

            if (!requiredState.contains(cr.u.getState()))
                throw new VerifyException(badStateType);
            
            cr.v = new Verification();
            cr.l = vl;

            // make sure link hasn't already been used.  "Used" means that the user has
            // actually changed his/her password.  If the user has set the password, the
            // the User's nonce field will be empty (see PrivUserLogic.setPassword().
            if (reqType == VerifyType.RESET_PASSWORD)
            {
                String nonce = cr.u.getUpdateNonce();

                // nonce can be null (meaning user has never requested a password reset)
                // but if it's "" then that means the password has been set.  a non-empty
                // means that it's in the middle of a password reset.
                cr.v.linkUsed = NonceUtil.passwordWasChanged(cr.u);
            }

            if (!cr.v.linkUsed)
            {
                partial = sysServices.getTokens().createExternalToken();

                partial.setAuthState(AuthState.PARTIALLY_AUTHENTICATED);
                partial.setUserID(cr.u.getID());
                partial.setIpAddress(ipAddress);
                partial.setUserAgent(userAgent);

                sysServices.getTokens().update(partial);

                // setting the password during reset or initial set password requires that
                // the final call to setPassword have a nonce that matches the
                // updateNonce.  This is to try to thwart an attack (& code) that blindly
                // does a setPassword without either the existing password or this nonce.
                // We set the nonce here because the user has clicked on the link that
                // has the VerifyLink ID.
                NonceUtil.setRealNonce(cr.u, cr.l.getNonce());
                privUsers.update(cr.u);

                cr.v.partialAuthToken = partial;
                cr.v.emailAddress     = cr.u.getEmailAddress().toString();
            }

            cr.v.status = cr.u.getState();
            cr.v.nonce  = vl.getNonce();
        }

        return cr;
    }

}
