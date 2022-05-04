package com.apixio.useracct.buslog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.UserGroupLogic;
import com.apixio.aclsys.dao.UserGroupDao;
import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.dao.Tokens;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.PasswordPolicyLogic.CriteriaCheck;
import com.apixio.useracct.buslog.PasswordPolicyLogic.PasswordHistory;
import com.apixio.useracct.dao.OldPrivRoles;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.email.EmailTemplate;
import com.apixio.useracct.email.Mailer.Envelope;
import com.apixio.useracct.entity.*;
import com.apixio.useracct.eprops.UserProperties.ModifyUserParams;
import com.apixio.useracct.eprops.UserProperties;
import com.apixio.useracct.messager.AWSSNSMessenger;
import com.apixio.useracct.util.NonceUtil;
import com.apixio.useracct.util.OTPUtil;

/**
 * Contains reusable user-account level logic/code that sits above the persistence
 * layer but below the external "access" layer (e.g., the jersey-invoked methods).
 * This level allows more than one type of access into the system, with RESTful
 * access being just one type (command line access is also possible).
 */
public class PrivUserLogic extends UserLogic {

    public final static String SELF_ID = "me";
    private static final int LOCK_CREATE_TIME_MS = 180 * 1000;

    // E.164 is international phone number format https://www.itu.int/rec/T-REC-E.164/en
    private static final String E164_REGEX = "^\\+[1-9]\\d{1,14}$";

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_SEC = 10 * 60;

    private String          urlBase;          // for new account verification links
    private String          resetUrlBase;     // for reset password links
    private String          emailImageUrl;    // for images in the Html emails
    private String          unlockUrlBase;    // for unlock account
    private int             resetTimeout;
    private int             unlockTimeout;
    private PrivSysServices privSysServices;  //!! hack
    private String          clientCareEmail;
    private DistLock        distLock;         // for blocking multiple accounts with same email

    /**
     * ModStatus is return on Modification user props
     */
    public static class ModStatus
    {
        public boolean passwordChanged;
    }
    /**
     * Constructor.
     */
    public PrivUserLogic(PrivSysServices sysServices, ServiceConfiguration configuration)
    {
        super(sysServices);

        this.privSysServices = sysServices;
        this.urlBase         = configuration.getVerifyLinkConfig().getUrlBase();
        this.resetUrlBase    = configuration.getResetLinkConfig().getUrlBase();
        this.emailImageUrl   = configuration.getEmailConfig().getImageBase();
        this.resetTimeout    = configuration.getResetLinkConfig().getLinkTimeout();
        this.unlockUrlBase   = configuration.getUnlockLinkConfig().getUrlBase();
        this.unlockTimeout   = configuration.getUnlockLinkConfig().getLinkTimeout();
        this.clientCareEmail = configuration.getUnlockLinkConfig().getClientCareEmail();
        this.distLock        = new DistLock(sysServices.getRedisOps(), "user-lock-");
    }

    /**
     * create a new account with the given email address.  A side effect of
     * successful creation is an email that is sent to the email address.
     * This email has an activation link that the user must click on in order
     * to verify the email address.  No access is granted to the user until
     * that link is clicked on.
     *
     * API Details:
     *
     *  * must be called with ADMIN role (=> token required)
     */
    public User createUser(String emailAddress, XUUID orgID, OldRole role, boolean verifyEmail, String cellPhone, Boolean needsTwoFactor) throws IOException
    {
        CanonicalEmail    email    = checkEmail(emailAddress);
        OrganizationLogic orgLogic = privSysServices.getOrganizationLogic();
        User user = null;
        Organization org;

        if ((orgID == null) || ((org = privSysServices.getOrganizations().findOrganizationByID(orgID)) == null))
            throw new UserException(FailureType.MISSING_ORG);
        else if (sysServices.getUsers().findUserByEmail(email.toString()) != null)
            throw new UserException(FailureType.EMAILADDR_ALREADYUSED);

        // Verify the phone number
        // if the phone number was provided
        if(cellPhone!=null)
        {
            verifyPhoneNumber(cellPhone);
        }

        String orgEmailKey = "createUser-" + emailAddress;
        String orgEmailLock = distLock.lock(orgEmailKey, LOCK_CREATE_TIME_MS);

        if(orgEmailLock == null)
            throw new UserException(FailureType.CONCURRENT_USER_CREATION);

        System.out.println(String.format("Lock acquired with lock %s and key %s ", orgEmailLock, orgEmailKey));

        user = privSysServices.getPrivUsers().createUser(email, AccountState.NEW, role);

        // needs two factor can be null
        user.setNeedsTwoFactor(needsTwoFactor);
        // set the phone number on the user
        if(cellPhone!=null) {
            user.setCellPhone(cellPhone);
        }
        privSysServices.getPrivUsers().update(user);

        adjustRolesMembership(user, user.getRoleNames(), true);

        orgLogic.addMemberToOrganization(orgID, user.getID());

        if (verifyEmail)
            sendVerificationEmail(user);

        return user;
    }

    /**
     * Allow the initial password to be set w/o sending any emails (that would be sent
     * if the password set was forced).  This is mostly a hack to allow user creation
     * and setting of password to be done without sending out any emails and is therefore
     * useful for automated testing.
     */
    public void setInitialPassword(Token caller, User user, String password) throws IOException
    {
        if (user.getHashedPassword() != null)
            throw new IllegalStateException("Attempting to set initial password on user that already has password set");

        ModifyUserParams mup   = new ModifyUserParams();
        long             nonce = NonceUtil.createNonce();

        // create a nonce and set it on the user and then use it when setting password;
        // this emulates what a user would do 

        NonceUtil.setRealNonce(user, nonce);
        privSysServices.getPrivUsers().update(user);

        mup.newPassword = password;
        mup.nonce       = Long.toString(nonce);

        modifyUser(caller, SELF_ID, user, mup);
    }

    /**
     * A test-only method to delete a user.  It does minimal cleanup of relationships and is
     * meant only as a way to run some basic testing of the code.
     */
    public void deleteUser(XUUID userID) throws IOException
    {
        OrganizationLogic orgLogic  = privSysServices.getOrganizationLogic();
        PrivUsers         privUsers = privSysServices.getPrivUsers();
        User              user      = privUsers.findUserByID(userID);

        // this will NOT clean up
        //  * user properties
        //  * assigned roles (to organizations or projects)

        if (user != null)
        {
            adjustRolesMembership(user, (new ArrayList<String>()), false);  // remove user from all roles lists/groups

            // doing this here is okay as all users are required to belong
            // to an Organization so it's in the business model...
            for (Organization org : orgLogic.getUsersOrganizations(userID))
                orgLogic.removeMemberFromOrganization(org.getID(), userID);

            privUsers.deleteUser(user);
        }
    }

    /**
     * Resends the email that is used to verify the email address (contains a link).
     */
    public User resendVerification(
        String       emailAddress
        )
    {
        CanonicalEmail  email = checkEmail(emailAddress);
        User            user;

        if ((user = sysServices.getUsers().findUserByEmail(email.toString())) == null)
            throw new UserException(FailureType.NO_SUCH_USER);
        else if (user.getState() != AccountState.NEW)
            throw new UserException(FailureType.ALREADY_VERIFIED);

        sendVerificationEmail(user);

        return user;
    }

    /**
     * Resends the email that is used to verify the email address (contains a link).
     */
    public void sendResetPassword(String emailAddress){
        sendResetPassword(emailAddress,"forgotpassword");
    }
    public void sendResetPassword(
        String       emailAddress,
        String       templateName
        )
    {
        CanonicalEmail  email = checkEmail(emailAddress);
        User            user;

        if ((user = sysServices.getUsers().findUserByEmail(email.toString())) == null)
            throw new UserException(FailureType.NO_SUCH_USER);
        else if (!((user.getState() == AccountState.ACTIVE) || (user.getState() == AccountState.EXPIRED_PASSWORD)))
            throw new UserException(FailureType.NOT_ACTIVE);

        sendResetPasswordEmail(user, templateName);
    }

    /**
     * Modify the User object by setting the properties to the values in the modifications
     * parameter.  Note that ALL fields are set (as there's no way to specify a 'use current
     * value' flag), so the caller should prefill with existing values as necessary.
     * @return
     */
    public ModStatus modifyUser(Token caller, String userID, User user, ModifyUserParams modifications) throws IOException
    {
        boolean isExplicitSelf  = SELF_ID.equals(userID);  // meaning:  userID=="me"
        ModStatus modStatus = new ModStatus();
        // presence of modifications.test means to test against password policy only
        
        if (user != null)
        {
            // verify phone number before modification if
            // it was changed
            if (((modifications.cellPhone) != null) &&
                    (!modifications.cellPhone.equals(user.getCellPhone())))
            {
                this.verifyPhoneNumber(modifications.cellPhone);
            }

            //!! should probably be in separate method to allow returning the full passcheck so the
            // RESTful client actually knows what specific bits were okay...
            if (modifications.test != null)
            {
                PasswordPolicy passPolicy = getUserPasswordPolicy(user);
                CriteriaCheck  passCheck;

                passCheck  = (passPolicy != null) ? sysServices.getPasswordPolicyLogic().checkPassword(passPolicy,
                                                                                                       modifications.newPassword,
                                                                                                       user.getEmailAddress(),
                                                                                                       getPasswordHistory(user)) : null;
                if ((passCheck != null) && !passCheck.ok)
                    throw new UserException(FailureType.PASSWORD_POLICY_FAILURE, makePasswordFailureDetails(passCheck));
            }
            else
            {
                boolean setRoles        = ((modifications.roles        != null) && (modifications.roles.size()        > 0));
                boolean setAllowedRoles = ((modifications.allowedRoles != null) && (modifications.allowedRoles.size() > 0));

                // password is a special case (also, note that password can never be cleared)
                if (isExplicitSelf && (modifications.newPassword != null))
                {
                    modStatus.passwordChanged =
                            setPassword(caller, user, false, isExplicitSelf, modifications.currentPassword, modifications.newPassword, modifications.nonce);
                    if (!modStatus.passwordChanged)
                        throw new UserException(FailureType.EXISTING_PASSWORD_MISMATCH);
                }

                // accountState and roles are special cases:  only root can do that
                if ((modifications.accountState != null) || setRoles || setAllowedRoles)
                {
                    boolean isRealSelf;

                    if (modifications.accountState != null)
                    {
                        user.setState(modifications.accountState);
                        if ((modifications.accountState == AccountState.DISABLED) ||
                            (modifications.accountState == AccountState.CLOSED))
                            sysServices.getTokens().deleteTokensForUser(XUUID.fromString(userID), null);
                    }

                    // users can't modify their own roles (safety check so ROOT can't kill the entire system
                    // by removing its own ROOT role)
                    isRealSelf = caller.getUserID().toString().equals(userID);

                    if (!isRealSelf && setAllowedRoles)
                        user.setAllowedRoles(modifications.allowedRoles);

                    if (!isRealSelf && setRoles && canSetRoles(caller, modifications.roles))
                    {
                        // no need to validate modifications.roles as anything out of the known
                        // won't be settable via canSetRoles.

                        adjustRolesMembership(user, modifications.roles, false);

                        user.setRoles(modifications.roles);
                    }
                }

                // NOTE:  field values can be deleted/removed/cleared by setting them to ""
                UserProperties.autoProperties.copyToEntity(modifications, user);

                // below two are not handled by user properties
                user.setNeedsTwoFactor(modifications.needsTwoFactor);
                user.setTimeoutOverride(modifications.timeoutOverride);

                sysServices.getUsers().update(user);
            }
        }
        return modStatus;
    }

    /**
     * This is a privileged (enforced via API ACLs) method that forcefully changes a user's password
     * WITHOUT requiring either the current password or a valid nonce.
     */
    public boolean forcePasswordSet(XUUID userID, String password) throws IOException
    {
        User user = sysServices.getUsers().findUserByID(userID);

        if (user != null)
        {
            EmailTemplate  tpl   = privSysServices.getTemplateManager().getEmailTemplate("passwordforcereset");

            setPassword(null, user, true, false, null, password, null);

            if (tpl != null)
            {
                Envelope            env   = new Envelope();
                Map<String, Object> model = new HashMap<String, Object>();

                env.setSubject(tpl.getSubject(model));
                env.addTo(user.getEmailAddress().toString());

                privSysServices.getMailer().sendEmail(env, tpl.getPlain(model), tpl.getHtml(model));
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * This is a privileged (enforced via API ACLs) method that unsets a
     * user's cellphone number
     */
    public boolean deleteCellPhone(XUUID userID)
    {
        User user = sysServices.getUsers().findUserByID(userID);

        if (user != null)
        {
            user.setCellPhone("");
            privSysServices.getPrivUsers().update(user);

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Return the list of Users that have the given Role.  This list is taken from the
     * indexes kept in Redis, by role, NOT by the UserGroup membership (which is actually
     * managed by the indexes in Redis).
     */
    @Deprecated // but still needed for migration to user-accounts-3.0.0
    public List<User> getUsersInRole(OldRole role)
    {
        List<User> users   = new ArrayList<User>();
        Users      userDAO = privSysServices.getUsers();

        for (String id : privSysServices.getOldRoles().getRoleMembers(role))
            users.add(userDAO.findUserByID(XUUID.fromString(id, User.OBJTYPE)));

        return users;
    }

    /**
     * Makes changes to the indexes that keep track of which users are have which roles.
     */
    private void adjustRolesMembership(User user, List<String> toSet, boolean initial) throws IOException
    {
        List<String>   current   = user.getRoleNames();
        XUUID          userIDx   = user.getID();
        String         userID    = userIDx.toString();
        OldPrivRoles   privRoles = privSysServices.getOldPrivRoles();
        UserGroupLogic ugl       = privSysServices.getUserGroupLogicExt(SysServices.UGDAO_ACL);
        UserGroupDao   ugs       = privSysServices.getUserGroupDaoExt(SysServices.UGDAO_ACL);
        OldRoleLogic   rl        = privSysServices.getOldRoleLogic();

        // drop membership in roles that are in current but are not in toSet:
        if (!initial)
        {
            for (String role : current)
            {
                if (!toSet.contains(role))
                {
                    privRoles.removeUserFromRole(role, userID);
                    ugs.removeMemberFromGroup(ugl.findRoleGroup(rl.getRole(role)), userIDx);
                }
            }
        }

        // add membership in roles that are in toSet but are not in current
        for (String role : toSet)
        {
            if (initial || !current.contains(role))
            {
                privRoles.addUserToRole(role, userID);
                ugs.addMemberToGroup(ugl.findRoleGroup(rl.getRole(role)), userIDx);
            }
        }
    }

    /**
     * Checks if the user represented by the caller is allowed to set all of the roles
     * in rolesToSet.  This check looks that "assignable" roles in each of the roles
     * that the user has.
     */
    private boolean canSetRoles(Token caller, List<String> rolesToSet)
    {
        List<String> userRoles     = getUserRoles(caller.getUserID());
        OldRoleLogic rl            = sysServices.getOldRoleLogic();
        Set<String>  canBeAssigned = new HashSet<String>();
        boolean      canSet        = true;  // note that this assumes rolesToSet.size() > 0

        for (String rn : userRoles)
        {
            OldRole       role  = rl.getRole(rn);
            List<String>  roles = (role != null) ? role.getAssignableRoles() : null;

            if (roles.size() > 0)
                canBeAssigned.addAll(roles);
        }

        for (String roleToSet : rolesToSet)
        {
            if (!canBeAssigned.contains(roleToSet))
            {
                canSet = false;
                break;
            }
        }

        return canSet;
    }

    /**
     * Setting the password requires the following to be true:
     *
     *  1.  the caller (in the token) == the user whose password is being set
     *  2.  either a match existing password is passed or a matching nonce is passed
     */
    private boolean setPassword(Token caller, User user, boolean forced, boolean isSelf, String currentPassword, String newPassword, String nonce) throws IOException
    {
        PrivUsers users        = privSysServices.getPrivUsers();
        String    existingHash = user.getHashedPassword();
        boolean   goodNonce    = NonceUtil.nonceMatches(user, nonce);
        boolean   specExisting = (currentPassword != null) && (currentPassword.length() > 0);  // did caller specify existing password?
        boolean   changed      = false;

        // set the password if password has never been set (and make sure caller didn't
        // try to specify an existing password) OR if the passed-in existing password
        // matches the stored (hashed) one.
        if (forced || goodNonce || ((currentPassword != null) && users.checkPassword(user, currentPassword)))
        {
            PasswordPolicy passPolicy = getUserPasswordPolicy(user);
            CriteriaCheck  passCheck  = (passPolicy != null) ? sysServices.getPasswordPolicyLogic().checkPassword(passPolicy, newPassword,
                                                                                                                  user.getEmailAddress(),
                                                                                                                  getPasswordHistory(user)) : null;
            String         hash;
            Date           last;

            if ((passCheck != null) && !passCheck.ok)
                throw new UserException(FailureType.PASSWORD_POLICY_FAILURE, makePasswordFailureDetails(passCheck));

            hash = user.getHashedPassword();
            last = user.getLastPasswordChange();

            if ((hash != null) && (last != null))
                users.addOldPassword(user, hash, last.getTime());

            users.setUserPassword(user, newPassword);
            NonceUtil.markPasswordChanged(user);

            changed = true;
            user.setLastPasswordChange(System.currentTimeMillis());

            // if the user state is new here after a good nonce and password verified
            // I am setting his state to active
            if ((AccountState.EXPIRED_PASSWORD == user.getState()) || (AccountState.NEW == user.getState()))
                user.setState(AccountState.ACTIVE);

            // All good, we need to reset failed login count
            user.setFailedLogins(0);

            users.update(user);


            // Nonce's are passed in ONLY when the user is partially authenticated:
            //  when setting the initial password and when resetting the password.
            // Handle this by upgrading the external (!) token to fully authenticated...
            // Above implementation is changed to delete the external and internal token
            if (isSelf && goodNonce)
            {
                Tokens tokens = sysServices.getTokens();
                Token exToken = tokens.findTokenByID(caller.getExternalToken());

                if (exToken != null)
                {
                    tokens.delete(exToken);
                    // caller is the internal token
                    tokens.delete(caller);
                }
            }
        }

        return changed;
    }

    /**
     * Creates the "details" Map of the reasons that the set password operation failed (due
     * to password policy restrictions).
     */
    private Map<String, String> makePasswordFailureDetails(
        CriteriaCheck passCheck
        )
    {
        Map<String, String> details = new HashMap<String, String>();

        details.put("minChars",   Boolean.valueOf(passCheck.minCharsOk).toString());
        details.put("maxChars",   Boolean.valueOf(passCheck.maxCharsOk).toString());
        details.put("minUpper",   Boolean.valueOf(passCheck.minUpperOk).toString());
        details.put("minDigits",  Boolean.valueOf(passCheck.minDigitsOk).toString());
        details.put("minSymbols", Boolean.valueOf(passCheck.minSymbolsOk).toString());
        details.put("noReuseOk",  Boolean.valueOf(passCheck.noReuseOk).toString());
        details.put("noUserID",   Boolean.valueOf(passCheck.nameInPassword).toString());

        return details;
    }

    /**
     * Return the detailed history of the password changes.  The list will be empty if no
     * password has ever been set.  The oldest password is at [0] and the current password
     * is at the end of the list.
     */
    private List<PasswordHistory> getPasswordHistory(User user)
    {
        List<PasswordHistory> history = new ArrayList<PasswordHistory>();
        String                hashed  = user.getHashedPassword();
        Date                  last    = user.getLastPasswordChange();

        // time ordered from old to new
        for (String item : user.getOldPasswords())
            history.add(new PasswordHistory(User.getPasswordChangeDateMs(item), User.getPasswordChangeHash(item)));

        // this is most recent, so add last
        if ((hashed != null) && (last != null))
            history.add(new PasswordHistory(last.getTime(), hashed));

        return history;
    }        

    /**
     * Checks the syntax of the email, throwing an exception if it's invalid and
     * returning the CanonicalEmail if valid;
     */
    private CanonicalEmail checkEmail(String emailAddr)
    {
        try
        {
            return new CanonicalEmail(emailAddr);
        }
        catch (IllegalArgumentException iae)
        {
            throw new UserException(FailureType.BAD_EMAIL_SYNTAX);
        }
    }


    /**
     * Creates and sends the new account verification email to the email address
     * of the given User.
     */
    private void sendVerificationEmail(User user)
    {
        VerifyLink          vl    = privSysServices.getVerifyLinks().createNewAccountLink(user.getID());
        EmailTemplate       tpl   = privSysServices.getTemplateManager().getEmailTemplate("newaccount");
        Envelope            env   = new Envelope();
        Map<String, Object> model = new HashMap<String, Object>();

        model.put("verifyUrlBase", urlBase);
        model.put("verifyLink", vl.getID().toString());
        model.put("imageBase",emailImageUrl);

        env.setSubject(tpl.getSubject(model));
        env.addTo(user.getEmailAddress().toString());

        privSysServices.getMailer().sendEmail(env, tpl.getPlain(model), tpl.getHtml(model));
    }

    /**
     * Creates and sends an email to the email address of the given
     * User that contains a link that lets the user reset the account
     * password.
     */
    private void sendResetPasswordEmail(User user, String templateName){
        VerifyLink          vl    = privSysServices.getVerifyLinks().createResetPasswordLink(user.getID(), resetTimeout);
        EmailTemplate       tpl   = privSysServices.getTemplateManager().getEmailTemplate(templateName);
        Envelope            env   = new Envelope();
        Map<String, Object> model = new HashMap<String, Object>();

        model.put("resetUrlBase", resetUrlBase);
        model.put("resetLink", vl.getID().toString());
        model.put("imageBase",emailImageUrl);

        env.setSubject(tpl.getSubject(model));
        env.addTo(user.getEmailAddress().toString());

        NonceUtil.markLinkSent(user);
        privSysServices.getPrivUsers().update(user);

        privSysServices.getMailer().sendEmail(env, tpl.getPlain(model), tpl.getHtml(model));
    }

    /**
     * Creates and sends an email to the email address of the given
     * User to unlock account after a certain number of failed logins
     */
    public void sendUnlockAccountEmail(String emailAddress, String caller)
    {
        sendUnlockAccountEmail(emailAddress, caller, "unlockaccount");
    }

    public void sendUnlockAccountEmail(
        String       emailAddress,
        String       caller,
        String       templateName
        )
    {
        CanonicalEmail  email = checkEmail(emailAddress);
        User            user;

        if ((user = sysServices.getUsers().findUserByEmail(email.toString())) == null)
            throw new UserException(FailureType.NO_SUCH_USER);

        else if (!(user.getState() == AccountState.ACTIVE))
            throw new UserException(FailureType.NOT_ACTIVE);

        sendUnlockAccountEmail(user, caller, templateName);
    }

    private String duration2Text(long duration)
    {
        long seconds = duration % 60;
        duration = duration / 60;
        long minutes = duration % 60;
        duration = duration / 60;
        long hours = duration % 24;
        duration = duration / 24;
        StringBuffer buf = new StringBuffer();

        if (duration == 1)
            buf.append(" 1 day");
        else if (duration > 1)
            buf.append(String.format(" %d days", duration));

        if (hours == 1)
            buf.append(" 1 hour");
        else if (hours > 1)
            buf.append(String.format(" %d hours", hours));

        if (minutes == 1)
            buf.append(" 1 hour");
        else if (minutes > 1)
            buf.append(String.format(" %d minutes", minutes));

        if (seconds == 1)
            buf.append(" 1 second");
        else if (seconds > 1)
            buf.append(String.format(" %d seconds", seconds));

        return buf.toString().trim();
    }

    /**
     * Overloaded functions
     */
    public void sendUnlockAccountEmail(User user) {
        sendUnlockAccountEmail(user, "");
    }

    public void sendUnlockAccountEmail(User user, String caller) {
        sendUnlockAccountEmail(user, caller, "unlockaccount");
    }

    public void sendUnlockAccountEmail(User user, String caller, String templateName)
    {
        VerifyLink          vl    = privSysServices.getVerifyLinks().createUnlockAccountLink(user.getID(), unlockTimeout);
        EmailTemplate       tpl   = privSysServices.getTemplateManager().getEmailTemplate(templateName);
        Envelope            env   = new Envelope();
        Map<String, Object> model = new HashMap<String, Object>();
        VerifyLink          rvl   = privSysServices.getVerifyLinks().createResetPasswordLink(user.getID(), resetTimeout);

        model.put("loginUrlBase", unlockUrlBase);
        model.put("unlockLink", vl.getID().toString());
        model.put("caller", caller == null ? "" : caller);
        model.put("imageBase",  emailImageUrl);
        model.put("expiration", duration2Text(unlockTimeout));
        model.put("resetUrlBase", resetUrlBase);
        model.put("resetLink", rvl.getID().toString());

        env.setSubject(tpl.getSubject(model));
        env.addTo(user.getEmailAddress().toString());

        NonceUtil.markLinkSent(user);
        privSysServices.getPrivUsers().update(user);

        privSysServices.getMailer().sendEmail(env, tpl.getPlain(model), tpl.getHtml(model));
    }

    public void sendAccountLockedNotification(User user, String ipAddress, String userAgent)
    {
        EmailTemplate       tpl   = privSysServices.getTemplateManager().getEmailTemplate("accountlocknotification");
        Envelope            env   = new Envelope();

        if (tpl == null || clientCareEmail == null)
            return;

        env.addTo(clientCareEmail);

        Map<String, Object> model = new HashMap<>();
        model.put("user", user.getEmailAddr());
        model.put("ipaddress", ipAddress);
        model.put("useragent", userAgent);

        env.setSubject(tpl.getSubject(model));
        privSysServices.getMailer().sendEmail(env, tpl.getPlain(model), tpl.getHtml(model));
    }

    public void sendIpAddressNotInWhitelist(User user, String ipAddress)
    {
        EmailTemplate tpl = privSysServices.getTemplateManager().getEmailTemplate("ipaddrnotinwhitelist");

        if (tpl != null)
        {
            Envelope            env   = new Envelope();
            Map<String, Object> model = new HashMap<>();

            env.addTo(user.getEmailAddress().toString());

            model.put("user",      user.getEmailAddr());
            model.put("ipaddress", ipAddress);

            env.setSubject(tpl.getSubject(model));
            privSysServices.getMailer().sendEmail(env, tpl.getPlain(model), tpl.getHtml(model));
        }
    }

    /**
     * Verify phone number if its in E.164 format
     */
    public void verifyPhoneNumber(String cellPhone)
    {
        if ((cellPhone == null) || cellPhone.isEmpty())
        {
            throw new UserException(FailureType.ERR_PHONE_MISSING);
        }
        if(!cellPhone.matches(E164_REGEX))
        {
            UserException ex = new UserException(FailureType.INVALID_PHONE_NUMBER);
            ex.detail("Phone number should of the E.164 form eg. +12133457658 ", cellPhone);
            throw ex;
        }
    }

    /**
     * Creates an OTP and save the OTP in Redis with the key and ttl.
     * Older users might not have a phone number or it might be of invalid format,
     * so we have to check for that
     */
    public void sendOTP(User user, Organization org, boolean resend)
    {
        PrivUsers privUsers    = privSysServices.getPrivUsers();
        String otp             = OTPUtil.createOTP(OTP_LENGTH);

        // check if the have a phone number
        // Also verify if the phone number is of the required format
        String phoneNumber = user.getCellPhone();

        verifyPhoneNumber(phoneNumber);

        privUsers.addOTPByUserWithExpiry(user, otp, OTP_TTL_SEC);

        String message = String.format("APIXIO security code:\n%s\n\n" +
                "Code expires in %d minutes.    Msg and data rates may apply", otp, OTP_TTL_SEC/60);

        if(!resend)
            // deliver messages using the default type
            deliverMessage(user, phoneNumber, message, null);
        else {
            // if resend is true deliver messages for both types
            deliverMessage(user, phoneNumber, message, AWSSNSMessenger.TRANSACTIONAL);
            deliverMessage(user, phoneNumber, message, AWSSNSMessenger.PROMOTIONAL);
        }

    }

    private void deliverMessage(User user, String phoneNumber, String message, String messageType) {
        boolean delivered = privSysServices.getMessenger().sendMessage(message, phoneNumber, messageType);

        if(!delivered)
        {
            throw BaseException.badRequest("Message sending failed for " + user.getEmailAddr() + " with phone number " + user.getCellPhone());
        }
    }

}
