package com.apixio.model.utils

import com.apixio.model.profiler._
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}

import java.io.{ByteArrayInputStream, InputStream}
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import com.google.common.cache.{CacheBuilder, CacheLoader}

object TestUtils {

  val CODE_MAPPING = "code-mapping.json"
  val CODE_MAPPING_V2 = "code-mapping-v2.json"
  val HCC_MODEL = "hcc-model.json"
  val HCC_MODEL_V2 = "hcc-model-v2.json"
  val ICD_MODEL = "icd-model.json"
  val ICD_MODEL_V2 = "icd-model-v2.json"
  val CPT_MODEL = "cpt-model.json"
  val CPT_MODEL_V2 = "cpt-model-v2.json"
  val BILLTYPE_MODEL = "billtype-model.json"
  val QUALITYMEASURE_MODEL = "qualitymeasure-model.json"
  val QUALITYCONJECTURE_MODEL = "qualityconjecture-model.json"
  val QUALITYFACT_MODEL = "qualityfact-model.json"
  val RISKGROUPS_MODEL = "riskgroups-model.json"
  val SNOMED_MODEL="snomed-model.json"
  val MAPPING_BASE = "https://mapping-stg.apixio.com:8443"

  private def notFoundString(mapping:String): String = s"""
      | [WARNING]
      | $mapping not found in resource folder. Mapping data will be fetched from
      | apx-mapping service instead. This API call can be avoided by placing
      | $mapping file in src/test/resources directory.
      | You can generate all needed mapping by running the following commands,
      | from project root folder:
      |
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/code > model/src/test/resources/code-mapping.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/icd > model/src/test/resources/icd-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/hcc > model/src/test/resources/hcc-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/cpt > model/src/test/resources/cpt-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/measure > model/src/test/resources/qualitymeasure-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/fact > model/src/test/resources/qualityfact-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/conjecture > model/src/test/resources/qualityconjecture-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/billtype > model/src/test/resources/billtype-model.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v0/riskgroups > model/src/test/resources/riskgroups-model.json
      |
      | curl https://mapping-stg.apixio.com:8443/mapping/v2/code > model/src/test/resources/code-mapping-v2.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v2/icd > model/src/test/resources/icd-model-v2.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v2/hcc > model/src/test/resources/hcc-model-v2.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v2/cpt > model/src/test/resources/cpt-model-v2.json
      | curl https://mapping-stg.apixio.com:8443/mapping/v2/snomed > model/src/test/resources/snomed-model.json
      |
      | Please don't check those files in, to encourage testing against the latest mapping.
    """.stripMargin

  private val mappingCache = CacheBuilder.newBuilder()
    .expireAfterWrite(60, TimeUnit.MINUTES)
    .maximumSize(5)
    .build(
      new CacheLoader[String, String] {
        def load(mapping: String): String = {
          val connectionURL = mapping match {
            case CODE_MAPPING => s"$MAPPING_BASE/mapping/v0/code"
            case ICD_MODEL => s"$MAPPING_BASE/mapping/v0/icd"
            case HCC_MODEL => s"$MAPPING_BASE/mapping/v0/hcc"
            case CPT_MODEL => s"$MAPPING_BASE/mapping/v0/cpt"
            case QUALITYMEASURE_MODEL => s"$MAPPING_BASE/mapping/v0/measure"
            case QUALITYCONJECTURE_MODEL => s"$MAPPING_BASE/mapping/v0/conjecture"
            case QUALITYFACT_MODEL => s"$MAPPING_BASE/mapping/v0/fact"
            case BILLTYPE_MODEL =>s"$MAPPING_BASE/mapping/v0/billtype"
            case RISKGROUPS_MODEL => s"$MAPPING_BASE/mapping/v0/riskgroups"
            case CODE_MAPPING_V2 => s"$MAPPING_BASE/mapping/v2/code"
            case ICD_MODEL_V2 => s"$MAPPING_BASE/mapping/v2/icd"
            case HCC_MODEL_V2 => s"$MAPPING_BASE/mapping/v2/hcc"
            case CPT_MODEL_V2 => s"$MAPPING_BASE/mapping/v2/cpt"
            case SNOMED_MODEL => s"$MAPPING_BASE/mapping/v2/snomed"
          }
          val connection = (new URL(connectionURL).openConnection.asInstanceOf[HttpURLConnection])
          connection.setConnectTimeout(5000)
          connection.setReadTimeout(20000)
          connection.setRequestMethod("GET")
          io.Source.fromInputStream(connection.getInputStream).mkString
        }
      }
    )

  private def getInputStreamFromMappingService(mapping: String): InputStream = {
    try {
      val data: String = mappingCache(mapping)
      new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))
    } catch {
      case e: Throwable => {
        println(s"[ERROR] No $mapping file found and unable to fetch mapping from apx-mapping")
        throw e
      }
    }
  }

  def getCodeMappingIfNotExist(mapping: String) : InputStream = {
    var data = getClass.getClassLoader.getResourceAsStream(mapping)
    if (data == null) {
      println(notFoundString(mapping))
      data = getInputStreamFromMappingService(mapping)
    }
    data
  }

  def initMappings() = {
    implicit val mapper = {
      val m = new ObjectMapper() with ScalaObjectMapper
      m.registerModule(DefaultScalaModule)
      m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
      m
    }

    CodeMapping.init(getCodeMappingIfNotExist(CODE_MAPPING))
    HccModel.init(getCodeMappingIfNotExist(HCC_MODEL))
    CptModel.init(getCodeMappingIfNotExist(CPT_MODEL))
    IcdModel.init(getCodeMappingIfNotExist(ICD_MODEL))
    BillTypeModel.init(getCodeMappingIfNotExist(BILLTYPE_MODEL))
    CodeMappingV2.init(getCodeMappingIfNotExist(CODE_MAPPING_V2))
    HccModelV2.init(getCodeMappingIfNotExist(HCC_MODEL_V2))
    CptModelV2.init(getCodeMappingIfNotExist(CPT_MODEL_V2))
    IcdModelV2.init(getCodeMappingIfNotExist(ICD_MODEL_V2))
    RiskGroupModel.init(getCodeMappingIfNotExist(RISKGROUPS_MODEL))
    SnomedModel.init(getCodeMappingIfNotExist(SNOMED_MODEL))
  }
}
