# Apixio's kitchen sink of Scala related shims: `scala-common`

Born of need to make use of Dropwizard from the Scala programming
language, `scala-common` has grown beyond that and also provides:

- Dropwizard Scala support (via a 3rd party repo. Use `ScalaBundle`!)

- `apxapi` -- a wrapper over REST APIs for many of our other
  microservices.
  -  Including that `Mapping` implementation that can cause some
  startup delay as DB tables are pulled into memory.

- Logging. This is where `withAPILogging`, and its lesser known
  cousin, `withLogging` reside.

And more!
