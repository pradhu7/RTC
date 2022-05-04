# run from buildable/ directory

. $(dirname $0)/v2_setup.sh

# usage:  EncryptV2Data [-scope scope] -in in -out out

# this also tests specifying BC jars via env vars
APXSECV2_BCFIPSPATH=$bcfips APXSECV2_BCPROVPATH=$bcprov $JAVA -cp $cp $($GET_ROLESECRET props) com.apixio.v2sectest.EncryptDataWithV2 $scope "$@"
