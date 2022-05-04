December 17, 2014

Brief explanation of RoleToolCmd
================================


RoleToolCmd is a (temporary?) command line tool to manage things having to do with roles.  This tool
is necessary as the user-account system (along with other systems) is relying more on role based
access checks, to there is a need to be able to do management of the set of roles and management of
user roles.

The role-level management operations are as follows:

* Create a role
* List existing roles
* Modify a role

Additionally, RoleToolCmd allows the assigning of roles to users and listing users to see their roles.

====
Roles are actually quite a simple structure: they have a unique name (conventionally the name is is
all UPPERCASE), a description, and an optional declaration of what other roles a user with that
role can assign to other users.

The system requires two roles: ROOT and USER.  A user with the ROOT role can do anything (API-wise)
in the system.  A USER, by contrast, has very limited privileges.  A user without any roles defined
has an implied USER role.

The only conceptually odd thing is the concept of "assignable roles".  Assignable roles are
necessary to allow non-ROOT users the ability to assign some roles to other users.  If these
admin-like roles could assign the ROOT role to a user, then the system is insecure, so each
admin-like role has a list of roles that are allowed to be assigned (by a user with the admin-like
role).

====
To create a role:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -addrole \
      theNewRoleName=theRoleDescription \
	  assignable=ROLE1,ROLE2,...

To create, say, an HCC_ADMIN role, the following is reasonable:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -addrole \
      HCC_ADMIN="Allows management of the HCC application" \
	  assignable=HCC_USER

(which assumes that the role HCC_USER already exists).

Note that roles CANNOT be deleted.

====
To list all roles:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -roles

====
Both the role description and the assignable roles can be modified (the role name is the identifier
of the role, so it can't be modified), together or independently:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -modrole \
      HCC_ADMIN \
	  description="Allows management of the HCC application" \
	  assignable=HCC_USER,USER


====
To manage users' roles, it's necessary to be able to list the users and their roles, and it's
necessary to be able to assign roles to users.

The list users, along with their roles:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -users

That will list all users in the system, quite possibly a large number.  The "-users" command
accepts a regular expression to limit what's listed:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -users "*@apixio.com"

which will list only users whose email addresses end in "@apixio.com".

To assign a role(s) to a user:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -set someone@apixio.com=HCC_ADMIN,USER

You can assign more than one user a role(s) by adding more addr=roles to the end:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -set someone@apixio.com=HCC_ADMIN,USER other@apixio.com=ROOT


There are two common reasons that setting roles will fail:

1.  the user attempting to set the role has a role that doesn't allow the desired role in the
    list of "assignable roles".  If a ROOT-role user uses RoleToolCmd but the ROOT role hasn't
	(yet) been set to allow assigning the role, say, of HCC_ADMIN, then attempting to set
	a user's role to HCC_ADMIN will fail

2.  the user is attempting to set the role(s) for him/herself.  As a safety precaution it's not
    possible to change your own roles, so a ROOT-roled user can't change his/her roles.

====
Usually more than one invocation of RoleToolCmd is needed when administering roles and users' roles,
an interactive shell mode is available.  When used, the user need log in only at the beginning.  The
syntax of the commands, once in the shell, is identical to the outside-the-shell syntax, except there
is no need to specify "-c <yamlfile>".

Usage help within the shell is available via "help" or "?".

To invoke the interactive shell:

  $ java -jar apixio-useracct-roletool*.jar -c config.yaml -shell

and then to list users, for example:

  roletool> -users *@apixio.com

