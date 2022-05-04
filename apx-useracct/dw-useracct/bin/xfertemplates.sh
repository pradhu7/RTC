base=$(dirname $0)/..
cd ${base}
tar=target/classes
clp=$(echo ${tar} ${tar}/lib/*.jar | sed 's/ /:/g')

if [ $# -ne 0 ]
then
  args="$*"
else
  args="coderforgotpass forgotpassword newaccount passwordforcereset"
fi

java -cp ${clp} com.apixio.useracct.cmdline.TemplateXfer -c conf/bootstrap.yaml $args
