
# usage:  bootstrap.sh keyprefix email password

if [ "$1" = "" ]; then echo "Missing keyprefix.  Usage: bootstrap.sh keyprefix email password"; exit 1; fi
if [ "$2" = "" ]; then echo "Missing email address.  Usage: bootstrap.sh keyprefix email password"; exit 1; fi
if [ "$3" = "" ]; then echo "Missing root password.  Usage: bootstrap.sh keyprefix email password"; exit 1; fi

BASE=$(dirname $0)/..
CLASSPATH=${BASE}/target/apixio-useracct-dw*.jar
PREFIX=$1
EMAIL=$2
PASSWORD=$3

ROOTUSER="U_$(uuidgen)"

ROOTROLE="R_$(uuidgen)"
USERROLE="R_$(uuidgen)"

# the value of ROOTROLENAME must match the value of com.apixio.useracct.entity.Role.ROOT
# and .USER
ROOTROLENAME="ROOT"
USERROLENAME="USER"

CREATED="$(date '+%s')000"

BCRYPTED="$(java -cp $CLASSPATH com.apixio.useracct.dao.PrivUsers $PASSWORD | awk '{print $NF}')"

#
# Note that all of these constants below are taken from Java code (entity files, mostly)
#
cat <<oof
hmset ${PREFIX}${ROOTUSER}       id ${ROOTUSER} account-state ACTIVE roles ROOT bcrypt-pass ${BCRYPTED} created-at ${CREATED} email-addr ${EMAIL}
hset  ${PREFIX}users-x-byemail   ${EMAIL} ${ROOTUSER}
rpush ${PREFIX}users-x-all       ${ROOTUSER}

hmset ${PREFIX}${ROOTROLE}       id ${ROOTROLE} name ${ROOTROLENAME} created-at ${CREATED} description "The ROOT role allows access to all operations and data"
hset  ${PREFIX}roles-x-byname    ${ROOTROLENAME} ${ROOTROLE}
rpush ${PREFIX}roles-x-all       ${ROOTROLE}

hmset ${PREFIX}${USERROLE}       id ${USERROLE} name ${USERROLENAME} created-at ${CREATED} description "Normal user with no special privileges"
hset  ${PREFIX}roles-x-byname    ${USERROLENAME} ${USERROLE}
rpush ${PREFIX}roles-x-all       ${USERROLE}

oof
