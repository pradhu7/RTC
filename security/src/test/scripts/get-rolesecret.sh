cd $(dirname $0)

if [ ! -r "config.sh" ]; then echo "Error:  missing config.sh in $(pwd)" 1>&2 ; exit 1; fi
. config.sh

role_id=$($VAULT_EXE read auth/approle/role/$role_name/role-id | grep role_id | awk '{print $NF}')
secret_id=$($VAULT_EXE write -f auth/approle/role/$role_name/secret-id | grep "secret_id " | awk '{print $NF}')

if [ "$1" = "json" ]
then
    dq='"'
    echo "{${dq}role_id${dq}:${dq}${role_id}${dq},${dq}secret_id${dq}:${dq}${secret_id}${dq}}"
elif [ "$1" = "props" ]
then
    echo "-DAPX_VAULT_ROLE_ID=$role_id -DAPX_VAULT_SECRET_ID=$secret_id"
else    
    # for setting env vars
    echo "export APX_VAULT_ROLE_ID=$role_id"
    echo "export APX_VAULT_SECRET_ID=$secret_id"
fi
