#!/bin/bash

CDUMP="java -cp dw-useracct/target/apixio-useracct-dw*.jar com.apixio.useracct.cmdline.ConfigDump"

# this MUST match what's in dw-useracct/bin/start-svc.sh:
BOOT="$HOME/ua.properties"

YAML="dw-useracct/src/main/resources/user-account.yaml"
ROOT_EMAIL="root@api.apixio.com"
ROOT_PASS="thePassword"

KEYPREFIX="$($CDUMP -bootconfig ${YAML} -bootprops ${BOOT} | grep microserviceConfig.persistenceConfig.redisConfig.keyPrefix | awk '{print $NF}')"

echo "Using KEYPREFIX $KEYPREFIX"

alias rediscli="~/apixio/redis/redis-cli"


################################################################
function startkill_dw () {
	(cd dw-useracct; bin/start-svc.sh) & pid=$!
	sleep 7

	# big hack:  look for any pids that look like what we started
    pid=$(ps -ef | grep $pid | grep "java " | grep " -jar" | awk '{print $2}')
    if [ "$pid" != "" ]; then
	  echo "Killing dw-useracct process now"
	  kill -9 $pid
	fi

	# the "-" in -$pid _should_ kill processgroup but it doesn't work...
	#kill -s INT -$pid
}


################################################################ main code:

dwua_pid=$(ps -ef | grep apixio-useracct-dw | grep -v grep | awk '{print $2}')
if [ "$dwua_pid" != "" ]; then echo "apixio-useracct-dw process is running.  Initialization requires that this process not be running so please manually kill process $dwua_pid"; exit 1; fi

dw-useracct/bin/bootstrap.sh $KEYPREFIX $ROOT_EMAIL $ROOT_PASS | rediscli

startkill_dw
sleep 3

(cd dw-useracct/migration; ./migrate-to-3.0.0.sh -l ../target -c localhost -a apx_cfAcl -r localhost -k $KEYPREFIX -u $ROOT_EMAIL)

startkill_dw
sleep 1

echo ""
echo "################### User-account system initialized"
