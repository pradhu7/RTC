# run from buildable/ directory

. $(dirname $0)/v2_setup.sh

v1mode="-Dapx-security-v1-encryption-mode=true"

# usage:  EncryptV2Text -in file -out file [-hex] ...

# this also tests specifying BC jars via env vars
APXSECV2_BCFIPSPATH=$bcfips APXSECV2_BCPROVPATH=$bcprov $JAVA -cp $cp $v1mode $($GET_ROLESECRET props) com.apixio.v2sectest.EncryptDataWithV2 "$@"
