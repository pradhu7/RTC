base=$(dirname $0)
cd ${base}

if [ "$#" -eq "0" ]; then echo "Usage:  $0 [-l /path/to/libs] -c cassandraHost -a aclColumnFamily -r redishost -k redisKeyPrefix -u useremailwithrootrole"; exit 1; fi

if [ ! -d conf ]; then echo "Missing 'conf' directory."; exit 1; fi

# usage:  migclean.sh [-l /path/to/libs] -c cassandraHost -a aclColumnFamily -r redishost -k redisKeyPrefix -u useremailwithrootrole -f data.yaml
#  -l defaults to /usr/lib/apx-useracct/lib

libs="/usr/lib/apx-useracct/lib"

while getopts l:c:a:r:k:u:f:m: opt ; do
	case $opt in
		l)
			libs="$OPTARG"
			;;
		c)
			chost="$OPTARG"
			;;
		a)
			aclcf="$OPTARG"
			;;
		r)
			rhost="$OPTARG"
			;;
		k)
			keyprefix="$OPTARG"
			;;
		u)
			rootuser="$OPTARG"
			;;
		f)
			datayaml="$OPTARG"
			;;
		m)
			mode="$OPTARG"
			;;
	esac
done
	
if [ ! -d "$libs" ]; then echo "invalid jar directory [$libs]"; exit 1; fi

if [ "$chost"     = "" ]; then echo "Missing option [-c cassandraHost] for cassandra hosts.  Comma-separated list of IP addresses/DNS."; exit 1; fi
if [ "$aclcf"     = "" ]; then echo "Missing option [-a aclColumnFamily] for ACL column family name."; exit 1; fi
if [ "$rhost"     = "" ]; then echo "Missing option [-r redisHost] for redis host.  IP address/DNS."; exit 1; fi
if [ "$keyprefix" = "" ]; then echo "Missing option [-k prefix] for redis keyPrefix."; exit 1; fi
if [ "$rootuser"  = "" ]; then echo "Missing option [-u rootUserEmail] for email address of user with ROOT role."; exit 1; fi
if [ "$datayaml"  = "" ]; then echo "Missing option [-f data.yaml] for all migration and cleaning details"; exit 1; fi

if [ "$mode"      = "" ]; then echo "Defaulting to test-mode (no changes will be made); override with '-m change'"; mode="test"; fi

#exit 0

clp=$(echo ${libs}/*.jar | sed 's/ /:/g')

# order of migration actions:
#
# .  java com.apixio.useracct.cmdline.MigClean -c <connection.yaml> -f <data.yaml> -u <rootuseremail> -m <test,change>

common="-Dacl.columnFamily=$aclcf -Dredis.host=$rhost -Dredis.keyPrefix=$keyprefix -Dcassandra.hosts=$chost"

echo "################################################################"
echo "################################################################"
echo ">>>> Migrating and cleaning"
echo "################################################################"
echo "################################################################"
java -cp ${clp} $common com.apixio.useracct.cmdline.MigClean -c conf/migration.yaml -f $datayaml -u $rootuser -m $mode
echo ""
echo ""

