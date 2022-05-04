# run from buildable/ directory

. $(dirname $0)/v1_setup.sh

# usage:  DecryptV1Data in out
$JAVA -cp $cp -Dkeymanager_key=puttherealoldpasswordhere com.apixio.v1sectest.DecryptDataWithV1 "$@"
