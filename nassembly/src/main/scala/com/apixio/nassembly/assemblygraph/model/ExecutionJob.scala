package com.apixio.nassembly.assemblygraph.model

import com.apixio.model.nassembly.AssemblyContext
import com.apixio.nassembly.assemblygraph.model.ExecutionJob.{allDataType, dataTypeSpecificJobs}
import com.apixio.nassembly.assemblygraph.{Graph, GraphNode}

import scala.collection.JavaConversions._
import scala.collection.mutable

case class ExecutionJob(executionType: AssemblyExecutionTypes, graphNode: GraphNode, ac: AssemblyContext) {
  def dataType: String = if (dataTypeSpecificJobs.contains(executionType)) {
    graphNode.getDataTypeName
  } else {
    allDataType
  }
}

object ExecutionJob {

  val allDataType = "All_PDS"
  lazy val dataTypeSpecificJobs = Seq(LoaderStream, TransformExecution, PartExecution, MergeExecution, CombineExecution)
  lazy val pdsLevelJobs = Seq(PersistLinkExecution, BlobPersistExecution, CassandraPersistExecution, IndexExecution)

  def getAllNecessaryJobs(job: ExecutionJob, graph: Graph): Seq[ExecutionJob] = {
    val graphNode = job.graphNode

    def getMergeSinkNodes(partJob: ExecutionJob): Seq[ExecutionJob] = {
      if (partJob.graphNode.hasGarbageCollector) {
        getAllNecessaryJobs(partJob.copy(executionType = MergeExecution), graph)
      } else {
        Seq.empty[ExecutionJob]
      }
    }
    val ac = job.ac
    job.executionType match {
      case MergeExecution | CombineExecution =>
        val jobs: mutable.ListBuffer[ExecutionJob] = mutable.ListBuffer.empty[ExecutionJob]

        if (job.graphNode.hasCPersistable) jobs.append(job.copy(executionType = CassandraPersistExecution))
        if (graphNode.hasBlobPersistable) jobs.append(job.copy(executionType = BlobPersistExecution))
        if (graphNode.hasIndexer) jobs.append(job.copy(executionType = IndexExecution))

        val combineJobs = graphNode.getCombineSinkNodes.toSeq.map(sinkNode =>
          ExecutionJob(CombineExecution, sinkNode, ac))

        val combineRecursion = combineJobs.flatMap(combineJob => getAllNecessaryJobs(combineJob, graph))

        Seq(job) ++ jobs.toList ++ combineJobs ++ combineRecursion

      case TransformExecution =>
        Seq(job) ++ getAllNecessaryJobs(job.copy(executionType = PartExecution), graph)

      case PartExecution | LoaderStream =>
        // Merge dataType
        val mergeRecursion = getMergeSinkNodes(job)
        // Merge parts
        val extractedPartsMergeRecursion = graphNode.getExchange.getParts(ac).keys.toSeq.flatMap(dt => {
          val graphNode = graph.getGraphNode(dt)
          getMergeSinkNodes(job.copy(graphNode = graphNode))
        })

        val persistLinkRecursion = getAllNecessaryJobs(job.copy(executionType = PersistLinkExecution), graph)

        Seq(job) ++ persistLinkRecursion ++ mergeRecursion ++ extractedPartsMergeRecursion


      case BlobPersistExecution | CassandraPersistExecution | IndexExecution | PersistLinkExecution =>
        // They are all sink nodes
        Seq(job)
      case _ =>
        Seq(job)
    }
  }

  /**
   * Reverse the list, deduplicate, then reverse again
   * This way the deduplicated sink nodes are at the end
   * @param jobs List of Execution Jobs
   */
  def deduplicateJobs(jobs: Seq[ExecutionJob]): Seq[ExecutionJob] = {
    val set = mutable.ListBuffer.empty[ExecutionJob]

    // Reverse the order so we can do sink nodes only once
    jobs.reverse.foreach(job => {
      val isDuplicate = set.exists(existing => {
        val sameExecution = existing.executionType == job.executionType
        lazy val sameDataType = existing.dataType == job.dataType
        lazy val samePds = existing.ac.pdsId() == job.ac.pdsId()
        sameExecution && sameDataType && samePds
      })
      if (!isDuplicate) {
        set.append(job)
      }
    })
    set.toList.reverse
  }
}
