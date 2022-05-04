function pause(){
 read -s -n 1 -p "Press any key to continue . . ."
 echo ""
}

# FILE=resources/model.zip
# if [ -f "$FILE" ]; then
#     echo "$FILE exists."
# else 
#     echo "=====FAIL=====\n$FILE does not exist. You need to download this file as $FILE:"
#     echo "https://repos.apixio.com/artifactory/webapp/#/artifacts/browse/tree/General/models/models/production_model/9.4.0/production_model_9_4_0.zip"
#     exit
# fi

setup_dir="/tmp/pythonfxtest"
setup_dir="/Users/bpeintner/Downloads/pythonfxtest"

#### set things up so that the (not really there) asset URLs have the correct names
#### for the setup directory

if [ ! -d $setup_dir ]
then
    mkdir -p $setup_dir
    cp resources/* $setup_dir/
fi

cd ..
# always copy since we want latest code
pyTest_impl_jar="fximplcode/target/apixio-fx-impls-1.0.8-SNAPSHOT.jar"
pyInstallerExec="/Users/bpeintner/Downloads/pythonfxtest/sdkannotest"
cp $pyTest_impl_jar $setup_dir
cp $pyInstallerExec $setup_dir

comp_jars=$(echo accessors/target/apixio-fx-accessors*.jar \
                 $setup_dir/*.jar \
                | sed 's/ /,/g')

# fake base dir for assets in that actual assets files won't exist here (we just
# want to match on last element of path/name).  it just has to be a parseable URL
fbase=file://$(pwd -P)

#### these two steps create the protobuf.fx def and impl info

anno_def_file="${setup_dir}/anno.fx"
FXD='list<apixio.Signal> transformSignals(list<apixio.Signal>)'
echo "./scripts/mkfxdef.sh idl="$FXD" out=${anno_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${anno_def_file}


anno_impl_file="$setup_dir/annoimpl.fxi"
anno_entry="svc::AnnotationFx"
echo "./scripts/mkfximpl.sh def=${anno_def_file} impl=${pyInstallerExec} entry=${anno_entry} out=${anno_impl_file} config=$fbase/test/resources/config.yaml model.zip=$fbase/model.zip"
# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${anno_def_file} \
              impl="file://${pyInstallerExec}" \
              entry=${anno_entry} \
              out=${anno_impl_file} \
              \
              config=$fbase/test/resources/config.yaml

#### this step uses the fximpl info to load and invoke the f(x) implementation

# accessors (besides built-in ones)
ACC_PWS=com.apixio.accessors.PageWindowsAccessor
ACC_APO=com.apixio.accessors.SinglePartialPatientAccessor
# ACC_APO2=com.apixio.accessors.JSONPatientAccessor
ACC_APO3=com.apixio.accessors.PatientAccessor
ACC_SG=com.apixio.accessors.SignalGroupsAccessor
AC_PAA=com.apixio.accessors.S3PatientAnnotationsAccessor
ACC_ALL=$ACC_PWS,$ACC_APO,$ACC_APO3,$ACC_SG,$AC_PAA

# converters
CVT_SIGNAL=com.apixio.converter.SignalConverter,com.apixio.converter.EventTypeConverter

# writer (dataURI manager) (u)
AGLODUM_APXQUERY=com.apixio.umcs.AlgoPatientTestUmCreator

# eval config options
EVC_ANNO='patientannotations(request("annopatientuuid"))'

ANNO_QUERY="apxquery://devel/q?patientuuid=48156abe-cbe8-440a-9dfd-42b4ea7b223d&algo=com.apixio.ensemblesdk.impl.AnnotationFx"

anno_eval_file="$setup_dir/anno_genericecc.eval"
./scripts/mkeval.sh eval="$EVC_ANNO" out="${anno_eval_file}"


# Run Annotations
./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir}/annoimpl.fxi \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_ALL \
             converters=$CVT_SIGNAL \
             umcreator=$AGLODUM_APXQUERY \
             evalfile=${anno_eval_file}
