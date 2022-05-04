
################ template stuff for doing things like checking deps and changing lifecycle state, since mctool doesn't work on these MCIDs

. mcids.sh

################

MCS="https://modelcatalogsvc.apixio.com:8443"

function urlencode () {
  echo $(echo $1 | sed 's/:/%3a/g' | sed 's/\//%2f/g')
}

################

for i in $anno_mcid $capv_mcid $har2_mcid $slim_mcid $susp_mcid
do
    curl -s -X GET $MCS/mcs/models/$i/metadata | python -m json.tool
done

# verify logical deps

LOGID="X10-295-Union-New-Zero:2.9.5/10.0.0"
LOGID="CAPV_fx:1.0.4/1.0.1-V23"
curl -s -X GET $MCS/mcs/logids/$(urlencode $LOGID)/deps | python -m json.tool

# verify ownership
curl -s -X GET "$MCS/mcs/logids/$(urlencode $LOGID)/deps/owners?mode=uses"
curl -s -X GET "$MCS/mcs/logids/$(urlencode $LOGID)/owner"

# promote to ACCEPTED
for i in $anno_mcid $capv_mcid $har2_mcid $slim_mcid $susp_mcid
do
    curl -s -X PUT -H "Content-Type: text/plain" --data-binary 'ACCEPTED' $MCS/mcs/models/$i/state
done

