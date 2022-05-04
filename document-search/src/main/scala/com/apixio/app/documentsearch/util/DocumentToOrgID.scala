package com.apixio.app.documentsearch.util

import java.util.UUID

import com.apixio.restbase.apiacl.model.ApiDef
import com.apixio.restbase.apiacl.perm.Extractor
import com.apixio.restbase.apiacl.{ApiAcls, CheckContext, HttpRequestInfo, MatchResults}
import com.apixio.scala.dw.ApxServices
import com.apixio.useracct.buslog.PatientDataSetLogic

class DocumentToOrgID extends Extractor {

  @Override
  def requiresHttpEntity(): Boolean = {
    false
  }

  @Override
  def extract(ctx: CheckContext, matchres: MatchResults, info: HttpRequestInfo, prevInChain: Object, httpEntity: Object): Object = {
    if (prevInChain == null) {
      null
    } else {
      try {
        val id = ApxServices.patientAdminLogic.getPdsIDByDocumentUUID(UUID.fromString(prevInChain.asInstanceOf[String]))
        PatientDataSetLogic.patientDataSetIDFromLong(id.toLong)
        // "O_00000000-0000-0000-0000-" + id.toString.reverse.padTo(12, 0).reverse.mkString   // from hcc-v4's IdUtil.scala; assumes (oldOrg) prefix of "O_"
      } catch  { case ex : Exception => null }
    }
  }

  def init(var1: ApiAcls.InitInfo, var2: ApiDef, var3: String) = {
  }
}