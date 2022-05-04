package com.apixio.nassembly.patient.partial

import com.apixio.datacatalog.PatientProto.{Patient => PatientProto}
import com.apixio.model.file.ApxPackagedStream
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.nassembly.{AssemblyContext, Exchange}
import com.apixio.model.patient.Patient
import com.apixio.nassembly.allergy.AllergyExchange
import com.apixio.nassembly.apo.{APOGenerator, ApoParserManager}
import com.apixio.nassembly.biometricvalue.BiometricValueExchange
import com.apixio.nassembly.clinicalactor.ClinicalActorExchange
import com.apixio.nassembly.contactdetails.ContactDetailsExchange
import com.apixio.nassembly.demographics.DemographicsExchange
import com.apixio.nassembly.documentmeta.DocumentMetaExchange
import com.apixio.nassembly.encounter.EncounterExchange
import com.apixio.nassembly.exchangeutils.{ApoToProtoConverter, EidUtils}
import com.apixio.nassembly.extractedtext.{ExtractedTextExchangeImpl, HocrExchangeImpl, StringContentExchangeImpl}
import com.apixio.nassembly.familyhistory.FamilyHistoryExchange
import com.apixio.nassembly.ffsclaims.FfsClaimExchange
import com.apixio.nassembly.immunization.ImmunizationExchange
import com.apixio.nassembly.labresult.LabResultExchange
import com.apixio.nassembly.legacycoverage.LegacyCoverageExchange
import com.apixio.nassembly.mao004.Mao004Exchange
import com.apixio.nassembly.patient.meta.PatientMetaExchange
import com.apixio.nassembly.patient.partial.PartialPatientExchange.clearExtractedText
import com.apixio.nassembly.patient.{PatientUtils, SeparatorUtils}
import com.apixio.nassembly.patientactor.PatientActorExchange
import com.apixio.nassembly.prescription.PrescriptionExchange
import com.apixio.nassembly.problem.ProblemExchange
import com.apixio.nassembly.procedure.ProcedureExchange
import com.apixio.nassembly.raclaim.RaClaimExchange
import com.apixio.nassembly.socialhistory.SocialHistoryExchange
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import java.util
import java.util.UUID
import scala.collection.JavaConverters._

class PartialPatientExchange extends Exchange {

  private var proto: PatientProto = _

  def getPatient: PatientProto = proto

  def setPatient(patient: PatientProto): Unit = {
    proto = patient
  }

  override def setIds(cid: UUID, primaryEid: Array[Byte]): Unit = {
    if (proto != null) {
      val newPatientMeta = EidUtils.updatePatientMetaIds(proto.getBase.getPatientMeta, cid, primaryEid)

      proto = proto
        .toBuilder
        .setBase(proto.getBase.toBuilder.setPatientMeta(newPatientMeta))
        .build()
    }
  }

  override def getDataTypeName: String = {
    PartialPatientExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientProto.getDescriptor
  }

  override def getCid: String = {
    getPatientMeta.getPatientId.getUuid
  }

  private def getPatientMeta = {
    getBase.getPatientMeta
  }

  private def getBase = {
    proto.getBase
  }

  override def getPrimaryEid: String = {
    EidUtils.getPatientKey(getPatientMeta)
  }

  override def getExternalIds: Array[Array[Byte]] = {
    getBase.getPatientMeta.getExternalIdsList.asScala.toArray
      .map(_.toByteArray)
  }

  override def getEidExchange: Exchange = {
    val exchange = new PatientMetaExchange
    exchange.setProto(getPatientMeta)

    exchange
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    if (iterator.hasNext) {
      proto = PatientProto.parseFrom(iterator.next())
    }
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    if (iterator.hasNext) {
      proto = PatientProto.parseFrom(iterator.next())
    }
  }

  // Same as existing CV2
  override def parse(inputFile: ApxPackagedStream, sourceFileName: String, customerProperties: Any, ac: AssemblyContext): Unit = {
    val fileTypesText = "HTML,TXT,RTF,DOC,DOCX,ODT,AXM,APO,CCD,FHIR".split(",").toList.asJava // TODO pass through context
    val apo = ApoParserManager.parsePackagedStream(inputFile, sourceFileName, customerProperties,fileTypesText, ac)
    parse(apo, ac)
  }

  override def parse(apo: Patient, ac: AssemblyContext): Unit = {
    val converter = new ApoToProtoConverter()
    proto = converter.convertAPO(apo, ac.pdsId())
  }

  override def getParts(ac: AssemblyContext): util.Map[String, java.lang.Iterable[Exchange]] = {
    Option(proto).map(_ => {

      // Don't all belong to the same Cid. Create separate exchanges
      val clinicalActorExchanges = SeparatorUtils.separateActors(proto)
        .map(actor => {
          val exchange = new ClinicalActorExchange
          exchange.setActors(Iterable(actor))
          exchange.asInstanceOf[Exchange]
        })

      val patientClinicalActorExchanges = {
        SeparatorUtils.separatePatientClinicalActors(proto) match {
          case Nil => Iterable.empty[Exchange]
          case actors =>
            val exchange = new PatientActorExchange
            exchange.setProtos(actors)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val procedureExchanges = SeparatorUtils.separateProcedures(proto).map(procedures => {
        val exchange = new ProcedureExchange
        exchange.setProtos(procedures)
        exchange.asInstanceOf[Exchange]
      })

      val ffsExchanges = SeparatorUtils.separateFfsClaims(proto).map(ffs => {
        val exchange = new FfsClaimExchange
        exchange.setProtos(ffs)
        exchange.asInstanceOf[Exchange]
      })

      val problemExchanges = SeparatorUtils.separateProblems(proto).map(p => {
        val exchange = new ProblemExchange
        exchange.setProtos(Iterable(p))
        exchange.asInstanceOf[Exchange]
      })

      val raClaimExchanges = SeparatorUtils.separateRaClaims(proto).map(c => {
          val exchange = new RaClaimExchange
          exchange.setProtos(Iterable(c))
          exchange.asInstanceOf[Exchange]
      })

      val mao004Exchanges = SeparatorUtils.separateMao004s(proto).map(m => {
        val exchange = new Mao004Exchange
        exchange.setProtos(Iterable(m))
        exchange.asInstanceOf[Exchange]
      })

      val documentExchanges = {
        SeparatorUtils.separateDocuments(proto) match {
          case Nil => Iterable.empty[Exchange]
          case docs =>
            val exchange = new DocumentMetaExchange
            exchange.setDocuments(docs)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      // 1 exchange per document
      val stringContentExchanges = SeparatorUtils.separateStringContent(proto).map(content => {
        val exchange = new StringContentExchangeImpl
        exchange.setContent(content)
        exchange.asInstanceOf[Exchange]
      })

      val extractedTextExchanges = SeparatorUtils.separateExtractedText(proto).map(content => {
        val exchange = new ExtractedTextExchangeImpl
        exchange.setContent(content)
        exchange.asInstanceOf[Exchange]
      })

      val hocrTextExchanges = SeparatorUtils.separateHocrText(proto).map(content => {
        val exchange = new HocrExchangeImpl
        exchange.setContent(content)
        exchange.asInstanceOf[Exchange]
      })

      val encounterExchanges = {
        // All belong to the same cid
        SeparatorUtils.separateEncounters(proto) match {
          case Nil => Iterable.empty[Exchange]
          case encounters =>
            val exchange = new EncounterExchange
            exchange.setProtos(encounters)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val allergyExchanges = {
        // All belong to the same cid
        SeparatorUtils.separateAllergies(proto) match {
          case Nil => Iterable.empty[Exchange]
          case allergies =>
            val exchange = new AllergyExchange
            exchange.setProtos(allergies)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val coverageExchanges = {
        SeparatorUtils.separateCoverage(proto) match {
          case Nil => Iterable.empty[Exchange]
          case coverages =>
            val exchange = new LegacyCoverageExchange
            exchange.setProtos(coverages)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val contactDetailsExchanges = {
        // All belong to same cid
        SeparatorUtils.separateContactDetails(proto) match {
          case Nil => Iterable.empty[Exchange]
          case contactDetails =>
            val exchange = new ContactDetailsExchange
            exchange.setProtos(contactDetails)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val biometricExchanges = {
        // All belong to same cid
        SeparatorUtils.separateBiometrics(proto) match {
          case Nil => Iterable.empty[Exchange]
          case bioValues =>
            val exchange = new BiometricValueExchange
            exchange.setProtos(bioValues)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val socialHistoryExchanges = {
        // All belong to same cid
        SeparatorUtils.separateSocialHistories(proto) match {
          case Nil => Iterable.empty[Exchange]
          case socialHistories =>
            val exchange = new SocialHistoryExchange
            exchange.setProtos(socialHistories)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val demographicsExchanges = {
        // All belong to same cid
        SeparatorUtils.separateDemographics(proto) match {
          case Nil => Iterable.empty[Exchange]
          case demographics =>
            val exchange = new DemographicsExchange
            exchange.setProtos(demographics)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val labResultExchanges = {
        // All belong to same cid
        SeparatorUtils.separateLabResults(proto) match {
          case Nil => Iterable.empty[Exchange]
          case labResults =>
            val exchange = new LabResultExchange
            exchange.setProtos(labResults)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val prescriptionExchanges = {
        // All belong to same cid
        SeparatorUtils.separatePrescriptions(proto) match {
          case Nil => Iterable.empty[Exchange]
          case prescriptions =>
            val exchange = new PrescriptionExchange
            exchange.setProtos(prescriptions)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val familyHistoryExchanges = {
        // All belong to same cid
        SeparatorUtils.separateFamilyHistories(proto) match {
          case Nil => Iterable.empty[Exchange]
          case fHSummaries =>
            val exchange = new FamilyHistoryExchange
            exchange.setProtos(fHSummaries)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      val immunizationExchanges = {
        // All belong to same cid
        SeparatorUtils.separateImmunization(proto) match {
          case Nil => Iterable.empty[Exchange]
          case summaries =>
            val exchange = new ImmunizationExchange
            exchange.setProtos(summaries)
            Iterable(exchange.asInstanceOf[Exchange])
        }
      }

      Map(ClinicalActorExchange.dataTypeName -> clinicalActorExchanges.asJava,
        PatientActorExchange.dataTypeName -> patientClinicalActorExchanges.asJava,
        ProcedureExchange.dataTypeName -> procedureExchanges.asJava,
        FfsClaimExchange.dataTypeName -> ffsExchanges.asJava,
        ProblemExchange.dataTypeName -> problemExchanges.asJava,
        RaClaimExchange.dataTypeName -> raClaimExchanges.asJava,
        Mao004Exchange.dataTypeName -> mao004Exchanges.asJava,
        DocumentMetaExchange.dataTypeName -> documentExchanges.asJava,
        StringContentExchangeImpl.dataTypeName -> stringContentExchanges.asJava,
        ExtractedTextExchangeImpl.dataTypeName -> extractedTextExchanges.asJava,
        HocrExchangeImpl.dataTypeName -> hocrTextExchanges.asJava,
        EncounterExchange.dataTypeName -> encounterExchanges.asJava,
        AllergyExchange.dataTypeName -> allergyExchanges.asJava,
        LegacyCoverageExchange.dataTypeName -> coverageExchanges.asJava,
        ContactDetailsExchange.dataTypeName -> contactDetailsExchanges.asJava,
        BiometricValueExchange.dataTypeName -> biometricExchanges.asJava,
        SocialHistoryExchange.dataTypeName -> socialHistoryExchanges.asJava,
        DemographicsExchange.dataTypeName -> demographicsExchanges.asJava,
        LabResultExchange.dataTypeName -> labResultExchanges.asJava,
        PrescriptionExchange.dataTypeName -> prescriptionExchanges.asJava,
        FamilyHistoryExchange.dataTypeName -> familyHistoryExchanges.asJava,
        ImmunizationExchange.dataTypeName -> immunizationExchanges.asJava
      ).asJava
    }).getOrElse(Map.empty[String, java.lang.Iterable[Exchange]].asJava)
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    val byteData = proto.toByteArray
    val parsingDetails = proto.getBase.getParsingDetailsList
    val oid = proto.getBase.getDataCatalogMeta.getOid
    val oidKey = SeparatorUtils.separateDocuments(proto).headOption match {
      case Some(doc) => PatientUtils.getDocumentKey(doc)
      case None => PatientUtils.getOidKeyFromParsingDetailsList(parsingDetails)
    }

    val jsonData = JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(clearExtractedText(proto))
    Iterable(new ProtoEnvelop(oid, oidKey, byteData, jsonData)).asJava
  }

  override def toApo: java.lang.Iterable[Patient] = {
    Iterable(APOGenerator.fromProto(proto)).asJava
  }
}

object PartialPatientExchange {
  val dataTypeName = "apo"

  // Used to remove the extracted text from json
  def clearExtractedText(proto: PatientProto): PatientProto = {
    proto
      .toBuilder
      .clearExtractedText()
      .build()
  }
}
