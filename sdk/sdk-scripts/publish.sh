
BC_HOME=$HOME/bc-jars

if [ ! -d $BC_HOME ]; then echo "Error:  missing $BC_HOME directory; this directory must contain bc-fips-*.jar and bcprov-jdk15on-*.jar"; usage; exit 1; fi

bcprov=$(ls $BC_HOME/bcprov-jdk15on-*.jar | tail -1)     # tail -1 to get the latest
bcfips=$(ls $BC_HOME/bc-fips-*.jar | tail -1)

VAULT_TOKEN=${VAULT_TOKEN:-$APX_VAULT_TOKEN}
if [ "$VAULT_TOKEN" = "" ]
then
    if [ -f $HOME/.vault-token ]
    then
	VAULT_TOKEN=$(cat $HOME/.vault-token)
	LOCAL_VAULT=1
	echo "[info] Using Vault token from $HOME/.vault-token"
    else
	echo "An exported VAULT_TOKEN with a valid Vault token is required."
	exit 1
    fi
fi

if [ "$LOCAL_VAULT" = "" -a "$APXSECV2_VAULT_SERVER" = "" ]
then
    read -p "Use staging vault server [Y/n] " stg
    if [ "$stg" = "" -o "$stg" = "y" ]
    then
	unset APXSECV2_VAULT_SERVER     # default in code is to use staging vault
    else
	APXSECV2_VAULT_SERVER="https://vault.apixio.com:8200"
    fi
    
fi

vault="-DAPX_VAULT_TOKEN=$VAULT_TOKEN"
sec="-Dbcprovpath=$bcprov -Dbcfipspath=$bcfips"

CP=$(echo $(dirname $0)/../cmdline/target/apixio-sdk-cmdline-*.jar | sed 's/ /:/g')

export APXSECV2_VAULT_SERVER
java -cp $CP $sec $vault com.apixio.sdk.cmdline.PublishFxImpl "$@"
