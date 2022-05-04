cd $(dirname $0)

if [ ! -r "config.sh" ]; then echo "Error:  missing config.sh in $(pwd)"; exit 1; fi
. config.sh

out="/tmp/_text$$"

function rotate () {
    echo ""
    echo ""
    echo "################################################################"
    echo "################################################################"
  keyname=$1

#echo  curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/$path_transit_mount/keys/$keyname/rotate
  curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/$path_transit_mount/keys/$keyname/rotate

#echo  curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/$path_transit_mount/datakey/plaintext/$keyname
  curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/$path_transit_mount/datakey/plaintext/$keyname \
      | python -m json.tool \
      | grep 'text":' > $out

#echo $VAULT_EXE kv put $path_kv_mount/$curkey_base/$keyname \
#	   ciphertext=$(grep ciphertext $out | awk '{print $2}' | sed 's/"//g' | sed 's/,//') \
#	   plaintext=$( grep  plaintext $out | awk '{print $2}' | sed 's/"//g')

  $VAULT_EXE kv put $path_kv_mount/$curkey_base/$keyname \
	   ciphertext=$(grep ciphertext $out | awk '{print $2}' | sed 's/"//g' | sed 's/,//') \
	   plaintext=$( grep  plaintext $out | awk '{print $2}' | sed 's/"//g')

echo  $VAULT_EXE kv get $path_kv_mount/$curkey_base/$keyname
  $VAULT_EXE kv get $path_kv_mount/$curkey_base/$keyname

  rm -f $out
}

if [ "$1" = "" ]
then
  key=$curkey_global_secret
else
  key=${curkey_nonglobal_secret_prefix}$1
fi

rotate $key
