base=$(dirname $0)/..
cd ${base}
tar=target/classes
clp=$(echo ${tar} ${tar}/lib/*.jar | sed 's/ /:/g')

java -cp ${clp} com.apixio.useracct.cmdline.RbacBootstrap $*
