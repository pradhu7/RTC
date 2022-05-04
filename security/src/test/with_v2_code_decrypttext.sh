# run from buildable/ directory

. $(dirname $0)/v2_setup.sh

# usage:  DecryptV1Text [-in file -out file] [-hex] ...

# this also tests -Dbcfipspath and -Dbcprovpath
$JAVA -cp $cp $($GET_ROLESECRET props) -Dbcfipspath=$bcfips -Dbcprovpath=$bcprov com.apixio.v2sectest.DecryptTextWithV2 "$@"
