function pause(){
 read -s -n 1 -p "Press any key to continue . . ."
 echo ""
}

FILE=resources/model.zip
if [ -f "$FILE" ]; then
    echo "$FILE exists."
else 
    echo "=====FAIL=====\n$FILE does not exist. You need to download this file as $FILE:"
    echo "https://repos.apixio.com/artifactory/webapp/#/artifacts/browse/tree/General/models/models/production_model/9.4.0/production_model_9_4_0.zip"
    exit
fi

setup_dir="/tmp/capvstagingtest"

#### set things up so that the (not really there) asset URLs have the correct names
#### for the setup directory

if [ ! -d $setup_dir ]
then
    mkdir -p $setup_dir
    cp resources/* $setup_dir/
fi

cd ..
# always copy since we want latest code
capvTest_impl_jar="fximplcode/target/apixio-fx-impls-1.0.5-SNAPSHOT.jar"
cp $capvTest_impl_jar $setup_dir

comp_jars=$(echo accessors/target/apixio-fx-accessors*.jar \
                 $setup_dir/*.jar \
                | sed 's/ /,/g')

# fake base dir for assets in that actual assets files won't exist here (we just
# want to match on last element of path/name).  it just has to be a parseable URL
fbase=file://$(pwd -P)

#### these two steps create the protobuf.fx def and impl info

har2test_def_file="${setup_dir}/har2.fx"
FXD='list<apixio.Signal> extractSignals(apixio.PageWindow)'
echo "./scripts/mkfxdef.sh idl="$FXD" out=${har2test_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${har2test_def_file}

anno_def_file="${setup_dir}/anno.fx"
FXD='list<apixio.Signal> transformSignals(list<apixio.Signal>)'
echo "./scripts/mkfxdef.sh idl="$FXD" out=${anno_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${anno_def_file}

sstest_def_file="${setup_dir}/structsuspects.fx"
FXD='list<apixio.Signal> extractPatientSignals(apixio.Patient)'
echo "./scripts/mkfxdef.sh idl="$FXD" out=${sstest_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${sstest_def_file}

combinetest_def_file="${setup_dir}/capv.fx"
FXD='list<apixio.Event> combineSignals(apixio.SignalGroups)'
echo "./scripts/mkfxdef.sh idl="$FXD" out=${combinetest_def_file}"
./scripts/mkfxdef.sh idl="$FXD" out=${combinetest_def_file}

har2Test_impl_file="$setup_dir/har2impl.fxi"
har2Test_entry="com.apixio.ensemblesdk.impl.Har2Fx"
echo "./scripts/mkfximpl.sh def=${har2test_def_file} impl=$fbase/${capvTest_impl_jar} entry=${har2Test_entry} out=${har2Test_impl_file} config=$fbase/fximplcode/test/resources/config.yaml model.zip=$fbase/model.zip"
# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${har2test_def_file} \
              impl=$fbase/${capvTest_impl_jar} \
              entry=${har2Test_entry} \
              out=${har2Test_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip \
              har2config=$fbase/capvconfig.yaml

ssTest_impl_file="$setup_dir/structsuspectsimpl.fxi"
ssTest_entry="com.apixio.ensemblesdk.impl.StructuredSuspectsFx"
echo "./scripts/mkfximpl.sh def=${sstest_def_file} impl=$fbase/${capvTest_impl_jar} entry=${ssTest_entry} out=${ssTest_impl_file} config=$fbase/fximplcode/test/resources/config.yaml model.zip=$fbase/model.zip"
# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${sstest_def_file} \
              impl=$fbase/${capvTest_impl_jar} \
              entry=${ssTest_entry} \
              out=${ssTest_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip

anno_impl_file="$setup_dir/annoimpl.fxi"
anno_entry="com.apixio.ensemblesdk.impl.AnnotationFx"
echo "./scripts/mkfximpl.sh def=${anno_def_file} impl=$fbase/${capvTest_impl_jar} entry=${anno_entry} out=${anno_impl_file} config=$fbase/fximplcode/test/resources/config.yaml model.zip=$fbase/model.zip"
# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${anno_def_file} \
              impl=$fbase/${capvTest_impl_jar} \
              entry=${anno_entry} \
              out=${anno_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip

slimlyntyTest_impl_file="$setup_dir/slimlyntyimpl.fxi"
slimlyntyTest_entry="com.apixio.ensemblesdk.impl.SlimLyntyFx"
echo "./scripts/mkfximpl.sh def=${sstest_def_file} impl=$fbase/${capvTest_impl_jar} entry=${slimlyntyTest_entry} out=${slimlyntyTest_impl_file} config=$fbase/fximplcode/test/resources/config.yaml model.zip=$fbase/model.zip"
# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${sstest_def_file} \
              impl=$fbase/${capvTest_impl_jar} \
              entry=${slimlyntyTest_entry} \
              out=${slimlyntyTest_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip \
              slimlyntyconfig=$fbase/capvconfig.yaml

capv_impl_file="$setup_dir/capvimpl.fxi"
capvTest_entry="com.apixio.ensemblesdk.impl.CAPVFx"
echo "./scripts/mkfximpl.sh def=${combinetest_def_file} impl=$fbase/${capvTest_impl_jar} entry=${capvTest_entry} out=${capv_impl_file} config=$fbase/fximplcode/src/test/resources/config.yaml model.zip=$fbase/model.zip"
# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${combinetest_def_file} \
              impl=$fbase/${capvTest_impl_jar} \
              entry=${capvTest_entry} \
              out=${capv_impl_file} \
              \
              config=$fbase/config.yaml \
              model.zip=$fbase/model.zip \
              capvconfig=$fbase/capvconfig.yaml

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
HAR2DUM_APXQUERY=com.apixio.umcs.Har2TestUmCreator
AGLODUM_APXQUERY=com.apixio.umcs.AlgoPatientTestUmCreator

# eval config options
EVC_HAR2='pageWindows(patient(request("patientuuid")))'
EVC_SS_SL='patient(request("patientuuid"))'
EVC_ANNO='patientannotations(request("annopatientuuid"))'

HAR2_QUERY="apxquery://devel/q?patientuuid=48156abe-cbe8-440a-9dfd-42b4ea7b223d&algo=com.apixio.ensemblesdk.impl.Har2Fx"
SS_QUERY="apxquery://devel/q?patientuuid=48156abe-cbe8-440a-9dfd-42b4ea7b223d&algo=com.apixio.ensemblesdk.impl.StructuredSuspectsFx"
SL_QUERY="apxquery://devel/q?patientuuid=48156abe-cbe8-440a-9dfd-42b4ea7b223d&algo=com.apixio.ensemblesdk.impl.SlimLyntyFx"
ANNO_QUERY="apxquery://devel/q?patientuuid=48156abe-cbe8-440a-9dfd-42b4ea7b223d&algo=com.apixio.ensemblesdk.impl.AnnotationFx"
# EVC_CAPV="signalGroup(request('patientuuid'),\"$HAR2_QUERY\",\"$SS_QUERY\",\"$SL_QUERY\",\"$ANNO_QUERY\")"
EVC_CAPV="signalGroup(request('patientuuid'),'B_98f0729c-1866-43c0-8c8e-fda36735f52d','B_5a7c5ad6-c1ee-49f8-b32a-a45f574be928','B_c6db2701-77e1-4aca-a9ff-2eb3ff94e302','B_325a4157-82c4-41d5-a175-b4bb7b203651')"
har2_eval_file="$setup_dir/har2genericecc.eval"
./scripts/mkeval.sh eval="$EVC_HAR2" out="${har2_eval_file}"

ss_sl_eval_file="$setup_dir/ss_sl_genericecc.eval"
./scripts/mkeval.sh eval="$EVC_SS_SL" out="${ss_sl_eval_file}"

anno_eval_file="$setup_dir/anno_genericecc.eval"
./scripts/mkeval.sh eval="$EVC_ANNO" out="${anno_eval_file}"

capv_sl_eval_file="$setup_dir/capv_genericecc.eval"
./scripts/mkeval.sh eval="$EVC_CAPV" out="${capv_sl_eval_file}"

# Run HAR2
./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir}/har2impl.fxi \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_ALL \
             converters=$CVT_SIGNAL \
             umcreator=$HAR2DUM_APXQUERY \
             evalfile=${har2_eval_file}

# Run Annotations
./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir}/annoimpl.fxi \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_ALL \
             converters=$CVT_SIGNAL \
             umcreator=$AGLODUM_APXQUERY \
             evalfile=${anno_eval_file}

# Run StructuredSuspects
./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir}/structsuspectsimpl.fxi \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_ALL \
             converters=$CVT_SIGNAL \
             umcreator=$AGLODUM_APXQUERY \
             evalfile=${ss_sl_eval_file}


# Run SlimLynty
./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir}/slimlyntyimpl.fxi \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_ALL \
             converters=$CVT_SIGNAL \
             umcreator=$AGLODUM_APXQUERY \
             evalfile=${ss_sl_eval_file}

# Run CAPV
./scripts/loadrun.sh config=test/staging.yaml \
             impldir=${setup_dir}/capvimpl.fxi \
             compjars=${comp_jars} \
             mode="directory" \
             accessors=$ACC_ALL \
             converters=$CVT_SIGNAL \
             umcreator=$AGLODUM_APXQUERY \
             evalfile=${capv_sl_eval_file}

