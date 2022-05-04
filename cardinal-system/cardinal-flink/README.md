# cardinal-flink

# building the cassndra connector

clone the flink repository

```
git clone https://github.com/apache/flink
git checkout release-1.14.4
```

cd into the connector directory

```
cd flink-connectors/flink-connector-cassandra
```

modify the pom.xml to include the groupId

```
gsed 's|\(\s\+\)\(.*artifactId>flink-connector.*\)|\1\2\n\1<groupId>com.apixio.flink.connectors</groupId>|g' pom.xml
```

build the connector and deploy

```
mvn clean deploy -Ddriver.version=3.11.1 -P apixio -DaltReleaseDeploymentRepository=apixio.releases.build::default::https://repos.apixio.com/artifactory/releases 
```
