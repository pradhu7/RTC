
cd $(dirname $0)/../..
scr=$(pwd)/src/test

randtext="/tmp/randomtext"

# let's use a big file of text for testing
hexdump -C -n 2000 /dev/random > $randtext.sm
rm -f $randtext
for i in {1..200}; do
  cat $randtext.sm $randtext.sm $randtext.sm $randtext.sm $randtext.sm $randtext.sm $randtext.sm $randtext.sm >> $randtext
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

# test cases:
#
# migration-related:
#
# * encrypt text using v1 code, decrypt using v1 code; for sanity sake
# * encrypt data using v1 code, decrypt using v1 code; for sanity sake
# * encrypt text using v1 code, decrypt using v2 code
# * encrypt data using v1 code, decrypt using v2 code
#
# v2-specific tests:
#
# * encrypt text using v2 code, decrypt using v2 code
# * encrypt data using v2 code, decrypt using v2 code
# * encrypt text using v2 code, decrypt using v2 code
# * encrypt data using v2 code, decrypt using v2 code

################
## encrypt text and data using v1
################

echo -e "\n################################################################"
echo -e "######### Only V1 code:  encrypting and decrypting"
echo -e "################################################################"

################################################################
echo -e "\n################ v1 encryption in data mode"
$scr/with_v1_code_encryptdata.sh ${randtext} ${randtext}.v1dataenc

################################################################
echo -e "\n################ v1 encryption in text mode"
$scr/with_v1_code_encrypttext.sh -in ${randtext} -out ${randtext}.v1textenc


################
## decrypt text and data using v1 code.  this better not fail
################


################################################################
echo -e "\n################ v1 decryption in data mode"
$scr/with_v1_code_decryptdata.sh ${randtext}.v1dataenc ${randtext}.v1datadec

compare ${randtext} ${randtext}.v1datadec "v1 data encryption, v1 decryption"

################################################################
echo -e "\n################ v1 decryption in text mode"
$scr/with_v1_code_decrypttext.sh -in ${randtext}.v1textenc -out ${randtext}.v1textdec

compare ${randtext} ${randtext}.v1textdec "v1 text encryption, v1 decryption"


echo -e "\n################################################################"
echo -e "######### V1 data w/ v2 code"
echo -e "################################################################"

################
## decrypt v1 text and data using v2 code
################


################################################################
echo -e "\n################ v2 decryption of v1 data in data mode"
$scr/with_v2_code_decryptdata.sh ${randtext}.v1dataenc ${randtext}.v2datadec

compare ${randtext} ${randtext}.v2datadec "v1 data encryption, v2 decryption"

################################################################
echo -e "\n################ v2 decryption of v1 text in text mode"
$scr/with_v2_code_decrypttext.sh -in ${randtext}.v1textenc -out ${randtext}.v2textdec

compare ${randtext} ${randtext}.v2textdec "v1 text encryption, v2 decryption"


echo -e "\n################################################################"
echo -e "######### Only v2 operations; no scope"
echo -e "################################################################"

################
## encrypt text and data using v2, no scope
################

################################################################
echo -e "\n################ v2 encryption in data mode"
$scr/with_v2_code_encryptdata.sh -in ${randtext} -out ${randtext}.v2dataenc


################################################################
echo -e "\n################ v2 encryption in text mode"
$scr/with_v2_code_encrypttext.sh -in ${randtext} -out ${randtext}.v2textenc


################
## decrypt text and data using v2 code.
################

################################################################
echo -e "\n################ v2 decryption in data mode"
$scr/with_v2_code_decryptdata.sh ${randtext}.v2dataenc ${randtext}.v2datadec

compare ${randtext} ${randtext}.v2datadec "v2 data encryption, v2 decryption"

################################################################
echo -e "\n################ v2 decryption in text mode"
$scr/with_v2_code_decrypttext.sh -in ${randtext}.v2textenc -out ${randtext}.v2textdec

compare ${randtext} ${randtext}.v2textdec "v2 text encryption, v2 decryption"


echo -e "\n################################################################"
echo -e "######### Only v2 operations; scope=xyz"
echo -e "################################################################"

################
## encrypt text and data using v2, scope=xyz
################

export scope="-scope xyz"

################################################################
echo -e "\n################ v2 encryption in data mode with scope"
$scr/with_v2_code_encryptdata.sh -in ${randtext} -out ${randtext}.v2dataenc


################################################################
echo -e "\n################ v2 encryption in text mode with scope"
$scr/with_v2_code_encrypttext.sh -in ${randtext} -out ${randtext}.v2textenc


################
## decrypt text and data using v2 code.
################

scope=

################################################################
echo -e "\n################ v2 decryption in data mode using embedded scope"
$scr/with_v2_code_decryptdata.sh ${randtext}.v2dataenc ${randtext}.v2datadec

compare ${randtext} ${randtext}.v2datadec "v2 data encryption, v2 decryption"

################################################################
echo -e "\n################ v2 decryption in text mode using embedded scope"
$scr/with_v2_code_decrypttext.sh -in ${randtext}.v2textenc -out ${randtext}.v2textdec

compare ${randtext} ${randtext}.v2textdec "v2 text encryption, v2 decryption"


################################################################
################################################################
################ v2 code encrypting to v1 tests
################################################################
################################################################

echo -e "\n################################################################"
echo -e "######### V2 code producing v1 data"
echo -e "################################################################"

################
## encrypt text and data using v2 but into v1 format
################

################################################################
echo -e "\n################ v2 encryption in data mode"
$scr/with_v2_code_encryptdata_to_v1.sh -in ${randtext} -out ${randtext}.v1dataenc

################################################################
echo -e "\n################ v2 encryption in text mode"
$scr/with_v2_code_encrypttext_to_v1.sh -in ${randtext} -out ${randtext}.v1textenc


################
## decrypt text and data using v1 code.  this better not fail
################

################################################################
echo -e "\n################ v1 decryption in data mode"
$scr/with_v1_code_decryptdata.sh ${randtext}.v1dataenc ${randtext}.v1datadec

compare ${randtext} ${randtext}.v1datadec "v1 data encryption, v1 decryption"

################################################################
echo -e "\n################ v1 decryption in text mode"
$scr/with_v1_code_decrypttext.sh -in ${randtext}.v1textenc -out ${randtext}.v1textdec

compare ${randtext} ${randtext}.v1textdec "v1 text encryption, v1 decryption"



echo -e "\n################################################################"
echo -e "######### V2 code producing v1 encrypted text using hex mode"
echo -e "################################################################"

################
## encrypt text using v2 but into v1 format in hex format
################

################################################################
echo -e "\n################ v2 encryption in text mode"
$scr/with_v2_code_encrypttext_to_v1.sh -in ${randtext} -out ${randtext}.v1textenc -hex true

################
## decrypt text and data using v1 code.  this better not fail
################

################################################################
echo -e "\n################ v1 decryption in text mode"
$scr/with_v1_code_decrypttext.sh -in ${randtext}.v1textenc -out ${randtext}.v1textdec -hex

compare ${randtext} ${randtext}.v1textdec "v1 text encryption, v1 decryption"

################################################################
echo -e "\n################ v2 decryption in text mode"
$scr/with_v2_code_decrypttext.sh -in ${randtext}.v1textenc -out ${randtext}.v2textdec -hex

compare ${randtext} ${randtext}.v2textdec "v2 text (hex) encryption, v2 decryption"

