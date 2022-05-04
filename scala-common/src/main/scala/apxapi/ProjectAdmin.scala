package com.apixio.scala.apxapi

class ProjectAdmin(connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def version() = {
    get[String](s"/version",
      auth = false)
  }

  def createHccProject(projectParams: ProjectParams): Map[String, String] = {
    val data = asJsonBytes(projectParams)
    post[Map[String,String]]("/projadmin", headers = jsonContent, data = data)
  }

  def getHccProject(projectId: String): Map[String, String] = {
    get[Map[String, String]](s"/projadmin/$projectId")
  }

  def updateHccProject(projectId: String, projectParams: ProjectParams): Map[String, String] = {
    val data = asJsonBytes(projectParams)
    put[Map[String,String]](s"/projadmin/$projectId", headers = jsonContent, data = data)
  }
}

case class ProjectParams(organizationID: String, // UUID (not XUUID)
                         patientDataSetID: String, // UUID (not XUUID)
                         name: String,
                         projectType: String,
                         completionDate: String, // YYYY-mm-dd
                         codingType: String,
                         isDocGap: Boolean,
                         noteType: String,
                         market: String,
                         submissionYear: Int, // YYYY
                         submissionMonth: String,
                         billingUnit: String,
                         enforceCoverage: Boolean,
                         claimsType: String,
                         renewDocument: String,
                         servingMode: String,
                         style: String,
                         rejectReasons: List[String],
                         budget: Double = 0.0,
                         expectedCharts: Int = 0,
                         analyzedCharts: Int = 0,
                         batches: String = "",
                         tags: String = "",
                         projectId: Option[String] = None
                        )