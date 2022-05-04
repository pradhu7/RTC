package com.apixio.scala.apxapi


import akka.actor.ActorSystem
import apxapi.{Cerebro, ModelCatalog, Webster}
import com.apixio.restbase.RestUtil

import scala.concurrent.duration._
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.apixio.scala.logging.ApixioLoggable

import scala.util.{Random, Try}

class ApxApi(
    config: ApxConfiguration,
    var username: Option[String] = None,
    var password: Option[String] = None,
    var external_token: Option[String] = None,
    var internal_token: Option[String] = None) {

  def isDefined(s: String): Boolean = {
    ApxApi.servicesCache.keySet contains s
  }

  val session = new ApxSession(username, password, external_token, internal_token,
    if (isDefined(ApxApi.USERACCOUNTS)) Some(new UserAccounts(ApxApi.service(ApxApi.USERACCOUNTS), null)) else None,
    if (isDefined(ApxApi.TOKENIZER)) Some(new Tokenizer(ApxApi.service(ApxApi.TOKENIZER), null)) else None)

  val bundler: Bundler = if (isDefined(ApxApi.BUNDLER)) new Bundler(ApxApi.service(ApxApi.BUNDLER), session)  else null
  val cmp: CMP = if (isDefined(ApxApi.CMP)) new CMP(ApxApi.service(ApxApi.CMP), session) else null
  val dataorchestrator: DataOrchestrator = if (isDefined(ApxApi.DATAORCHESTRATOR)) new DataOrchestrator(ApxApi.service(ApxApi.DATAORCHESTRATOR), session) else null

  val eventcloudhcc: Null = if (isDefined(ApxApi.EVENTCLOUDHCC)) null else null
  val reports: Null = if (isDefined(ApxApi.REPORTS)) null else null
  val projectadmin: ProjectAdmin = if (isDefined(ApxApi.PROJECTADMIN)) new ProjectAdmin(ApxApi.service(ApxApi.PROJECTADMIN), session) else null
  val router: Router = if (isDefined(ApxApi.ROUTER)) new Router(ApxApi.service(ApxApi.ROUTER), session) else null
  val tokenizer: Tokenizer = if (isDefined(ApxApi.TOKENIZER)) new Tokenizer(ApxApi.service(ApxApi.TOKENIZER), session) else null
  val useraccounts: UserAccounts = if (isDefined(ApxApi.USERACCOUNTS)) new UserAccounts(ApxApi.service(ApxApi.USERACCOUNTS), session) else null
  val cv2: CV2 = if (isDefined(ApxApi.CV2)) new CV2(ApxApi.service(ApxApi.CV2), session) else null
  val npi: Npi = if (isDefined(ApxApi.NPI)) new Npi(ApxApi.service(ApxApi.NPI), session) else null
  val workqueue: Workqueue = if (isDefined(ApxApi.WORKQUEUE)) new Workqueue(ApxApi.service(ApxApi.WORKQUEUE), session) else null
  val signalmanageradmin: SignalManagerAdmin = if (isDefined(ApxApi.SIGNALMANAGERADMIN)) new SignalManagerAdmin(ApxApi.service(ApxApi.SIGNALMANAGERADMIN), session) else null
  val metrics: Metrics = if (isDefined(ApxApi.METRICS)) new Metrics(ApxApi.service(ApxApi.METRICS), session) else null
  val reporting: Reporting = if (isDefined(ApxApi.REPORTING)) new Reporting(ApxApi.service(ApxApi.REPORTING), session) else null
  val sessionmanager: Sessionmanager =  if (isDefined(ApxApi.SESSIONMANAGER)) new Sessionmanager(ApxApi.service(ApxApi.SESSIONMANAGER), session) else null
  val patientsvc: Patientsvc = if (isDefined(ApxApi.PATIENTSVC)) new Patientsvc(ApxApi.service(ApxApi.PATIENTSVC), session) else null
  val mcs: ModelCatalog = if (isDefined(ApxApi.MODELCATALOGSVC)) new ModelCatalog(ApxApi.service(ApxApi.MODELCATALOGSVC), session) else null
  val prospective: Prospective = if(isDefined(ApxApi.PROSPECTIVE)) new Prospective(ApxApi.service(ApxApi.PROSPECTIVE), session) else null
  val cerebro: Cerebro = if(isDefined(ApxApi.CEREBRO)) new Cerebro(ApxApi.service(ApxApi.CEREBRO), session) else null
  val documentSearch: DocumentSearch = if (isDefined(ApxApi.DOCUMENTSEARCH)) new DocumentSearch(ApxApi.service(ApxApi.DOCUMENTSEARCH), session) else null
  val webster: Webster = if (isDefined(ApxApi.WEBSTER)) new Webster(ApxApi.service(ApxApi.WEBSTER), session) else null
}

case class MicroServiceStat(var microservice: String, connection: ServiceConnection, var nCall: Int,
                            var lastUsed: Long, var lastFiveCalls: Int, var nGoodCall: Int, var nBadCall: Int)

object ApxApi extends ApixioLoggable {
  this.setupLog(this.getClass.getName)
  val BUNDLER = "bundler"
  val CMP = "cmpmsvc"
  val DATAORCHESTRATOR = "dataorch"
  val EVENTCLOUDHCC = "events"
  val REPORTS = "reports"
  val ROUTER = "router"
  val TOKENIZER = "tokenizer"
  val USERACCOUNTS = "useracct"
  val CV2 = "cv2"
  val NPI = "npi"
  val WORKQUEUE = "workqueue"
  val METRICS = "metrics"
  val REPORTING = "reporting"
  val SESSIONMANAGER = "sessionmanager"
  val SIGNALMANAGERADMIN = "signalmgradmin-reporting"
  val MAPPING = "mapping"
  val PROJECTADMIN =  "projectadmin"
  val PATIENTSVC = "patientsvc"
  val MODELCATALOGSVC = "modelcatalogsvc"
  val PROSPECTIVE = "prospective"
  val CEREBRO = "cerebro"
  val DOCUMENTSEARCH = "documentsearch"
  val WEBSTER = "webster"

  val DEFAULT_TIMEOUT = 10000
  val DEFAULT_RST_TIMEOUT = 3000
  val DEFAULT_RETRIES = 5

  val RANDOM_STRATEGY = "RANDOM"
  val FIRST_STRATEGY = "FIRST"

  val BINARY_11111 = 31
  val SUCCESS = true
  val FAIL = false

  val servicesCache: Map[String, List[String]] = loadServices()

  implicit val system: ActorSystem = ActorSystem("scala-common")

  def internal() : ApxApi = new ApxApi(
    ApxServices.configuration,
    internal_token = Option(RestUtil.getInternalToken.getID).map(_.toString)
  )

  def external(externalToken: String) : ApxApi = new ApxApi(
    ApxServices.configuration, external_token = Some(externalToken),
    internal_token = Option(RestUtil.getInternalToken.getID).map(_.toString)
  )

  var microserviceStats: Map[String, List[MicroServiceStat]] = loadMicroservices()
  var lastMicroservicesUsed: Map[String, ServiceConnection] = Map[String, ServiceConnection]()

  def service(name: String, strategy:String = RANDOM_STRATEGY) : ServiceConnection = {
    if (microserviceStats(name).isEmpty) {
      warn(s"Unable to find a microservices stat with name: $name. Reloading configuration...")
      reload()
    }

    // @ToDo: take into account lastFiveCalls result
    val connection:Option[ServiceConnection] = strategy match {
      case RANDOM_STRATEGY =>
        if ((microserviceStats(name).length == 1) || lastMicroservicesUsed.isEmpty || !lastMicroservicesUsed.keySet.contains(name))
          Some(this.service(name, FIRST_STRATEGY))
        else {
          info("Last Service Used for " + lastMicroservicesUsed(name))
          Some(Random.shuffle(microserviceStats(name).filterNot(_.connection.url==lastMicroservicesUsed(name))).head.connection)
        }


      case FIRST_STRATEGY => Some(microserviceStats(name).head.connection)
      case _ => None
    }

    connection match {
      case Some(x:ServiceConnection) => {
        lastMicroservicesUsed ++= Map(name -> x)
        x
      }
      case None => throw new Exception("Strategy: " + strategy + " Not Recognized")
    }
  }

  /**
   * Create CircuitBreakerConfig
   *
   * Will try to get values for callTimeout, readTimeout and maxFailures from
   * configuration via key formatted as: microserviceConfig: {service-name}:
   * with keys timeout, resetTimeout, maxFailures,
   * i.e. timeout: 10
   *
   * If config is not available will fall back to default values
   *
   * @param configName
   * @param circuitBreakerName
   * @return CircuitBreakerConfig
   */
  def getCircuitBreakerConfig(configName: String, circuitBreakerName: String): CircuitBreakerConfig = {
    val serviceConfig = Try(ApxServices.configuration.microServiceConfig.getOrElse(configName, Map()).asInstanceOf[Map[String, Object]])
    val callTimeout = Try({
      serviceConfig.get("timeout").toString.toInt
    }).getOrElse(DEFAULT_TIMEOUT) / 1000
    val resetTimeout = Try({
      serviceConfig.get("resetTimeout").toString.toInt
    }).getOrElse(DEFAULT_RST_TIMEOUT) / 1000
    val maxFailures = Try({
      serviceConfig.get("maxFailures").toString.toInt
    }).getOrElse(DEFAULT_RETRIES)

    CircuitBreakerConfig(circuitBreakerName, callTimeout, resetTimeout, maxFailures)
  }

  def updateMicroservicesStat(url: String, success: Boolean, timestamp: Long): Unit = {
    // find the stat, its service name and index
    val stats = microserviceStats.flatMap(_._2).filter(_.connection.url==url)
    if (stats.isEmpty) {
      val current = microserviceStats.values.map(_.map(s => s"${s.microservice} ${s.connection.url}"))
      warn(s"Unable to update microservices stat for url: $url. Current stats are: $current")
      loadMicroservices()
    } else {
      val stat = stats.head
      val name = stat.microservice
      val index = microserviceStats(name).indexOf(stat)

      // update lastFileCalls by: SHIFT LEFT, ADD 1 or 0 depending on success or not, then LIMIT to 5 bits
      val newStat = if (success)
        MicroServiceStat(name, stat.connection, stat.nCall + 1, timestamp, (stat.lastFiveCalls << 1 | 1) & BINARY_11111, stat.nGoodCall + 1, stat.nBadCall)
      else
        MicroServiceStat(name, stat.connection, stat.nCall + 1, timestamp, (stat.lastFiveCalls << 1) & BINARY_11111, stat.nGoodCall, stat.nBadCall + 1)

      microserviceStats(name).updated(index, newStat)
    }
  }

  private def loadMicroservices(): Map[String, List[MicroServiceStat]] = {
    // an example of service is router -> http://router1.com, http://router2.com
    val services = servicesCache
      .map { case (serviceName, serviceUrls) =>
        val microServiceStat = serviceUrls.map(url => {
          val cleanUrl = url.trim()
          val featureConfig = Try({
            ApxServices.configuration.featureFlagsConfig.getOrElse("feature", Map()).asInstanceOf[Map[String, String]]
          }).getOrElse(Map())
          val callProxyObj = if (featureConfig.getOrElse("circuitBreaker", "true").toBoolean) {
            val circuitBreakerConfig = getCircuitBreakerConfig(serviceName, cleanUrl)
            new CircuitBreakerProxy(circuitBreakerConfig)
          } else {
            val timeout = Try({
              val serviceConfig = ApxServices.configuration.microServiceConfig.getOrElse(serviceName, Map()).asInstanceOf[Map[String, Object]]
              serviceConfig.get("timeout").toString.toInt
            }).getOrElse(DEFAULT_TIMEOUT)
            new CallProxy(timeout milliseconds)
          }
          val serviceConnection = ServiceConnection(cleanUrl, callProxyObj)
          MicroServiceStat(serviceName, serviceConnection, 0, 0, 0, 0, 0)
        })
        serviceName -> microServiceStat
      }

    val str = services.values.map(_.map(s => s"${s.microservice} ${s.connection.url}"))
    info(s"Microservice Updated: $str")
    services
  }

  //load services configuration from yaml
  def loadServices(): Map[String, List[String]] = {
    val result: Option[Map[String, List[String]]] =
      if (Try(ApxServices.configuration.microservices).getOrElse(null) != null) {
        Some(ApxServices.configuration.microservices.map({
          case (service_name, location) => service_name -> List(location)
        }))
      } else {
        warn("Could not find any microservice configurations. Returning empty microservice configuration")
        None
      }
    result.getOrElse(Map())
  }

  def reload(): Unit = {
    // ToDo: improve this by retaining stats. This is not a must but would be nice
    microserviceStats ++= loadMicroservices()
  }
}
