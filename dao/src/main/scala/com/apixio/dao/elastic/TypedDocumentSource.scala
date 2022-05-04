package com.apixio.dao.elastic

import com.sksamuel.elastic4s.source.DocumentSource

/**
 * This is an extended document source which encodes the document type.  This allows
 * us to set the type easily within an indexThis() call.
 * @author rbelcinski@apixio.com
 */
trait TypedDocumentSource extends DocumentSource {
  /**
   * This is a value to use for an indexing key for the wrapped document.
   * @return A key to use, or None to simply use the elastic search default index value.
   */
  def key:Option[String] = None
  /**
   * The type of the document.
   * @return The type of the document.
   */
  def documentType:String
}
