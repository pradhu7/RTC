package com.apixio.useracct.buslog;

import com.apixio.ConfigConstants;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.AuthState;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.entity.TokenType;
import com.apixio.restbase.web.BaseException;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.config.AuthConfig;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.dao.VerifyLinks;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.dw.resources.AuthRS;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.util.NonceUtil;
import com.apixio.useracct.util.PhoneUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Contains reusable authentication logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 */
public class AuthLogic extends LogicBase<SysServices>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthLogic.class);

    /**
     * Defined interpretations of the return value from checkPasswordExpiration
     */
    private final static long EXPIRES_NEVER = -1L;
    private final static long EXPIRED       =  0L;
    private final static int  UUID_LENGTH   = 36;
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String VERIFICATION = "VERIFICATION";

    /**
     * internalIpAddrs holds the pattern that defines if an incoming request is from
     * an internal machine.
     */
    private Pattern internalIpAddrs;

    /**
     * Default TTL for internal tokens returned for internal network requests.
     */
    private int internalAuthTTL;

    /**
     * Max # of failed logins allowed before setting AUTO_LOCKOUT
     */
    private int maxFailedLogins;

    /**
     * Increasing delay factor:  delaySeconds = failedLoginDelayFactor * numberOfFailedLogins
     */
    private double failedLoginDelayFactor;

    /**
     * To handle distributed lock using redis
     */
    private DistLock distLock;

    /**
     * DAOs
     */
    private VerifyLinks verifyLinks;

    /**
     * Business logic objects
     */
    private AclLogic aclLogic;

    /**
     * Authentication is the structure returned on a successful authentication.  It
     * contains ancillary details on the authentication.
     */
    public static class Authentication
    {
        public Token   token;
        public boolean needsTwoFactor;
        public boolean needsNewPassword;
        public boolean passwordExpired;
        public long    passwordExpiresIn;  // in minutes
        public long    expiredNonce;
        public String partialPhoneNumber;
        public String phoneMissing;
    }

    /**
     * Structure returned on a successful verification
     */
    public static class Verification
    {
        public Token token;
        public boolean twoFactorCodeValid;
        public long passwordExpiresIn;
        public boolean passwordExpired;
    }

    /**
     * Structure for returning the auth state of a internal token
     */
    public static class AuthenticationStatus
    {
        public boolean authenticated;
    }

    /**
     * The various types of authentication failure (so many ways to fail).
     */
    public enum FailureType
    {
        /**
         * Actual authentication failed:  either unknown email address or password mismatch
         */ 
        AUTH_FAILED,

        /**
         * These indicate that while the emailaddr/password were good, the user can't log in
         * due to some account-related condition:
         *
         *  ACCOUNT_NEW:  user has not verified the email for the account
         *  ACCOUNT_LOCKEDOUT:  too many failed attempts to log in has locked the account
         *  ACCOUNT_DISABLED:  administrator has disabled the account; this should be temporary
         *  ACCOUNT_CLOSED:  administrator has permanently closed the account
         */
        ACCOUNT_NEW,
        ACCOUNT_LOCKEDOUT,
        ACCOUNT_DISABLED,
        ACCOUNT_CLOSED,

        /**
         * Request was for an internal token but the IP address of the request was not from
         * an internal machine.
         */
        NOT_INTERNALIP,
        TOO_MANY_REQUEST,

        /**
         * Miscellaneous
         */
        DISALLOWED_IPADDR,

        /**
         * if the token is not password authenticated before two factor verification
         */
        TOKEN_NOT_AUTHENTICATED,

        /**
         * if the two code is invalid
         */
        INVALID_TWO_FACTOR_CODE,

        /**
         * if the password is expired during two factor verification
         */
        PASSWORD_EXPIRED,

        /**
         *  if the token is null then the user is invalid
         */
        INVALID_USER;
    }

    /**
     * If authentication fails it will throw an exception of this class.
     */
    public static class AuthenticationException extends BaseException
    {
        private FailureType failureType;

        public AuthenticationException(FailureType failureType)
        {
            super(failureType);
            this.failureType = failureType;
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * Constructor.  The Java regex pattern that defines the internal IP addresses is
     * required configuration.
     */
    public AuthLogic(PrivSysServices sysServices, ServiceConfiguration configuration)
    {
        super(sysServices);

        AuthConfig ac = configuration.getAuthConfig();

        this.maxFailedLogins        = ac.getMaxFailedLogins();
        this.failedLoginDelayFactor = ac.getFailedLoginDelayFactor();
        this.internalIpAddrs        = Pattern.compile(ac.getInternalIP());
        this.internalAuthTTL        = ac.getInternalAuthTTL();
        this.distLock               = new DistLock(sysServices.getRedisOps(), sysServices.getRedisKeyPrefix());
        this.verifyLinks            = sysServices.getVerifyLinks();
        this.aclLogic               = sysServices.getAclLogic();
    }

    /**
     * Attempt to authenticate a user.  If authentication is successful, meaning that the email
     * address was found and the supplied password got bcrypted to the stored hashed password,
     * and there were no reasons to disallow the user from logging in, an Authentication object
     * is returned.
     *
     * Any failure will result in an exception being thrown.
     */
    public Authentication authenticateUser(
        AuthRS.AuthenticationParams params,
        String                      ipAddress,                // if via a proxy/LBA, this will be a csv
        String                      userAgent,
        boolean                     internalRequest,
        int                         internalTTL               // 0 to use default
        ) throws AuthenticationException, IOException
    {
        if ((params.email == null) || (params.password == null))
            throw AuthenticationException.badRequest("Missing email address or password");

        Users             users       = sysServices.getUsers();
        User              user        = users.findUserByEmail(new CanonicalEmail(params.email).toString());
        OrganizationLogic orgLogic    = sysServices.getOrganizationLogic();
        PrivUserLogic privUserLogic   = ((PrivSysServices)sysServices).getPrivUserLogic();
        boolean           expired     = false;
        VerifyLink        unlockLink  = null;
        long              expiresIn;
        int               failedCount; 
        AccountState      state;
        Token             token;
        Authentication    auth;
        boolean needsTwoFactor;

        if (user == null)
            throw new AuthenticationException(FailureType.AUTH_FAILED);
        else if ((state = user.getState()) == AccountState.NEW)
            throw new AuthenticationException(FailureType.ACCOUNT_NEW);
        else if (state == AccountState.AUTO_LOCKOUT)
            throw new AuthenticationException(FailureType.ACCOUNT_LOCKEDOUT);
        else if (state == AccountState.DISABLED)
            throw new AuthenticationException(FailureType.ACCOUNT_DISABLED);
        else if (state == AccountState.CLOSED)
            throw new AuthenticationException(FailureType.ACCOUNT_CLOSED);

        Organization org       = orgLogic.getUsersOrganization(user.getID());
        needsTwoFactor         = isTwoFactorEnabled(user, org);

        int          maxFailed = org.getMaxFailedLogins() != null ? org.getMaxFailedLogins() : maxFailedLogins;

        if (user.getFailedLogins() >= maxFailed)
        {
            if ((params.code == null) || (params.code.length() < UUID_LENGTH)) // SAFE CHECK
                throw new AuthenticationException(FailureType.ACCOUNT_LOCKEDOUT);

            // validate token
            XUUID linkID = null;

            try
            {
                linkID = XUUID.fromString(params.code, VerifyLink.OBJTYPE);
            }
            catch (IllegalArgumentException e)
            {
                throw new AuthenticationException(FailureType.ACCOUNT_LOCKEDOUT);
            }

            unlockLink = verifyLinks.findVerifyLinkByID(linkID);

            if ((unlockLink == null)
                || (unlockLink.getVerifyType() != VerifyType.UNLOCK_ACCOUNT)
                || unlockLink.getUsed()
                || !unlockLink.getUserID().equals(user.getID()))
                throw new AuthenticationException(FailureType.ACCOUNT_LOCKEDOUT);
        }

        if (!isAllowedIpAddress(org.getIpAddressWhitelist(), ipAddress))
        {
            AuthenticationException ex = new AuthenticationException(FailureType.DISALLOWED_IPADDR);

            privUserLogic.sendIpAddressNotInWhitelist(user, ipAddress);

            ex.detail("orgId", org.getID().toString());

            LOGGER.warn("IP whitelist failure for user " + user.getEmailAddress() + ", org " + org.getID() + "; bad IP [" + ipAddress + "]");

            throw ex;
        }

        String lockKey = user.getID().toString() + "-" +
                VerifyType.UNLOCK_ACCOUNT.toString() + "-" + AUTHENTICATION;

        boolean authenticated = lockAuthsProcess(AUTHENTICATION, user, maxFailed, lockKey, params, null);

        if (!authenticated)
            throw new AuthenticationException(FailureType.AUTH_FAILED);

        if ((expiresIn = checkPasswordExpiration(user)) == EXPIRED)
        {
            user.setState(AccountState.EXPIRED_PASSWORD);
            expired = true;
        }

        user.setFailedLogins(0);
        users.update(user);

        // Remove unlock token only when successfully logged in
        if (unlockLink != null)
        {
            verifyLinks.delete(unlockLink);
        }

        if (internalRequest)
        {
            if (!ipAddrAllowed(ipAddress))
                throw new AuthenticationException(FailureType.NOT_INTERNALIP);

            token = sysServices.getTokens().createInternalToken(user.getID(),
                                                                (internalTTL > 0) ? internalTTL : internalAuthTTL);
        }
        else
        {
            Integer timeout    = user.getTimeoutOverride();
            int     orgTimeout = org.getActivityTimeoutOverride();   // 0 is default and means no override

            token = sysServices.getTokens().createExternalToken();

            // HACK for now:  if user is demo user, then allow a long activity
            // better long term solution is to have "timeout" field on user
            if (isDemoUser(user)) //!! replace !!  unit is 'seconds'
                token.setInactivityOverride(60 * 60 * 24); // 24 hours

            // timeout set on the user takes precedence
            if (timeout != null)
                token.setInactivityOverride(timeout.intValue());
            else if (orgTimeout > 0)
                token.setInactivityOverride(orgTimeout);

            // if account/org configured to require 2nd factor, send it out.
            // use IPAddr to override this behavior:  if inside firewall then don't 2nd-factor it
        }

        auth = new Authentication();
        auth.token = token;
        auth.passwordExpiresIn = expiresIn;

        token.setUserID(user.getID());
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);

        if(needsTwoFactor)
        {
            // by now password verification was successful
            // so if there is no phone number we want a successful
            // auth but return the error code for phone missing
            // only send an OTP when account sate is active
            if((user.getState() == AccountState.ACTIVE) || (user.getState() == AccountState.EXPIRED_PASSWORD)) {
                try {
                    privUserLogic.sendOTP(user, org, false);
                    // user should have a verified phone number by now
                    auth.partialPhoneNumber = PhoneUtil.getLast2DigitsPhNo(user.getCellPhone());
                } catch (UserLogic.UserException ex) {
                    auth.phoneMissing = ex.getDetails().get("reason");
                }
            }
            token.setAuthState(AuthState.PASSWORD_AUTHENTICATED);
        }
        else
        {
            if(user.getState() == AccountState.ACTIVE)
                token.setAuthState(AuthState.AUTHENTICATED);
            else
                token.setAuthState(AuthState.PASSWORD_AUTHENTICATED);
        }

        sysServices.getTokens().update(token);

        if (!allowMultipleSessions(user))
            sysServices.getTokens().deleteTokensForUser(user.getID(), token.getID());

        if (token.getTokenType() == TokenType.EXTERNAL)
        {
            // setting .needs2ndFactor as necessary
            auth.needsTwoFactor = needsTwoFactor;

            // check initial condition AND check password policies...
            auth.needsNewPassword = (user.getHashedPassword() == null) || expired;
            auth.passwordExpired  = expired;

            if (expired)
            {
                auth.expiredNonce = NonceUtil.createNonce();
                NonceUtil.setRealNonce(user, auth.expiredNonce);
                users.update(user);
            }
        }

        return auth;
    }

    /**
     * User.2FA trumps all the time if its null then org.2FA takes precedence
     */
    private boolean isTwoFactorEnabled(User user, Organization org) {
        if (user.getNeedsTwoFactor() != null) {
            return user.getNeedsTwoFactor();
        } else {
            return org.getNeedsTwoFactor();
        }
    }

    /**
     *
     * @param processName can be either
     *                    AUTHENTICATION = check Password or
     *                    VERIFICATION = verify otp
     * @return
     */
    private boolean lockAuthsProcess(String processName, User user, int maxFailed,
                                     String lockKey, AuthRS.AuthenticationParams authenticationParams, String inputOTP)
    {
        int failedCount;
        String validOTP = null;

        PrivSysServices sysServices = (PrivSysServices) this.sysServices;
        PrivUsers privUsers = sysServices.getPrivUsers();

        String lock    = distLock.lock(lockKey, (long) (failedLoginDelayFactor * 1000));

        if (lock == null)
            throw new AuthenticationException(FailureType.TOO_MANY_REQUEST);

        boolean success = false;
        boolean accountLocked = false;

        try
        {
            if (processName.equals(AUTHENTICATION))
                success = privUsers.checkPassword(user, authenticationParams.password);


            if (processName.equals(VERIFICATION)) {
                validOTP = privUsers.findOTPByUser(user);
                success = inputOTP.equals(validOTP);
            }

            // SOFT-LOCK ACCOUNT
            if (!success)
            {
                failedCount = user.getFailedLogins() + 1;
                user.setFailedLogins(failedCount);

                // TODO: SCOTT TO UPDATE THIS
                // ABORT ALL ACTIONS UP TO NOW AND START A NEW TRANSACTION
                this.sysServices.getRedisTransactions().abort();
                this.sysServices.getRedisTransactions().begin();

                privUsers.update(user);
                // SEND UNLOCK EMAIL IF EXCEED MAX FAILED LOGINS
                if (failedCount >= maxFailed)
                {
                    if(processName.equals(AUTHENTICATION))
                        sysServices.getPrivUserLogic().sendUnlockAccountEmail(user, authenticationParams.caller);
                    if(processName.equals(VERIFICATION))
                        sysServices.getPrivUserLogic().sendUnlockAccountEmail(user);

                    accountLocked = true;
                }

                this.sysServices.getRedisTransactions().commit(); // COMMIT OPERATIONS FOR SOFT-LOCK
                this.sysServices.getRedisTransactions().begin();  // START A NEW TRANSACTION
            }
        }
        finally
        {
            distLock.unlock(lock, lockKey);
        }

        if (accountLocked)
            throw new AuthenticationException(FailureType.ACCOUNT_LOCKEDOUT);

        return success;
    }

    /**
     *There are two thing we test here if the token is passsword authenticated
     * and if user has correct otp if these conditions are satisfied we stamp the
     * token as fully authenticated
     */
    public Verification verifyTwoFactor(BaseRS.ApiLogger logger, String code, Token itoken) throws IOException
    {
        Tokens tokens        = sysServices.getTokens();
        PrivUsers privUsers  = ((PrivSysServices)sysServices).getPrivUsers();

        if(itoken == null)
        {
            logger.addParameter("internalToken", "null");
            throw new AuthenticationException(FailureType.INVALID_USER);
        }
        Token xToken  = tokens.findTokenByID(itoken.getExternalToken());
        logger.addParameter("externalToken.state", xToken.getAuthState());

        // verify that the token is password authenticated
        // is this necessary since we are also verifying this in validateToken class ??
        // anyway lets make sure
        if(xToken.getAuthState() != AuthState.PASSWORD_AUTHENTICATED)
        {
            throw new AuthenticationException(FailureType.TOKEN_NOT_AUTHENTICATED);
        }

        User user = sysServices.getUsers().findUserByID(xToken.getUserID());

        // Verify if the account is not locked
        if (user.getFailedLogins() >= maxFailedLogins)
            throw new AuthenticationException(FailureType.ACCOUNT_LOCKEDOUT);

        // verify the otp
        String lockKey = user.getID().toString() + "-" +
                VerifyType.UNLOCK_ACCOUNT.toString() + "-" + VERIFICATION;
        boolean validOTP = lockAuthsProcess(VERIFICATION, user, maxFailedLogins, lockKey, null, code);

        if(!validOTP)
        {
            throw new AuthenticationException(FailureType.INVALID_TWO_FACTOR_CODE);
        }

        // need to delete the token after use
        privUsers.deleteOTPByUser(user);
        // update the login back to zero after success
        user.setFailedLogins(0);
        privUsers.update(user);

        // get expiry time for the external token
        long expiry = checkPasswordExpiration(user);
        Verification verification = new Verification();

        if (expiry != EXPIRED)
        {
            xToken.setAuthState(AuthState.AUTHENTICATED);
            verification.passwordExpired = false;
        }
        else
        {
            verification.passwordExpired = true;
        }

        // update the token
        tokens.update(xToken);
        verification.twoFactorCodeValid = true;
        verification.token              = xToken;
        verification.passwordExpiresIn  = expiry;

        return verification;
    }


    /**
     *  authenticated
     *  => true: valid token, 2FA not required (AUTHENTICATED token)
     *  => true: valid token, 2FA verified (AUTHENTICATED token)
     *  => false: valid token, 2FA not verified (PASSWORD_AUTHENTICATED token)
     *  => false: invalid token/logged out (internal token is null)
     *  => false: partially authenticated, locked out, etc.  anything not already true.
     */
    public AuthenticationStatus getAuthenticationStatus(BaseRS.ApiLogger logger, Token iToken)
    {
        AuthenticationStatus authenticationStatus = new AuthenticationStatus();
        if(iToken == null)
        {
            logger.addParameter("itoken", "null");
            authenticationStatus.authenticated = false;
            return authenticationStatus;
        }

        Token xToken = sysServices.getTokens().findTokenByID(iToken.getExternalToken());

        logger.addParameter("xtoken", xToken.getID().toString());

        authenticationStatus.authenticated = (xToken != null) && (xToken.getAuthState() == AuthState.AUTHENTICATED);

        return authenticationStatus;
    }
    /**
     * Tests if the given IP address falls within the configured IP address whitelist
     * for the organization.  If the organization doesn't have such a list, then true
     * is returned.  If the list is empty then that's interpreted as not having a list.
     *
     * Note that the client IP address could actually be a csv list of addresses, as
     * the X-Forwarded-For "spec" is to append proxy addresses.  To support this
     * case we look only at the first element
     */
    private static boolean isAllowedIpAddress(String[] whitelist, String ipAddress)
    {
        if ((whitelist != null) && (whitelist.length > 0))
        {
            int     comma = ipAddress.indexOf(',');
            boolean match = false;

            if (comma != -1)
                ipAddress = ipAddress.substring(0, comma).trim();  // trim just in case poorly formatted

            for (String cidr : whitelist)
            {
                IpAddressMatcher subnet = new IpAddressMatcher(cidr);

                if (subnet.matches(ipAddress))
                {
                    match = true;
                    break;
                }
            }

            if (!match)
                return false;
        }

        return true;
    }

    /**
     * Returns -1 if there is no password expiration time for the user, 0 if the
     * password has expired, or a positive number that indicates how many minutes
     * left until the password expires.
     *
     * The policy in effect is taken from the UserOrg that the user is a member of first, and
     * the global policy second.
     */
    private long checkPasswordExpiration(User user) throws IOException
    {
        PasswordPolicy policy = sysServices.getUserLogic().getUserPasswordPolicy(user);
        long           timeMs;
        Date           last;

        if ((policy == null) || ((timeMs = policy.getExpirationTime()) == 0L))
            return EXPIRES_NEVER;

        last = user.getLastPasswordChange();

        if (last == null)
            return EXPIRED;

        timeMs = last.getTime() + timeMs - System.currentTimeMillis();

        if (timeMs <= 0L)
            return EXPIRED;
        else
            return timeMs / 60000L;  // milliseconds in a minute
    }

    /**
     * Return true if the IP address matches the pattern for internal IP addresses.
     */
    private boolean ipAddrAllowed(String ip)
    {
        return internalIpAddrs.matcher(ip).matches();
    }

    /**
     * Returns true if the user is allowed to 
     */
    private boolean allowMultipleSessions(User user) throws IOException
    {
        return aclLogic.hasPermission(user.getID(), ConfigConstants.ALLOWMULTIPLESESSIONS_OPERATION, AclLogic.ANY_OBJECT);
    }

   /**
    * Returns true if the user is a demo user.
    *
    * TODO: Identify the user as a demo user with a better approach, i.e. ACL role or group
    */
    private boolean isDemoUser(User user)
    {
      return user.getEmailAddress().getEmailAddress().endsWith("demo@apixio.com");
    }

    /**
     * Test only
     */
    public static void main(String... args)
    {
        // arg[0] is CIDR, rest are IPs to test against it
        String[] whitelist = new String[] { args[0] };

        for (int i = 1; i < args.length; i++)
        {
            String ip = args[i];

            System.out.println(" IP [" + ip + "] in [" + args[0] + "]:  " + isAllowedIpAddress(whitelist, ip));
        }
    }

}
