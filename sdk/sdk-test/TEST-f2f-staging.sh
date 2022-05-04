FILE=resources/model.zip
if [ -f "$FILE" ]; then
    echo "$FILE exists."
else 
    echo "=====FAIL=====\n$FILE does not exist. You need to download this file as $FILE:"
    echo "https://repos.apixio.com/artifactory/webapp/#/artifacts/browse/tree/General/models/models/production_model/9.4.0/production_model_9_4_0.zip"
    exit
fi

setup_dir="/tmp/f2fstagingtest"

f2ftest_def_file="/tmp/f2f.fx"
eval_file="/tmp/f2fgenericecc.eval"

f2fTest_impl_file="$setup_dir/f2fimpl.fxi"
f2fTest_impl_jar="fximplcode/target/apixio-fx-impls-0.0.2-SNAPSHOT.jar"
f2fTest_entry="com.apixio.ensemblesdk.impl.FaceToFace"


#### set things up so that the (not really there) asset URLs have the correct names
#### for the setup directory

if [ ! -d $setup_dir ]
then
    mkdir -p $setup_dir
    cp resources/* $setup_dir/
fi

cd ..

# always copy since we want latest code
cp $f2fTest_impl_jar $setup_dir

comp_jars=$(echo accessors/target/apixio-fx-accessors*.jar \
                 $setup_dir/*.jar \
                | sed 's/ /,/g')

# fake base dir for assets in that actual assets files won't exist here (we just
# want to match on last element of path/name).  it just has to be a parseable URL
fbase=file://$(pwd -P)

#### these two steps create the protobuf.fx def and impl info

FXD='list<apixio.Signal> extractSignals(apixio.PageWindow)'


echo "./scripts/mkfxdef.sh idl="$FXD" out=${f2ftest_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${f2ftest_def_file}

echo "./scripts/mkfximpl.sh def=${f2ftest_def_file} impl=$fbase/${f2fTest_impl_jar} entry=${f2fTest_entry} ut=${f2fTest_impl_file} config=$fbase/config.yaml model.zip=$fbase/model.zip"

# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${f2ftest_def_file} \
              impl=$fbase/${f2fTest_impl_jar} \
              entry=${f2fTest_entry} \
              out=${f2fTest_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip


#### this step uses the fximpl info to load and invoke the f(x) implementation

# accessors (besides built-in ones)
ACC_PWS=com.apixio.accessors.PageWindowsAccessor
ACC_APO=com.apixio.accessors.SinglePartialPatientAccessor

# converters
CVT_SIGNAL=com.apixio.converter.SignalConverter

# writer (dataURI manager)
DUM_APXQUERY=com.apixio.umcs.F2fTestUmCreator

# eval config options
EVC_F2F='pageWindows(singlePartialPatient(request("docuuid")))'

./scripts/mkeval.sh eval="$EVC_F2F" out="${eval_file}"

./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir} \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_PWS,$ACC_APO \
             converters=$CVT_SIGNAL \
             umcreator=$DUM_APXQUERY \
             evalfile=${eval_file}

