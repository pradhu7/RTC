package com.apixio.scala.dw

import java.util
import java.util.Properties

import com.apixio.SysServices
import com.apixio.bizlogic.patient.logic.{PatientAdminLogic, PatientLogic}
import com.apixio.dao.customerproperties.CustomerProperties
import com.apixio.dao.seqstore.SeqStoreDAO
import com.apixio.dao.utility.{DaoServices, DaoServicesSet}
import com.apixio.datasource.cassandra.{CqlConnector, CqlCrud, ThreadLocalCqlCache}
import com.apixio.datasource.kafka.{SimpleKafkaEventProducer, StreamConsumer}
import com.apixio.datasource.redis.{DistLock, RedisOps, Transactions}
import com.apixio.datasource.utility.KafkaConsumerUtility
import com.apixio.model.profiler._
import com.apixio.model.quality.{ConjectureModel, FactModel, MeasureModel}
import com.apixio.model.utility.{ApixioDateDeserialzer, ApixioDateSerializer}
import com.apixio.restbase.apiacl.ApiAcls
import com.apixio.restbase.apiacl.dwutil.ApiReaderInterceptor
import com.apixio.restbase.config.{ConfigSet, MicroserviceConfig}
import com.apixio.restbase.web._
import com.apixio.restbase.{DaoBase, DataServices}
import com.apixio.scala.logging.ApixioLoggable
import com.apixio.scala.utility.{Mapping, SlackService}
import com.apixio.security.Security
import com.apixio.signalmanager.bizlogic.SignalLogic
import com.apixio.useracct.web.AttachUser
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.introspect.{AnnotationIntrospectorPair, JacksonAnnotationIntrospector}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import io.dropwizard.setup.Environment
import javax.servlet.DispatcherType
import org.joda.time.DateTime
import com.apixio.bizlogic.provider.ProviderLogic
import com.apixio.restbase.apiacl.ApiAcls.InitInfo

import scala.collection.JavaConverters._

object ApxServices extends ApixioLoggable {

  private var config: ApxConfiguration = null
  private var internalMapper: ObjectMapper with ScalaObjectMapper = null

  def init(configuration: ApxConfiguration) : Unit = {
    config = configuration
  }

  def setupObjectMapper(mapper: ObjectMapper) : Unit = {
    val dateModule = new SimpleModule("DateTimeSerdeModule")
    dateModule.addDeserializer(classOf[DateTime], new ApixioDateDeserialzer)
    dateModule.addSerializer(classOf[DateTime], new ApixioDateSerializer)

    mapper.registerModule(DefaultScalaModule)
      .registerModule(dateModule)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
      .setAnnotationIntrospector(new AnnotationIntrospectorPair(ScalaAnnotationIntrospector, new JacksonAnnotationIntrospector()))

    internalMapper = new ObjectMapper(mapper.getFactory) with ScalaObjectMapper
    internalMapper.registerModule(DefaultScalaModule)
          .registerModule(dateModule)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
          .setAnnotationIntrospector(new AnnotationIntrospectorPair(ScalaAnnotationIntrospector, new JacksonAnnotationIntrospector()))
  }

  def setupApxLogging() : Unit = {
    ApixioLoggable.initialize { case key => config.apxLogging.get(key) }
    setupLog(getClass.getCanonicalName)
  }

  def setupApiAcls(filterName: String, environment: Environment, urlPattern: String = "/*", enableCqlTransaction: Boolean = false, initInfo: InitInfo = null) : Unit = {
    val acls = ApiAcls.fromJsonFile(initInfo, sysServices, config.acl("jsonFile"))
    val interceptor = new ApiReaderInterceptor(acls.getPermissionEnforcer)
    acls.setLogLevel(ApiAcls.LogLevel.valueOf(config.acl.getOrElse("level", "ACCESS")), log.get)
    environment.jersey.register(interceptor)
    environment.jersey.register(new AclContainerFilter)

    val filterConfig = if (config.filterConfig == null) { Map[String, Object]() } else { config.filterConfig }
    val tokConfig = getValidateTokenConfig(filterConfig)

    val validToken = new ValidateToken
    validToken.configure(tokConfig, sysServices)

    val attachUser = new AttachUser
    attachUser.configure(null, sysServices)

    val aclChecker = new AclChecker
    aclChecker.setApiAcls(acls)

    val filter = new Dispatcher

    if(enableCqlTransaction) {
      val doingThisBecauseVramIsntThreadSafe = cqlConnection
      val cqlTransaction = new CqlTransaction
      cqlTransaction.configure(null, sysServices)
      filter.setMicrofilters(List[Microfilter[_ <: DataServices]](validToken, attachUser, cqlTransaction, aclChecker).asJava)
    } else {
      filter.setMicrofilters(List[Microfilter[_ <: DataServices]](validToken, attachUser, aclChecker).asJava)
    }


    val reg = environment.servlets.addFilter(filterName, filter)

    reg.addMappingForUrlPatterns(java.util.EnumSet.of(DispatcherType.REQUEST), false, urlPattern)

    info("Setup api acls")
  }

  private def getValidateTokenConfig(filterConfig: Map[String, Object]): util.HashMap[String, Object] = {
    val tokConfig = new java.util.HashMap[String, Object]

    if (filterConfig != null) {
      filterConfig.get("validateToken") match {
        case Some(m) =>
          val jTokenConfig = m.asInstanceOf[Map[String, List[String]]]
          // Validate token class only accepts java list
          tokConfig.put("publicURLs", jTokenConfig.getOrElse("publicURLs", null).asJava)
          tokConfig.put("tokenType", jTokenConfig.getOrElse("tokenType", "INTERNAL"))
          tokConfig.put("authCookieName", jTokenConfig.getOrElse("authCookieName", "ApxToken"))
        case None =>
          //hard codded here for backward compatibility
          tokConfig.put("tokenType", "INTERNAL")
      }
    } else {
        //hard codded here for backward compatibility
        tokConfig.put("tokenType", "INTERNAL")
    }

    tokConfig
  }

  lazy val setupDefaultModels = reloadDefaultModels()

  /*
  Reloads data models
  This method should be public to allow to refresh models data if needed (EN-14219)
   */
  def reloadDefaultModels() : Unit = {
    this.synchronized {
      CodeMapping.init(Mapping.getCodeMapping())(internalMapper)
      CptModel.init(Mapping.getCptModel())(internalMapper)
      BillTypeModel.init(Mapping.getBilltypeModel())(internalMapper)
      HccModel.init(Mapping.getHccModel())(internalMapper)
      IcdModel.init(Mapping.getIcdModel())(internalMapper)
      MeasureModel.init(Mapping.getMeasureModel())(internalMapper)
      FactModel.init(Mapping.getFactModel())(internalMapper)
      ConjectureModel.init(Mapping.getConjectureModel())(internalMapper)
      RiskGroupModel.init(Mapping.getRiskGroupModel())(internalMapper)

      CodeMappingV2.init(Mapping.getCodeMappingV2())(internalMapper)
      CptModelV2.init(Mapping.getCptModelV2())(internalMapper)
      HccModelV2.init(Mapping.getHccModelV2())(internalMapper)
      IcdModelV2.init(Mapping.getIcdModelV2())(internalMapper)
      SnomedModel.init(Mapping.getSnomedModel())(internalMapper)
    }
  }

  def configuration : ApxConfiguration = config

  lazy val mapper : ObjectMapper with ScalaObjectMapper = {
    internalMapper
  }

  lazy val patientLogic: PatientLogic = new PatientLogic(ApxServices.daoServices)
  lazy val patientAdminLogic = new PatientAdminLogic(daoServices)
  lazy val signalLogic= new SignalLogic(daoServices)
  lazy val providerLogic = new ProviderLogic(daoServices)

  lazy val daoServices : DaoServices = {
    val jedisConfig = new java.util.HashMap[String, Object]
    jedisConfig.put("testWhileIdle", new java.lang.Boolean(config.redis.getOrElse("testWhileIdle", "true")))
    jedisConfig.put("testOnBorrow", new java.lang.Boolean(config.redis.getOrElse("testOnBorrow", "true")))
    jedisConfig.put("maxTotal", new Integer(config.redis.getOrElse("maxTotal", "100")))

    val jedisPool = new java.util.HashMap[String, Object]
    jedisPool.put("timeout", new Integer(config.redis.getOrElse("pool.timeout", "60000")))

    val redisConfig = new java.util.HashMap[String, Object]
    redisConfig.put("host", config.redis("host"))
    redisConfig.put("port", new Integer(config.redis.getOrElse("port", "6379")))
    redisConfig.put("keyPrefix", config.redis("prefix"))

    val loggingConfig = new java.util.HashMap[String, Object]
    loggingConfig.put("fluent.url", config.apxLogging.getOrElse("prefix", null))
    loggingConfig.put("graphiteHost", config.apxLogging.getOrElse("graphiteHost", null))
    loggingConfig.put("fluentHost", config.apxLogging.getOrElse("fluentHost", null))

    config.apxLogging.getOrElse("appName", null) match  {
      case null => //backwards compatible for app, that don't use eventLogger from java
      case _ => //required for sysServices setup on java side.
        loggingConfig.put("appName", config.apxLogging("appName"))

        val loggingConfigProperties = new java.util.HashMap[String, Object]
        loggingConfigProperties.put("fluent", config.apxLogging.getOrElse("fluentHost.enable", "true"))
        loggingConfigProperties.put("fluent.url", config.apxLogging.getOrElse("fluentHost", null))
        loggingConfigProperties.put("fluent.tag", config.apxLogging.getOrElse("prefix", null))

        loggingConfig.put ("properties", loggingConfigProperties)
    }

    val persistenceServiceConfigMap = new java.util.HashMap[String, Object]
    persistenceServiceConfigMap.put("jedisConfig", jedisConfig)
    persistenceServiceConfigMap.put("jedisPool", jedisPool)
    persistenceServiceConfigMap.put("redisConfig", redisConfig)
    config.cql = config.cql match {
      case null => Map()
      case _ => config.cql
    }
    persistenceServiceConfigMap.put("cassandraConfig_science", config.cql match {
                                      case m if m.isEmpty => new java.util.HashMap[String, Object]
                                      case config => getCassandraConfig(config)
                                    })
    config.cqlInternal = config.cqlInternal match {
      case null => Map()
      case _ => config.cqlInternal
    }
    persistenceServiceConfigMap.put("cassandraConfig_internal", config.cqlInternal match {
                                      case m if m.isEmpty => new java.util.HashMap[String, Object]
                                      case config => getCassandraConfig(config)
                                    })
    config.cqlApplication = config.cqlApplication match {
      case null => Map()
      case _ => config.cqlApplication
    }
    persistenceServiceConfigMap.put("cassandraConfig_application", config.cqlApplication match {
                                      case m if m.isEmpty => new java.util.HashMap[String, Object]
                                      case config => getCassandraConfig(config)
                                    })

    config.cqlSignal = config.cqlSignal match {
      case null => Map()
      case _ => config.cqlSignal
    }
    persistenceServiceConfigMap.put("cassandraConfig_signal", config.cqlSignal match {
      case m if m.isEmpty => new java.util.HashMap[String, Object]
      case config => getCassandraConfig(config)
    })
    if (config.searchChartSpace != null && !config.searchChartSpace.isEmpty) {
      persistenceServiceConfigMap.put("searchChartSpace", getSearchConfig(config.searchChartSpace))
    }

    val jdbcSignalControl = new java.util.HashMap[String, Object]

    persistenceServiceConfigMap.put("jdbc_signal_control", config.jdbcSignalControl != null match {
      case true =>
        jdbcSignalControl.putAll(config.jdbcSignalControl.asJava)
        jdbcSignalControl.put("verboseSql", new java.lang.Boolean(config.jdbcSignalControl.getOrElse("verboseSql", "true")))
        jdbcSignalControl.put("maxTotal", new Integer((config.jdbcSignalControl.getOrElse("maxTotal", "25"))))
        jdbcSignalControl
      case false => jdbcSignalControl
    })

    val jdbcSignalDocmeta = new java.util.HashMap[String, Object]

    persistenceServiceConfigMap.put("jdbc_signal_docmeta", config.jdbcSignalDocmeta != null match {
      case true =>
        jdbcSignalDocmeta.putAll(config.jdbcSignalDocmeta.asJava)
        jdbcSignalDocmeta.put("verboseSql", new java.lang.Boolean(config.jdbcSignalDocmeta.getOrElse("verboseSql", "true")))
        jdbcSignalDocmeta.put("maxTotal", new Integer((config.jdbcSignalDocmeta.getOrElse("maxTotal", "25"))))
        jdbcSignalDocmeta
      case false => jdbcSignalDocmeta
    })

    val jdbcProvider = new java.util.HashMap[String, Object]

    persistenceServiceConfigMap.put("jdbc_provider", config.jdbcProvider != null match {
      case true =>
        jdbcProvider.putAll(config.jdbcProvider.asJava)
        jdbcProvider.put("verboseSql", new java.lang.Boolean(config.jdbcProvider.getOrElse("verboseSql", "false")))
        jdbcProvider.put("maxTotal", new Integer((config.jdbcProvider.getOrElse("maxTotal", "25"))))
        jdbcProvider
      case false => jdbcProvider
    })

    val jdbcApxDataDao = new java.util.HashMap[String, Object]

    persistenceServiceConfigMap.put("jdbc_apxdatadao", config.jdbcApxDataDao != null match {
      case true =>
        jdbcApxDataDao.putAll(config.jdbcApxDataDao.asJava)
        jdbcApxDataDao.put("verboseSql", new java.lang.Boolean(config.jdbcApxDataDao.getOrElse("verboseSql", "true")))
        jdbcApxDataDao.put("maxTotal", new Integer(config.jdbcApxDataDao.getOrElse("maxTotal", "25")))
        jdbcApxDataDao
      case false => jdbcApxDataDao
    })

    persistenceServiceConfigMap.put("kafka_signal", kafkaSignalMap)

    persistenceServiceConfigMap.put("loggingConfig", loggingConfig)

    val kafkaMetricMap = new java.util.HashMap[String, Object]
    val jdbcDdlMap = new java.util.HashMap[String, Object]
    val jdbcDmlMap = new java.util.HashMap[String, Object]

    persistenceServiceConfigMap.put("kafka_metric", config.kafka_metric != null match {
      case true =>
        kafkaMetricMap.putAll(config.kafka_metric.asJava)
        kafkaMetricMap
      case false => kafkaMetricMap
    })

    persistenceServiceConfigMap.put("jdbc_ddl", config.jdbc_ddl != null match {
      case true =>
        jdbcDdlMap.putAll(config.jdbc_ddl.asJava)
        jdbcDdlMap
      case false => jdbcDdlMap
    })
    persistenceServiceConfigMap.put("jdbc_dml", config.jdbc_dml != null match {
      case true =>
        jdbcDmlMap.putAll(config.jdbc_dml.asJava)
        jdbcDmlMap
      case false => jdbcDmlMap
    })


    val customerPropertiesConfig = new java.util.HashMap[String, Object]
    customerPropertiesConfig.put("local", java.lang.Boolean.FALSE)

    val propertyHelperConfig = new java.util.HashMap[String, Object]
    config.propertyHelperConfig != null match {
      case true =>
        propertyHelperConfig.putAll(config.propertyHelperConfig.asJava)
        propertyHelperConfig
      case false =>
        propertyHelperConfig.put("prefix", "")
    }

    val s3Config = new java.util.HashMap[String, String]
    s3Config.put("accessKey", Option(config.s3).flatMap(_.get("accessKey")).getOrElse(""))
    s3Config.put("secretKey", Option(config.s3).flatMap(_.get("secretKey")).getOrElse(""))

    val apixioFSConfig = new java.util.HashMap[String, Object]
    apixioFSConfig.put("mountPoint", Option(config.s3).flatMap(_.get("container")).getOrElse(""))
    apixioFSConfig.put("fromStorageType", "S3")
    apixioFSConfig.put("toStorageTypes", "S3")

    val storageConfig = new java.util.HashMap[String, Object]
    storageConfig.put("s3Config", s3Config)
    storageConfig.put("apixioFSConfig", apixioFSConfig)

    val cqlCacheConfig = new java.util.HashMap[String, Object]
    cqlCacheConfig.put("bufferSize", new Integer(config.cql.getOrElse("bufferSize", "100")))

    val globalTableConfig = new java.util.HashMap[String, Object]
    globalTableConfig.put("link", config.cql.getOrElse("linkTable", "apx_cflink"))
    globalTableConfig.put("link2", config.cql.getOrElse("linkTable2", "apx_cflink"))
    globalTableConfig.put("assemblyDataType", config.cql.getOrElse("assemblyDataType", "apx_cfnassemblydt"))
    globalTableConfig.put("index", config.cql.getOrElse("indexCF", "apx_cfindex"))
    globalTableConfig.put("index2", config.cql.getOrElse("indexCF2", "apx_cfindex"))
    globalTableConfig.put("seq", config.cql.getOrElse("sequenceStoreTable", "apx_cfSequenceStore"))
    globalTableConfig.put("metric", config.cql.getOrElse("metricTable", "apx_cfMetric"))

    val seqStoreConfig = new java.util.HashMap[String, Object]
    seqStoreConfig.put("paths", Option(config.seqstore).flatMap(_.get("paths")).getOrElse(""))
    seqStoreConfig.put("annotation_paths", Option(config.seqstore).flatMap(_.get("annotation_paths")).getOrElse(""))
    seqStoreConfig.put("noninferred_paths", Option(config.seqstore).flatMap(_.get("noninferred_paths")).getOrElse(""))
    seqStoreConfig.put("inferred_paths", Option(config.seqstore).flatMap(_.get("inferred_paths")).getOrElse(""))

    val summaryConfig = new java.util.HashMap[String, Object]
    summaryConfig.put("mergeMinNumThreshold", new Integer(0))
    summaryConfig.put("mergeMaxNumThreshold", new Integer(5))
    summaryConfig.put("mergeMinPeriodThresholdInMs", new Integer(0))
    summaryConfig.put("mergeMaxPeriodThresholdInMs", new Integer(5))

    val queryStoreConfig = new java.util.HashMap[String, Object]
    queryStoreConfig.put("batchSize", new Integer(config.cql.getOrElse("batchSize", "10000")))

    val daoConfig = new java.util.HashMap[String, Object]
    daoConfig.put("sequenceStoreEnhancementEnabled", java.lang.Boolean.TRUE)
    daoConfig.put("cqlCacheConfig", cqlCacheConfig)
    daoConfig.put("globalTableConfig", globalTableConfig)
    daoConfig.put("seqStoreConfig", seqStoreConfig)
    daoConfig.put("summaryConfig", summaryConfig)
    daoConfig.put("queryStore", queryStoreConfig)

    val pscMap = new java.util.HashMap[String, Object]
    pscMap.put("persistenceConfig", persistenceServiceConfigMap)
    pscMap.put("loggingConfig", loggingConfig)
    pscMap.put("customerPropertiesConfig", customerPropertiesConfig)
    pscMap.put("propertyHelperConfig", propertyHelperConfig)
    pscMap.put("storageConfig", storageConfig)
    pscMap.put("daoConfig", daoConfig)

    val uploadBatchConfig = new java.util.HashMap[String, Integer]
    uploadBatchConfig.put("cacheDuration", config.uploadBatchConfig.getOrElse("cacheDuration", 0));
    pscMap.put("uploadBatchConfig", uploadBatchConfig)

    val persistenceServiceConfigSet = ConfigSet.fromMap(pscMap)

    val d = DaoServicesSet.createDaoServices(persistenceServiceConfigSet)

    info("Setup dao services")
    d
  }

  lazy val sysServices: SysServices = {
    val tokenConfig = new java.util.HashMap[String, Object]
    tokenConfig.put("externalActivityTimeout", new Integer(config.acl.getOrElse("externalActivityTimeout", "300")))
    tokenConfig.put("externalMaxTTL", new Integer(config.acl.getOrElse("externalMaxTTL", "86400")))
    tokenConfig.put("internalMaxTTL", new Integer(config.acl.getOrElse("internalMaxTTL", "60")))

    val apiAclConfig = new java.util.HashMap[String, Object]
    apiAclConfig.put("aclColumnFamilyName", config.acl("cassandraColumnFamily"))

    val microServiceConfigMap = new java.util.HashMap[String, Object]
    microServiceConfigMap.put("tokenConfig", tokenConfig)
    microServiceConfigMap.put("apiaclConfig", apiAclConfig)

    val microServiceConfig = new MicroserviceConfig
    microServiceConfig.setMicroserviceConfig(microServiceConfigMap)

    val s = new SysServices(new DaoBase(daoServices), microServiceConfig)

    s.getAclLogic.setHasPermissionCacheTimeout(config.acl.getOrElse("aclHpCacheTimeout", "5000").toLong)

    info("Setup SysServices")
    s
  }

  lazy val distLock: DistLock = {
    new DistLock(redisOps, config.redis("prefix"))
  }

  lazy val cqlConnection : CqlConnector = crud.getCqlConnector

  lazy val crud: CqlCrud = daoServices.getCqlCrud

  lazy val applicationCrud = daoServices.getApplicationCqlCrud

  lazy val redisOps: RedisOps = daoServices.getRedisOps

  lazy val redisTrans: Transactions = daoServices.getRedisTransactions

  lazy val customerProperties: CustomerProperties = daoServices.getCustomerProperties

  lazy val seqStoreDAO: SeqStoreDAO = daoServices.getSeqStoreDAO

  lazy val kafkaConsumer: StreamConsumer[_,_] = {
    val props = new Properties
    config.kafka.filter(_._1.contains( """.""")).foreach(c => props.put(c._1, c._2))
    val kconsumer = new StreamConsumer(props)
    config.kafka.get("errorTopic") match {
      case Some(x) if x.size > 0 => kconsumer.setErrorTopic(x)
      case _ =>
    }
    val t = config.kafka("topic")
    val p = config.kafka("partition").toInt
    kconsumer.subscribe(t, p)
    config.kafka.get("offset") match {
      case Some("-1") => kconsumer.seekToEnd(t, p)
      case Some("-2") => kconsumer.seekToBeginning(t, p)
      case Some(x) if x.size > 0 => kconsumer.seek(t, p, x.toLong)
      case _ =>
        val offset = KafkaConsumerUtility.lastCommitted(config.kafka("bootstrap.servers"), config.kafka("group.id"), t, p)
        if (offset.isEmpty)
          kconsumer.seekToBeginning(t, p)
        else
          kconsumer.seek(t, p, offset.get + 1)
    }
    info("Setup kafka consumer")
    kconsumer
  }

  lazy val kafkaEventProducer: SimpleKafkaEventProducer = {
    val props = new Properties()
    props.put("metadata.broker.list", config.application("brokers").toString)
    props.put("request.required.acks", config.application.getOrElse("ackLevel","0").toString)
    val producer = new SimpleKafkaEventProducer(props)
    producer.setShutdownHook
    info("Setup kafka event producer")
    producer
  }

  // Original elasticClient is built on old es4 version (v2.x.x) for health check. However, since some micro-services start using new es4 version (v6.x.x)
  // and some classes in old versions are deprecated (here is ElasticClient) in new version, it is needed to refactor the elasticClient in scala-common for healthcheck
  lazy val elasticClient: ElasticClient = {
    val servers: Array[String] = config.elastic.getOrElse("hosts", ",0").asInstanceOf[String].split(',')
    val serverInfo: Array[String] = servers.map(_.split(':')).headOption.getOrElse(throw new RuntimeException(s"Cannot fetch config for elasticsearch"))
    val esServer: String = serverInfo(0)
    val port: Int = serverInfo(1).toInt
    val esClient = ElasticClient(JavaClient(ElasticProperties(s"$esServer:$port")))
    info("Setup elasticsearch client")
    esClient
  }

  lazy val security: Security = {
    val s = Security.getInstance()
    info("Setup security instance")
    s
  }

  lazy val cqlCache: ThreadLocalCqlCache = {
    val cache = new ThreadLocalCqlCache
    cache.setBufferSize(new Integer(new Integer(config.cql.getOrElse("bufferSize", "100"))))
    cache.setCqlCrud(applicationCrud)

    //if we create a thread local cache, make sure we register a shutdown hook to flush
    //the cache if we shut down the jvm..
    sys.addShutdownHook(ApxServices.cqlCache.flush())

    cache
  }


  def getCassandraConfig(config:Map[String,String]) = {
    val cassandraConfig = new java.util.HashMap[String, Object]
    cassandraConfig.put("hosts", config("hosts"))
    cassandraConfig.put("localDC", config("localDC"))
    cassandraConfig.put("binaryPort", new Integer(config.getOrElse("binaryPort", "9042")))
    cassandraConfig.put("keyspaceName", config.getOrElse("keyspace", "apixio"))
    cassandraConfig.put("baseDelayMs", new Integer(config.getOrElse("connections.baseDelayMs", "50")))
    cassandraConfig.put("maxDelayMs", new Integer(config.getOrElse("connections.maxDelayMs", "250")))

    cassandraConfig.put("readConsistencyLevel", config.getOrElse("consistencylevel.read", "LOCAL_QUORUM"))
    cassandraConfig.put("writeConsistencyLevel", config.getOrElse("consistencylevel.write", "LOCAL_QUORUM"))
    cassandraConfig.put("minConnections", new Integer(config.getOrElse("connections.min", "2")))
    cassandraConfig.put("maxConnections", new Integer(config.getOrElse("connections.max", "5")))
    cassandraConfig.put("cqlDebug", new java.lang.Boolean(config.getOrElse("debug", "false")))
    cassandraConfig.put("batchSyncEnabled", new java.lang.Boolean(config.getOrElse("batchSync", "true")))
    cassandraConfig.put("cqlMonitor", new java.lang.Boolean(config.getOrElse("monitor", "false")))

    val username = config.getOrElse("username", null)
    val password = config.getOrElse("password", null)

    if(username != null)
      cassandraConfig.put("username", username)

    if(password != null)
      cassandraConfig.put("password", password.toString)

    cassandraConfig
  }

  def getSearchConfig(config: Map[String, String]) = {
    val searchConfig = new util.HashMap[String, Object]
    searchConfig.put("hosts", config("hosts"))
    searchConfig.put("binaryPort", new Integer(config.getOrElse("binaryPort", "9200")))
    searchConfig.put("threadCount", new Integer(config.getOrElse("threadCount", "1")))
    searchConfig.put("connectionTimeoutMs", new Integer(config.getOrElse("connectionTimeoutMs", "10000")))
    searchConfig.put("socketTimeoutMs", new Integer(config.getOrElse("socketTimeoutMs", "60000")))
    searchConfig
  }

  lazy val slackService: SlackService = {
    val s = new SlackService(config.application("slack").asInstanceOf[Map[String, String]])
    info("Setup SlackService instance")
    s
  }

  def scalaMapToJavaMapGuts(smap: Map[String, Object], jmap: java.util.Map[String, Object]) {
    for ((k, v) <- smap) {
      if (v.isInstanceOf[Map[String, Object]]) {
        val submap = new java.util.HashMap[String, Object]

        scalaMapToJavaMapGuts(v.asInstanceOf[Map[String, Object]], submap)
        jmap.put(k, submap)
      }
      else {
        jmap.put(k, v)
      }
    }
  }

  lazy private val kafkaSignalMap = {
    val kafkaSignalMap = new java.util.HashMap[String, Object]
    config.kafkaSignal != null match {
      case true =>
        scalaMapToJavaMapGuts(config.kafkaSignal, kafkaSignalMap)
        kafkaSignalMap
      case false => kafkaSignalMap
    }
  }

}
