##
## To run the DumpOrgIdPatientIDLinkData and ImportOrgIdPatientIdLinkData commandline
## programs, you'll need a yaml file that is somewhat like this.
##
## This particular file points to staging, you must be **VERY** careful not to blindly
## use this for ImportOrgIdPatientIdLinkData **OR** you will be writing to the write place
## and (potentially) corrupting data..
##

persistenceConfig:

  jedisConfig:
    testWhileIdle: true
    testOnBorrow: true
    maxTotal: 100

  jedisPool:
    timeout: 60000

  redisConfig:
    host: changeme-redis-1-stg.apixio.com
    port: 6379
    keyPrefix: development-

  cassandraConfig:
    hosts: change me..
    ##52.27.56.1,52.26.58.193,52.27.56.74,52.27.56.0,52.27.56.75,52.27.56.12
    binaryPort: 9042
    keyspaceName: apixio
    baseDelayMs: 50
    maxDelayMs: 250
    readConsistencyLevel: QUORUM
    writeConsistencyLevel: QUORUM
    minConnections: 1
    maxConnections: 2

apiaclConfig:
    aclColumnFamilyName: apx_cfAcl

loggingConfig:
  appName: useraccount
  defaultLoggerName: global
  properties:
    graphite: true
    graphite.url: 127.0.0.1
    graphite.folderPrefix: test.useraccount
    # Write to console. Change to true to start logging using fluent
    fluent: false
    fluent.url: 127.0.0.1
    fluent.tag: test


emailConfig:
  templates:     /emailTemplates

customerPropertiesConfig:
  local: false  # this is so that we can run unit tests without using Redis

propertyHelperConfig:
  prefix: Staging


storageConfig:

    s3Config:
      accessKey: 241751759417465355120V01xJjYIXB5LizTyh+y34/4BSStIp7mOs/ojRmFVNcbq3mI=
      secretKey: 241725635732741663792V01xMhYgRVsVCFyT4GZ7GwEq2w/LUlLbPcrPclJ4Gza2ljvA9Zx3ghgsF4LJQGlPcuAS
    apixioFSConfig:
      #mountPoint: S3
      mountPoint: apixio-documents-test-oregon
      fromStorageType: S3
      toStorageTypes: S3

daoConfig:
  #This is a feature toggle, by default this will be set to false, until we enable write path
  sequenceStoreEnhancementEnabled: false

  cqlCacheConfig:
    bufferSize: 100

  globalTableConfig:
    link: apx_cfLink_new
    index: apx_cfIndex
    seq: apx_cfSequenceStore
    metric: apx_cfMetric

  seqStoreConfig:
    paths: attributes.sourceType,source.type,attributes.bucketName,attributes.$batchId
    annotation.paths:
    noninferred.paths: source.type,attributes.$batchId
    inferred.paths: attributes.bucketName,attributes.$batchId

  summaryConfig:
    mergeMinNumThreshold: 0
    mergeMaxNumThreshold: 5
    mergeMinPeriodThresholdInMs: 0
    mergeMaxPeriodThresholdInMs: 5
