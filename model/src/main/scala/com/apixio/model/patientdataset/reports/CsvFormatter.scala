package com.apixio.model.patientdataset.reports

class CsvFormatter {
  def progressReportToCsv(report: ProgressReport): List[ProgressReportCsvRow] = {
    val csvReport =
      report.documentProgresses.map { doc =>
        ProgressReportCsvRow(report.projectDataSetUuid, report.projectDataSetName, doc.documentUuid, doc.patientUuid,
          doc.documentSignalsReady, doc.patientSignalsReady, doc.allSignalsReady, doc.predictionsReady, doc.predictionsCount, doc.predictionsSentAt)
      }
    csvReport
  }

  def signalCompletenessReportToCsv(report: SignalCompletenessReport): List[SignalCompletenessReportCsvRow] = {
    val csvReport =
      report.documentCompleteness.map { doc =>
        // First, flatten the signal presence

        // Then construct the row
        SignalCompletenessReportCsvRow(report.projectDataSetUuid, report.projectDataSetName, doc.documentUuid,
          doc.patientUuid, doc.hasDemographic, doc.hasEligibility, doc.allSignalsReady, doc.signalPresence)
      }
      csvReport
  }
}
