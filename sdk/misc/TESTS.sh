

setup_dir="/tmp/theFxTest"

extract_feature_def_file="/tmp/extfeat.fx"
eval_file="/tmp/genericecc.eval"

accessor_jars=$(echo accessors/target/apixio-accessor*.jar)
facetoface_impl_file="$setup_dir/f2fimpl.fxi"
facetoface_impl_jar="fximplcode/target/apixio-fx-impls-0.0.1-SNAPSHOT.jar"
#facetoface_entry="com.apixio.impls.FaceToFaceImpl"
facetoface_entry="com.apixio.ensemblesdk.impl.FaceToFace"


#### set things up so that the (not really there) asset URLs have the correct names
#### for the setup directory

if [ ! -d $setup_dir ]
then
    mkdir -p $setup_dir
    cp TESTDATA/* $setup_dir/
fi

# always copy since we want latest code
cp $facetoface_impl_jar $setup_dir

comp_jars=$(echo accessors/target/apixio-fx-accessors*.jar \
                 $setup_dir/*.jar \
                | sed 's/ /,/g')

# fake base dir for assets in that actual assets files won't exist here (we just
# want to match on last element of path/name).  it just has to be a parseable URL
fbase=file://$(pwd -P)

#### these two steps create the protobuf.fx def and impl info

FXD_LISTPW='list<apixio.Signal> extractSignals(list<apixio.PageWindow>)'
FXD_PW='list<apixio.Signal> extractSignals(apixio.PageWindow)'
./scripts/mkfxdef.sh idl="$FXD_PW" out=${extract_feature_def_file}

# everything after the "\" on a line by itself forms asset list
./scripts/mkfximpl.sh def=${extract_feature_def_file} \
		      impl=$fbase/${facetoface_impl_jar} \
		      entry=${facetoface_entry} \
		      out=${facetoface_impl_file} \
		      \
		      config=$fbase/config.yaml \
		      model.zip=$fbase/model.zip


#### this step uses the fximpl info to load and invoke the f(x) implementation

ACC_PAGEWINDOW=com.apixio.accessors.PageWindowAccessor
ACC_PAGEWINDOWS=com.apixio.accessors.PageWindowsAccessor
ACC_APO=com.apixio.accessors.SinglePartialPatientAccessor

# converters
CVT_SIGNAL=com.apixio.converter.SignalConverter

# writer (dataURI manager)
DUM_APXQUERY=com.apixio.umcs.F2fTestUmCreator

# eval config options
EVC_CONST32='32'
EVC_PAGEWINDOW='pageWindow("a")'
EVC_PAGEWINDOWS='pageWindows(singlePartialPatient("027b0e4f-fdc5-4fcc-91a3-79a4d3a3d39b"))'

./scripts/mkeval.sh eval="$EVC_PAGEWINDOWS" out="${eval_file}"

./scripts/loadrun.sh config=test/local.yaml \
		     impldir=${setup_dir} \
		     compjars=${comp_jars} \
		     converters=$CVT_SIGNAL \
		     umcreator=$DUM_APXQUERY \
		     accessors=$ACC_PAGEWINDOW,$ACC_PAGEWINDOWS,$ACC_APO \
		     evalfile=${eval_file}
