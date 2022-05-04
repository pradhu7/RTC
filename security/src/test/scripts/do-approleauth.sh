cd $(dirname $0)

if [ ! -r "config.sh" ]; then echo "Error:  missing config.sh in $(pwd)"; exit 1; fi
. config.sh

# dup of get-rolesecret.sh
role_id=$($VAULT_EXE read auth/approle/role/$role_name/role-id | grep role_id | awk '{print $NF}')
secret_id=$($VAULT_EXE write -f auth/approle/role/$role_name/secret-id | grep "secret_id " | awk '{print $NF}')

$VAULT_EXE write auth/approle/login role_id=$role_id secret_id=$secret_id
