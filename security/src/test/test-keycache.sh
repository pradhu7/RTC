
cd $(dirname $0)/../..
scr=$(pwd)/src/test

randtext="/tmp/randomtext"

hexdump -C -n 2000 /dev/random > $randtext.sm
rm -f $randtext
for i in {1..2}; do
  cat $randtext.sm $randtext.sm >> $randtext
done

function compare() {
    diff $1 $2 >& /dev/null
    if [ "$?" -eq "0" ]
    then
	echo "=======> Test [$3] succeeded"
    else
	echo "=======> Uh oh, test [$3] FAILED"
    fi
}

if [ "$VAULT_HOME" = "" ]; then echo "This test requires env var VAULT_HOME to be set for key rotation"; exit 1; fi

echo -e "\n################################################################"
echo -e "######### Encrypting data with multiple datakeys and scopes"
echo -e "################################################################"

################
## encrypt data only using v2
################

for i in {1..10}; do

export scope=""
$scr/with_v2_code_encryptdata.sh -in ${randtext} -out ${randtext}.dkdata_$i >& /dev/null

export scope="-scope xyz"
$scr/with_v2_code_encryptdata.sh -in ${randtext} -out ${randtext}.dkdatascope_$i >& /dev/null

$scr/scripts/do-rotatekey.sh >& /dev/null
$scr/scripts/do-rotatekey.sh xyz >& /dev/null

done

################
## decrypt data using v2 code.
################

################################################################
echo -e "\n################ v2 decryption in data mode"

rm -rf ${randtext}.outdir
mkdir ${randtext}.outdir

# two copies of dkdata files to make sure it doesn't refetch keys
$scr/with_v2_code_decryptdata.sh -outdir ${randtext}.outdir ${randtext}.dkdata* ${randtext}.dkdata*


