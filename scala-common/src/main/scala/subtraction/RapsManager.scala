package com.apixio.scala.subtraction

import java.util.UUID

import com.apixio.bizlogic.patient.assembly.PatientAssembly
import com.apixio.model.patient.{Patient, Problem}
import com.apixio.model.profiler.Code
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.utility.IcdMapper
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.JavaConversions._

case class RapsCluster (
                         code: Code,
                         dosStart: DateTime,
                         dosEnd: DateTime,
                         providerType: Option[Code],
                         transactionDate: DateTime,
                         delete: Boolean = false
                       )
{
  def eventMessage: Map[String, String] = Map(
    "code" -> code.key,
    "dosStart" -> RapsCluster.formatDos(dosStart),
    "dosEnd" -> RapsCluster.formatDos(dosEnd),
    "providerType" -> providerType.map(_.code).getOrElse(""),
    "transaction" -> RapsCluster.formatDos(transactionDate)
  )
}

object RapsCluster {
  val dosDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy")
  def formatDos: DateTime => String = _.toDateTime(DateTimeZone.UTC).toString(dosDateFormatter)
}

case class RapsManager(mapping: String,
                          dosStart: Option[DateTime] = None,
                          dosEnd: Option[DateTime] = None,
                          transactionStart: Option[DateTime] = None,
                          transactionEnd: Option[DateTime] = None,
                          providerTypes: Option[Set[String]] = None,
                          problems: List[Problem] = List.empty
                         )  {

  def cluster: List[Problem] => List[RapsCluster] = RapsManager.cluster

  val isValid: Problem => Boolean = problem => {
    problem.getMetadata.get("CLAIM_TYPE") == RapsManager.RAPS_CLAIM_TYPE &&
      dosStart.forall(!_.isAfter(problem.getEndDate)) &&
      dosEnd.forall(!_.isBefore(problem.getEndDate)) &&
      transactionStart.forall(d => RapsManager.transactionDate(problem).forall(!_.isBefore(d))) &&
      transactionEnd.forall(d => RapsManager.transactionDate(problem).forall(_.isBefore(d))) &&
      providerTypes.forall(_.contains(RapsManager.providerType(problem).getOrElse("")))
  }

  def filter: List[Problem] => List[Problem] = _.filter(isValid)

  lazy val clusters: List[RapsCluster] = RapsManager.cluster(filter(problems))

  def codes(claimCluster: List[RapsCluster] = clusters, withChildren: Boolean = true): List[Code] =
    claimCluster.flatMap(cluster => {
      val hccMapping: String = IcdMapper.byDOS(cluster.dosEnd.toString(RapsCluster.dosDateFormatter), mapping)
      cluster.code.toHcc(hccMapping, withChildren)
    })
}

case class RapsTransaction(clusterId: String,
                           code: Code,
                           dosStart: DateTime, dosEnd: DateTime,
                           providerType: Option[Code],
                           transactionDate: DateTime,
                           problems: List[Problem],
                           isDelete: Boolean,
                           isError: Boolean)

object RapsManager {
  val dosDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy")
  val defaultDate = new DateTime(1900, 1, 1, 0, 0, DateTimeZone.UTC)
  val dtFormatter: DateTimeFormatter = ISODateTimeFormat.dateTime
  val knownErrors: Set[String] = Set("301", "302", "303", "304", "305", "306", "307", "308", "309",
    "310", "311", "313", "314", "315", "316", "317", "318", "319", "350", "353", "354",
    "400", "401", "402", "403", "404", "405", "406", "407", "408", "409",
    "410", "411", "412", "413", "414", "415", "416", "417", "418", "419",
    "420", "421", "422", "423", "424", "425", "450", "451", "453", "454", "455", "460", "490", "491", "492",
    "500", "502")
  val RAPS_CLAIM_TYPE: String = "RISK_ADJUSTMENT_CLAIM"

  def forProject(project: Project): RapsManager = {
    val claimstype = project.properties.getOrElse("gen", Map.empty).get("claimstype").map(_.asInstanceOf[String]).getOrElse("empty")
    val options: Map[String, String] = claimstype match {
      case s: String if s.startsWith("byproblem,") => s.split(",").lift(1)
        .map(_.split(";")
          .map(
            _.split("=")
          )
          .map(
            l => l.head -> l.lift(1).orNull
          )
          .filter(_._2 != null)
          .toMap
        ).getOrElse(Map.empty)
      case _ => Map.empty
    }
    RapsManager(
      project.icdMapping,
      dosStart = options.get("dosStart").map(DateTime.parse).orElse(Option(project.start)),
      dosEnd = options.get("dosEnd").map(DateTime.parse).orElse(Option(project.end)),
      transactionStart = options.get("transactionStart").map(DateTime.parse),
      transactionEnd = options.get("transactionEnd").map(DateTime.parse),
      providerTypes = options.get("providerTypes").filter(_.nonEmpty).map(_.split("\\|").toSet)
    )

  }

  def diagnosisCode(p: Problem): Code = Code(p.getCode.getCode, p.getCode.getCodingSystemOID)

  def providerType(p: Problem): Option[String] = Option(p.getMetadata.get("PROVIDER_TYPE"))

  def errors(p: Problem): List[String] = Option(p.getMetadata).map(_.filterKeys(_.contains("_ERROR")).values.toList)
    .getOrElse(List.empty).filter(x => Option(x).nonEmpty)

  def isDelete(p: Problem): Boolean = Option(p.getMetadata)
    .map(m => m.getOrDefault("DELETE_INDICATOR", m.getOrDefault("DELETE", "")))
    .filterNot(_ == null).exists(_.toLowerCase == "true")

  def transactionDate(p: Problem): Option[DateTime] = Option(p.getMetadata.get("TRANSACTION_DATE"))
    .map(DateTime.parse)
    .map(_.toDateTime(DateTimeZone.UTC))

  def clusterId(p: Problem): String =
    List(diagnosisCode(p).key,
      p.getStartDate.toDateTime(DateTimeZone.UTC).toString(dtFormatter),
      p.getEndDate.toDateTime(DateTimeZone.UTC).toString(dtFormatter),
      providerType(p).getOrElse("")
    ).mkString(",")

  def transactionId(p: Problem): String =
    s"${clusterId(p)},${transactionDate(p).map(_.toString(dtFormatter)).getOrElse("")}"

  def transaction(problems: List[Problem]): RapsTransaction = {
    val providerTypes = problems.flatMap(providerType).distinct
    assert(providerTypes.size < 2, s"${transactionId(problems.head)} has more than one provider")
    val code = diagnosisCode(problems.head)
    val isError: Boolean = problems.exists(p => errors(p).toSet.intersect(knownErrors).nonEmpty)
    val isDeleteTransaction: Boolean = problems.exists(isDelete)
    val isConflicted: Boolean = isDeleteTransaction && !problems.forall(isDelete)
    if (transactionDate(problems.head).nonEmpty)
      assert(!isConflicted, s"${transactionId(problems.head)} has both add and delete indicator")
    RapsTransaction(
      problems.headOption.map(clusterId).getOrElse(""),
      code,
      problems.head.getStartDate,
      problems.head.getEndDate,
      providerTypes.headOption.map(Code(_, Code.RAPSPROV)),
      transactionDate(problems.head).getOrElse(defaultDate),
      problems,
      isDeleteTransaction,
      isError
    )
  }

  /**
    * Convert a list of transactions (with the same cluster id)
    * into a single raps cluster
    * @param transactions list of transaction
    * @return Optional RapsCluster
    */
  def clusterTransactions(transactions: Iterable[RapsTransaction]): Option[RapsCluster] = {
    val isDelete = transactions.isEmpty || transactions.maxBy(_.transactionDate.getMillis).isDelete
    Some(RapsCluster(
      transactions.head.code,
      transactions.head.dosStart,
      transactions.head.dosEnd,
      transactions.head.providerType,
      transactions.maxBy(_.transactionDate.getMillis).transactionDate,
      isDelete
    ))
  }

  /**
    * Clustering a list of patient problems into clusters
    * @param problems list of input patient problems
    * @return list of raps clusters
    */
  def cluster(problems: List[Problem]): List[RapsCluster] = {
    val transactions = problems.groupBy(transactionId).mapValues(
      transaction
    ).values

    transactions.filterNot(_.isError) // ignore error
      .groupBy(_.clusterId)  // group by cluster ID
      .mapValues(clusterTransactions)
      .values.flatten.toList
  }

  /**
    * Getting patient problems data source
    * @param patientId Patient UUID
    * @param pdsId Patient data set ID
    * @param claimsBatches list of batches to filter problems
    * @return
    */
  def getPatientProblems(patientId: String,
                         pdsId: String,
                         claimsBatches: Option[List[String]] = None): List[Problem] = {
    val pat = ApxServices.patientLogic.getMergedPatientSummaryForCategory(pdsId, UUID.fromString(patientId), PatientAssembly.PatientCategory.PROBLEM.getCategory)
    // val pat = ApxServices.patientLogic.getPatient(pdsId, UUID.fromString(patientId))

    getPatientProblemsFromApo(List(pat).filterNot(_ == null), claimsBatches).distinct
  }

  def getPatientProblemsFromApo(pats: List[Patient], claimsBatches: Option[List[String]] = None): List[Problem] = {
    pats.flatMap(pat => {
      pat.getProblems.filter(p => {
        val parsingDetail = pat.getParsingDetailById(p.getParsingDetailsId)
        claimsBatches match {
          case None => true
          case Some(list) => Option(parsingDetail).map(_.getSourceUploadBatch).forall(d => list.contains(d))
        }
      })
    })
  }
}
