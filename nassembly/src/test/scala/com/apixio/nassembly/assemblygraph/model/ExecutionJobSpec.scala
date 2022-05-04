//package com.apixio.nassembly.assemblygraph.model
//
//import com.apixio.model.nassembly.AssemblyContext
//import com.apixio.nassembly.assemblygraph.Graph
//import com.apixio.nassembly.patient.partial.PartialPatientExchange
//import org.scalatest.Ignore
//import org.scalatest.flatspec.AnyFlatSpec
//
//import java.util.UUID
//
//@Ignore // TODO: Graph isn't able to load exchanges. Something wrong with META-INF?
//class ExecutionJobSpec extends AnyFlatSpec {
//
//
//  private val assemblyContextImpl = new AssemblyContext() {
//    override def pdsId: String = "1"
//    override def batchId: String = "444"
//    override def runId: String = UUID.randomUUID.toString
//    override def codeVersion = "LIG"
//    override def timestamp: Long = System.nanoTime
//  }
//
//  val graph = new Graph
//
//  "Execution Job" should "create a list of all downstream nodes" in {
//    val apoJob: ExecutionJob = ExecutionJob(TransformExecution, graph.getGraphNode(PartialPatientExchange.dataTypeName), assemblyContextImpl)
//
//    val allJobs: Seq[ExecutionJob] = ExecutionJob.getAllNecessaryJobs(apoJob, graph)
//    val dedupedJobs: Seq[ExecutionJob] = ExecutionJob.deduplicateJobs(allJobs)
//
//    assert(allJobs.nonEmpty)
//    assert(dedupedJobs.nonEmpty)
//    assert(dedupedJobs.size < allJobs.size)
//
//    assert(dedupedJobs.find(_.executionType == PersistLinkExecution).size == 1)
//    assert(dedupedJobs.find(_.executionType == CassandraPersistExecution).size == 1)
//    assert(dedupedJobs.find(_.executionType == BlobPersistExecution).size == 1)
//    assert(dedupedJobs.find(_.executionType == IndexExecution).size == 1)
//  }
//
//}
