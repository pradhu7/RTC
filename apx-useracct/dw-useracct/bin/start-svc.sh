base=$(dirname $0)/..

YAML="src/main/resources/user-account-stg.yaml"
#YAML="/Users/dyee/work/user-account/dw-useracct/bin/user-account.yaml"

exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5008 -D_bootprops=$HOME/ua.properties -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost -DmicroserviceConfig.apiaclConfig.apiAclDefs=src/main/resources/apiacls.json -Dbcprovpath=$HOME/bc-jars/bcprov-jdk15on-1.47.jar -Dbcfipspath=$HOME/bc-jars/bc-fips-1.0.2.jar -jar ${base}/target/apixio-useracct-dw-*.jar server $YAML
