
# this will publish the following f(x) definitions to staging MCS server:
#
#

cd $(dirname $0)
scripts=$(cd ../../scripts; pwd -P)

for mcc in \
    mc-configs/anno.publish \
    mc-configs/capv.publish \
    mc-configs/har2.publish \
    mc-configs/slimlynty.publish \
    mc-configs/structsuspects.publish
do

    echo "\n################ Publishing MC at $mcc"

    $scripts/publish.sh creator=smccarty@apixio.com config=pubconfig.yaml publish=$mcc

done
