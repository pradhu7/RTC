
BC_HOME=$HOME/bc-jars

if [ ! -d $BC_HOME ]; then echo "Error:  missing $BC_HOME directory; this directory must contain bc-fips-*.jar and bcprov-jdk15on-*.jar"; usage; exit 1; fi

bcprov=$(ls $BC_HOME/bcprov-jdk15on-*.jar | tail -1)     # tail -1 to get the latest
bcfips=$(ls $BC_HOME/bc-fips-*.jar | tail -1)

VAULT_TOKEN=${VAULT_TOKEN:-$APX_VAULT_TOKEN}
if [ "$VAULT_TOKEN" = "" ]; then echo "An exported VAULT_TOKEN or APX_VAULT_TOKEN with a valid Vault token is required."; exit 1; fi

vault="-DAPX_VAULT_TOKEN=$VAULT_TOKEN"
sec="-Dbcprovpath=$bcprov -Dbcfipspath=$bcfips"

exec java $vault $sec -cp target/modelcatalogsvc-*.jar com.apixio.mcs.admin.dw.McsApplication server src/main/resources/mcs.yaml
