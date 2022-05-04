

. mcids.sh
. eccnodes.sh

cat > /tmp/har2.json <<eof
{
"inboundTopic":"lambda-request-topic-prd",
"outboundTopic":"lambda-return-topic-prd",
"errorTopic":"lambda-topic-error-fx",
"deadLetterTopic":"lambda-topic-deadletter-fx",
"parallelism":1,
"algorithmID":"$har2_mcid",
"evalText":"pageWindows(request('docuuid'))",
"compJars":"",
"accessors":"com.apixio.accessors.PageWindowsAccessor,com.apixio.accessors.SinglePartialPatientAccessor",
"converters":"com.apixio.converter.SignalConverter",
"dumCreator":"com.apixio.umcs.Har2TestUmCreator",
"javaBindings":"com.apixio.binding.DefaultJavaBindings"
}
eof

curl -v --insecure -X POST \
     -H "Content-Type: application/json" \
     -H "Accept: application/json"  \
     --data-binary @/tmp/har2.json \
     https://$har2_host:8443/the-pagmatic-ecc/initialize
