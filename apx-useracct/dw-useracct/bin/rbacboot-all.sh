base=$(dirname $0)/..
cd ${base}

bin/rbacboot.sh -c conf/bootstrap.yaml -b conf/bootstrap-ops.yaml 
bin/rbacboot.sh -c conf/bootstrap.yaml -b conf/bootstrap-data.yaml 
