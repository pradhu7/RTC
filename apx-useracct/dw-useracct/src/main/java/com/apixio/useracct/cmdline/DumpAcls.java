package com.apixio.useracct.cmdline;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import com.apixio.SysServices;
import com.apixio.aclsys.buslog.*;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.entity.*;

/**
 */
public class DumpAcls extends CmdlineBase
{
    private static final String cUsage = ("Usage:  DumpAcls -c <conn-yamlfile>\n\n");

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        String       connectionYamlFile;

        Options(String connectionYaml)
        {
            this.connectionYamlFile = connectionYaml;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    // ################################################################

    private static PrivSysServices   sysServices;

    private static AclLogic          aclLogic;
    private static OrganizationLogic orgLogic;
    private static ProjectLogic      prjLogic;
    private static RoleLogic         roleLogic;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options            options   = parseOptions(new ParseState(args));
        ConfigSet          config    = null;
        List<Organization> orgs;
        List<Project>      prjs;

        if ((options == null) ||
            ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        sysServices = setupServices(config);

        aclLogic  = sysServices.getAclLogic();
        orgLogic  = sysServices.getOrganizationLogic();
        prjLogic  = sysServices.getProjectLogic();
        roleLogic = sysServices.getRoleLogic();

        try
        {
            orgs = orgLogic.getAllOrganizations(true);
            prjs = prjLogic.getAllProjects();

            System.out.println(">>>>>>>>>>>>>>>>>>>>>>> Dumping ACLs for " + orgs.size() + " organization(s)");
            for (Organization org : orgs)
                System.out.println(roleLogic.dumpOrgPermissions(org.getID()));

            System.out.println("\n\n>>>>>>>>>>>>>>>>>>>>>>> Dumping ACLs for " + prjs.size() + " project(s)");
            for (Project prj : prjs)
                System.out.println(roleLogic.dumpProjPermissions(prj.getID()));

            System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>> Dumping low level ACLs");
            System.out.println(aclLogic.dumpAclPermissions());

            System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>> Dumping UserGroups[MEMBEROF]");
            System.out.println(sysServices.getUserGroupLogicExt(SysServices.UGDAO_MEMBEROF).dumpUserGroups());

            System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>> Dumping UserGroups[ACL]");
            System.out.println(sysServices.getUserGroupLogicExt(SysServices.UGDAO_ACL).dumpUserGroups());
        }
        finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");

        if (connection == null)
            return null;

        return new Options(connection);
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
