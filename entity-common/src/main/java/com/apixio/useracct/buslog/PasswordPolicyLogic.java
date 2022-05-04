package com.apixio.useracct.buslog;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.apixio.restbase.LogicBase;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.useracct.entity.PasswordPolicy;

/**
 * Handles password policy-specific business logic.
 */
public class PasswordPolicyLogic extends LogicBase<SysServices> {

    /**
     * CriteriaCheck captures the pass/fail status for each of the various types
     * of constraints on passwords.
     */
    public static class CriteriaCheck {
        public boolean ok;           // true if all is good
        public boolean minCharsOk;
        public boolean maxCharsOk;
        public boolean minLowerOk;
        public boolean minUpperOk;
        public boolean minDigitsOk;
        public boolean minSymbolsOk;
        public boolean nameInPassword;
        public boolean noReuseOk;
    }

    /**
     * PasswordHistory represents a single time a password was changed and includes
     * both the time (epoch ms) that it was changed and the brcrypt hash of the new
     * password.
     */
    public static class PasswordHistory {
        public long   dateChangedMs;
        public String hashedPassword;

        public PasswordHistory(long dateChangedMs, String hashedPassword)
        {
            this.dateChangedMs  = dateChangedMs;
            this.hashedPassword = hashedPassword;
        }
    }

    /**
     * The various types of PasswordPolicy management failure.
     */
    public enum FailureType {
        /**
         * for createPasswordPolicy
         */ 
        DUPLICATE_NAME
    }

    /**
     * If role operations fail they will throw an exception of this class.
     */
    public static class PasswordPolicyException extends BaseException {

        private FailureType failureType;

        public PasswordPolicyException(FailureType failureType)
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
     * Constructor.
     */
    public PasswordPolicyLogic(SysServices sysServices)
    {
        super(sysServices);
    }

    /**
     * Refreshes the cache of roles as necessary and returns the role with the
     * given name, if any.
     */
    public PasswordPolicy getPasswordPolicy(String policyName)
    {
        return sysServices.getPasswordPolicies().findPasswordPolicy(policyName);
    }
    public PasswordPolicy getPasswordPolicy(XUUID id)
    {
        return sysServices.getPasswordPolicies().findPasswordPolicyByID(id);
    }

    /**
     * Return a list of all PasswordPolicies known by the system.
     */
    public List<PasswordPolicy> getAllPasswordPolicies()
    {
        return sysServices.getPasswordPolicies().getAllPasswordPolicies();
    }

    /**
     * Creates a new password policy with the given unique name.
     */
    public PasswordPolicy createPasswordPolicy(PasswordPolicy policy)
    {
        if (policy.getName() == null)
            throw PasswordPolicyException.badRequest();
        else if (getPasswordPolicy(policy.getName()) != null)
            throw new PasswordPolicyException(FailureType.DUPLICATE_NAME);

        sysServices.getPasswordPolicies().create(policy);

        return policy;
    }

    /**
     * Modify the PasswordPolicy object by setting the properties to the values in the modifications
     * parameter.
     */
    public void modifyPasswordPolicy(
        PasswordPolicy  policy
        )
    {
        PasswordPolicy byName = getPasswordPolicy(policy.getName());  // trying to handle a rename

        if ((byName == null) || !policy.getID().equals(byName.getID()))
            throw new PasswordPolicyException(FailureType.DUPLICATE_NAME);

        sysServices.getPasswordPolicies().update(policy);
    }

    /**
     * Tests the password against the policy and returns either OK or a list of
     * criteria that were succesfully passed.
     */
    public CriteriaCheck checkPassword(PasswordPolicy policy, String password, CanonicalEmail email, List<PasswordHistory> history)
    {
        CriteriaCheck cc = new CriteriaCheck();
        int           n;

        password = password.trim();

        cc.minCharsOk   = ((n = policy.getMinChars())   > 0) ? (password.length() >= n) : true;
        cc.maxCharsOk   = ((n = policy.getMaxChars())   > 0) ? (password.length() <= n) : true;
        cc.minLowerOk   = ((n = policy.getMinLower())   > 0) ? (lower(password)   >= n) : true;
        cc.minUpperOk   = ((n = policy.getMinUpper())   > 0) ? (upper(password)   >= n) : true;
        cc.minDigitsOk  = ((n = policy.getMinDigits())  > 0) ? (digits(password)  >= n) : true;
        cc.minSymbolsOk = ((n = policy.getMinSymbols()) > 0) ? (symbols(password) >= n) : true;

        cc.noReuseOk      = checkReuse(policy, password, history);
        cc.nameInPassword = checkNameInPassword(policy, password, email);

        cc.ok = (cc.minCharsOk && cc.maxCharsOk && cc.minLowerOk && cc.minUpperOk &&
                 cc.minDigitsOk && cc.minSymbolsOk && cc.noReuseOk && cc.nameInPassword);

        return cc;
    }

    /**
     * Checks that the "no reuse of passwords" constraints are met.  Returns true if
     * constraints pass.  The password history list is assumed to be in time order with
     * the oldest password at history[0] and the current at history[history.length - 1].
     */
    private boolean checkReuse(PasswordPolicy policy, String password, List<PasswordHistory> history)
    {
        boolean passes  = true;
        int     noReuse = policy.getNoReuseCount();

        if (noReuse > 0)
        {
            int i = history.size() - 1;

            // history[history.size - 1] is most recent (i.e., current password if password has ever been set)

            while ((i >= 0) && (noReuse > 0))
            {
                if (BCrypt.checkpw(password, history.get(i).hashedPassword))     // yes, this is slow; no other way i know of to securely store this info...
                {
                    passes = false;
                    break;
                }

                i--;
                noReuse--;
            }
        }

        if (passes && (noReuse = policy.getNoReuseDays()) > 0)
        {
            //???
        }

        return passes;
    }

    /**
     * Returns false iff the policy prohibits the user name (taken as the local part of the email address)
     * from being part of the password AND it actually is part of the password.
     */
    private boolean checkNameInPassword(PasswordPolicy policy, String password, CanonicalEmail email)
    {
        return !(policy.getNoUserID() && (password.toLowerCase().indexOf(email.getLocalName()) != -1));
    }

    /**
     * Returns the count of lowercase characters in the string
     */
    private static int lower(String str)
    {
        int n = 0;

        for (int p = 0, mx = str.length(); p < mx; p++)
        {
            if (Character.isLowerCase(str.charAt(p)))
                n++;
        }

        return n;
    }

    /**
     * Returns the count of uppercase characters in the string
     */
    private static int upper(String str)
    {
        int n = 0;

        for (int p = 0, mx = str.length(); p < mx; p++)
        {
            if (Character.isUpperCase(str.charAt(p)))
                n++;
        }

        return n;
    }

    /**
     * Returns the count of digits in the string
     */
    private static int digits(String str)
    {
        int n = 0;

        for (int p = 0, mx = str.length(); p < mx; p++)
        {
            if (Character.isDigit(str.charAt(p)))
                n++;
        }

        return n;
    }

    /**
     * Symbol is not easily defined in Unicode, so we'll call it anything other than
     * a letter or digit.
     */
    private static int symbols(String str)
    {
        int n = 0;

        for (int p = 0, mx = str.length(); p < mx; p++)
        {
            char ch = str.charAt(p);

            if (!Character.isLetter(ch) && !Character.isDigit(ch))
                n++;
        }

        return n;
    }


}
