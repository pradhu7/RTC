

. mcids.sh
. eccnodes.sh

cat > /tmp/slim.json <<eof
{
"inboundTopic":"lambda-request-topic-prd",
"outboundTopic":"lambda-return-topic-prd",
"errorTopic":"lambda-topic-error-fx",
"deadLetterTopic":"lambda-topic-deadletter-fx",
"parallelism":1,
"algorithmID":"$slim_mcid",
"evalText":"patient(request('patientuuid'))",
"compJars":"",
"accessors":"com.apixio.accessors.PageWindowsAccessor,com.apixio.accessors.PatientAccessor",
"converters":"com.apixio.converter.SignalConverter",
"dumCreator":"com.apixio.umcs.AlgoPatientTestUmCreator",
"javaBindings":"com.apixio.binding.DefaultJavaBindings"
}
eof

curl -v --insecure -X POST \
     -H "Content-Type: application/json" \
     -H "Accept: application/json"  \
     --data-binary @/tmp/slim.json \
     https://$slim_host:8443/the-pagmatic-ecc/initialize
