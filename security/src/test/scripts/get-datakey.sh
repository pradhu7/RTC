cd $(dirname $0)

if [ ! -r "config.sh" ]; then echo "Error:  missing config.sh in $(pwd)"; exit 1; fi
. config.sh

curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/$path_transit_mount/datakey/plaintext/$global_datakey_name | python -m json.tool
