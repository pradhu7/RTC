June 2020

Make sure the values in apixio-security.properties match what's in scripts/config.sh

################

To start local Vault:

  $ export VAULT_HOME=...     # to the dir that holds the server exe
  $ scripts/local-dev-init.sh

and you should see something like:

    Success! Enabled the kv secrets engine at: theKvMountPoint/
    Success! Data written to: theKvMountPoint/shared/encryption
    Success! Enabled approle auth method at: approle/
    Success! Data written to: auth/approle/role/dev-role
    Success! Uploaded policy: thepolicyname
    Success! Data written to: auth/approle/role/dev-role
    Success! Enabled the transit secrets engine at: theTransitMountPoint/
    Success! Data written to: theTransitMountPoint/keys/phi-key

################

In order to test v1 code make sure bcprov*.jar is in common/buildable directory along with
a v1 apixio-security.properties:

  $ cp src/test/apixio-security.properties .

################

In order to test v2 encryption code you'll need to compile with -DskipTests:

  $ mvn -DskipTests clean install

because doing a build with tests will fail some tests (due to hardcoded directory
paths that aren't valid on most machines) and will not compile the needed test
code.

The following procedure should work:

  $ cd common/buildable/security
  $ mvn -DskipTests clean install
  $ export VAULT_HOME=pathToVaultDir
  $ export BC_HOME=pathToBouncyCastleDir
  $ sh src/test/scripts/local-dev-init.sh
  $ sh src/test/init-vault.sh init puttherealoldpasswordhere V01  # that password is in other scripts...
  $ sh src/test/test-lots.sh
  $ sh src/test/test-keycache.sh

