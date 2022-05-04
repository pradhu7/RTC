package com.apixio.scala.subtraction

import java.util.UUID

import com.apixio.bizlogic.patient.assembly.PatientAssembly
import com.apixio.model.patient.{Patient, Procedure}
import com.apixio.model.profiler.{BillTypeModel, Code, CptModel}
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.utility.IcdMapper
import com.apixio.scala.utility.alignment.DocumentMetadata.providerTypes
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.JavaConversions._

case class FFSCluster (
                         clusterId: String,
                         code: Code,
                         dosStart: DateTime,
                         dosEnd: DateTime,
                         providerType: Option[Code],
                         transactionDate: DateTime,
                         delete: Boolean = false
                       )

@deprecated("Duplicate: please update to use com.apixio.app.subtraction.FFSManager")
case class FFSManager(icdMapping: String,
                      cptVersion: Option[String] = None,
                      billTypeVersion: Option[String] = None,
                      dosStart: Option[DateTime] = None,
                      dosEnd: Option[DateTime] = None,
                      transactionStart: Option[DateTime] = None,
                      transactionEnd: Option[DateTime] = None) {
  import FFSManager.RichProcedure

  def isValid(procedure: Procedure): Boolean = {
    val validDate: Boolean = procedure.getMetadata.get("CLAIM_TYPE") == FFSManager.FFS_CLAIM_TYPE &&
      dosStart.forall(!_.isAfter(procedure.getEndDate)) &&
      dosEnd.forall(!_.isBefore(procedure.getEndDate)) &&
      transactionStart.forall(d => procedure.transactionDate.forall(!_.isBefore(d))) &&
      transactionEnd.forall(d => procedure.transactionDate.forall(_.isBefore(d)))

    validDate
  }

  def filter: List[Procedure] => List[Procedure] = _.filter(isValid)

  def cluster: List[Procedure] => List[FFSCluster] = procedures => FFSManager.cluster(cptVersion, billTypeVersion)(filter(procedures))

  def codes(claimCluster: List[FFSCluster], withChildren: Boolean = true): List[Code] =
    claimCluster.flatMap(cluster => {
      val hccMapping: String = IcdMapper.byDOS(cluster.dosEnd.toString(RapsCluster.dosDateFormatter), icdMapping)
      cluster.code.toHcc(hccMapping, withChildren)
    }).distinct

}

case class FFSTransaction(clusterId: String,
                          providerType: Option[Code],
                          diagnosis: List[Code],
                          dosStart: DateTime,
                          dosEnd: DateTime,
                          transactionDate: DateTime,
                          procedures: List[Procedure],
                          isDelete: Boolean
                         )

@deprecated("Duplicate: please update to use com.apixio.app.subtraction.FFSManager")
object FFSManager {
  val FFS_CLAIM_TYPE: String = "FEE_FOR_SERVICE_CLAIM"

  def forProject(project: Project): FFSManager = {
    val claimstype = project.properties.getOrElse("gen", Map.empty).get("claimstype").map(_.asInstanceOf[String]).getOrElse("empty")
    val cptVersion = project.properties.getOrElse("gen", Map.empty).get("cptversion").map(_.asInstanceOf[String])
    val billTypeVersion = project.properties.getOrElse("gen", Map.empty).get("billtypeversion").map(_.asInstanceOf[String])
    val options: Map[String, String] = claimstype match {
      case s: String if s.startsWith("byprocedure,") => s.split(",").lift(1)
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

    FFSManager(
      project.icdMapping,
      cptVersion = cptVersion,
      billTypeVersion = billTypeVersion,
      dosStart = options.get("dosStart").map(DateTime.parse).orElse(Option(project.start)),
      dosEnd = options.get("dosEnd").map(DateTime.parse).orElse(Option(project.end)),
      transactionStart = options.get("transactionStart").map(DateTime.parse),
      transactionEnd = options.get("transactionEnd").map(DateTime.parse)
    )
  }

  /**
    * Validate and create a transaction from a list of patient procedures object
    * Practically there should be a single procedure for a transaction
    * @param procedures List of Patient Procedures
    * @return a FFS transaction object
    */
  def transaction(cptVersion: Option[String], billTypeVersion: Option[String])(procedures: List[Procedure]): FFSTransaction = {
    val providerTypes = procedures.flatMap(_.providerType).distinct
    assert(providerTypes.size < 2, s"${procedures.head.transactionId} has more than one provider")
    // val billTypes = procedures.flatMap(_.billType).distinct
    // assert(billTypes.size < 2, s"${procedures.head.transactionId} has more than one bill type")
    // val cptCodes = procedures.flatMap(_.cptCode).distinct
    // assert(cptCodes.size < 2, s"${procedures.head.transactionId} has more than one CPT code")
    val isDeleteTransaction: Boolean = procedures.exists(_.isDelete)
    val isConflicted: Boolean = isDeleteTransaction && !procedures.forall(_.isDelete)
    if (procedures.head.transactionDate.nonEmpty)
      assert(!isConflicted, s"{transactionId(procedures.head)} has both add and delete indicator")
    FFSTransaction(
      procedures.headOption.map(_.clusterId).getOrElse(""),
      providerTypes.headOption,
      procedures.filter(_.isValidFFS(cptVersion, billTypeVersion)).flatMap(_.diagnosisCodes),
      procedures.head.getPerformedOn,
      procedures.head.getEndDate,
      procedures.head.transactionDate.getOrElse(defaultDate),
      procedures,
      isDeleteTransaction
    )
  }

  /**
    * Convert a list of transactions (with the same cluster id)
    * into a single raps cluster
    * @param transactions list of transaction
    * @return Optional RapsCluster
    */
  def clusterTransactions(transactions: Iterable[FFSTransaction]): List[FFSCluster] = {
    val isDelete = transactions.isEmpty || transactions.maxBy(_.transactionDate.getMillis).isDelete
    val dx: List[Code] = transactions.filterNot(_.isDelete).flatMap(_.diagnosis).toList.distinct
    dx.map(
      dx => FFSCluster(
        transactions.head.clusterId,
        dx,
        transactions.head.dosStart,
        transactions.head.dosEnd,
        transactions.head.providerType,
        transactions.maxBy(_.transactionDate.getMillis).transactionDate,
        isDelete
      )
    )
  }

  /**
    * Clustering a list of patient problems into clusters
    * @param procedures list of input patient procedures
    * @return list of ffs clusters
    */
  def cluster(cptVersion: Option[String] = None, billTypeVersion: Option[String] = None)(procedures: List[Procedure]): List[FFSCluster] = {
    val transactions: Iterable[FFSTransaction] = procedures.groupBy(_.transactionId).mapValues(
      transaction(cptVersion, billTypeVersion)
    ).values

    transactions
      .groupBy(_.clusterId)  // group by cluster ID
      .mapValues(clusterTransactions)
      .values.flatten.toList
  }

  /**
    * Get patient procedures
    * @param patientId Patient ID
    * @param pdsId Patient Data Set ID
    * @param claimsBatches list of upload batches to filter
    * @return list of procedures of patient
    */
  def getPatientProcedures(patientId: String,
                          pdsId: String,
                          claimsBatches: Option[List[String]] = None): List[Procedure] = {
    val pat = ApxServices.patientLogic.getMergedPatientSummaryForCategory(
      pdsId,
      UUID.fromString(patientId),
      PatientAssembly.PatientCategory.PROCEDURE.getCategory
    )

    getPatientProceduresFromApo(List(pat).filterNot(_ == null), claimsBatches)
  }

  /**
    * Get patient procedures from a list of patient objects
    * @param pats list of patient objects
    * @param claimsBatches filter procedures by upload batch if provided
    * @return list of procedures for patient
    */
  def getPatientProceduresFromApo(pats: List[Patient], claimsBatches: Option[List[String]]): List[Procedure] =
    pats.flatMap(pat => {
      pat.getProcedures.filter(p => {
        claimsBatches match {
          case None => true
          case Some(list) =>
            val parsingDetail = pat.getParsingDetailById(p.getParsingDetailsId)
            list.contains(parsingDetail.getSourceUploadBatch)
        }
      })
    })

  val dosDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy")
  val defaultDate = new DateTime(1900, 1, 1, 0, 0, DateTimeZone.UTC)
  val dtFormatter: DateTimeFormatter = ISODateTimeFormat.dateTime

  implicit class RichProcedure(p: Procedure) {
    def cptCode: Option[Code] = Option(Code(p.getCode.getCode, p.getCode.getCodingSystemOID)).filter(_.isCpt())

    def metadata: Map[String, String] = Option(p.getMetadata).map(_.toMap).getOrElse(Map.empty)

    def billType: Option[Code] = metadata.get("BILL_TYPE")
      .map(_.split("\\^")).filter(_.length == 4)
      .filter(l => l(3) == Code.BILLTYPE)
      .map(l => Code(l.head, l(3)))

    def parseDate(s: String): Option[DateTime] = try {
      Some(DateTime.parse(s)) // default
    } catch {
      case _: Throwable =>
        try {
          Some(FFSManager.dosDateFormatter.parseDateTime(s))
        } catch {
          case _: Throwable => None
        }
    }

    def providerType: Option[Code] = metadata.get("PROVIDER_TYPE")
      .map(_.split("\\^")).filter(_.length == 4)
      .filter(l => l(3) == Code.RAPSPROV || l(3) == Code.FFSPROV)
      .filter(l => providerTypes.contains(l.head))
      .map(l => Code(l.head, l(3)))

    def transactionDate: Option[DateTime] = metadata.get("TRANSACTION_DATE")
      .flatMap(parseDate)
      .map(_.toDateTime(DateTimeZone.UTC))

    def isDelete: Boolean = metadata.get("DELETE_INDICATOR").filter(_ != null).map(_.toLowerCase)
      .contains("true")

    def clusterId: String = {
      List(
        p.getOriginalId.getId,
        p.getOriginalId.getAssignAuthority
      ).mkString("^^")
    }

    def transactionId: String =
      s"$clusterId,${transactionDate.map(_.toString(dtFormatter)).getOrElse("")}"

    def diagnosisCodes: List[Code] = p.getSupportingDiagnosis.map(c => Code(c.getCode, c.getCodingSystemOID)).toList

    // get version of CPT to use.
    def cptVersion: String = p.getEndDate.getYear.toString

    // get version of CPT to use.
    def billTypeVersion: String = p.getEndDate.getYear.toString

    def isValidFFS(cptModelVersion: Option[String] = None, billTypeModelVersion: Option[String] = None): Boolean = p.providerType match {
      case Some(pv) if pv.isProfessional() =>
        p.cptCode.exists(c => CptModel.isValid(cptModelVersion.getOrElse(cptVersion), c))
      case Some(pv) if pv.isInpatient() =>
        p.billType.exists(c => BillTypeModel.getProviderType(billTypeModelVersion.getOrElse(billTypeVersion), c) == "Inpatient")
      case Some(pv) if pv.isOutpatient() =>
        p.billType.exists(c => BillTypeModel.getProviderType(billTypeModelVersion.getOrElse(billTypeVersion), c) == "Outpatient" &&
          p.cptCode.exists(c => CptModel.isValid(cptVersion, c)))
      case _ => false
    }
  }
}
