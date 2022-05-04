# **A How-To Guide For Using the SDK**
*April 13, 2021*

------------------
## Configuring the Execution Environment

As the SDK was designed to support a generic execution environment--meaning specifically that no
application/`f(x)`-specific code needs to be linked in at compile time--the actual runtime execution
of it needs to be configured.  The set of configuration information needed is as follows:

1. a list of Java canonical classnames for all the accessors that are needed for evaluating the desired
f(x) implementation; the system will do a `Class.forName()` on each classname and then cast it to
`Class<com.apixio.sdk.Accessor>`

2. a list of Java canonical classnames for all the datatype converters needed evaluation; the system
will do a `Class.forName()` on each classname and then cast it to `Class<com.apixio.sdk.Converter>`

3. the single Java classname for the Data Uri Manager Creator; the system will do a `Class.forName()`
on it and cast it to `Class<com.apixio.sdk.UmCreator>`

4. the list of `FxImpl` objects (protobuf class) that represent the `f(x)` implementations that the
execution environment should load and be ready to invoke.  The implementation jar file references
declared in **all** of these objects **must** be to the same file.

5. a list of URLs that contain all the classes referenced above (implicitly referenced in the case of
the `FxImpl` objects)

6. a parsed "arg evaluation" declaration that declares *how* the values for each of the parameters in
`f(x)` are to be constructed for each invocation.

A few notes on the above:

* the execution environment can be initialized only once

* the initialization of the execution environment object returns a list of `FxInvokable` objects; the
client must decide which one to use when processing a computation request

* if the named classes (items 1-3, possibly 4) are available via the JVM classpath then the list of
URLs can be empty--the system just needs to be able to load them dynamically

### Arg Evaluation

The parsed "arg evaluation" in item 6 is a protobuf-based object
(`com.apixio.sdk.protos.EvalProtos.ArgList`) and can be created at runtime or restored from a file.
In either case it's critical to know how to declare it in text form (which is then parsed).  The syntax
is simply a comma-separated list of expressions, where each expression is either a constant or an
accessor call (and whose parameters are also the same csv-list of expressions).  You can think of
an arg evaluation declaration as what's *inside* a function's parentheses, like so:

        pageWindows(singlePartialPatient(request("docuuid"))), request("pdsid")

which really is just the parameter list to the function:

        extract(list<PageWindow>, string)

The types of constants supported are:  integer-type, boolean, floating-point-type, and string.

### Common Arg Evaluation Accessors

1. `request(string)`; e.g., `request("docuuid")`.  This just pulls string values from the request that's
currently being processed/executed.  The execution environment is responsible for supplying these values.

2. `environment(string)`; e.g., `environment("apx.domain")`.  This just pulls string values from the
execution environment that was derived from initialization of the execution environment.  The sample
"apx.domain" could be [production,staging,development] (as one example).

3. `restore(string)`; e.g., `restore(request("signalURI"))`.  This restores previously persisted data
from the feature store (currently ApxDataDao with data being stored in Cassandra).  All of the restored
data will be of the same type and a list of the datatype will be presented as the arg value.  Note that this
"list of datatype" is an intentional limitation currently.

Regarding item 3, there's an interaction with cerebro (or whatever system component is issuing a
request for compuatation) that needs to be described.  The use of `restore` accessor implies a
composition of functions, like `combiner(signalGenerator(pageWindow))` where the output of
`signalGenerator(pageWindow)` is persisted in between the computations of `signalGenerator` and
`combiner`.  For the restore of the output from `signalGenerator()` to work, the data URI passed in
from cerebro as part of the computation request must refer to the data that was persisted from the
computation of `signalGenerator`.  Depending on what the `UmCreator` sets up as the `DataUriManager`
for the execution context, the data URI must either be *remembered* by cerebro (the data URI for a
given f(x) output is, or can be, returned from the execution environment) or *constructed* when issuing
a request for computation of `g()`.  In the latter case, the same parameters/values used for
computing f(x) **must** be used when constructing its data URI.  The proper construction of that is
outside the responsibility of the SDK.


------------------
## New Datatype

A datatype from the SDK perspective is a non-SDK-defined Java class (or set of related classes) that can
be supplied to some `f(x)` implementation or returned from one.  It's important to note that a datatype
that's only ever used as input to an f(x) and is never produced by some f(x) is considered a "principal"
datatype and has fewer constraints with respect to being added (and generally they just require an
Accessor to be created to be useful).

Non-principal datatypes (which could be termed "derived" datatypes) must be persistable, and as the
mechanism for persistence within the SDK is via Google protobufs, this aspect of datatypes dictates a
number of things about how to add datatypes.

That said, the f(x) declaration and implementations do *not* deal directly with persistable objects as
forcing the persistence mechanism into Science code is not practical/desired.  Rather, datatypes are
presented via pure Java interfaces (for JVM-based f(x) implementations), which means that there are
two representations of a single datatype: interface-based and protobuf-based.

With the above in mind, there are a number of steps to add a new datatype:

1. defining the Java interface
2. defining the protobuf schema
3. creating a Converter that translates objects to and from the two representations

Currently all of these artifacts are contained within the "fxdef" module as all of them are definitional
and non-specific to all implementations.

### Defining the Java Interface

The Java interface(s) that defines a datatype should expose a bean-like set of methods that make sense for
the datatype; that is, if an f(x) implementation shouldn't be able to set some property/value then there
shouldn't be a setter for it.

There are no constraints on the property types or use of other (related) interfaces/objects, as long as
the full construct can also be expressed as a protobuf schema (which does mean there are some constraints
on the inclusion and use of third-party-defined data constructs).

By current convention, these Java interfaces are contained in the package `com.apixio.fxifc` although
as the set of datatypes expands and/or includes third-party types, this convention will need to be
revisited.

### Defining the Protobuf schema

In order to support persistence of a datatype a proto3 syntax .proto file must be created that mirrors the
properties declared in the Java interfaces.  The creation of such a file is not covered here as there
are numerous resources/examples on how to do that.

By convention the Java package name of the generated .java files ends with "protos" and the java_outer_classname
declares the general type of the (related) message definitions.  For example:

        package com.apixio.sdkexample.protos;
        option java_outer_classname = "Tests";

### Creating a Converter


### Datatypes and Accessors


------------------
## New f(x) Signature

An `f(x)` signature is a foundational concept of the SDK as it separates the concerns and activities of
the various components of the larger system.  The idea of an `f(x)` is simply that it declares a named
function with its list of parameter types and a return type.  This declaration takes for the canonical
form of a .proto-defined construct `FxDef` (with related schema elements).  The persisted form of some
`f(x)` is then able to be used by the various parts of the SDK.

The easiest way to create a persisted `FxDef` is to use a command line mini-parser that accepts a Java-like
function declaration and creates and persists an `FxDef` from the textual declaration.

The (loose) syntax recognized by the mini-parser is given by the following BNF-like description:

        fxdef :: type id "(" type "," ... ")"
        type  :: "list" "<" fullid ">" |
                 "int" | "long" | ...
        id     :: alpha [alphanum ...]
        fullid :: id [ "." id ...]              # for java classnames

In short, the syntax allows a function declaration using primitive types, references to Java classnames,
and lists of types.  Parameters are declared only with a type--no parameter name.  Some examples:

* `int square(int)`
* `list<int> squares(int, int)`
* `java.util.Map getData(string, boolean)`

The command line tool that accepts the above syntax and creates persisted `FxDef` objects is currently at
`scripts/mkfxdef.sh` and its usage is:

        mkfxdef.sh idl='declaration goes here' out='pathto.fx'

and an example invocation is

        ./scripts/mkfxdef.sh idl="list<com.apixio.ensemble.ifc.Signal> extractSignals(com.apixio.ensemble.ifc.PageWindow)" out=/tmp/extract.fx

The resulting `.fx` file (with `.fx` suffix being just a convention) is then used when declaring and
implementation of that `f(x)` signature (covered elsewhere).

Once an `f(x)` signature has been created it's necessary to manually create the language-specific
artifacts.  Note that the assumption here is that the non-primitive datatypes referenced in an `f(x)`
signature must separately have been properly declared with interfaces, .proto files, and converters.

### Defining the Java interface

The `f(x)` declaration within JVM-based languages is expressed as a pure Java interface (as compared to
an abstract base class) in order to minimize assumptions made on implementation.  The hope is that this
will lead to better isolatibility in general so the execution environments are more able to do whatever
is needed for scaling, etc.  (This is a philosophical decision, for sure, for what that's worth.)

As this Java interface declaration is manually created, care must be taken to make the interface match
exactly the declaration given in the `FxDef` as the SDK uses the function name and the parameter types to
do runtime reflection to locate the actual implementation method (i.e., the `java.lang.reflect.Method`
instance) and any deviation from the `FxDef` declaration will cause this lookup to fail.

The name of the function given in `FxDef` is used to location the **method** name within an implementation
class and it not related to the name of the Java interface.  By convention, the interface name should be
the same (with camel-casing) as the method name (*for now*...).

Within the current directory structure (April 2021), the Java interface should go somewhere under

        fxdef/src/main/java/com/apixio/fxifc

### Defining Base Class

A companion to the above Java interface is an abstract base class that *is for convenience only* (at this
point in time).  The intention here is to provide some reusable implementation-specific functionality.
An SDK-supplied interface `com.apixio.sdk.FxImplementation` is defined as a mechanism to declare to the
SDK runtime code that the implementation needs the `FxEnvironment` and/or one or more assets injected
into it.  It is highly likely that the implementation needs at least the `FxEnvironment` so this base
class will very likely declare that it implements both the `f(x)`-specific interface and the
`FxImplementation` interface.

As with the interface, this Java class by convention (currently) should be under

        fxdef/src/main/java/com/apixio/fxifc

and should have the same name as the interface but with `Base` as a prefix.  For example, if the
Java interface is `ExtractSignals` then the abstract base class should be `BaseExtractSignals`.

------------------
## New Accessor



------------------
## New (or modified) DataURI scheme

There are two supported data URI schemes currently in the system: `apxdata://` and `apxquery://` and
both of them are generalized in that application-level code shouldn't need to add more schemes to the
mix as long as the current use of `ApxDataDao` as the feature store is sufficient.  That said, it's quite
likely that the configuration details of at least `apxquery://` will need to be specified, and the
only way to specify that is by implementing an interface.

As is described elsewhere, a data URI is the inter-component mechanism to identify some collection of
data that was persisted by the SDK.  This identification is closely related to the parameters that
are specified when computation on some `f(x)` implementation is requested.  For example, feature
extraction might be done on the a document (page windows) and with a particular MCID (which contains
the actual extraction code).  The resulting features (signal data) is likely to be persisted using
the label of `[docuuid, mcid]`.  In contrast, something like IRR accpepts a projectID and a date range
and so the label is likely `[projectID, dateRange]`.

The `ApxDataDao` system supports associating an arbitrary set of `name=value` keys with a set of persisted
data, and then later using the `name=value` set when querying to restore the data.  The concern here is
in specifying to the SDK system *what* the set of `name=value` keys are, along with both the name and the
value parts of each key.

This specification is done via what's called a `UmCreator` (for Uri Manager Creator) and it's this
implementation code that application code will likely be doing.

The `apxdata://` URI scheme is handled by the Java class `ApxGroupingDataUriManager` and the
configuration of that requires a single configuration value, and that's the "arg list" specification of
how to construct the "grouping id".  At runtime the arg list is evaluated and the resulting values are
converted to string and separated with the `-` character.  This arg list is generally hardcoded in a
particular implementation of `UmCreator` and the class of the `UmCreator` implementation is specified
when initializing the execution environment.

The `apxquery://` URI scheme is handled by the Java class `ApxQueryDataUriManager` and the configuration
of that requires two complementary pieces of information: the list of key names (e.g., `docuuid`) and
the accessors used to retrieve the value for each of the key names.  Both of these are generally
hardcoded in a particular implementation of `UmCreator` and the class of the `UmCreator` implementation
is specified when initializing the execution environment.

### Notes on the Supported Schemes

The full scheme of `apxdata://` is:

        apxdata://{domain}/gid/{id}

where the `{domain}` element is the URI "host" and is taken to be the environment that the URI is
valid within.  The expected domain values are:  `production`, `staging`, and `development` and that
actual value is currently taken from the execution environment's attribute of `apx.domain`.  The `/gid`
URI path component is for grouping id and the final component of `{id}` is the actual grouping id
that contains the data (this grouping id is used in forming the Cassandra rowkey).

The full scheme of `apxquery://` is:

        apxquery://{domain}/q?n1=v1&n2=v2...

where the `{domain}` URI element is the "host" and is taken to be the environment that the URI is valid
within.  The expected domain values are: `production`, `staging`, and `development` and that actual
value is currently taken from the execution environment's attribute of `apx.domain`.  The `/q` URI path
component stands for "query" and the query parameters are the key names and values, **both** of which are
URL-encoded.

### New URI Schemes

The above notwithstanding, it is likely that a new scheme is eventually required.  The likeliest
impetus for this is when a real feature store is used.  The main design assumption made in the
idea of a data URI is that it's possible to label (identify) a set/vector of features such that
that set of data can be retrieved using that label.

One crucial capability of the overall data URI system is that *something* must keep track of the
actual protoc-generated class that was used to serialize the data to the storage system as that
exact same class must be used when deserializing the data.  The `ApxDataDao` system stores this
"protoClass" as part of the metadata for some group of objects persisted, but if `ApxDataDao` is
not used, then this type information must either be carried in the data URI (perhaps as a `type`
query parameter value) or it must be stored as metadata or an attribute that's accessible via
the base identifier.

