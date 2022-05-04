base=$(dirname $0)/..

exec java -D_bootprops=$HOME/ua.properties -jar ${base}/target/apixio-tokenizer-dw-*.jar server ${base}/src/main/resources/tokenizer-stg.yaml
