package com.apixio.model.cdi

import java.util.UUID

import org.joda.time.DateTime

case class UploadedFileOutput(uploadbatchName: String,
                              uploadedAt: DateTime,
                              patientExtId: String,
                              apxPackageUuid: UUID,
                              patientUuid: UUID,
                              originalFileId: String,
                              fileUrl: String,
                              documentTitle: String,
                              apxPackageType: ApxPackageType,
                              lastStatusCode: Int,
                              lastStatusMessage: String)
