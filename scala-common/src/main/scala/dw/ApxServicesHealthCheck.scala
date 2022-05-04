package com.apixio.scala.dw

import com.apixio.scala.dw.ApxServicesHealthCheck.{checkIsResultHealthy, getUnhealthyMsg}
import com.apixio.scala.logging.ApixioLoggable
import com.codahale.metrics.health.HealthCheck
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, Response}

import scala.util.{Failure, Success, Try}

/**
 * The common apixio health check
 */
class ApxServicesHealthCheck(conf: ApxConfiguration) extends HealthCheck {
  override def check() : HealthCheck.Result = {
    val results = ApxServicesHealthCheck.checkServices(conf)
    val (isResultHealthy: Boolean, unHealthyErrorMsg: String) = (checkIsResultHealthy(results), getUnhealthyMsg(results))

    if (isResultHealthy == true || results.isEmpty)
      HealthCheck.Result.healthy
    else {
      HealthCheck.Result.unhealthy(unHealthyErrorMsg)
    }
  }
}

object ApxServicesHealthCheck extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)

  def checkServices(config: ApxConfiguration) : Map[String,(Boolean,String)] = {
    val serviceName = ApixioLoggable.getName()

    // extend the results if more health checks implemented in the future
    var results: Map[String,(Boolean,String)] = Map[String,(Boolean,String)]()
    if (config.cql != null && config.cql.nonEmpty)
      results = results ++ checkCql(config, serviceName)
    if (config.elastic != null && config.elastic.nonEmpty)
      results = results ++ checkES(config, serviceName)
    if (config.redis != null && config.redis.nonEmpty)
      results = results ++ checkRedis(config, serviceName)

    debug(s"health check for service: ${serviceName} in ApxServicesHealthCheck.checkServices. Result: ${results}")
    val (isResultHealthy: Boolean, unHealthyErrorMsg: String) = (checkIsResultHealthy(results), getUnhealthyMsg(results))
    if (isResultHealthy == false)
      warn(s"Found unhealthy healthcheck result for service: ${serviceName}. Error Msg: ${unHealthyErrorMsg}")
    results
  }

  //check each of the configured services for healthiness if applicable
  //application will register its own health check if it wants
  //acl, check redis
  //apxLogging, check appenders exist (TODO)
  //cql, check version
  def checkCql(config: ApxConfiguration, serviceName: String): Map[String, (Boolean, String)] = {
    Try {
      val res = ApxServices.cqlConnection.version()
      Map("cql" -> (res.nonEmpty, s"(Cassandra: ${res} for service: ${serviceName})"))
    } match {
      case Failure(t) =>
        warn(s"Fail to pass Cassandra health check for service: ${serviceName}. Error msg: ${t.getMessage}. Stack Trace: ${t.getStackTrace.toString}")
        Map("cql" -> (false, s"(Cassandra: failed to query for service: ${serviceName}). Error Msg: ${t.getMessage}. Stack Trace: ${t.getStackTrace.toString}"))
      case Success(res) =>
        res
    }
  }
  //elastic
  def checkES(config: ApxConfiguration, serviceName: String): Map[String, (Boolean, String)] = {
    Try {
      // Will throw error if trying to connecting wrong port. And will get Response if success (what status code response does not matter)
      val servers: Array[String] = config.elastic.getOrElse("hosts", ",0").asInstanceOf[String].split(',')
      val serverInfo: Array[String] = servers.map(_.split(':')).headOption.getOrElse(throw new RuntimeException(s"Cannot fetch config for elasticsearch"))
      val esServer: String = serverInfo(0)
      val port: Int = serverInfo(1).toInt
      val esClient = ElasticClient(JavaClient(ElasticProperties(s"$esServer:$port")))
      info(s"Setup elasticsearch client for service: ${serviceName}")
      val connectionTest: Response[IndexExistsResponse] = esClient.execute(indexExists("1")).await
      info(s"Complete elacticsearch healthcheck execute for service: ${serviceName}")
      esClient.close()
      info(s"Successfully close the elasticsearch client for service: ${serviceName}")
      if (connectionTest.isSuccess)
        Map("elastic" -> (true, s"(Elastic: successfully connect to elastic for service: ${serviceName})"))
      else
        Map("elastic" -> (false, s"(Elastic: ${connectionTest}) for service: ${serviceName}"))
    } match {
      case Failure(t) =>
        t.printStackTrace()
        warn(s"Fail to pass Elastic health check for service: ${serviceName}. Error msg: ${t.getMessage}. Stack Trace: ${t.getStackTrace.toString}")
        Map("elastic" -> (false, s"(Elastic: failed to query for service: ${serviceName}). Error Msg: ${t.getMessage}. Stack Trace: ${t.getStackTrace}"))
      case Success(res) => res
    }
  }
  //kafka, nothing immediately easy, probably something through metrics
  //microservices, unneeded (stateless)
  //mysql, deprecated?
  //redis
  def checkRedis(config: ApxConfiguration, serviceName: String): Map[String, (Boolean, String)] = {
    Try {
      val res = ApxServices.redisOps.ping()
      Map("redis" -> (res, s"(Redis: healthy: ${res}) for service: ${serviceName}"))
    } match {
      case Failure(t) =>
        warn(s"Fail to pass Redis health check for service: ${serviceName}. Error msg: ${t.getMessage}. Stack Trace: ${t.getStackTrace.toString}")
        Map("redis" -> (false, s"(Redis: failed to query). Error Msg: ${t.getMessage}. Stack Trace: ${t.getStackTrace.toString}"))
      case Success(res) => res
    }
  }


  //s3, no idea
  //security, pretty sure no check needed since services fails to start if not available
  //seqstore, check cql
  //wrap it all up

  def checkIsResultHealthy(results: Map[String,(Boolean,String)]): Boolean = results.map(x => x match {
    case (_, (eachHealthyResult, _)) => eachHealthyResult
  }).forall(_ == true)

  def getUnhealthyMsg(results: Map[String,(Boolean,String)]): String = results.map(x => x match {
    case(_, (_, eachCheckErrMsg)) => eachCheckErrMsg
  }).mkString(", ")
}
