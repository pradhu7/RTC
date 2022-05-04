package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.yaml.snakeyaml.Yaml;

import com.apixio.useracct.cmdline.RoleSetsProxy.RoleSetInfo;
import com.apixio.useracct.cmdline.RoleSetsProxy.RoleInfo;
import com.apixio.useracct.cmdline.RoleSetsProxy.RolePrivilege;
import com.apixio.useracct.cmdline.ParseState.NVPair;
import com.apixio.useracct.email.CanonicalEmail;

/**
 * RbacToolCmd allows management of role-based access control specifics via
 * a command line interface.  Invoking the command results in a little command
 * shell where the real commands are entered.
 *
 * Usage:
 *
 *  * to list all role sets:  rolesets
 *  * to create a role set:   newroleset {nameID} type={type} name={name} description={description}
 *  * to create a role:       newrole name=roleset/rolename description=abc 
 *  * to update role perms:   addpriv/delpriv role privileges=...
 *
 * Privileges are specified via a list of triplets of [true/false, operation, acltarget], where
 * the true/false is for the "forMember" value.  Example:
 *
 *    privileges=(true,ManageSystem,*),(false,CanViewDocs,*),(false,ManageProject,target-proj)
 *
 * (the parsing allows including "forMember=", etc. before the value for clarity).  The entire
 * name=value can be enclosed in double-quotes to protect white spaces also.
 *
 * Yaml config file contains redis connection configuration in a simple "name: value" structure:
 *
 *  tokenizerServer:  http://localhost:8075
 *  userAcctServer:   http://localhost:8076
 *
 * This command tool uses the RESTful API instead of directly accessing redis data (via
 * privileged Java code).
 */
public class RbacToolCmd {

    private static final String cShellUsage = (
                                          "\trolesets\tlist all rolesets\n" +
                                          "\troleset\t\tlist one roleset\n" +
                                          "\tnewroleset\tcreate a new roleset\n" +
                                          "\tnewrole\t\tcreate a new role\n" +
                                          "\taddpriv\t\tadd new privilege on existing role\n" +
                                          "\tdelpriv\t\tremove privilege on existing role\n"
        );

    private static final String cUsage = ("Usage:  RbacToolCmd -c <yamlfile> <cmd>, where cmd is one of:\n\n" +
                                          cShellUsage +
                                          "\tshell\t\t(enter interactive shell)\n\n" +
                                          "This tool requires a valid name/password for a User that has ManageSystem privileges on '*'.  " +
                                          "It must be invoked in a real shell (due to password prompting limitations)."
        );

    private enum Command {
        LIST_ROLESETS,
        LIST_ROLESET,
        ADD_ROLESET,
        ADD_ROLE,
        ADD_PRIV,
        DEL_PRIV,
        SHELL
    }

    /**
     * The option/commands supported.
     */
    private static final String LISTROLESETS_CMD  = "rolesets";
    private static final String LISTROLESET_CMD   = "roleset";
    private static final String ADDROLESET_CMD    = "newroleset";
    private static final String ADDROLE_CMD       = "newrole";
    private static final String ADDPRIV_CMD       = "addpriv";
    private static final String DELPRIV_CMD       = "delpriv";
    private static final String SHELL_CMD         = "shell";

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        Command              command;
        String               yamlFile;
        String               roleSet;
        String               roleName;
        List<RolePrivilege>  addDelPrivs;
        Map<String, String>  roleSetParams;
        Map<String, Object>  roleParams;

        Options(String yaml)
        {
            this.yamlFile = yaml;
        }

        public String toString()
        {
            return ("[opt command=" + command +
                    "; yaml=" + yamlFile +
                    "]");
        }
    }

    /**
     * Actual connection/proxy for talking to user-account service.
     */
    private static AuthProxy     authProxy;
    private static String        authToken;
    private static RoleSetsProxy roleSetsProxy;

    // ################################################################

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options      options = parseOptions(new ParseState(args), true);
        Config       config;

        if ((options == null) ||
            ((config = readConfig(options.yamlFile)) == null) ||
            !loginUser(config))
        {
            usage();
            System.exit(1);
        }

        try
        {
            dispatch(options, true);
        }
        finally
        {
            //!! it's not worth the effort to delete the (internal) token:  we've authenticated 
            //   and received an INTERNAL token but the logout RESTful API requires that we pass
            //   in the EXTERNAL token (since it's longer lived than an internal one).  If we
            //   really want to do the logout, then we'd need to auth to get an external token
            //   and then exchange it for an internal one to make the *Proxy calls...

            //logoutUser();
        }
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

    /**
     * Dispatch the parsed command.
     */
    private static void dispatch(Options options, boolean allowShell) throws Exception
    {
        switch (options.command)
        {
            case LIST_ROLESETS:
            {
                listRolesets(options);
                break;
            }
            case LIST_ROLESET:
            {
                listRoleset(options);
                break;
            }
            case ADD_ROLESET:
            {
                break;
            }
            case ADD_ROLE:
            {
                doAddRole(options);
                break;
            }

            case ADD_PRIV:  // fall through
            case DEL_PRIV:
            {
                doAddDelPrivilege(options);
                break;
            }

            case SHELL:
            {
                if (allowShell)
                    shell();
                break;
            }
        }
    }

    /**
     * Enter an interactive shell (to avoid having to login in every time).
     */
    private static void shell() throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\nThe interactive shell lets you enter the base commands without having to log in each time." +
                           "\nEnter as you would on command line except for no '-c yaml'." +
                           "\n'?' for help.  EOF, 'quit', or ^C to exit.\n");

        while (true)
        {
            String       line;
            List<String> parsed;

            System.out.print("rbactool> ");
            line = br.readLine();

            if (line == null)
                break;

            try
            {
                parsed = LineParser.parseLine(line);

                if (parsed.size() > 0)
                {
                    String first = parsed.get(0);

                    if (first.startsWith("#"))
                    {
                        System.out.println(line);
                    }
                    else if (first.equalsIgnoreCase("quit"))
                    {
                        break;
                    }
                    else if (first.equals("?") || first.equalsIgnoreCase("help"))
                    {
                        System.out.println(cShellUsage);
                    }
                    else
                    {
                        Options options = parseOptions(new ParseState(parsed), false);

                        if (options == null)
                            System.out.println("Invalid command/bad parse");
                        else
                            dispatch(options, false);
                        }
                }
            }
            catch (Exception x)
            {
                x.printStackTrace();
                System.out.println(x.getMessage());
            }
        }
    }

    private static void listRolesets(Options options) throws Exception
    {
        RoleSetInfo[] rsInfo = roleSetsProxy.getAllRoleSets(authToken);

        for (RoleSetInfo rsi : rsInfo)
            displayRoleSet(rsi);
    }

    private static void listRoleset(Options options) throws Exception
    {
        RoleSetInfo rsInfo = roleSetsProxy.getRoleSet(authToken, options.roleSet);

        if (rsInfo != null)
            displayRoleSet(rsInfo);
    }

    private static void displayRoleSet(RoleSetInfo rsi)
    {
        System.out.println("\n#####################################################");
        System.out.println("Role set '" + rsi.nameID + "'");
        System.out.println("    ID:  " + rsi.id);
        System.out.println("  type:  " + rsi.type);
        System.out.println("  name:  " + rsi.name);
        System.out.println("  desc:  " + rsi.description);
        System.out.println(" roles:");

        for (RoleInfo ri : rsi.roles)
        {
            boolean first = true;

            System.out.println("       ################");
            System.out.println("       Role '" + ri.name + "'");
            System.out.println("              id:  " + ri.id);
            System.out.print  ("           privs:  ");

            for (RolePrivilege rp : ri.privileges)
            {
                if (!first)
                    System.out.print(";");

                first = false;

                System.out.print("(forMember=" + rp.forMember +
                                   ",operation=" + rp.operation +
                                   ",aclTarget=" + rp.aclTarget + ")");
            }
            System.out.println("");
        }
    }

    private static void doAddRole(Options options) throws Exception
    {
        roleSetsProxy.createRole(authToken,
                                 (String) options.roleParams.get("roleSet"),
                                 (String) options.roleParams.get("roleName"),
                                 (String) options.roleParams.get("description"),
                                 (List<RolePrivilege>) options.roleParams.get("parsedPrivileges"));
    }

    private static void doAddDelPrivilege(Options options) throws Exception
    {
        RoleSetInfo rs    = roleSetsProxy.getRoleSet(authToken, options.roleSet);
        boolean     found = false;

        // the REST api doesn't allow dipping down into a role itself so we have to search for it
        if (rs != null)
        {

            for (RoleInfo ri : rs.roles)
            {
                if (ri.name.equals(options.roleName))
                {
                    boolean update = false;

                    if (options.command == Command.ADD_PRIV)
                    {
                        ri.privileges.addAll(options.addDelPrivs);
                        update = true;
                    }
                    else
                    {
                        for (RolePrivilege toDel : options.addDelPrivs)
                        {
                            for (Iterator<RolePrivilege> it = ri.privileges.iterator(); it.hasNext(); )
                            {
                                RolePrivilege rp = it.next();

                                if (equalPriv(rp, toDel))
                                {
                                    update = true;
                                    it.remove();
                                }
                            }
                        }
                    }

                    if (update)
                        roleSetsProxy.setRolePrivileges(authToken, rs.nameID, ri.name, ri.privileges);

                    found = true;
                    break;
                }
            }
        }

        if (!found)
            System.out.println("Role '" + options.roleSet + "/" + options.roleName + "' not found.");
    }

    private static boolean equalPriv(RolePrivilege rp1, RolePrivilege rp2)
    {
        return ((rp1.forMember == rp2.forMember) &&
                rp1.operation.equals(rp2.operation) &&
                rp1.aclTarget.equals(rp2.aclTarget));
    }

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps, boolean requireYaml)
    {
        String  yaml = ps.getOptionArg("-c");
        String  cmd;
        Options opt;

        if (requireYaml && (yaml == null))
            return null;

        cmd = ps.getNext();

        if (cmd == null)
            return null;

        opt = new Options(yaml);

        if (cmd.equals(SHELL_CMD))
        {
            opt.command = Command.SHELL;
        }
        else if (cmd.equals(LISTROLESETS_CMD))
        {
            opt.command = Command.LIST_ROLESETS;
        }
        else if (cmd.equals(LISTROLESET_CMD))
        {
            opt.command = Command.LIST_ROLESET;
            opt.roleSet = getRoleSetName(ps);
        }
        else if (cmd.equals(ADDROLESET_CMD))
        {
            opt.command       = Command.ADD_ROLESET;
            opt.roleSetParams = getRolesetParams(ps);
        }
        else if (cmd.equals(ADDROLE_CMD))
        {
            opt.command = Command.ADD_ROLE;
            opt.roleParams = getRoleParams(ps);  // use Map<> because we have multiple things to return
        }
        else if (cmd.equals(ADDPRIV_CMD))
        {
            opt.command = Command.ADD_PRIV;
            getAddDelPrivilege(ps, opt);
        }
        else if (cmd.equals(DELPRIV_CMD))
        {
            opt.command = Command.DEL_PRIV;
            getAddDelPrivilege(ps, opt);
        }
        else
        {
            opt = null;
        }

        return opt;
    }

    /**
     * Input syntax:  roleset {nameID}
     *
     * Example:  newroleset System
     */
    private static String getRoleSetName(ParseState ps)
    {
        String  nameID = ps.getNext();

        if ((nameID == null) || ((nameID = nameID.trim()).length() == 0) ||
            nameID.startsWith("-") || (nameID.indexOf('/') != -1))
            throw new IllegalArgumentException("Missing/bad nameID for roleset.   Syntax:  roleset {nameID}");

        return nameID;
    }

    /**
     * Input syntax:  newroleset {nameID} type={type} name={name} description={description}
     *
     * Example:  newroleset System type=organization name=System description="System role set"
     *
     * Converts {nameID} to map key of "nameID"
     */
    private static Map<String, String> getRolesetParams(ParseState ps)
    {
        Map<String, String> params = new HashMap<>();
        String              nameID = ps.getNext();
        NVPair              nvp;

        if ((nameID == null) || nameID.startsWith("-") || (nameID.indexOf('=') != -1))
            throw new IllegalArgumentException("Missing nameID for newroleset.   Syntax:  newroleset {nameID} type=organization|project name={name} description={desc}");

        params.put("nameID", nameID.trim());

        while ((nvp = ps.getNextOptionNV()) != null)
            params.put(nvp.name, nvp.value.trim());

        if (!(params.containsKey("name") && params.containsKey("type") && params.containsKey("description")))
            throw new IllegalArgumentException("Missing value(s) for newroleset.  Syntax:  newroleset {nameID} type=organization|project name={name} description={desc}");

        return params;
    }

    /**
     * Syntax:  newrole name=roleset/rolename description=abc 
     *
     * Example:  newrole System/ROOT description="Root role" privileges="[(...,...,...),...]"
     */
    private static Map<String, Object> getRoleParams(ParseState ps)
    {
        Map<String, Object> params = new HashMap<>();
        String              role   = ps.getNext();
        NVPair              nvp;
        int                 slash;

        if ((role == null) || ((slash = role.indexOf('/')) == -1))
            throw new IllegalArgumentException("Missing/bad role for newrole.   " +
                                               "Syntax:  newrole {set/name} description={desc} " +
                                               "privileges=(forMember=true|false,operation=blah,aclTarget=blap);(...);...");

        params.put("roleSet",  role.substring(0, slash));
        params.put("roleName", role.substring(slash + 1));

        while ((nvp = ps.getNextOptionNV()) != null)
            params.put(nvp.name, nvp.value);

        if (!(params.containsKey("description") && params.containsKey("privileges")))
            throw new IllegalArgumentException("Missing value(s) for newrole.  " +
                                               "Syntax:  newrole {set/name} description={desc} " +
                                               "privileges=(forMember=true|false,operation=blah,aclTarget=blap);(...);...");

        params.put("parsedPrivileges", parsePrivileges((String) params.get("privileges")));

        return params;
    }

    /**
     * Syntax:  modrole role privileges=...
     *
     * Example:  newrole System/ROOT privileges="[(...,...,...),...]"
     */
    private static Map<String, Object> getModifyRoleParams(ParseState ps)
    {
        Map<String, Object> params = new HashMap<>();
        String              role   = ps.getNext();
        NVPair              nvp;
        int                 slash;

        if ((role == null) || ((slash = role.indexOf('/')) == -1))
            throw new IllegalArgumentException("Missing/bad role for modrole.   " +
                                               "Syntax:  modrole {set/name} " +
                                               "privileges=(forMember=true|false,operation=blah,aclTarget=blap);(...);...");

        params.put("roleSet",  role.substring(0, slash));
        params.put("roleName", role.substring(slash + 1));

        while ((nvp = ps.getNextOptionNV()) != null)
            params.put(nvp.name, nvp.value);

        params.put("parsedPrivileges", parsePrivileges((String) params.get("privileges")));

        return params;
    }

    /**
     * Syntax:  add/delpriv {set/role} {privilege};...
     *
     */
    private static void getAddDelPrivilege(ParseState ps, Options options)
    {
        String      role   = ps.getNext();
        String      privs  = ps.getNext();
        int         slash;

        if ((role == null) || ((slash = role.indexOf('/')) == -1))
            throw new IllegalArgumentException("Missing/bad role for modrole.   " +
                                               "Syntax:  modrole {set/name} " +
                                               "privileges=(forMember=true|false,operation=blah,aclTarget=blap);(...);...");

        options.roleSet     = role.substring(0, slash);
        options.roleName    = role.substring(slash + 1);
        options.addDelPrivs = parsePrivileges(privs);
    }

    /**
     * Parses privileges value, which is a list of triplets.
     *
     * Syntax:  triplet ";" ... , where triplet is:
     *
     *     "(" [forMember=] true/false, [operation=] string, [acltarget=] string ")"
     *
     * Example:
     *
     *    (true, ManageSystem, *); (forMember=false, ManageOther, target-proj)
     */
    private static List<RolePrivilege> parsePrivileges(String privString)
    {
        List<RolePrivilege> privileges = new ArrayList<>();
        String[]            triplets   = privString.split(";");

        for (String triple : triplets)
        {
            int      rParen;
            String[] eles;

            triple = triple.trim();

            if (!triple.startsWith("("))
                throw new IllegalArgumentException("Expected '(' in privilege list [" + triple + "]");
            else if ((rParen = triple.indexOf(')')) == -1)
                throw new IllegalArgumentException("Missing closing paren in privilege list [" + triple + "]");

            triple = triple.substring(1, rParen);
            eles   = triple.split(",");

            if (eles.length != 3)
                throw new IllegalArgumentException("Expected 3 elements in triplet [" + triple + "]");

            privileges.add(new RolePrivilege(Boolean.valueOf(stripName("forMember=", eles[0])),
                                             stripName("operation=", eles[1]),
                                             stripName("aclTarget=", eles[2])));
        }

        return privileges;
    }

    private static String stripName(String optName, String nv)
    {
        if (nv.startsWith(optName))
            nv = nv.substring(optName.length());

        return nv;
    }

    /**
     * Read the yaml configuration file and populate the Config object from it.
     */
    private static Config readConfig(String filepath) throws Exception
    {
        Config               config = new Config();
        Map<String, Object>  yaml   = loadYaml(filepath);
        Map<String, String>  params = new HashMap<String, String>();

        config.setTokenizerServer((String) yaml.get("tokenizerServer"));
        config.setUserAcctServer( (String) yaml.get("userAcctServer") );

        return config;
    }

    /**
     * Load configuration from .yaml file into generic Map.
     */
    private static Map<String, Object> loadYaml(String filepath) throws Exception
    {
        InputStream input = new FileInputStream(new File(filepath));
        Yaml        yaml  = new Yaml();
        Object      data = yaml.load(input);

        input.close();

        return (Map<String, Object>) data;
    }

    private static boolean isEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     * Attempts to log in the user by prompting for the user email address and password.
     * On successful login an internal auth token is put in the "authToken" field.
     */
    private static boolean loginUser(Config config) throws Exception
    {
        Console cs = System.console();

        if (cs == null)
        {
            System.out.println("Fatal error:  unable to get a console to prompt for username/password.\n");
            return false;
        }
        else if (isEmpty(config.getTokenizerServer()) || isEmpty(config.getUserAcctServer()))
        {
            System.out.println("Fatal error:  yaml config doesn't specify both tokenizerServer and userAcctServer");
            return false;
        }

        AuthProxy.Token token = null;
        String          username;
        String          password;

        authProxy     = new AuthProxy(config.getTokenizerServer(), config.getUserAcctServer());
        roleSetsProxy = new RoleSetsProxy(config.getTokenizerServer(), config.getUserAcctServer());

        cs.format("\nROOT role Login required:\n");

        // loop until they enter a well-formed email address
        while ((username = cs.readLine("email address of user with ROOT role: ").trim()) != null)
        {
            try
            {
                if (username.length() > 0)
                {
                    username = (new CanonicalEmail(username)).toString();
                    break;
                }
            }
            catch (Exception x)
            {
                System.out.println("\"" + username + "\" is not a well-formed email address.  Please re-enter.");
            }
        }

        // loop until they enter a non-empty password
        while ((password = new String(cs.readPassword("Password:")).trim()).length() == 0)
            ;

        try
        {
            token = authProxy.authenticate(username, password);
        }
        catch (HttpStatusCodeException x)
        {
            HttpStatus hs = x.getStatusCode();

            if (hs == HttpStatus.FORBIDDEN)
                System.out.println("Login failed:  wrong email address and/or password.\n");
            else
                System.out.println(hs);
        }

        if (token != null)
            authToken = token.token;

        return (authToken != null);
    }

    /**
     * Log out the user.  Not currently used as we would need to authenticate to an external token
     * and then exchange that for an internal one...
     */
    private static void logoutUser() throws Exception
    {
        if (authToken != null)
        {
            authProxy.logout(authToken, null);
            authToken = null;
        }
    }

}
