
if [ "$BC_HOME" = "" ]; then echo "Variable BC_HOME must be set to the directory that contains bcprov*.jar and bc-fips*.jar"; exit 1; fi

bcfips=$(echo $BC_HOME/bc-fips-*.jar)
bcprov=$(echo $BC_HOME/bcprov-jdk15on-*.jar)

m2="$HOME/.m2/repository"
cp=$(echo \
          $m2/com/google/guava/guava/11.0.2/guava-11.0.2.jar \
          $m2/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar \
          $m2/commons-lang/commons-lang/2.4/commons-lang-2.4.jar \
          $m2/commons-configuration/commons-configuration/1.6/commons-configuration-1.6.jar \
          $m2/com/fasterxml/jackson/core/jackson-annotations/2.5.5/jackson-annotations-2.5.5.jar \
          $m2/com/fasterxml/jackson/core/jackson-core/2.5.5/jackson-core-2.5.5.jar \
          $m2/com/fasterxml/jackson/core/jackson-databind/2.5.5/jackson-databind-2.5.5.jar \
          $m2/commons-cli/commons-cli/1.3.1/commons-cli-1.3.1.jar \
          $m2/commons-codec/commons-codec/1.10/commons-codec-1.10.jar \
          $m2/commons-io/commons-io/2.4/commons-io-2.4.jar \
          $m2/commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar \
          $m2/log4j/log4j/1.2.17/log4j-1.2.17.jar \
          $m2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar \
          $m2/org/slf4j/slf4j-log4j12/1.7.7/slf4j-log4j12-1.7.7.jar \
          target/test-classes | sed 's/ /:/g')

# -Dlog4j.configuration because we're using log4j 1.2; if we don't include this
# then the file src/test/resources/log4j.xml is used, which causes no output
JAVA_OPTS="-Xmx10G -Xms10G -Dlog4j.configuration=file:///$(pwd)/src/test/resources/log4j.properties"

JAVA="java $JAVA_OPTS"
