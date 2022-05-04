# **Notes related to non-Java F(x)s**
*Sept 16, 2021*

Note: this document assumes non-Java F(x)s will behave in a fundamentally different way as Java Fxs. This is all in flux, so we may take steps to reduce the differences.

------------------
## High-level description of how it works

The idea is to re-use as much Java-Fx code as possible to avoid dual maintenance. Rather than having one version of the full ECC for each language, we instead push non-Java F(x)s to a separate process and use service calls to setup and invoke the non-Java F(x).

## These elements are common among all F(x)s:

- The construction, loading, and representation of FxDefs
- The construction, loading, and execution of Accessors
- The persistence of outputs and the generation of their data uris
- The early processing of F(x) requests


## Changes to FxImpl

Because the implementation is not encapsulated in a jar, the `impl`' element of FxImpl needs to be an executable. e.g., instead of a jar pointer like 
`"fximplcode/target/apixio-fx-impls-1.0.5-SNAPSHOT.jar"`

it must be a pointer to an executable file that accepts these optional arguments

- the port to which the service should bind.
- the number of concurrent requests that should be handled (careful: multiplies memory usage)
- the host of the fluent logger
- the port of the fluent logger

(The sdk itself processes the arguments, so the F(x) designer needs to know only one thing: "your program should run without arguments; all arguments/config should be passed in via the asset mechanism)

Example options for executable:

- A single-file python executable 
- A docker image file build with `EXPOSE 8080/tcp`
- A C++ binary

In addition,  `entry` element of impl needs to change. Rather than

`anno_entry="com.apixio.ensemblesdk.impl.AnnotationFx"`

we have

`anno_entry="svc::AnnotationFx"`

The schema `svc` tells the ECC that a service will need to be started and managed. The fxname `AnnotationFx` will be passed to the service with every API call, which allows the service to host multiple F(x)s with common FxDef signatures.


## Changes to ECC init. 

`EccSystem.createInvokable` now splits based on the Fx type.

For Java-based Fxs, it constructs `JavaFxInvokable`.

For service-based Fxs, it constructs `SvcFxInvokable`


## `SvcFxInvokable` and `SvcFxImplementation`

These classes are the analog of `JavaFxInvokable` (formerly `FxInvokable`) and `FxImplementation`, respectively.

The `callSetup` method of `SvcFxInvokable` first checks to see if the service is running and reachable on the configured port. If not, it invokes the single-file executable, and polls until it can connect to the port.

Once running, the 'setAssets' call is made to the F(x) service. 

`invokeInner` of `SvcFxInvokable` is called by the parent class (`FxInvokable`) inside it's `invoke` function. This make a service call to the external service.

## The Python side of things

Here is a simple Python F(x)

```
from apxsdk import sdk

@sdk.register_set_assets("AnnotationFx")
def myset_assets(config):
    print("AnnotationFx assets", config)

@sdk.register_fx("AnnotationFx")
def myfx(signals):
    print("called AnnotationFx ", signals)
    return signals

sdk.init_app()
```

The `apxsdk` package will be available on an apixio package repository available to partners.

To connect an F(x) to the sdk, two functions should be annotated:

* A setup function that will receive the F(x) assets. This function must have argument names that exactly match the assets defined in FxImpl.
* An F(x) function that accepts parameters defined by the corresponding FxDef (order matters)

The output of the F(x) function must exactly match the output defined in the corresponding FxDef. Currently, this means that the object is a primitive type or parsable by the Google Protobuf function `ParseDict`.

The last line of the example file above `sdk.init_app()` starts a Flask app with endpoints `/apxsdk/invoke` and `/apxsdk/setassets` that are used by the ECC.

For implementations that already use a Flask-derived app, the sdk will attach to that service if passed in the init: `sdk.init_app(MY_APP)`. Important: in this case MY_APP needs to be constructed on the port found in `sys.argv[1]`

```
More detail can be found in the comments at the top of `sdk.py`
```

## FxTest in Python

** Note: in flux, this is a proposal.... **

The point of FxTest is to provide Fx designers a convenient way to "prove" their F(x) will work in the production environment without knowing much about how the production environment works. It's Engineering's job to - over time - make FxTest as realistic as possible. Success, from the Engineering POV, is when FxTest enforces all of the constraints of the production environment, which would enable automatic publishing of new F(x)s without any Engineering involvement.

These are the known constraints:
1. The F(x) should be packaged in an executable that only requires the sdk-specific arguments (port, num-processes, etc)
2. All config and dependencies should be passed in via set_assets
3. All request-specific inputs should be loaded with the accessor mechanism
4. All F(x) outputs must be persistable/loadable in the Fx system

FxTest is a class in the sdk that help validate that an Fx respects these constraints. There are two modes with FxTest:

1. Direct (debug) mode: this mode enables fast debugging by running the sdk code and the Fx in the same process. Accessors are called directly with a function, the actual Fx code can be stepped into with a debugger, and the conversions needed for persistance can be debugged as well.

2. Validation mode: this mode simulates the real system. The single-file executable is executed by FxTest, it is initialized and invoked via api calls, and the converted outputs are serialized and transmitted over HTTP. This is the mode to be used right before publishing an Fx (or handing it over to Engineering).

Here's an example of both modes:

```
from apxsdk.fxtest import FxTest
import sdkannotest

execPath = "fximplcode/sdkannotest"
annoFxName = "AnnotationFx"
transformSignalsFxDef = "list<apixio.Signal> transformSignals(list<apixio.Signal>)"
annoAccessor = "patientannotations(request('patientuuid'))"

class AnnoFxTest(FxTest):
    assets = None

    def __init__(self) -> None:
        super().__init__()
        self.assets = {"config": "/tmp/annoconfig.yaml"}
    
    def test_direct(self, patientuuid):
        sdkannotest.myset_assets(**self.assets)
        annos = self.runAccessor(annoAccessor, dict(patientuuid=patientuuid))
        result = sdkannotest.convertAnnotations(annos)
        dataUri = self.generateUri(dict(algo=annoFxName, patientuuid=patientuuid))
        self.persist(dataUri, result)

    def validate_fx(self, patientuuid):
        annos = self.runAccessor(annoAccessor, dict(patientuuid=patientuuid))
        pid = self.initFx(execPath, self.assets)
        result = self.runFx(annoFxName, annos)
        dataUri = self.generateUri(dict(algo=annoFxName, patientuuid=patientuuid))
        self.persist(dataUri, result)
        self.shutdownFx(pid)

if __name__ == "__main__":
    AnnoFxTest().test_direct("48156abe-cbe8-440a-9dfd-42b4ea7b223d")

```

