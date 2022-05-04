January 26, 2015

Brief explanation of PassPolicyCmd
==================================

PassPolicyCmd is a (temporary?) command line tool to manage things having to do with password policies.

The management operations are as follows:

* list existing password policies
* create a new password policy
* modify an existing password policy

====
Password policies are intended to force users to enter passwords that are more secure
than if they're allowed to enter any password (such as "123456").

Password policies have a name and a set of attributes (name=value, basically), where these
attributes are as follows:

* maxDays:  if present, indicates the maximum number of days allowed before the user is
  forced to change his/her password

* minChars:  if present, indicates the minimum length (characters) of a new password.  There
  is a hard-coded minimum of 1.

* maxChars:  if present, indicates the maximum length (characters) of a new password

* minLower:  if present, indicates the minimum number of lowercase characters required in
  new passwords.

* minUpper:  if present, indicates the minimum number of uppercase characters required in
  new passwords.

* minDigits:  if present, indicates the minimum number of digits [0..9] required in the
  new passwords.

* minSymbols:  if present indicates the minimum number of non-alphanumeric characters
  required in new passwords.

The above attribute names are used as the parameter name on the command line; the value is
appended after "=", like so:

  maxDays=30 minUpper=2 ...

Password policies can be created and modified (except for the name, which is used to
identify the password policy).

The name "Default" is a special password policy:  passwords of all users are governed by
this policy.

The "maxDays" parameter has a default unit of "day", but this can be changed by appending
a single character, as follows:

* d:  day (default)
* w:  week (7 days)
* m:  month (30 days)
* z:  minute (for testing only)

====
To create an example password policy:

  $ java -cp apixio-useracct-roletool*.jar com.apixio.useracct.cmdline.PassPolicyToolCmd \
      -c config.yaml -add thePolicyName \
	  maxDays=180 minChars=6 # add more param=value pairs here...

Note that password policies CANNOT be deleted.

====
To list all password policies:

  $ java -cp apixio-useracct-roletool*.jar com.apixio.useracct.cmdline.PassPolicyToolCmd \
      -c config.yaml -policies

====
To modify an example password policy:

  $ java -cp apixio-useracct-roletool*.jar com.apixio.useracct.cmdline.PassPolicyToolCmd \
      -c config.yaml -mod thePolicyName \
	  maxDays=90 minChars=7 # add more param=value pairs here...

Note that password policies CANNOT be deleted.

====

Because it's common that more than one invocation of PassPolicyCmd is needed when administering
policies, an interactive shell mode is available.  When used, the user need log in only at the
beginning.  The syntax of the commands, once in the shell, is identical to the outside-the-shell
syntax, except there is no need to specify "-c <yamlfile>".

Usage help within the shell is available via "help" or "?".

To invoke the interactive shell:

  $ java -cp apixio-useracct-roletool*.jar com.apixio.useracct.cmdline.PassPolicyToolCmd -c config.yaml -shell

and then to list policies, for example:

  roletool> -policies

