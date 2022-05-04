
CP=$(echo $(dirname $0)/../cmdline/target/apixio-sdk-cmdline-*.jar | sed 's/ /:/g')

java -cp $CP com.apixio.sdk.cmdline.MakeEval "$@"
