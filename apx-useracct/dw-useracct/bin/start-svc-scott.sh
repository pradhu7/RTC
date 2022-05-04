base=$(dirname $0)/..

YAML="src/main/resources/user-account.yaml"
#YAML="src/main/resources/user-account-stg.yaml"
#YAML="/Users/dyee/work/user-account/dw-useracct/bin/user-account.yaml"

export APX_VAULT_TOKEN=$(cat $HOME/.vault-token)
export APXSECV2_LOGCONFIG=true

exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5008 \
          -D_bootprops=$HOME/ua.properties \
	  -DmicroserviceConfig.apiaclConfig.apiAclDefs=src/main/resources/apiacls.json \
	  -Dbcprovpath=$HOME/bc-jars/bcprov-jdk15on-1.52.jar \
	  -Dbcfipspath=$HOME/bc-jars/bc-fips-1.0.1.jar \
	  -jar ${base}/target/apixio-useracct-dw-*.jar server $YAML
