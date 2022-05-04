
# this will publish the following f(x) definitions to staging MCS server:
#
#  'list<apixio.Signal> extractSignals(apixio.PageWindow)'              HAR2
#  'list<apixio.Signal> transformSignals(list<apixio.Signal>)'          Annotations xform
#  'list<apixio.Signal> extractPatientSignals(apixio.Patient)'          StructuredSuspect, SlimLynty
#  'list<apixio.Event> combineSignals(apixio.SignalGroups)'             CAPV
#

cd $(dirname $0)
scripts=$(cd ../../scripts; pwd -P)

for fx in \
    'list<apixio.Signal> extractSignals(apixio.PageWindow)'       \
    'list<apixio.Signal> transformSignals(list<apixio.Signal>)'   \
    'list<apixio.Signal> extractPatientSignals(apixio.Patient)'   \
    'list<apixio.Event>  combineSignals(apixio.SignalGroups)'
do

    echo "\n################ Creating f(x) for IDL $fx"

    name=$(echo $fx | awk '{print $NF}' | awk -F\( '{print $1}')

    $scripts/publish-fxdef.sh "$fx" $creator $name $name

done

