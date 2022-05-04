# The Apixio Document Object (ADO)

The centralized abstraction for interacting with Document objects.

## Purpose

- In progress: Provide a centralized location for various
  implementations of manipulating Document objects.

- In design: Create a Logical abstraction over Document operations,
  both high-level and low-level.

## Background

Most Document manipulation begins by referencing it in context of the
Apixio Patient Object (APO) to which it belongs, then directly
manipulating the result without aid of existing libraries to assist.

- This results in direct manipulation of the concrete abstractions of
  the document, such as XML, HOCR, Page Text Extract HTML,
  etc. without using the same libraries which created them.

- Repeated work for various operations result in diverging
  implementations, scattered across Apixio's codebase, some handling
  some aspects better than others, and making updates tricky.

## Related documentation

https://docs.google.com/document/d/1h7o7kNWz-aSUsY7-7ZOBdiUX8ZIfzzqKGcIHnikmukg
