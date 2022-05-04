package com.apixio.scala.apxapi

case class ParamValueElement(param: String, value: List[String])
case class FilterParam(filters: List[ParamValueElement])

class Reporting(connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def version() = {
    get[String](s"/version", auth = false)
  }

  def getCount(projectId: String, postBody: Map[String, Any] = Map.empty) = {
    post[Int](s"/projects/${projectId}/reportableCodes/count",
              data = asJsonBytes(postBody),
              headers = jsonContent)
  }

  /**
   * Fetches total count of reportable ICD codes for a Project
   * @param projectId - UUID if project in which count is desired
   * @param filterParam - Filters applied to reportable ICD counts. Only supports filtering by
   *                    node ids. Defaults to no filtering.
   *                    Usage example:
   *                    val filterParam = FilterParam(List(ParamValueElement("nodes", List("prediction-review"))))
   * @return Count of ICD codes after filtering.
   */
  def getCountV2(projectId: String, filterParam: FilterParam = FilterParam(List())): Int = {
    post[Int](s"/v2/project/$projectId/reportableCodes/count",
      data = asJsonBytes(filterParam),
      headers = jsonContent)
  }

  def getRaf(projectId: String, postBody: Map[String, Any] = Map.empty) = {
    post[Map[String, Any]](s"/projects/${projectId}/reportableCodes/raf",
                           data = asJsonBytes(postBody),
                           headers = jsonContent)
  }
}
