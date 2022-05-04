package com.apixio.useracct.entity;

import java.util.ArrayList;
import java.util.List;

import com.apixio.ObjectTypes;
import com.apixio.XUUID;
import com.apixio.restbase.entity.NamedEntity;
import com.apixio.restbase.entity.ParamSet;
import com.apixio.utility.StringList;

/**
 * Role is a role specific to an Organization.
 */
public class Role extends NamedEntity {

    public static final String OBJTYPE = ObjectTypes.ROLE;

    public static final String CUSTOMEROPS = "CUSTOMEROPS";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String QALEAD = "QALEAD";

    // HCC roles
    public static final String CLIENT_SUPERVISOR = "CLIENT_SUPERVISOR";
    public static final String REVIEWER = "REVIEWER";
    public static final String VENDOR_SUPERVISOR = "VENDOR_SUPERVISOR";
    public static final String ORG_SUPERVISOR = "ORG_SUPERVISOR";
    public static final String APIXIO_NON_PHI = "APIXIO_NON_PHI";

    /**
     * Fields
     */
    private final static String F_ROLE_ROLESET    = "roleSet";  // XUUID of RoleSet that is the "parent" of this Role
    private final static String F_ROLE_PRIVILEGES = "privileges";

    /**
     * Actual fields
     */
    private XUUID           roleSet;
    private List<Privilege> privileges;

    /**
     *
     */
    public Role(ParamSet paramSet)
    {
        super(paramSet);

        this.roleSet     = XUUID.fromString(paramSet.get(F_ROLE_ROLESET));
        this.privileges  = restorePrivileges(paramSet.get(F_ROLE_PRIVILEGES));
    }

    public Role(XUUID roleSet, String name)
    {
        super(OBJTYPE, name);

        this.roleSet     = roleSet;
        this.privileges  = new ArrayList<>();
    }

    /**
     *
     */
    public XUUID getRoleSet()
    {
        return roleSet;
    }

    public List<Privilege> getPrivileges()
    {
        return new ArrayList<Privilege>(privileges);
    }

    public void addPrivilege(Privilege p)
    {
        privileges.add(p);
    }

    public void setPrivileges(List<Privilege> ps)  // this is the only way to delete...
    {
        privileges = ps;
    }

    /**
     *
     */
    @Override
    public void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_ROLE_ROLESET,    roleSet.toString());
        fields.put(F_ROLE_PRIVILEGES, persistPrivileges());
    }

    /**
     * Convert Privilege POJO to a string so it can be saved to redis.
     */
    private String persistPrivileges()
    {
        List<String> privs = new ArrayList<String>();

        for (Privilege p : privileges)
            privs.add(p.persist());

        return StringList.flattenList(privs);
    }

    /**
     * Reverse persistPrivileges.
     */
    private List<Privilege> restorePrivileges(String p)
    {
        List<Privilege> privs = new ArrayList<Privilege>();

        if (p != null)
        {
            for (String priv : StringList.restoreList(p))
                privs.add(Privilege.restore(priv));
        }

        return privs;
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return ("[Role " + super.toString() +
                "; name=" + getName() +
                "; desc=" + getDescription() +
                "; roleSet=" + roleSet +
                "; privs=" + privileges +
                "]");
    }
}
