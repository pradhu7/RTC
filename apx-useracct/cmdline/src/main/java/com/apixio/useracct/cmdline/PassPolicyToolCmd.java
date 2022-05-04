package com.apixio.useracct.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.yaml.snakeyaml.Yaml;

import com.apixio.utility.StringUtil;
import com.apixio.useracct.cmdline.ParseState.NVPair;
import com.apixio.useracct.cmdline.PassPoliciesProxy.PolicyInfo;
import com.apixio.useracct.email.CanonicalEmail;

/**
 * PassPolicyToolCmd allows management of PasswordPolicies in the system.
 *
 * Usage:
 *
 *   To list policies:  PassPolicyToolCmd -c yamlfile -policies
 *   To add a policy:   PassPolicyToolCmd -c yamlfile -add name=description [maxDays=n] [minChars=n] [maxChars=n] [minDigits=n] [minSymbols=n] [noReuseCount=n] [noUserID=true/false]
 *
 * Yaml file contains redis connection configuration in a simple "name: value" structure:
 *
 *  tokenizerServer:  http://localhost:8075
 *  userAcctServer:   http://localhost:8076
 *
 * This command tool uses the RESTful API instead of directly accessing redis data (via
 * privileged Java code).
 */
public class PassPolicyToolCmd {

    private static final String cShellUsage = (
                                          "\t-policies [regex]\t(lists all policies; optional regex match on name)\n\n" +
                                          "\t-add name [param=n]...\t(add policy with given name and params of [maxDays,minChars,maxChars,minLower,minUpper,minDigits,minSymbols,noReuseCount,noUserID])\n\n" +
                                          "\t-mod name [param=n]...\t(modify given policy with params of [maxDays,minChars,maxChars,minLower,minUpper,minDigits,minSymbols,noReuseCount,noUserID])\n\n"
        );

    private static final String cUsage = ("Usage:  PassPolicyToolCmd -c <yamlfile> <cmd>, where cmd is one of:\n\n" +
                                          cShellUsage +
                                          "\t-shell\t\t\t(enter interactive shell)\n\n" +
                                          "This tool requires a valid name/password for a User that has ROOT role.  " +
                                          "It must be invoked in a real shell."
        );

    private enum Command {
        LIST_POLICIES,
        ADD_POLICY,
        MOD_POLICY,
        SHELL
    }

    private static final String MAX_DAYS_PARAM  = "maxDays";   // special case (not an int)
    private static final String NO_USERID_PARAM = "noUserID";  // special case (not an int)
    private static final List<String> VALID_PARAMS = Arrays.asList(new String[] {
            MAX_DAYS_PARAM, "minChars", "maxChars", "minLower", "minUpper", "minDigits", "minSymbols", "noReuseCount", NO_USERID_PARAM
        });

    /**
     * The option/commands supported.
     */
    private static final String LIST_CMD    = "-policies";
    private static final String ADD_CMD     = "-add";
    private static final String MOD_CMD     = "-mod";
    private static final String SHELL_CMD   = "-shell";

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options {
        Command              command;
        String               yamlFile;
        String               regex;
        String               policyName;
        Map<String, String>  params;

        Options(String yaml)
        {
            this.yamlFile = yaml;
        }

        public String toString()
        {
            return ("[opt command=" + command +
                    "; yaml=" + yamlFile +
                    "; regex=" + regex +
                    "; params=" + params +
                    "]");
        }
    }

    /**
     * Actual connection/proxy for talking to user-account service.
     */
    private static AuthProxy            authProxy;
    private static PassPoliciesProxy    passPoliciesProxy;
    private static String               authToken;

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
     * Fetch and display list of password policies
     */
    private static void listPolicies(String regex) throws Exception
    {
        PolicyInfo[] policies = passPoliciesProxy.getAllPolicies(authToken);

        if (policies.length == 0)
        {
            System.out.println("No password policies in system.");
        }
        else
        {
            Pattern pattern = (regex != null) ? (Pattern.compile(regex)) : null;

            System.out.println("Password Policies\n=====");

            for (PolicyInfo policy : policies)
            {
                Matcher matcher = (pattern != null) ? pattern.matcher(policy.name) : null;

                if ((matcher == null) || matcher.matches())
                    System.out.println(StringUtil.subargsPos("ID: {}; name: {}; maxDays {}, minChars {}, maxChars {}, minLower {}, minUpper {}, minDigits {}, minSymbols {}, noReuseCount {}, noUserID {}",
                                                       policy.policyID, policy.name,
                                                       policy.maxDays, policy.minChars, policy.maxChars, policy.minLower, policy.minUpper, policy.minDigits, policy.minSymbols,
                                                       policy.noReuseCount, policy.noUserID
                                           ));
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
            case LIST_POLICIES:
            {
                listPolicies(options.regex);
                break;
            }
            case MOD_POLICY:
            {
                modifyPolicy(options.policyName, options.params);
                break;
            }
            case ADD_POLICY:
            {
                addPolicy(options.policyName, options.params);
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
     * Add the role with name/description.
     */
    private static void addPolicy(String name, Map<String, String> params) throws Exception
    {
        passPoliciesProxy.addPolicy(authToken, name, params);
    }

    private static void modifyPolicy(String name, Map<String, String> params) throws Exception
    {
        passPoliciesProxy.modifyPolicy(authToken, name, params);
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

            System.out.print("policytool> ");
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
        boolean isAdd;

        if (requireYaml && (yaml == null))
            return null;

        cmd = ps.getNext();

        if (cmd == null)
            return null;

        opt = new Options(yaml);

        if (cmd.equals(LIST_CMD))
        {
            opt.command = Command.LIST_POLICIES;
            opt.regex   = ps.getNext();
        }
        else if (cmd.equals(SHELL_CMD))
        {
            opt.command = Command.SHELL;
        }
        else if ((isAdd = cmd.equals(ADD_CMD)) || (cmd.equals(MOD_CMD)))
        {
            boolean good = false;
            NVPair  param;

            opt.policyName = ps.getNext();

            if (opt.policyName == null)
            {
                System.out.println("Missing policy name");
            }
            else
            {
                opt.params = new HashMap<String, String>();

                while ((param = ps.getNextOptionNV()) != null)
                {
                    if (!(good = VALID_PARAMS.contains(param.name)))
                    {
                        System.out.println("Invalid param name '" + param.name + "'.  Must be one of " + VALID_PARAMS);
                        break;
                    }
                    else if (param.name.equals(NO_USERID_PARAM) && !validBoolean(param.value))
                    {
                        System.out.println("Invalid param value '" + param.name + "':  must be true/false");
                        good = false;
                        break;
                    }
                    /*
                    else if (param.name.equals(MAX_DAYS_PARAM) && !validInt(param.value))
                    {
                        System.out.println("Invalid param value '" + param.name + "':  must be a non-negative integer");
                        good = false;
                        break;
                    }
                    */

                    opt.params.put(param.name, param.value);
                }
            }

            if (good)
                opt.command = (isAdd) ? Command.ADD_POLICY : Command.MOD_POLICY;
            else
                opt = null;
        }
        else
        {
            opt = null;
        }

        return opt;
    }

    /**
     * Returns true if the string can be parsed to a non-negative number.
     */
    private static boolean validInt(String v)
    {
        try
        {
            int n = Integer.parseInt(v);

            if (n >= 0)
                return true;
        }
        catch (Exception x)
        {
        }

        return false;
    }

    /**
     * Returns true if the string can be parsed to a non-negative number.
     */
    private static boolean validBoolean(String v)
    {
        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false");
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

        authProxy         = new AuthProxy(config.getTokenizerServer(), config.getUserAcctServer());
        passPoliciesProxy = new PassPoliciesProxy(config.getTokenizerServer(), config.getUserAcctServer());

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
