January 2021
-------------

Configuration of v2 security code is categorized as follows:

* Vault server access configuration; note that this is specifically *access* info (and *not* server
  *info), meaning a vault token or the role+secret needed to get an actual token

* security provider jar configuration

* everything else, which includes (oddly, perhaps) Vault server endpoints+paths


The sources of configuration values are:

* environment variables

* Java system properties; i.e., as specified via -Dvar=val on the Java exec command line

* OS files


################################################################ Current info for the above:

* To get Vault server access config:

  * v2 code looks for a token value in the following order:
    1.  APX_VAULT_TOKEN as an environment variable
    2.  APX_VAULT_TOKEN as a  system property
    3.  VAULT_TOKEN     as an environment variable
    4.  VAULT_TOKEN     as a  system property

  The reason for looking for both APX_VAULT_TOKEN and VAULT_TOKEN is that APX_VAULT_TOKEN is
  consistent with the naming convention of the other Apixio-defined configuration (APX_ prefix) but
  Vault tools (cmdline) look for VAULT_TOKEN

  * if no token from the above, then it looks for BOTH of the following, first as an environment
    variable, and secondly as a system property:

    * APX_VAULT_ROLE_ID
    * APX_VAULT_SECRET_ID


* Security provider jar information requires that the paths to both the FIPS ("bcfips") and
  non-FIPS ("bcprov" in this documentation, to match the jar file name) are specified, and they can
  be specified by a Java system property or by an environment variable.  The name of the system
  property does *not* follow the naming convention used for other v2 system properties as the
  original v2 code didn't follow the convention and changing it now would cause backwards
  compatibility problems.  The naming convention for environment variables *is* followed, however.

  The Java system property names are:

  * bcprovpath; example:  java -Dbcprovpath=$HOME/bc-jars/bcprov-jdk15on-1.47.jar ...
  * bcfipspath; example:  java -Dbcprovpath=$HOME/bc-jars/bc-fips-1.0.1.jar ...

  The environment variables to use are:

  * APXSECV2_BCPROVPATH; example:  APXSECV2_BCPROVPATH=$HOME/bc-jars/bcprov-jdk15on-1.47.jar java ...
  * APXSECV2_BCFIPSPATH; example:  APXSECV2_BCFIPSPATH=$HOME/bc-jars/bc-fips-1.0.1.jar java ...

  (note that the above examples need paths to both providers specified but don't show that)



* All the other v2 security configuration (which is a lot) is treated consistently with respect to
  the possible sources.  For this set of config, there are three sources:

  1) apixio-security.properties classloader resource and files
  2) Java system properties
  3) environment variables

  Each configuration option has a base string name that can have consistent modifications to it for
  specific sources.  Two example base names are "v1-encryption-mode" and "algorithm".  For both
  Java system properties and environment variables we want to add a namespace to avoid collisions
  (and for environment variables we also need to conform to shell naming conventions), so the
  following modifications are done:

  * for system properties, the string "apx-security-" is prepended to the base name; e.g.,
    "apx-security-algorithm"

  * for environment variables, the string "APXSECV2_" is prepended to the base name, and then the
    whole string is modified to replace all "-"s with "_" and then converted to uppercase; e.g.,
    "APXSECV2_V1_ENCRYPTION_MODE"

  The base name is used as-is for configuration in apixio-security.properties.

  This set of v2 configuration also aggregates across these different sources, allowing a hierarchy
  of configuration, with the last loaded configuration overriding the more recently loaded.  The
  general principle here is that the easier it is to modify, the later it's loaded: The order of
  loading is:

  1) apixio-security.properties from classloader resource of that name; this should be considered
     the default for an app

  2) apixio-security.properties, first in the directory the Java JVM was launched, and second in
     $HOME (the user's home directory)

  3) from Java system properties

  4) from environment variables

