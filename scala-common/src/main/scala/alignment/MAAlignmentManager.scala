package com.apixio.scala.utility.alignment

@deprecated("Legacy")
object MAAlignmentManager {
  /**
    * Check if a document can be used as auto-aligned
    * @param metadata document metadata
    * @return true iff metadata contains both dos and provider type
    */
  def isMAAutoAlignment(metadata: DocumentMetadata): Boolean =
    metadata.providerType.nonEmpty && metadata.encounterDateStart.nonEmpty && metadata.encounterDateEnd.nonEmpty

  /**
    * Check if a document an be considered as partial aligned: only dos match
    * @param metadata document metadata
    * @return
    */
  def isMAPartialAutoAlignment(metadata: DocumentMetadata): Boolean =
    metadata.providerType.isEmpty && metadata.encounterDateStart.nonEmpty && metadata.encounterDateEnd.nonEmpty

  /**
    * Utility function to convert a document metadata into alignment item
    * @param metadata document metadata
    * @return An Ma Alignment Item
    */
  def maAutoAlignedItem(metadata: DocumentMetadata): MAAlignmentItem =
    MAAlignmentItem(
      metadata.encounterDateStart.get,
      metadata.encounterDateEnd.get,
      metadata.providerType.orNull
    )
}
