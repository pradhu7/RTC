package com.apixio.useracct.eprops;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.eprops.EntityProperties;
import com.apixio.restbase.eprops.EntityProperty;
import com.apixio.useracct.entity.AccountState;
import com.apixio.useracct.entity.User;

/**
 * UserProperties defines/centralizes the set of properties that are exposed via RESTful methods
 * in a way where the transfer of them can be done automatically while still allowing name flexibility
 * at each level of the system.
 */
public class UserProperties {

    /**
     * Modifying a user is a little twisted as we need to support updating subsets
     * of the fields and we need to specially handle passwords.  This class defines
     * fields for ALL things that can be updated and includes the special pseudo-field
     * needed for password update (the "current password" field).
     */
    public static class ModifyUserParams {
        // automatically handled properties:
        public String firstName;
        public String lastName;
        public String middleInitial;
        public String dateOfBirth;     // expected in ISO8601 format:  yyyy-mm-dd; validated by entity
        public String officePhone;
        public String cellPhone;
        public String emailAddr;

        // password is somewhat...complicated
        public String currentPassword;
        public String newPassword;
        public String nonce;
        public String test;     // if non-null then no currentPass is needed, and nothing will be set

        // special properties:
        public Date         createdAt;
        public String       state;  // unfortunately necessary due to EntityProperty model; ignored
        public AccountState accountState;
        public List<String> roles;
        public List<String> allowedRoles;
        public Integer      timeoutOverride;
        public Boolean      needsTwoFactor;

        public void copyFrom(User user)
        {
            autoProperties.copyFromEntity(user, this);
        }

        public void copyFrom(Map<String, String> props)
        {
            autoProperties.copyToModifyParams(props, this);
        }

        public String toString()
        {
            return ("ModifyUser(firstName=" + firstName +
                    ", lastName=" + lastName +
                    ", middleInitial=" + middleInitial +
                    ", dateOfBirth=" + dateOfBirth +
                    ", officePhone=" + officePhone +
                    ", cellPhone=" + cellPhone +
                    ", curPass=" + currentPassword +
                    ", newPass=" + newPassword +
                    ", nonce=" + nonce +
                    ", acctState=" + accountState +
                    ", roles=" + roles +
                    ", allowedRoles=" + allowedRoles +
                    ", timeoutOverride=" + timeoutOverride +
                    ", needsTwoFactor=" + needsTwoFactor +
                    ")");
        }
    }

    /**
     * The list of automatically handled properties of a User.
     */
    public final static EntityProperties<ModifyUserParams, User> autoProperties = new EntityProperties<ModifyUserParams, User>(
        ModifyUserParams.class, User.class,
        (new EntityProperty("createdAt")).readonly(true),
        (new EntityProperty("state")).readonly(true).entityPropertyName("stateStr"),
        new EntityProperty("firstName"),
        new EntityProperty("lastName"),
        new EntityProperty("middleInitial"),
        new EntityProperty("dateOfBirth"),
        new EntityProperty("officePhone"),
        new EntityProperty("cellPhone"),
        new EntityProperty("emailAddress").readonly(true).modifyPropertyName("emailAddr").entityPropertyName("emailAddr")
        );

}
