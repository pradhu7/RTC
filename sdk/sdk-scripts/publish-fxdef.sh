
if [ "$1" = "" ]; then echo "Usage:  $0 idl creatorEmail fxName fxDescription"; exit 1; fi

b=$(dirname $0)

fxf="/tmp/_fx_$$.fx"
fxm="/tmp/_fx_$$.fxm"

$b/mkfxdef.sh idl="$1" out="$fxf"

echo "Please confirm the following fx dump is correct:"
$b/rdfxdef.sh $fxf

read -p "Continue [Y/n]?  " con

if [ "$con" = "" -o "$con" = "y" ]
then

    echo "Publishing f(x) of " $1

    MCS_SERVER=${MCS_SERVER:-"https://modelcatalogsvc-stg.apixio.com:8443"}

    cat >> $fxm <<eof
{
  "fxdef": "$1",
  "createdBy": "$2",
  "name": "$3",
  "description": "$4"
}
eof

    curl --insecure -X POST \
	 -H "Content-Type: application/json" \
	 --data-binary @$fxm \
	 $MCS_SERVER/fx/fxdefs

else
    echo "NOT publishing " $1
fi

rm -f $fxf $fxm
