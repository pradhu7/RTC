#!/bin/bash

# either ROLE_ID and SECRET_ID must be set, or TOKEN must be set:
#export APX_VAULT_ROLE_ID=
#export APX_VAULT_SECRET_ID=
export APX_VAULT_TOKEN=

# these must be encrypted; use com.apixio.security.Security.main() with args of "encrypt somestring"
export S3ACCESSKEY=""
export S3SECRETKEY=""

# these must exist in classpath (which is set below in the script to include src/main/resources)
export bcfipsname=bc-fips-1.0.1.jar
export bcprovname=bcprov-jdk15on-1.47.jar

# one of DECRYPT_ONLY, ENCRYPT_ONLY, ENCRYPT_REPLACE
export RUN_MODE=DECRYPT_ONLY

# force v2 encryption
export APXSECV2_V1_ENCRYPTION_MODE=false

classpath=$(echo src/main/resources target/apixio-s3-lambdafuncs-*.jar | sed 's/ /:/g')

# to test plucking of PDS ID from key, if the first arg is "-" then all 4 params must be passed in
if [ "$1" = "-" ]
then
    shift
    java -cp $classpath \
	 com.apixio.s3lambda.S3RunLocal "$@"
else
    java -cp $classpath \
	 com.apixio.s3lambda.S3RunLocal apixio-documents-test-oregon document1111/blb7/a546/4f-d/ca2-/4615/-bab/0-e8/24f5/f5d162::png::200::1 "" false
fi
