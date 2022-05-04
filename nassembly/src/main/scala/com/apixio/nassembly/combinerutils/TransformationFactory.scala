package com.apixio.nassembly.combinerutils

import com.apixio.dao.utility.PageUtility
import com.apixio.model.nassembly.{Base, TransformationMeta}

import scala.collection.JavaConversions._

object TransformationFactory {

  val InputName = "inputName"
  val OutputName = "outputName"

  def yearlyQuarter(inputName: String, outputName: String = DataBuckets.YearQuarter.toString): TransformationMeta = {
    yearlyQuarterFallback(Array(inputName), outputName)
  }

  def yearlyQuarterFallback(dateColNames: Array[String], outputName: String = DataBuckets.YearQuarter.toString): TransformationMeta = {
    val inputArgs = Map(InputName -> dateColNames)
    val outputArgs = Map(OutputName -> outputName)
    new TransformationMeta(YMDYearQuarter.name, inputArgs, outputArgs)
  }

  def linkToDomain(domainName: String, columnName: String = Base.FromCid): TransformationMeta = {
    createOneToOneTransformation(DomainLinkTransformation.name, domainName, columnName)
  }

  def linkToPatientDomain(outputName: String = Base.FromCid): TransformationMeta = {
    linkToDomain(Base.defaultDomainName, outputName)
  }

  def linkToDataType(dataTypeName: String, outputName: String = Base.FromCid): TransformationMeta = {
    val inputArgs = Map(InputName -> dataTypeName)
    val outputArgs = Map(OutputName -> outputName)
    new TransformationMeta(DatatypeLinkTransformation.name, inputArgs, outputArgs)
  }

  def filter(condition: String): TransformationMeta = {
    createInputTransformation(FilterTransformation.name, condition)
  }

  def readStringContent(inputName: String, outputName: String = PageUtility.STRING_CONTENT): TransformationMeta = {
    createOneToOneTransformation(StringContentTransformation.name, inputName, outputName)
  }

  def clearJsonData: TransformationMeta = {
    new TransformationMeta(ClearDataTransformation.name, Map.empty[String, Object], Map.empty[String, Object])
  }

  def markAsDelete(colName: String, condition: String): TransformationMeta = {
    createOneToOneTransformation(MarkAsDeleteTransformation.name, colName, condition)
  }

  def rename(columnName: String, newName: String): TransformationMeta = {
    val inputArgs = Map(InputName -> columnName) // spark changes SocialHistoryInfo.type.code -> code when we do a nested select
    val outputArgs = Map(OutputName -> newName)
    new TransformationMeta(RenameTransformation.name, inputArgs, outputArgs)
  }

  def extractColumn(nestedColumnName: String, newName: String): TransformationMeta = {
    val inputArgs = Map(InputName -> nestedColumnName)
    val outputArgs = Map(OutputName -> newName)
    new TransformationMeta(ExtractNestedColumn.name, inputArgs, outputArgs)
  }

  def fillInNull(colName: String, colVal: AnyRef): TransformationMeta = {
    val inputArgs = Map(InputName -> colName)
    val outputArgs = Map(OutputName -> colVal)
    new TransformationMeta(FillInNull.name, inputArgs, outputArgs)
  }

  lazy val rebaseCid: TransformationMeta = {
    createOneToOneTransformation(RenameTransformation.name, Base.FromCid, Base.Cid)
  }

  /**
   * Use default map keys for input and output args
   * @param functionName Function Name
   * @param input Input under InputName
   * @param output output under OutputName
   * @return
   */
  private def createOneToOneTransformation(functionName: String, input: String, output: String): TransformationMeta = {
    val inputArgs = Map(InputName -> input)
    val outputArgs = Map(OutputName -> output)
    new TransformationMeta(functionName, inputArgs, outputArgs)
  }

  private def createInputTransformation(functionName: String, input: String): TransformationMeta ={
    val inputArgs = Map(InputName -> input)
    new TransformationMeta(functionName, inputArgs, Map.empty[String, Object])
  }
}
