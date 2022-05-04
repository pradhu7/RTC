package com.apixio.dao.elastic

/**
 * This is a returned source item from a query.
 * @param index The index where the item was found.
 * @param id The id of the item.
 * @param score The match score for the item.
 * @param entity The actual entity.
 * @author rbelcinski@apixio.com
 */
case class Result(index:String, id:String, score:Float, entity:TypedDocumentSource)
