# **Theory of Operation of the SDK Subsystem**
*April 12, 2021*

## f(x)

`f(x)` is just a named function signature (like the mathematical `sin` function could be
described via `float sin(float)`) that's used both to group like-minded actual functions
(e.g., signal generators) and to dynamically load/invoke implementations of such functions.
If two function signatures are identical but semantically different (perhaps unlikely), then
the names of the functions should be different so as not to cause confusion.  Function
signatures allow for a single return value and zero or more typed parameters.  Each parameter
can be either a primitive (from a limited set of them) or a (Java) classname or a list of
something (including list of lists).

Parameters declared to be a list of some type are handled specially in that if a function is
declared to take a parameter of type `T`, and the type of the actual argument we have at the
point of dynamic invocation is actually `List<T>` then the evaluation system automatically
iterates of the elements of the list and calls `f(x)` once for each item in the list.


----------------
## f(x) implementations

Implementations of `f(x)` (i.e., an actual function that conforms to a given function
signature) are much more complicated as they have to declare lots of things critical to the
correct runtime operation that can't or shouldn't be specified as part of the abstract `f(x)`.
This is covered in more detail later in this document.


----------------
## Function Execution Environment

A function execution environment is (ideally) a system component that really doesn't care
about the details of any given `f(x)` but is a container for all the pieces needed to "run"
some implementation(s) of `f(x)`.  Because of this generic capability, it needs to be
configured to run/support some `f(x)impl` execution.  One crucial configuration bit is the
declaration of how to form the arguments for all the declared parameters of the `f(x)` that the
function execution environment is supposed to run.  Note that this declaration is *not* a
function (although it can look like just a function call) but a specification of how to
construct each and every argument for `f(x)`.  This declaration looks like an argument list of
a function call; for example, if the `f(x)` signature is

        list<com.apixio.Signal> extract(list<com.apixio.PageWindow>, string)

then this configuration would have to declare how to construct both parameters, one for the
`PageWindow` type parameter and one for the string type parameter (whose purpose can't be
gleaned from its simple type declaration).  An example could be

        pageWindows(singlePartialPatient(Request("docuuid"))), Request("pdsid")

(note that there are two separate declarations, separated by a ",").

The declaration of argument construction is done using **accessors**, where the general concept
of accessor is that it's a named function that can supply data (somehow); as a function, it
can have multiple parameters, and the construction/evaluation of these parameters at runtime
can be specified (recursively) by other accessors or by constant values, both of which are
demonstrated in the above example (`pageWindows(singlePartialPatient(...))`).

What's not explicitly documented by accessors is their type information--both parameter and
output.  In the above example, it's not clear what's returned by `pageWindows()` accessor--is
it, for example, a `PageWindow` or a list of `PageWindows`--and that has implications on runtime
behavior.  If declarations for the function execution environment aren't consistent then
runtime invocation errors will occur (right now--although it's certainly reasonable to
augment accessor declarations with type information that's available to humans).


----------------
## Data URIs

In a fundamental sense, a function execution environment is an intermediary between a bunch of
`f(x)impls` and application-land; it is tasked with producing (and persisting) the `f(x)` data
(as requested by another intermediary) that can be directly used by applications.  A key concept
relevant here is that of a data URI.  A data URI is a URI with an Apixio-defined URI scheme that
contains the necessary information for the system to be able to locate and restore some data.
It is meant to be usable by system components outside the SDK itself.  There are different
schemes supported, some of which can be constructed by client code by using "query keys" (which
are similar to the expressions in a SQL `where` clause).

The details of a specific data URI are opaque to the lowest levels of the SDK subsystem as
the details of its construction are defined by the needs of applications (and composed
functions).  Because it sits in the middle of various system components these components must
agree on the details of the data URI construction.  Generally speaking a data URI will
consist of parameter values associated with the selection of data that's supplied to some
`f(x)impl` within some configured and executing function execution environment.

That said, given that an explicit goal of a generic function execution environment is to
avoid compiling in non-generic code (such as details of a data URI), there must be a way
of generically configuring execution environments to handle data URIs.  Current data URI
and persistence implementations record the low level protoc-generated Class that's required
to restore the (protobuf) data, so the data URI doesn't need to hold this information, but
future data URIs might need to keep track of persistence-level details such as type.

The data URI system has at least two levels: the generic data URI code that handles the various
Apixio-defined schemes, and the more application-specific code that resides outside the SDK
proper and configures the generic data URI code for its needs.  For example, the generic data
URI code for the `apxquery` scheme (i.e., the URI starts with `apxquery://`) is in
`com.apixio.sdk.builtin.ApxQueryDataUriManager` and it allows a client to declare both the
fields to be used in the query (e.g., a docUUID and a generatorID) as well as the mechanism to
get those values from a request for processing).  The application-specific (mostly) code that
actually configures an instance of ApxQueryDataUriManager is in--as an example--the fximplcode
module in com.apixio.umcs.PersonDbUmCreator and that code sets up both the field list and the
"arg declaration" string that's used to actually populate the fields for a specific request.

In order to support the above, the SDK provides what's termed a `UmCreator` interface that
has the single purpose of creating an instance of DataUriManager.

----------------
## Persistence

Persistence of data returned from some `f(x)` is required, both to provide as input to a
composed `g(f(x))` and to be available downstream at the application level.  Note that managing
the composition of functions is done outside the SDK system, but the SDK system must support
composition by (minimally) providing a persistence model.

Note that the SDK model provides that the constructs used when declaring `f(x)` at the
programming language level (e.g., Java interface declarations) are different from those used
to manage persistence of data (e.g., protobuf-generated Java classes).  That decision has
tradeoffs and implications on the architecture/design of the SDK in this area of persistence.

Persistence at the (de)serialization level is handled by protobuf-generated code and the
lowest level of persistence just deals with these serialized byte arrays of data.

The SDK model is concerned with converting between the interface-based `f(x)` parameter types
and the serialization-capable protobuf-based classes.  This conversion is handled by
**converter** classes that are, for now, hand-coded (which conveniently allows support for
existing Signal data with existing classes).

There are currently two very similar "storage engines" that actually store data returned from
some `f(x)`: `SignalLogic` and `ApxDataDao` (which is basically a generalized version of
`SignalLogic`).  Both of these support storing large lists of protobuf objects (of the same
type), where these lists of objects are serialized and converted to byte arrays, chunked to
"reasonable" sizes (10MB) and then zipped and encrypted (as required).  These chunks of
persisted data are then identified by a data URI that is defined by the client of the
persistence code.  Since this selection of data is done (mostly) via accessors (e.g., using
`Request("docuuid")` implies that the scope of data produced is restricted to a document,
possibly the patient of that document), there must be consistency & coordination between the
function execution environment's accessor setup and the creation of the data URIs used.

The ApxDataDao subsystem currently is mostly genericized to the point where the SDK system
can sit on top of it and just use its features/capabilities.  It can do this by creating
instances of `DataType` objects (defined by ApxDataDao) using configuration information in the
function execution environment.  (If it's required to also support the SignalLogic storage
engine, then the SDK design ought to provide a generalized/abstracted "storage engine"
construction.)

One feature of the ApxDataDao storage engine is the ability to write portions of data within
a single low level group in a way that doesn't conflict (write-time) with other threads/code
writing to the same grouping at the same time.  The way this is handled is that each
concurrent writer must use a unique "partition id".  Note that the reading of data for a
grouping will fetch all data written across all partition ids.  This partition id uniqueness
must be managed by the execution environment (e.g., by using a thread or worker id).

----------------
## In the Middle of it All

As mentioned above, persistence is required for both composition of functions and for feeding
data to an application(s).  If we look at the broader picture of this part of the overall
system, we see a flow of data (managed by cerebro) from function implementation(s) to
function implementation(s) and applications, with flexibility required for how these data
flow from source to destination.  Since cerebro doesn't know about the details of things like
the actual parameters to some `f(x)impl` that's being executed in some function execution
environment, we need to have a way to help the data get to the right place (as a function
arg).

It's helpful to view the output of some `f(x)impl` as the data (serialized, zipped, chunked,
and encrypted) and its metadata, which includes the actual type (to the level of detail
needed to deserialize and restore it), its logical type (which isn't the same as the actual
type--consider that all signal data currently is serialized with a single protobuf class but
that there are different types of signal data generators), and finally the data URI that
it was stored with.  It should be clear that the actual type and the data URI are required
to locate and restore it, but the question on the consuming side is "what argument of `g(y)` is
it destined for" as the definition of `g()` can have multiple parameters.

The suggestion here to support function composition is that this metadata of [dataURI, logicalType]
be returned from an `f(x)impl` execution and that it is stored by
cerebro.  When all predecessor nodes (function executions) in a DAG are completed, cerebro
would then issue a computation request for `g()impl` which would include all this
predecessor-function metadata.  The function execution environment would receive all this
metadata and would be able to use [dataURI] to do the read & restore of the
data, and would allow the "argument evaluation configuration" to declare the logicalType that
is to be used for a given function arg.  This would all be done via something like a
readRestore accessor.

Note that logicalType very well could include version information, much as what's done with
signal and prediction generation currently; the intent of the logicalType is only to
facilitate the linkage between read/restored data and function arg parameter, so whatever is
required for whatever level of granularity can be done as needed.


----------------
## Tying Things Together

The above description leaves out some details so a detailed example is in order.

1. let `f(x)` be declared as:  `list<com.apixio.Signal> extract(com.apixio.PageWindow)` this implies:
    * com.apixio.funcs.Extract is a Java interface that defines a method
        List<com.apixio.Signal> extract(com.apixio.PageWindow)
    * com.apixio.Signal is a Java interface the defines the attributes of a Signal
    * com.apixio.PageWindow is also a Java interface that defines the attributes of a PageWindow
2. there must be some (hand-coded, for now) elements:
    * a .proto definition that captures the attributes of the interface com.apixio.Signal
    * when that .proto is compiled, some protobuf class will be created that performs (de)serialization; let's call that class com.apixio.proto.Signal
    * there might or might not be a .proto file for com.apixio.PageWindow; this depends on if PageWindows need persistence
    * a converter that takes instances of com.apixio.Signal and produces equivalent com.apixio.proto.Signal instances and vice-versa; call this class com.apixio.converters.SignalConverter
3. let f(x)impl be an implementation of f(x) (i.e., conforms to the signature of the above f(x)); this implies:
    * there is some concrete class that has a method signature of
        List<com.apixio.Signal> extract(com.apixio.PageWindow)
    * the JVM-compatible code is packaged with whatever assets it needs to evaluate f(x)impl
4. there should be some accessor(s):
    * pageWindow which produces a single com.apixio.PageWindow instance, given some input parameter(s)
    * pageWindows which produces List<com.apixio.PageWindow>, given an APO
    * supplied by function execution environment:
        * request; pulls the given field from the kafka request, like request("docuuid")
        * singlePartialPatient; given a docuuid, loads and returns its APO
5. the function execution environment can be configured as follows:
    * function to evaluate:  f(x)impl
    * arg evaluation:  pageWindows(singlePartialPatient(request("docuuid")))
    * data URI creator:  {mcid} {docuuid} {patientuuid}        <<-- !! inconsistent with just using docuuid !!
    * converters:  com.apixio.converters.SignalConverter and somehow com.apixio.proto.Signal (which does actual (de)serialization)

The ApxDataDao subsystem uses DataType instances to direct its operations on conversions and low-level storage options and DataType instances require:

* the protobuf-generated Class, such as com.apixio.proto.Signal
* a unique storage prefix
* zip & encryption flags

(Also note that encryption ought to include pdsID in order to support per-PDS encryption keys.)


### Summary of what stuff goes where:
1. ModelCatalog (the published function implementation) contains:
    * function implementation metadata, which includes:
        function definition; e.g., list<com.apixio.Signal> extract(com.apixio.PageWindow)
2. Function execution environment must be configured with:
    * MCID to execute
    * required accessors, converters, datatypes
    * argument evaluation configuration; e.g., pageWindows(singlePartialPatient(request("docuuid")))
3. Cerebro manages:
    * persistence of [groupingID, datatypeID, logicalType] returned from some f(x)impl compute request


----------------
## Jar Handling

As the system is intended to support dynamic binding of certain types of code, it has to deal
with ClassLoaders.  At the same time the SDK system shouldn't force the function execution
environment to use dynamic loading on all components.  The current design is to support a set
of .jar files that are used to create a single `URLClassLoader` instance; there is only one
.jar type that's required to be specified and that's the implementation.jar for the `f(x)impl`.
The other types of code--accessors, converters--can be either linked in to the JVM via normal
`CLASSPATH` or via specifying a .jar file that's included in the `URLClassLoader` list.  Only one
.jar is allowed for each type of code (for now), so all accessors must reside in a single
.jar, etc.


----------------
## "Auto-Iterating" on Lists

One of the features of the SDK when executing/invoking some `f(x)` implementation is that it
will, under certain conditions, handle a mismatch of actual argument value and the declared type
of the corresponding parameter in the `f(x)` definition during function invocation.  It's not
100% clear if this feature is needed but for the use-case of supporting the existing MLC-based
signal generators it provides a potentially large benefit.

For background, the current runtime method declaration for signal generators is

        List<com.apixio.ensemble.ifc.Signal> process(com.apixio.ensemble.ifc.PageWindow pw)

where the idea of a `PageWindow` is that it provides a small view of contiguous pages into the
document to be processed.  The current framework code that both provides the PageWindow instances
and invokes the `process()` method walks through a document's pages in a sliding-window manner
and conceptually creates a new `PageWindow` instance before invoking `process()`.  Supporting
this implicit `List<com.apixio.ensemble.PageWindow>` parameter type operation in the SDK is easy
(via an Accessor), but would require that the signal generator signature, and thus
implementations, to change.

What the SDK invocation code does to bridge this list-to-object mismatch is to look at the type of the
arguments produced by the "arg eval" definition and if it's a list (with at least one element) of
a type that matches the type of the corresponding parameter defined by the `f(x)` signature, then
the invocation code enumerates the items in the list and invokes the `f(x)` implementation for
each item in the list.

For example, if the signature for `f(x)` is

        List<Signal> process(PageWindow pw)

but the actual argument produced from the accessor in the arg list evaluation phase is

        List<PageWindow>

(which would be tested via `arg instanceof java.util.List`), then the actual operation is
equivalent to the following code:

        List<Signal> flat = new ArrayList<>();
        for (PageWindow item : (List<PageWindow>) arg)
            flat.addAll((List<Signal>) fx.invoke(item))


----------------
## Data Creation and Data Reading Use Cases

In order to try to achieve high performance and reliability it's necessary to look at some
known scenarios to see how the above design/model works:

1. highly parallel f(x) execution; this would be the spark cluster case, such as with siggen.  The defining characteristic here is that f(x) for a given x is executed in more than one process
2. f(x) produces a very large amount of output data.  The defining characteristic here is that an OutOfMemory condition might occur
3. f(x) execution such that the size of x is very large (the read counterpart to point 2).  This has two potential impacts:  hitting OutOfMemory condition, and slow execution due to non-parallel computation (as compared to non-vectorized operations)
4. required parallel execution of f(x), which is really the second part of point 3 as a separate use case

It should be clear that the design needs to support highly parallel execution on both the write side and the read side.


