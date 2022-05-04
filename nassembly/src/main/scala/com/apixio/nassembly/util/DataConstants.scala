package com.apixio.nassembly.util

object DataConstants {
  //Separators
  val SEP_UNDERSCORE = "_"
  val SEP_DOT = "."
  val SEP_SEMICOLON = ";"
  val SEP_CARET = "\\^"
  val SEP_CARET_TWICE = "\\^\\^"
  val SEP_DASH = "-"
  val SEP_EQUALS = "="

  val BASE_SUMMARY = "base"
  private val PARSING_DETAILS = "parsingDetails"
  private val PARSING_DATE = "parsingDate"

  private val SOURCES = "sources"
  private val CREATION_DATE = "creationDate"


  val PARSING_DETAILS_DATE: String = s"$BASE_SUMMARY.$PARSING_DETAILS.$PARSING_DATE"
  val SOURCE_CREATION_DATE: String = s"$BASE_SUMMARY.$SOURCES.$CREATION_DATE"

}
