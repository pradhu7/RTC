base=$(dirname $0)/..
cd ${base}
tar=target/classes
clp=$(echo ${tar} ${tar}/lib/*.jar | sed 's/ /:/g')

java -cp ${clp} com.apixio.useracct.cmdline.MigrateAclsToRedis -c conf/migacls.yaml $*

