
FILE=resources/model.zip
if [ -f "$FILE" ]; then
    echo "$FILE exists."
else 
    echo "=====FAIL=====\n$FILE does not exist. You need to download this file as $FILE:"
    echo "https://repos.apixio.com/artifactory/webapp/#/artifacts/browse/tree/General/models/models/production_model/9.4.0/production_model_9_4_0.zip"
    exit
fi

setup_dir="/tmp/har2stagingtest"

har2test_def_file="/tmp/har2.fx"
eval_file="/tmp/har2genericecc.eval"

har2Test_impl_file="$setup_dir/har2impl.fxi"
har2Test_impl_jar="fximplcode/target/apixio-fx-impls-0.0.2-SNAPSHOT.jar"
har2Test_entry="com.apixio.ensemblesdk.impl.Har2Fx"


#### set things up so that the (not really there) asset URLs have the correct names
#### for the setup directory

if [ ! -d $setup_dir ]
then
    mkdir -p $setup_dir
    cp resources/* $setup_dir/
fi

cd ..
# always copy since we want latest code
cp $har2Test_impl_jar $setup_dir

comp_jars=$(echo accessors/target/apixio-fx-accessors*.jar \
                 $setup_dir/*.jar \
                | sed 's/ /,/g')

# fake base dir for assets in that actual assets files won't exist here (we just
# want to match on last element of path/name).  it just has to be a parseable URL
fbase=file://$(pwd -P)

#### these two steps create the protobuf.fx def and impl info

FXD='list<apixio.Signal> extractSignals(apixio.PageWindow)'

echo "./scripts/mkfxdef.sh idl="$FXD" out=${har2test_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${har2test_def_file}

echo "./scripts/mkfximpl.sh def=${har2test_def_file} impl=$fbase/${har2Test_impl_jar} entry=${har2Test_entry} ut=${har2Test_impl_file} config=$fbase/config.yaml model.zip=$fbase/model.zip"

# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${har2test_def_file} \
              impl=$fbase/${har2Test_impl_jar} \
              entry=${har2Test_entry} \
              out=${har2Test_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip


#### this step uses the fximpl info to load and invoke the f(x) implementation

# accessors (besides built-in ones)
ACC_PWS=com.apixio.accessors.PageWindowsAccessor
ACC_APO=com.apixio.accessors.SinglePartialPatientAccessor
ACC_APO2=com.apixio.accessors.JSONPatientAccessor

# converters
CVT_SIGNAL=com.apixio.converter.SignalConverter

# writer (dataURI manager)
DUM_APXQUERY=com.apixio.umcs.Har2TestUmCreator

# eval config options
EVC_HAR2='pageWindows(singlePartialPatient(request("patientuuid")))'

./scripts/mkeval.sh eval="$EVC_HAR2" out="${eval_file}"

./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir} \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_PWS,$ACC_APO,$ACC_APO2 \
             converters=$CVT_SIGNAL \
             umcreator=$DUM_APXQUERY \
             evalfile=${eval_file}

