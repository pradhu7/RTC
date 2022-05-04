################ This file does a few things: verifies that the critical VAULT_HOME variable
################ is set up, verifies that things look enough as expected that whatever command
################ is executing can continue, and it sets key configuration values.  These
################ configuration values are "app level" and as such need to be coordinated with
################ other components of the security system.

VAULT_EXE="$VAULT_HOME/vault"

if [      "$VAULT_HOME" = "" ]; then echo "Environment variable VAULT_HOME must be set to directory where vault executable is" 1>&2; exit 1; fi
if [ ! -d "$VAULT_HOME"      ]; then echo "VAULT_HOME ($VAULT_HOME) is not a directory" 1>&2; exit 1; fi
if [ ! -r "$VAULT_EXE"       ]; then echo "$VAULT_HOME/vault is not a readable file" 1>&2; exit 1; fi

# the default "vault" CLI behavior is to use https, so we force it not to (VAULT_ADDR is what CLI uses):
export VAULT_ADDR="http://127.0.0.1:8200"

################################################################
#### Define the "mount point" of the v1 key info (kept as a secret)
#### and the transit&datakey area.  The intent here is to make it
#### slightly more difficult for an attacker...
####
#### the value in path_transit is exposed in the REST endpoint as:
####
####   https://server:8200/v1/${path_transit_mount}/datakey/plaintext/${datakey_name}
####
path_transit_mount="transit"
path_kv_mount="apixio_encryption"
v1key="v1"

################################################################
#### role_name is the name of the role in Vault that this local instance
#### will use when performing "approle authentication".
####
role_name="dev-role"

################################################################
#### global_datakey_name is the name of the global/default "transit" key in
#### Vault that is the "level 1" AES-256 key that is used to generate datakeys.
global_datakey_name="local-phi-key"

################################################################
#### 
curkey_base="curkeys"
curkey_global_secret="$global_datakey_name"
curkey_nonglobal_secret_prefix="local-pds-"

################################################################
#### xyzscope_datakey_name is the name of a sample scope (pds) "transit" key in
#### Vault that is the "level 1" AES-256 key that is used to generate datakeys.
xyzscope_datakey_name="local-pds-xyz"       # "pds-" prefix is in config; "xyz" is the scope

################################################################
# hackish:  vault startup writes root token here so for convenience we read it here for non-startup commands
if [ -r "$HOME/.vault-token" ]; then VAULT_TOKEN=$(cat $HOME/.vault-token); fi
