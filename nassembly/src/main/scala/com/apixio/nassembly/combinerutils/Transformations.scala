package com.apixio.nassembly.combinerutils

sealed trait Transformations {def name: String}
final case object YMDYearQuarter extends Transformations {val name = "YMDYearQuarter"}
final case object YMDYearMonthTransformation extends Transformations {val name = "YMDYearMonth"}
final case object FillInNull extends Transformations {val name = "FillInNull"}
final case object ExtractNestedColumn extends Transformations {val name = "ExtractNestedColumn"}
final case object RenameTransformation extends Transformations {val name = "Rename"}
final case object SelectTransformation extends Transformations {val name = "Select"}
final case object DomainLinkTransformation extends Transformations {val name = "DomainLink"}
final case object DatatypeLinkTransformation extends Transformations {val name = "DataTypeLink"}
final case object FilterTransformation extends Transformations {val name = "Filter"}
final case object StringContentTransformation extends Transformations {val name = "StringContent"}
final case object MarkAsDeleteTransformation extends Transformations{val name = "markAsDelete"}
final case object ClearDataTransformation extends Transformations{val name = "clearData"}