# run from buildable/ directory

. $(dirname $0)/v2_setup.sh

v1mode="-Dapx-security-v1-encryption-mode=true"

# usage:  EncryptV2Text -in file -out file [-hex] ...
$JAVA -cp $cp $v1mode $($GET_ROLESECRET props) -Dbcfipspath=$bcfips -Dbcprovpath=$bcprov com.apixio.v2sectest.EncryptTextWithV2 "$@"
