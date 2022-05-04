package com.apixio.nassembly.exchangeutils

import java.util.UUID
import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CareSiteOuterClass.CareSite
import com.apixio.datacatalog.CodedBaseObjects.Encounter
import com.apixio.datacatalog.ParsingDetailsOuterClass.ParsingDetails
import com.apixio.datacatalog.SourceOuterClass.Source

import scala.collection.mutable

// internalId in our system is deterministic, but not in old assembly. So we need to convert just to get an Id. This saves us from converting multiple times
case class ConverterContext(parsingDetailsMap: mutable.Map[UUID, ParsingDetails] = mutable.Map.empty[UUID, ParsingDetails],
                            sourcesMap: mutable.Map[UUID, Source] = mutable.Map.empty[UUID, Source],
                            actorsMap: mutable.Map[UUID, ClinicalActor] = mutable.Map.empty[UUID, ClinicalActor],
                            encountersMap: mutable.Map[UUID, Encounter] = mutable.Map.empty[UUID, Encounter],
                            careSiteMap: mutable.Map[UUID, CareSite] = mutable.Map.empty[UUID, CareSite]) extends Serializable {


  def addParsingDetails(originalId: UUID, pd: ParsingDetails): Unit = {
    if (!parsingDetailsMap.contains(originalId)) parsingDetailsMap.put(originalId, pd)
  }

  def addSource(originalId: UUID, s: Source): Unit = {
    if (!sourcesMap.contains(originalId)) sourcesMap.put(originalId, s)
  }

  def addActor(originalId: UUID, a: ClinicalActor): Unit = {
    if (!actorsMap.contains(originalId)) actorsMap.put(originalId, a)
  }

  def addEncounter(originalId: UUID, e: Encounter): Unit = {
    if (!encountersMap.contains(originalId)) encountersMap.put(originalId, e)
  }

  def addCaresite(originalId: UUID, cs: CareSite): Unit = {
    if (!careSiteMap.contains(originalId)) careSiteMap.put(originalId, cs)
  }

  def getDistinctSources: Iterable[Source] = {
    sourcesMap.values.groupBy(_.getInternalId).values.map(_.head)
  }

  def getDistinctParsingDetails: Iterable[ParsingDetails] = {
    parsingDetailsMap.values.groupBy(_.getInternalId).values.map(_.head)
  }

  def getDistinctClinicalActors: Iterable[ClinicalActor] = {
    actorsMap.values.groupBy(_.getInternalId).values.map(_.head)
  }

  def getDistinctEncounters: Iterable[Encounter] = {
    encountersMap.values.groupBy(_.getInternalId).values.map(_.head)
  }

}

object ConverterContext {
  val empty = new ConverterContext()
}