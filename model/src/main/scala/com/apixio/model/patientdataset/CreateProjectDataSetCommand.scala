package com.apixio.model.patientdataset

case class CreateProjectDataSetCommand(projectDataSetName: String,
                                       customerUuid: String,
                                       projectUuid: String,
                                       pdsId: String,
                                       dataType: String,
                                       predictionEngine: String,
                                       predictionEngineVariant: String,
                                       criteria: List[String],
                                       criteriaType: String,
                                       documentTitles: List[String],
                                       documentPatientUuids: Map[String, String],
                                       eligibility: Eligibility,
                                       eligibilityDates: String,
                                       documentDateOverride: String,
                                       creator: String,
                                       pdsType: String,
                                       patientList: List[String],
                                       models: List[EngineVariantMetaData]
                                      )
