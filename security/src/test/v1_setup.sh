source $(dirname $0)/vall_setup.sh

# "." to pick up apixio-security.properties resource in classpath
cp=$(echo . src/test/resources $bcprov target/apixio-security-*.jar $cp | sed 's/ /:/g')
