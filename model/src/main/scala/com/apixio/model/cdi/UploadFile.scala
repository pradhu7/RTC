package com.apixio.model.cdi

import org.joda.time.LocalDate

case class UploadFile(patientId: String,
                      originalId: String,
                      documentTitle: String,
                      date: LocalDate,
                      encounterId: String,
                      providers: String,
                      fileUrl: String,
                      content: String,
                      mimeType: String,
                      code: String,
                      metaData: String,
                      editType: String)
