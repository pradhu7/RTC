source $(dirname $0)/vall_setup.sh

# "." to pick up apixio-security.properties resource in classpath
cp=$(echo src/test src/test/resources target/apixio-security-*.jar $cp | sed 's/ /:/g')

# for get-rolesecret.sh testing:
export VAULT_HOME=$HOME/apixio/vault
export GET_ROLESECRET="$HOME/apixio/git/mono/security/src/test/scripts/get-rolesecret.sh"

# actually evaluates and exports env vars
eval $($GET_ROLESECRET)
