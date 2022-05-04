package com.apixio.nassembly.assemblygraph.model

sealed trait AssemblyExecutionTypes {def name: String}
final case object UploadBatch extends AssemblyExecutionTypes {val name = "UploadBatch"}
final case object TransformExecution extends AssemblyExecutionTypes {val name = "Transform"}
final case object LoaderStream extends AssemblyExecutionTypes {val name = "LoaderStream"}
final case object PartExecution extends AssemblyExecutionTypes {val name = "Part"}
final case object MergeExecution extends AssemblyExecutionTypes {val name = "Merge"}
final case object OcrDocCacheStreamExecution extends AssemblyExecutionTypes { val name = "OcrDocCacheStream"}
final case object OcrStreamExecution extends AssemblyExecutionTypes { val name = "OcrStream"}
final case object KafkaSpyStream extends AssemblyExecutionTypes { val name = "KafkaSpy"}
final case object CombineExecution extends AssemblyExecutionTypes {val name = "Combine"}
final case object CassandraPersistExecution extends AssemblyExecutionTypes {val name = "CassandraPersist"}
final case object BlobPersistExecution extends AssemblyExecutionTypes {val name = "BlobPersist"}
final case object PersistLinkExecution extends AssemblyExecutionTypes {val name = "PersistLink"}
final case object IndexExecution extends AssemblyExecutionTypes {val name = "Index"}
final case object ValidateExecution extends AssemblyExecutionTypes {val name = "Validate"}
final case object TestExecution extends AssemblyExecutionTypes {val name = "Test" }
final case object MigrateExecution extends AssemblyExecutionTypes {val name = "Migrate"}
final case object BadBatchExecution extends AssemblyExecutionTypes {val name = "BadBatch"}
final case object TriggerCleanupExecution extends AssemblyExecutionTypes {val name = "TriggerCleanup"}
final case object MigrateReportExecution extends AssemblyExecutionTypes {val name = "MigrateReport"}
final case object ParserIngestStream extends AssemblyExecutionTypes {val name = "ParserIngestStream"}
final case object AllMergeExecutor extends AssemblyExecutionTypes { val name = "AllMerge"}
final case object AllCombineExecutor extends AssemblyExecutionTypes { val name = "AllCombine"}
final case object DataConcordanceTest extends AssemblyExecutionTypes {val name = "DataConcordanceTest"}
final case object MigrationFix extends AssemblyExecutionTypes {val name = "MigrationFix"}
