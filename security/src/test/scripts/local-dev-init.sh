################ This script is supposed to locally mirror the vault setup that will be
################ done by system administrators on staging and production.  Since the "dev"
################ mode of vault doesn't persist anything, this must be run each time the
################ local vault server must be started

cd $(dirname $0)

if [ ! -r "config.sh" ]; then echo "Error:  missing config.sh in $(pwd)"; exit 1; fi
. config.sh

################

(
cd $VAULT_HOME

#### start up server in dev mode
echo -e "\n\n################ Starting up vault server at $(date)" >> server.log
$VAULT_EXE server -dev >> server.log 2>&1 &       # successful startup will put root token in ~/.vault-token
sleep 1


#### put old master password; note that we're using v1!
$VAULT_EXE secrets enable -path=$path_kv_mount -version=1 kv


#### enable approle; token value for 1 day, etc.
$VAULT_EXE auth enable approle
$VAULT_EXE write auth/approle/role/$role_name\
    secret_id_ttl=1440m \
    token_num_uses=100 \
    token_ttl=1440m \
    token_max_ttl=1440m \
    secret_id_num_uses=100

#### set up ACL policy for the role_name
# note that the wildcard parts of the v1 password path of $path_kv_mount/+/*
# means that the value of v1_secret can't have a "/" in it (i believe...)

cat  > /tmp/thepolicy.hcl <<eof
# for creating datakeys.  note that both create and update perms are required for datakey creation for some reason:
path "$path_transit_mount/datakey/plaintext/*"
{
    capabilities = ["create", "update", "read", "list"]
}

# for decrypting datakeys:
path "$path_transit_mount/decrypt/*"
{
    capabilities = ["create", "update", "read", "list"]
}

# for getting v2 "current datakey"
path "$path_kv_mount/curkeys/*"
{
    capabilities = ["read", "list"]
}

# for getting v1 decryption key
path "$path_kv_mount/*"
{
    capabilities = ["read", "list"]
}
eof
$VAULT_EXE policy write thepolicyname /tmp/thepolicy.hcl
# delete:  $VAULT_EXE delete sys/policy/thepolicyname

$VAULT_EXE write auth/approle/role/$role_name policies="thepolicyname,default"


#### set up transit and datakey info
$VAULT_EXE secrets enable -path=$path_transit_mount transit
)

################ low-level vault setup is done; now use ../init-vault.sh to invoke Java cmd to finish

