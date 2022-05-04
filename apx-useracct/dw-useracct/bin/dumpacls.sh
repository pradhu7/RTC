base=$(dirname $0)/..
cd ${base}
tar=target/classes
clp=$(echo ${tar} ${tar}/lib/*.jar | sed 's/ /:/g')

java -Dacl.columnFamily=apx_cfAcl -cp ${clp} com.apixio.useracct.cmdline.DumpAcls -c conf/dumpacls.yaml

