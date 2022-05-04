base=$(dirname $0)
cd ${base}

if [ "$#" -eq "0" ]; then echo "Usage:  $0 [-l /path/to/libs] -c cassandraHost -a aclColumnFamily -r redishost -k redisKeyPrefix -u useremailwithrootrole"; exit 1; fi

if [ ! -d conf ]; then echo "Missing 'conf' directory."; exit 1; fi

# usage:  migrate-to-3.0.0 [-l /path/to/libs] -c cassandraHost -a aclColumnFamily -r redishost -k redisKeyPrefix -u useremailwithrootrole
#  -l defaults to /usr/lib/apx-useracct/lib

libs="/usr/lib/apx-useracct/lib"

while getopts l:c:a:r:k:u: opt ; do
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
	esac
done
	
if [ ! -d "$libs" ]; then echo "invalid jar directory [$libs]"; exit 1; fi

if [ "$chost"     = "" ]; then echo "Missing option [-c cassandraHost] for cassandra hosts.  Comma-separated list of IP addresses/DNS."; exit 1; fi
if [ "$aclcf"     = "" ]; then echo "Missing option [-a aclColumnFamily] for ACL column family name."; exit 1; fi
if [ "$rhost"     = "" ]; then echo "Missing option [-r redisHost] for redis host.  IP address/DNS."; exit 1; fi
if [ "$keyprefix" = "" ]; then echo "Missing option [-k prefix] for redis keyPrefix."; exit 1; fi
if [ "$rootuser"  = "" ]; then echo "Missing option [-u rootUserEmail] for email address of user with ROOT role."; exit 1; fi

#exit 0

clp=$(echo ${libs}/*.jar | sed 's/ /:/g')

#### list of email template names to transfer to redis
templates="coderforgotpass forgotpassword newaccount passwordforcereset"

# order of migration actions:
#
# 1.  make sure dw-useracct process is NOT running; can prompt user if it is running and then kill it
# 2.  java com.apixio.useracct.cmdline.RbacBootstrap conf/bootstrap-ops.yaml
# 3.  java com.apixio.useracct.cmdline.RbacBootstrap conf/bootstrap-data.yaml
# 4.  java com.apixio.useracct.cmdline.TemplateXfer conf/bootstrap.yaml $templates
# 5.  tell user to start up dw-useracct service

common="-Dacl.columnFamily=$aclcf -Dredis.host=$rhost -Dredis.keyPrefix=$keyprefix -Dcassandra.hosts=$chost"

dwua_pid=$(ps -ef | grep apixio-useracct-dw | grep -v grep | awk '{print $2}')
if [ "$dwua_pid" != "" ]; then echo "apixio-useracct-dw process is running.  Migration requires that this process not be running so please manually kill process $dwua_pid"; exit 1; fi

echo "################################################################"
echo "################################################################"
echo ">>>> Loading new Operations"
echo "################################################################"
echo "################################################################"
java -cp ${clp} $common com.apixio.useracct.cmdline.RbacBootstrap -c conf/migration.yaml -b conf/bootstrap-ops.yaml -u $rootuser
echo ""
echo ""

echo "################################################################"
echo "################################################################"
echo ">>>> Loading RBAC data"
echo "################################################################"
echo "################################################################"
java -cp ${clp} $common com.apixio.useracct.cmdline.RbacBootstrap -c conf/migration.yaml -b conf/bootstrap-data.yaml -u $rootuser
echo ""
echo ""

echo "################################################################"
echo "################################################################"
echo ">>>> Transferring email templates to redis"
echo "################################################################"
echo "################################################################"
java -cp ${clp} $common com.apixio.useracct.cmdline.TemplateXfer -c conf/migration.yaml $templates
echo ""
echo ""

echo "################################################################"
echo "################################################################"
echo "Migration steps are complete.  Review output and if there are no"
echo "errors then restart user-account dropwizard service."
echo "################################################################"
echo "################################################################"

