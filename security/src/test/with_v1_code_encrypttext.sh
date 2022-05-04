# run from buildable/ directory

. $(dirname $0)/v1_setup.sh

# usage:  EncryptV1Text [-in file -out file] [-hex] ...
$JAVA -cp $cp -Dkeymanager_key=puttherealoldpasswordhere com.apixio.v1sectest.EncryptTextWithV1 "$@"
