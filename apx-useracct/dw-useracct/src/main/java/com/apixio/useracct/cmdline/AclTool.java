package com.apixio.useracct.cmdline;

import java.io.*;
import java.util.*;

import org.yaml.snakeyaml.Yaml;

import com.apixio.XUUID;
import com.apixio.aclsys.dao.*;
import com.apixio.aclsys.entity.*;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.CqlRowData;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.config.*;
import com.apixio.restbase.dao.Tokens;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.*;
import com.apixio.useracct.entity.*;

/**
 */
public class AclTool extends CmdlineBase
{
    private static final String cUsage = ("Usage:  AclTool -c <conn-yamlfile>\n\n" +
        "Interactive ACL tool\n\n");

    private static String DESC_LINE1 = " ";
    private static String DESC_LINEN = "\n" + DESC_LINE1 + "  ";

    /**
     * Keep track of all that was parsed (at a coarse level).
     */
    private static class Options
    {
        String       connectionYamlFile;
        boolean      echo;

        Options(String connectionYaml, boolean echo)
        {
            this.connectionYamlFile = connectionYaml;
            this.echo               = echo;
        }

        public String toString()
        {
            return ("[opt connectionYaml=" + connectionYamlFile +
                    "]");
        }
    }

    private static abstract class Command
    {
        abstract String commandName();
        abstract String description();
        abstract void   process(List<String> args) throws IOException;
    }

    private static Map<String, Command> commands = new HashMap<>();

    // ################################################################

    private static PrivSysServices   sysServices;

    /**
     *
     */
    public static void main(String[] args) throws Exception
    {
        Options         options   = parseOptions(new ParseState(args));
        ConfigSet       config    = null;

        if ((options == null) ||
            ((config = readConfig(options.connectionYamlFile)) == null))
        {
            usage();
            System.exit(1);
        }

        sysServices = setupServices(config);

        setupCommands();

        try
        {
            aclTool(options);
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
        finally
        {
            System.exit(0);  // jedis or cql creates non-daemon thread.  boo
        }
    }

    /**
     *
     */
    private static void aclTool(Options options) throws Exception
    {
        BufferedReader  cs      = new BufferedReader(new InputStreamReader(System.in));
        String          cmdStr;

        while ((cmdStr = readCommand(cs)) != null)
        {
            if (options.echo)
                System.out.println("################ begin commnd [" + cmdStr + "]\n");

            processCommand(cmdStr);

            if (options.echo)
                System.out.println("\n################ end commnd [" + cmdStr + "]\n");
        }

        if (options.echo)
            System.out.println("<exiting>");
    }

    private static void setupCommands()
    {
        addCommand(new HelpCommand());
        addCommand(new UserIDCommand());
        addCommand(new UserRolesCommand());
        addCommand(new UserCanDoCommand());
        addCommand(new UserCanDo2Command());
        addCommand(new PermissionTestCommand());
        addCommand(new CheckRolesCommand());
    }

    private static void addCommand(Command cmd)
    {
        commands.put(cmd.commandName(), cmd);
    }


    private static String readCommand(BufferedReader con) throws IOException
    {
        System.out.print("acl> ");

        return con.readLine();
    }

    private static List<String> parseCommand(String cmd)
    {
        List<String>  args   = new ArrayList<>();

        for (String s : cmd.split(" "))
        {
            s = s.trim();

            if (s.length() > 0)
                args.add(s);
        }

        return args;
    }

    private static void processCommand(String cmdStr)
    {
        List<String> args = parseCommand(cmdStr);
        Command      cmd;

        if (args.size() > 0)
        {
            if ((cmd = commands.get(args.get(0))) != null)
            {
                try
                {
                    cmd.process(args.subList(1, args.size()));
                }
                catch (Exception x)
                {
                    System.out.println("Command failed with exception:");
                    x.printStackTrace();
                }
            }
            else
            {
                System.out.println("Unknown command '" + args.get(0) + "'");
            }
        }
    }

    // ################################################################
    // ################################################################
    //  Commands
    // ################################################################
    // ################################################################

    private static class HelpCommand extends Command
    {
        String commandName()
        {
            return "help";
        }

        String description()
        {
            return commandName();
        }

        void process(List<String> args)
        {
            System.out.println("Available commands:");

            for (Command cmd : commands.values())
            {
                System.out.print(DESC_LINE1);
                System.out.println(cmd.description());
            }

            System.out.println("");
        }
    }

    private static class UserIDCommand extends Command
    {
        String commandName()
        {
            return "userid";
        }

        String description()
        {
            return (commandName() + " emailaddr ..." +
                    DESC_LINEN + " lookup email address and show UserID for it");
        }

        void process(List<String> args)
        {
            for (String email : args)
            {
                User user = sysServices.getUsers().findUserByEmail(email);

                System.out.println(" " + email + ":  " + ((user != null) ? user.getID() : "<not found>"));
            }
        }
    }

    private static class UserRolesCommand extends Command
    {
        String commandName()
        {
            return "userroles";
        }

        String description()
        {
            return (commandName() + " emailaddr ..." +
                    DESC_LINEN + " show all roles assigned for users with given email addresses");
        }

        void process(List<String> args) throws IOException
        {
            for (String email : args)
            {
                User user = sysServices.getUsers().findUserByEmail(email);

                if (user != null)
                {
                    List<String> output = new ArrayList<>();
                    Set<Role>    roles  = new HashSet<>();

                    System.out.println(" " + email + ":");

                    for (RoleAssignment ra : sysServices.getRoleLogic().getRolesAssignedToUser(user))
                    {
                        roles.add(ra.role);
                        output.add("   * " + ra.role.getName() + " (" + ra.role.getID() + ") on target " + ra.targetID);
                    }

                    Collections.sort(output);

                    for (String out : output)
                        System.out.println(out);

                    System.out.println("\nRole details:");
                    for (Role role : roles)
                    {
                        System.out.println("  * " + role.getName() + " (" + role.getID() + "): ");

                        for (Privilege priv : role.getPrivileges())
                        {
                            System.out.println("     - " + priv.getOperationName() + ": for-member=" + priv.forMember() + ", resolver=" + priv.getObjectResolver());
                        }
                    }
                }
                else
                {
                    System.out.println(" " + email + ":  " + ((user != null) ? user.getID() : "<not found>"));
                }
            }
        }
    }

    private static class UserCanDoCommand extends Command
    {
        String commandName()
        {
            return "usercando";
        }

        String description()
        {
            return (commandName() + " emailaddr operation" +
                    DESC_LINEN + " show all targets the given user can perform given operation on");
        }

        void process(List<String> args) throws IOException
        {
            if (args.size() != 2)
            {
                System.out.println(description());
            }
            else
            {
                User      user = sysServices.getUsers().findUserByEmail(args.get(0));
                Operation oper = sysServices.getOperations().findOperationByName(args.get(1));

                if (oper == null)
                {
                    System.out.println("Operation " + args.get(1) + " not found");
                }
                else if (user == null)
                {
                    System.out.println("User " + args.get(0) + " not found");
                }
                else
                {
                    List<String>  targets = sysServices.getAclDao().getBySubjectOperation(user.getID(), oper);

                    System.out.println("User (" + user.getID() + " can do " + args.get(1) + " on the following targets:");
                    for (String target : targets)
                        System.out.println(" " + target);
                }
            }
        }
    }

    private static class UserCanDo2Command extends Command
    {
        String commandName()
        {
            return "usercando2";
        }

        String description()
        {
            return (commandName() + " emailaddr target" +
                    DESC_LINEN + " show all operations the given user can perform on the given targer");
        }

        void process(List<String> args) throws IOException
        {
            if (args.size() != 2)
            {
                System.out.println(description());
            }
            else
            {
                User user = sysServices.getUsers().findUserByEmail(args.get(0));

                if (user != null)
                {
                    List<String>  ops = sysServices.getAclDao().getBySubjectObject(user.getID(), args.get(1));

                    for (RoleAssignment role : sysServices.getRoleLogic().getRolesAssignedToUser(user))
                    {
                    }

                    System.out.println("User (" + user.getID() + " can do the following operations on target:");
                    for (String op : ops)
                        System.out.println(" " + op);
                }
                else
                {
                    System.out.println("User " + args.get(0) + " not found");
                }
            }
        }
    }

    private static class PermissionTestCommand extends Command
    {
        String commandName()
        {
            return "permtest";
        }

        String description()
        {
            return (commandName() + " emailaddr operation target" +
                    DESC_LINEN + "run 'hasPermission' check on parameters");
        }

        void process(List<String> args) throws IOException
        {
            if (args.size() != 3)
            {
                System.out.println("Usage:  " + description());
            }
            else
            {
                User user = sysServices.getUsers().findUserByEmail(args.get(0));

                if (user != null)
                {
                    boolean allowed = sysServices.getAclLogic().hasPermission(user.getID(), args.get(1), args.get(2));

                    System.out.println("hasPermission(" + user.getID() + ", " + args.get(1) + ", " + args.get(2) + ") returns " + allowed);
                }
                else
                {
                    System.out.println("User " + args.get(0) + " not found");
                }
            }
        }
    }

    private static class CheckRolesCommand extends Command
    {
        String commandName()
        {
            return "check-wildcards";
        }

        String description()
        {
            return (commandName() +
                    DESC_LINEN + "run consistency check on all privileges w/ '*' as target");
        }

        void process(List<String> args) throws IOException
        {
            List<String> wilds = new ArrayList<>();
            AclDao       dao   = sysServices.getAclDao();
            Operations   ops   = sysServices.getOperations();

            for (Role role : sysServices.getRoles().getAllRoles())
            {
                for (Privilege priv : role.getPrivileges())
                {
                    if (Privilege.RESOLVER_ANY_ALL.equals(priv.getObjectResolver()))
                        wilds.add(priv.getOperationName());
                }
            }

            System.out.println("Operation names of privileges with '*' as resolver:");
            for (String opname : wilds)
                System.out.println("  " + opname);

            for (String opname : wilds)
            {
                List<XUUID> subs = dao.getByObjectOperation("*", ops.findOperationByName(opname));

                // there should be a non-empty list (?) for the ACL semi-tuple (obj=*, op=opname)

                if (subs.size() == 0)
                    System.out.println("Warning:  no users have privilege to do " + opname + " on '*'.  Check existence of rowkey 'acl.*:" + opname + "' (be sure to escape the '*')");
            }
        }
    }

    // ################################################################
    // ################################################################
    //  Support
    // ################################################################
    // ################################################################

    /**
     * Parse the command line and build up the Options object from it.
     */
    private static Options parseOptions(ParseState ps)
    {
        String  connection = ps.getOptionArg("-c");

        if (connection == null)
            return null;
        else
            return new Options(connection, ps.getOptionFlag("-e"));
    }

    /**
     * Print out usage info.
     */
    private static void usage()
    {
        System.out.println(cUsage);
    }

}
