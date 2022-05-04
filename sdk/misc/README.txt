March 3, 2021
#############

The goal for Q1 proof-of-concept is to have a working(-ish) port of the FaceToFace
signal generator to the poc SDK.


Tasks:

DONE ENOUGH FOR POC 1. create human-level IDL for faceToFace(...)
   a) IDL is not anything more than an informal declaration of the formalized f(x) signature
   b) no need to carry over most ifc.signal constructs as all we need is 

2. create formalization artifacts
   DONE ENOUGH FOR POC a) Java interfaces for all param types and return value
   DONE ENOUGH FOR POC b) abstract base classes to be used by implementation
   DONE ENOUGH FOR POC c) .proto files for all data types  -- CARRY over Tim's signals.proto (from mono/schemas)
   DONE ENOUGH FOR POC d) create converter classes for interface<->proto exchange  -- CARRY over Tim's ifcwrapper code (from apx-signalmanagersvc/common)
   e) create data accessors
      - not sure what's needed

DONE 3. create .proto for persisting IDL constructs; hand-code creation of
   protobuf-based POJOs to represent constructs created in bullet point #1

SORT OF DONE FOR NOW 4. define f(x) initialization mechanism
   a) goes hand-in-hand with 2a and 2b

5. implement faceToFace
   a) identify all artifacts that need publishing
   b) copy/modify existing scala code to work with abstract base classes
   c) add in initialization

6. create scripts to publish an MC using just back-end REST API (avoids modification of mctool)
   a) simple list of blob files
   b) add defined blob for persisted f(x) signature

7. massively modify LambdaECC or copy useful code into a new service
   a) requires dynamic loading of impl and accessors
   b) should use converters
   c) should use ApxDataDao to try to persist output of f(x)

8. create test driver system
   a) essentially fakes out the "pull from kafka" part of LambaECC

9. miscellaneous
   a) create git repo and maven project that have required separation of code/artifacts


################
Directory Descriptions

* accessors:  contains reusable, dynamically loadable data accessors

* (fake-system not used currently)

* fxdef:  contains declarations of f(x)s; subdirectories:
  * the facetoface.fx file (for human consumption)
  * (temporary) proto builder code to represent hand-parsed facetoface.fx contents
  * genjava:  all (hand-)generated .java files derived from the .fx IDL contents
    * interface for f(x)
    * interfaces for all data  (this stuff really should be in a reusable place...right?)
    * abstract base classes
    * converter classes for data types (move to reusable area...)
  * genproto:  all (hand-)generated .proto files derived from the .fx IDL contents

* fximplcode:  the port of FaceToFace functionality; includes
  * subdir of something like "f2fimpl" with
    * .scala
    * model.zip (at least references it)
    * config

* genlambdaecc:  generic JVM-based execution container; will contain guts of
      current lambdaecc, most likely; also:
  * test driver that simulates reading from kafka, loading/initializing f(x) impl,
    invoking f(x), 

* sdkcode:  all official SDK code; includes:
  * .proto for IDL constructs
  * (temporary) builder code that uses above .proto to represent hand-parsed .fx contents
  * MC publishing scripts
  * f(x) locator, loading, initialization code
  * 



