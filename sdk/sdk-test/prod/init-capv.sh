

. mcids.sh
. eccnodes.sh

cat > /tmp/capv.json <<eof
{
"inboundTopic":"lambda-request-topic-prd",
"outboundTopic":"lambda-return-topic-prd",
"errorTopic":"lambda-topic-error-fx",
"deadLetterTopic":"lambda-topic-deadletter-fx",
"parallelism":1,
"algorithmID":"$capv_mcid",
"evalText":"signalGroup(request('patientuuid'),'$har2_mcid','$susp_mcid','$slim_mcid','$anno_mcid')",
"compJars":"",
"accessors":"com.apixio.accessors.SignalGroupsAccessor",
"converters":"com.apixio.converter.SignalConverter,com.apixio.converter.EventTypeConverter",
"dumCreator":"com.apixio.umcs.AlgoPatientTestUmCreator",
"javaBindings":"com.apixio.binding.DefaultJavaBindings"
}
eof

curl -v --insecure -X POST \
     -H "Content-Type: application/json" \
     -H "Accept: application/json"  \
     --data-binary @/tmp/capv.json \
     https://$capv_host:8443/the-pagmatic-ecc/initialize
