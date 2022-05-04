# apx-modelcatalogsvc

Model Catalog Service

## Local Development

Instructions are for MacOS, using Homebrew (https://brew.sh/).

### Set up local mariadb

- ```brew install mariadb```
- If mariadb >= 10.4 : ```sudo mysql``` (root user login has changed in 10.4)
- If mariadb < 10.4 : ```mysql -uroot```
- ```create database blobmgr;```
- ```CREATE USER 'mcsuser'@'localhost' IDENTIFIED BY 'notasecret';```
- ```GRANT ALL on blobmgr.* TO 'mcsuser'@'localhost';```

To test installation:

- ```mysql -umcsuser -pnotasecret blobmgr```

Should show mariadb prompt with no errors if the user and database were successfully created.

### Some more mariadb gotchas

- Load timezone: 
    - ```mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root -p mysql```
- Set timezone in mariadb
    - ```mysql -uroot -p```
    - ```SET GLOBAL time_zone = 'America/Los_Angeles'```
 
### Initialize local mysql tables (but see below for copying from staging)

- ```mysql -umcsuser -pnotasecret blobmgr < db/init-db.sql```

### Setup local redis

- ```brew install redis```
- ```brew services start redis```

To test installation:

- ```redis-cli ping```

Should print PONG at the prompt if redis was set up successfully.

### Set up dropwizard configuration

There is a skeleton dropwizard configuration file at ```mcs/src/main/resources/mcs.yaml```.

- Make a copy of this file i.e. ```local_mcs.yaml```
- Replace microserviceConfig.storageConfig.s3Config accessKey and secretKey with your encrypted 
S3 credentials.
- Replace microserviceConfig.persistenceConfig.jdbc_blobmanager username and password with the
credentials you created in mariadb setup (username: mcsuser, password: notasecret if you used
the example commands).
- Replace microserviceConfig.persistenceConfig.redisConfig.keyPrefix with your own value e.g. 
```local_mcs_prefix``` (for local dev this value is arbitrary, it just needs to be set).

### Build dropwizard jar

- ```mvn clean package```

### Start dropwizard container

- ```cd mcs/target```
- ```java -jar target/modelcatalogsvc-${VERSION}.jar server ../local_mcs.yaml ```
    - Replace ${VERSION} with the artifact version defined in pom.xml

If successful, should print ```org.eclipse.jetty.server.Server: Started```


### Copy MCS database from staging

- ```mysqldump -c --skip-column-statistics -h signalmgr-db.stg.apixio.com -usignalmgrctrl -pTHEPASSWORD blobmgr > stg-blobmgr-$(date "+%Y-%m-%d").sql ```
- ```mysql -uroot -p <  stg-blobmgr-$(date "+%Y-%m-%d").sql  # use same filename as mysqldump...```


## Testing mctool

For more details see the 
[git repository for mctool](https://github.com/Apixio/python-scripts/tree/master/mctool)

### Installation 

Recommended to use a virtual environment

```
python3 -m venv <your_venv_name>
source <your_venv_name>/bin/activate
pip install mctool
```

### Configure mctool to use local mcs server

```
export MCS_REST_ENDPOINT=http://localhost:8036
```

### Run a simple test

```
mctool search --engine=MA
```
