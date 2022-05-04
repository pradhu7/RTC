
# usage:  Security v1password v1version

cd $(dirname $0)/../..   # to buildable/security dir

source src/test/vall_setup.sh

export APX_VAULT_TOKEN="$(cat $HOME/.vault-token)"

icp=$(echo target/apixio-security-*.jar target/test-classes | sed 's/ /:/g')

$JAVA -cp $icp:$cp -Dbcfipspath=$bcfips -Dbcprovpath=$bcprov com.apixio.security.Security "$@"
