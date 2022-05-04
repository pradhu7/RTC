package com.apixio.util.nassembly

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CareSiteOuterClass.CareSite
import com.apixio.datacatalog.CodedBaseObjects.Encounter
import com.apixio.datacatalog.ParsingDetailsOuterClass.ParsingDetails
import com.apixio.datacatalog.SourceOuterClass.Source
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, EncounterSummary}

/**
 * This util is used to properly set the internalUUID of our nassembly proto objects
 * We use an identity function and convert that to UUID so we have a determinstic uuid used for merging
 */
object InternalUUIDUtils {

  def setInternalId(source: Source): Source = {
    setInternalId(source.toBuilder)
  }

  def setInternalId(builder: Source.Builder): Source = {
    val id = IdentityFunctions.getIdentity(builder.build())
    builder.setInternalId(IdentityFunctions.identityToUUID(id))
      .build()
  }

  def setInternalId(actor: ClinicalActor): ClinicalActor = {
    setInternalId(actor.toBuilder)
  }

  def setInternalId(builder: ClinicalActor.Builder): ClinicalActor = {
    val id = IdentityFunctions.getIdentity(builder.getClinicalActorInfo)
    builder.setInternalId(IdentityFunctions.identityToUUID(id))
      .build()
  }

  def setInternalId(actor: ClinicalActorSummary): ClinicalActorSummary = {
    val id = IdentityFunctions.getIdentity(actor.getClinicalActorInfo)
    actor.toBuilder
      .setInternalId(IdentityFunctions.identityToUUID(id))
      .build
  }

  def setInternalId(parsingDetails: ParsingDetails): ParsingDetails = {
    setInternalId(parsingDetails.toBuilder)
  }

  def setInternalId(builder: ParsingDetails.Builder): ParsingDetails = {
    val id = IdentityFunctions.getIdentity(builder.build())
    builder.setInternalId(IdentityFunctions.identityToUUID(id))
      .build()
  }

  def setInternalId(encounter: Encounter): Encounter = {
    setInternalId(encounter.toBuilder)
  }

  def setInternalId(builder: Encounter.Builder): Encounter = {
    val id = IdentityFunctions.getIdentity(builder.getEncounterInfo)
    builder
      .setInternalId(IdentityFunctions.identityToUUID(id))
      .build
  }

  def setInternalId(encounter: EncounterSummary): EncounterSummary = {
    setInternalId(encounter.toBuilder)
  }

  def setInternalId(builder: EncounterSummary.Builder): EncounterSummary = {
    val id = IdentityFunctions.getIdentity(builder.build())
    builder.setInternalId(IdentityFunctions.identityToUUID(id))
      .build()
  }

  def setInternalId(careSite: CareSite): CareSite = {
    setInternalId(careSite.toBuilder)
  }

  def setInternalId(builder: CareSite.Builder): CareSite = {
    val id = IdentityFunctions.getIdentity(builder.build())
    builder.setInternalId(IdentityFunctions.identityToUUID(id))
      .build()
  }

}
