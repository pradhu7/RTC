

. mcids.sh
. eccnodes.sh

cat > /tmp/anno.json <<eof
{
"inboundTopic":"lambda-request-topic-prd",
"outboundTopic":"lambda-return-topic-prd",
"errorTopic":"lambda-topic-error-fx",
"deadLetterTopic":"lambda-topic-deadletter-fx",
"parallelism":1,
"algorithmID":"$anno_mcid",
"evalText":"patientannotations(request('patientuuid'))",
"compJars":"",
"accessors":"com.apixio.accessors.S3PatientAnnotationsAccessor",
"converters":"com.apixio.converter.SignalConverter",
"dumCreator":"com.apixio.umcs.AlgoPatientTestUmCreator",
"javaBindings":"com.apixio.binding.DefaultJavaBindings"
}
eof

curl -v --insecure -X POST \
     -H "Content-Type: application/json" \
     -H "Accept: application/json"  \
     --data-binary @/tmp/anno.json \
     https://$anno_host:8443/the-pagmatic-ecc/initialize
