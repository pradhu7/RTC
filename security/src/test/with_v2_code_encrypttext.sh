# run from buildable/ directory

. $(dirname $0)/v2_setup.sh

# usage:  EncryptV2Text [-scope scope] -in file -out file [-hex] ...

$JAVA -cp $cp $($GET_ROLESECRET props) -Dbcfipspath=$bcfips -Dbcprovpath=$bcprov com.apixio.v2sectest.EncryptTextWithV2 $scope "$@"
