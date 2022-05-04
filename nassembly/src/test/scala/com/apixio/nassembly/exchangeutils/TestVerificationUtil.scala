package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.ClinicalCodeOuterClass.{ClinicalCode => ClinicalCodeProto}
import com.apixio.datacatalog.ExternalIdOuterClass
import com.apixio.datacatalog.YearMonthDayOuterClass.YearMonthDay
import com.apixio.model.external.{AxmCodeOrName, CaretParser}
import com.apixio.model.patient._
import com.apixio.nassembly.apo.{BaseConverter, CodedBaseConverter}
import com.apixio.util.nassembly.AxmToProtoConverters.convertAxmClinicalCode
import com.apixio.util.nassembly.DataCatalogProtoUtils
import org.joda.time.{DateTime, LocalDate}

import java.util

object TestVerificationUtil {

  def verifyStrings(srcString: String, destString: String) = {
    assert(destString.equals(srcString))
  }

  def verifyCodeTranslations(codeTranslationList: util.List[ClinicalCode],
                             protoCodeTranslationList: util.List[ClinicalCodeProto]) = {

    val convertedClinicalCodeList: util.List[ClinicalCode] = CodedBaseConverter
      .convertCodeTranslations(protoCodeTranslationList)

    assert(codeTranslationList.containsAll(convertedClinicalCodeList))
  }

  def verifyClinicalCodeByFields(clinicalCode: ClinicalCode, clinicalCodeProto: ClinicalCodeProto) = {
    verifyStrings(clinicalCodeProto.getCode, clinicalCode.getCode)
    verifyStrings(clinicalCodeProto.getSystem, clinicalCode.getCodingSystem)
    verifyStrings(clinicalCodeProto.getDisplayName, clinicalCode.getDisplayName)
    verifyStrings(clinicalCodeProto.getSystemVersion, clinicalCode.getCodingSystemVersions)
    verifyStrings(clinicalCodeProto.getSystemOid, clinicalCode.getCodingSystemOID)
  }

  def verifyClinicalCodeByFields(srcClinicalCode: ClinicalCode, destClinicalCode: ClinicalCode) = {
    verifyStrings(srcClinicalCode.getCode, destClinicalCode.getCode)
    verifyStrings(srcClinicalCode.getCodingSystem, destClinicalCode.getCodingSystem)
    verifyStrings(srcClinicalCode.getDisplayName, destClinicalCode.getDisplayName)
    verifyStrings(srcClinicalCode.getCodingSystemVersions, destClinicalCode.getCodingSystemVersions)
    verifyStrings(srcClinicalCode.getCodingSystemOID, destClinicalCode.getCodingSystemOID)
  }

  def verifyClinicalCodeByConverting(clinicalCodeProto: ClinicalCodeProto,
                                     modelClinicalCode: ClinicalCode) = {
    val convertedProtoPanelCode = BaseConverter.convertCode(clinicalCodeProto)
    assert(convertedProtoPanelCode.equals(modelClinicalCode))
  }

  def verifyClinicalCodeByConverting(srcCode: ClinicalCodeProto,
                                     destCode: ClinicalCodeProto) = {
    val srcConvertedCode = BaseConverter.convertCode(srcCode)
    val destConvertedCode = BaseConverter.convertCode(destCode)
    assert(srcConvertedCode.equals(destConvertedCode))
  }

  def verifyClinicalCodeByConverting(srcCode: ClinicalCodeProto,
                                     destCode: String) = {
    val srcConvertedCode = BaseConverter.convertCode(srcCode)
    val destConvertedCode = BaseConverter.convertCode(convertRawStringToClinicalCode(destCode))
    assert(srcConvertedCode.equals(destConvertedCode))
  }

  def convertRawStringToClinicalCode(raw: String) = {
    val code: AxmCodeOrName = CaretParser.toAxmCodeOrName(raw)
    if (code.isCode) {
      convertAxmClinicalCode(code.getCode)
    } else {
      ClinicalCodeProto.newBuilder().build()
    }
  }

  def verifyLocalDates(protoDate: YearMonthDay, localDate: LocalDate){
    val convertedProtoDate = DataCatalogProtoUtils.toDateTime(protoDate).toLocalDate
    assert(convertedProtoDate.equals(localDate))
  }

  def verifyDates(protoDate: YearMonthDay,
                  modelDate: DateTime) = {
    val convertedProtoDate = DataCatalogProtoUtils.toDateTime(protoDate)
    assert(convertedProtoDate.equals(modelDate))
  }

  def verifyDates(protoDate: YearMonthDay,
                  stringDateInYYYYMMDD: String) = {
    val convertedProtoDate = DataCatalogProtoUtils.dateToYYYYMMDD(protoDate)
    assert(convertedProtoDate.equals(stringDateInYYYYMMDD))
  }


  def verifyDates(srcDate: DateTime,
                  destDate: DateTime) = {
    assert(srcDate.equals(destDate))
  }

  def verifyExternalOriginalId(protoExternalId: ExternalIdOuterClass.ExternalId,
                               externalId: ExternalID) = {
    val doulbeConvertedExternalId = DataCatalogProtoUtils.convertOriginalIdToExternalId(protoExternalId)
    assert(externalId.equals(doulbeConvertedExternalId))
  }

  def verifyExternalOriginalId(srcExternalId: ExternalID,
                               destExternalId: ExternalID) = {

    assert(srcExternalId.getId.equals(destExternalId.getId))
    assert(srcExternalId.getAssignAuthority.equals(destExternalId.getAssignAuthority))
  }

}