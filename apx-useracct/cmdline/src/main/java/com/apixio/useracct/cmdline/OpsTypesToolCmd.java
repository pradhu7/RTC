package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.yaml.snakeyaml.Yaml;

import com.apixio.useracct.cmdline.AclFuncsProxy.OperationInfo;
import com.apixio.useracct.cmdline.AclFuncsProxy.AccessTypeInfo;
import com.apixio.useracct.cmdline.ParseState.NVPair;
import com.apixio.useracct.email.CanonicalEmail;
import com.apixio.utility.StringUtil;

/**
 * OpsTypesToolCmd allows listing and creation of ACL Operations and ACL AccessTypes via
 * a command line interface.
 *
 * Usage:
 *
 *   To list Operations:      OpsTypesToolCmd -c yamlfile -operations
 *   To add an Operation:     OpsTypesToolCmd -c yamlfile -addop name=description 
 *   To delete an Operation:  OpsTypesToolCmd -c yamlfile -delop name
 *
 * Yaml file contains redis connection configuration in a simple "name: value" structure:
 *
 *  tokenizerServer:  http://localhost:8075
 *  userAcctServer:   http://localhost:8076
 *
 * This command tool uses the RESTful API instead of directly accessing redis data (via
 * privileged Java code).
 */
public class OpsTypesToolCmd {

    private static final String cShellUsage = (
                                          "\t-operations\t\t(lists all Operations)\n\n" +
                                          "\t-addop name=desc [accessTypes]\t(add Operation with given name, description, and optional access types)\n\n" +
                                          "\t-remop name\t\t(remove Operation with given name)\n\n" +
                                          "\t-accesstypes\t\t(lists all AccessTypes)\n\n" +
                                          "\t-addat name=desc\t(add AccessType with given name and description)\n\n" +
                                          "\t-remat name\t\t(remove AccessType with given name)\n\n"
        );

    private static final String cUsage = ("Usage:  OpsTypesToolCmd -c <yamlfile> <cmd>, where cmd is one of:\n\n" +
                                          cShellUsage +
                                          "\t-shell\t\t\t(enter interactive shell)\n\n" +
                                          "This tool requires a valid name/password for a User that has ROOT role.  " +
                                          "It must be invoked in a real shell."
        );

    private enum Command {
        LIST_OPERATIONS,
        ADD_OPERATION,
        REMOVE_OPERATION,
        LIST_ACCESSTYPES,
        ADD_ACCESSTYPE,
        REMOVE_ACCESSTYPE,
        SHELL
    }

    /**
     * The option/commands supported.
     */
    private static final String LISTOPS_CMD  = "-operations";
    private static final String ADDOP_CMD    = "-addop";
    private static final String REMOP_CMD    = "-remop";
    private static final String LISTATS_CMD  = "-accesstypes";
    private static final String ADDAT_CMD    = "-addat";
    private static final String REMAT_CMD    = "-remat";
    private static final String SHELL_CMD    = "-shell";

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        Command      command;
        String       yamlFile;
        String       name;
        String       description;
        String       accessTypesCsv;

        Options(String yaml)
        {
            this.yamlFile = yaml;
        }

        public String toString()
        {
            return ("[opt command=" + command +
                    "; yaml=" + yamlFile +
                    "; name=" + name +
                    "; description=" + description +
                    "]");
        }
    }

    /**
     * Actual connection/proxy for talking to user-account service.
     */
    private static AuthProxy     authProxy;
    private static String        authToken;
    private static AclFuncsProxy aclFuncsProxy;

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
     * Fetch and display list of Operations.
     */
    private static void listOperations() throws Exception
    {
        OperationInfo[] ops = aclFuncsProxy.getAllOperations(authToken);

        if (ops.length == 0)
        {
            System.out.println("No Operations in system.");
        }
        else
        {
            System.out.println("Operations:\n=====");

            for (OperationInfo op : ops)
            {
                System.out.println(StringUtil.subargsPos("ID: {}; Name: {}; Description: {}\n  AccessTypes: {}",
                                                   op.id, op.name, op.description, op.accessTypes));
            }

            System.out.println("");
        }
    }

    /**
     * Fetch and display list of AccessTypes
     */
    private static void listAccessTypes() throws Exception
    {
        AccessTypeInfo[] ats = aclFuncsProxy.getAllAccessTypes(authToken);

        if (ats.length == 0)
        {
            System.out.println("No AccessTypes in system.");
        }
        else
        {
            System.out.println("AccessTypes:\n=====");

            for (AccessTypeInfo at : ats)
            {
                System.out.println(StringUtil.subargsPos("ID: {}; Name: {}; Description: {}\n",
                                                   at.id, at.name, at.description));
            }

            System.out.println("");
        }
    }

    /**
     * Dispatch the parsed command.
     */
    private static void dispatch(Options options, boolean allowShell) throws Exception
    {
        switch (options.command)
        {
            case LIST_OPERATIONS:
            {
                listOperations();
                break;
            }
            case ADD_OPERATION:
            {
                addOperation(options.name, options.description, options.accessTypesCsv);
                break;
            }
            case REMOVE_OPERATION:
            {
                removeOperation(options.name);
                break;
            }
            case LIST_ACCESSTYPES:
            {
                listAccessTypes();
                break;
            }
            case ADD_ACCESSTYPE:
            {
                addAccessType(options.name, options.description);
                break;
            }
            case REMOVE_ACCESSTYPE:
            {
                removeAccessType(options.name);
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
     * Add the operation with name/description.
     */
    private static void addOperation(String name, String desc, String accessTypesCsv) throws Exception
    {
        aclFuncsProxy.addOperation(authToken, name, desc);
    }

    /**
     * Remove the operation with name
     */
    private static void removeOperation(String name) throws Exception
    {
        aclFuncsProxy.removeOperation(authToken, name);
    }

    /**
     * Add the AccessType with name/description.
     */
    private static void addAccessType(String name, String desc) throws Exception
    {
        aclFuncsProxy.addAccessType(authToken, name, desc);
    }

    /**
     * Remove the accesstype with name
     */
    private static void removeAccessType(String name) throws Exception
    {
        aclFuncsProxy.removeAccessType(authToken, name);
    }

    /**
     * Enter an interactive shell (to avoid having to login in every time).
     */
    private static void shell() throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\nThe interactive shell lets you enter the base commands without having to log in each time." +
                           "\nEnter as you would on command line (including '-'s) except for no '-c yaml'." +
                           "\n'?' for help.  EOF, 'quit', or ^C to exit.\n");

        while (true)
        {
            String       line;
            List<String> parsed;

            System.out.print("acltool> ");
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
                System.out.println(x.getMessage());
            }
        }
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

        if (cmd.equals(LISTOPS_CMD))
        {
            opt.command = Command.LIST_OPERATIONS;
        }
        else if (cmd.equals(LISTATS_CMD))
        {
            opt.command = Command.LIST_ACCESSTYPES;
        }
        else if (cmd.equals(REMOP_CMD))
        {
            opt.command = Command.REMOVE_OPERATION;
            opt.name    = ps.getNext();

            if ((opt.name == null) || opt.name.isEmpty())
                System.out.println("-remop requires NAME parameter");
        }
        else if (cmd.equals(REMAT_CMD))
        {
            opt.command = Command.REMOVE_ACCESSTYPE;
            opt.name    = ps.getNext();

            if ((opt.name == null) || opt.name.isEmpty())
                System.out.println("-remat requires NAME parameter");
        }
        else if (cmd.equals(ADDOP_CMD))
        {
            NVPair  nameDesc   = ps.getNextOptionNV();
            boolean good       = false;

            // syntax:  -addop name=desc

            if (!ps.isEmpty())
            {
                System.out.println("Too many parameters [" + ps.whatsLeft() + "].  Syntax is --addop name=descriptionText");
            }
            else if (nameDesc != null)
            {
                opt.command = Command.ADD_OPERATION;

                opt.name        = nameDesc.name;
                opt.description = nameDesc.value;

                if ((opt.name.length() > 0) &&
                    (opt.description.length() > 0))
                    good = true;
                else
                    System.out.println("Both Operation name and description must be non-empty");
            }
            else
            {
                System.out.println("-addop requires NAME=VALUE syntax");
            }

            if (!good)
                opt = null;
        }
        else if (cmd.equals(ADDAT_CMD))
        {
            NVPair  nameDesc   = ps.getNextOptionNV();
            boolean good       = false;

            // syntax:  -addat name=desc

            if (!ps.isEmpty())
            {
                System.out.println("Too many parameters [" + ps.whatsLeft() + "].  Syntax is --addat name=descriptionText");
            }
            else if (nameDesc != null)
            {
                opt.command = Command.ADD_ACCESSTYPE;

                opt.name        = nameDesc.name;
                opt.description = nameDesc.value;

                good = true;
            }
            else
            {
                System.out.println("-addat requires NAME=VALUE syntax");
            }

            if (!good)
                opt = null;
        }
        else if (cmd.equals(SHELL_CMD))
        {
            opt.command = Command.SHELL;
        }
        else
        {
            return null;
        }

        return opt;
    }

    /**
     * Read the yaml configuration file and populate the Config object from it.
     */
    private static Config readConfig(String filepath) throws Exception
    {
        Config               config = new Config();
        Map<String, Object>  yaml   = loadYaml(filepath);
        Map<String, String>  params = new HashMap<String, String>();

        config.setTokenizerServer((String)  yaml.get("tokenizerServer"));
        config.setUserAcctServer( (String)  yaml.get("userAcctServer") );

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
        aclFuncsProxy = new AclFuncsProxy(config.getTokenizerServer(), config.getUserAcctServer());

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
