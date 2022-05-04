# **Publishing FxDefs and Model Combinations**
*May 25, 2021*

------------------
## Overview

The actual publishing of an implementation is done using a `YAML` file that collects all the information
needed in one place.  This file is just an evolution of the existing `mc.yaml` file with most elements
being identical or backwards compatible with existing `mc.yaml` elements.

The publishing of an `f(x)` implementation requires either that the `f(x)` that it implements has already been
published (which results in an XUUID) or that the full publishing information be included in the
`mc.yaml` file; this information includes:  IDL, creator, name, and description of the function.


------------------
## Publishing an `f(x)` definition

The Model Catalog Server (MCS) has been extended so that it can be the central repository for and management
of `f(x)` definitions.  There currently are no command line tools to publish a `f(x)` definition to MCS so
this operation must be done via raw `curl` commands.  The result of `POST`ing to the creation endpoint is an
`FXID` that can be used for publishing an implementation (or for whenever a `FxDef` reference is required).
Note that the implementation of this service will *not* a new `FXID` if the actual `f(x)` declaration has
already been registered; this existence test is done on the parsed IDL so whitespace differences in IDLs (for
example) won't be used in the comparison

To publish an `f(x)` definition the following information is required:

* the email address of the person publishing the definition
* the unique and human-readable name of the function (currently only recorded, but will be usable for searching later);
* a meaningful description of what the function implementations are supposed to do
* the actual function signature declaration expressed using the current IDL syntax

The above details needed for publishing are packaged into a JSON payload for the curl command like this:

    {
        "createdBy": "smccarty@apixio.com",
        "name": "FeatureExtractor",
        "description": "Function signature for feature (signal) extraction",
        "fxdef": "list<com.apixio.ensemble.ifc.Signal> extract(com.apixio.ensemble.ifc.PageWindow)"
    }

The `curl` command to publish a definition is:

    $ curl --insecure -X POST \
           -H "Content-Type: application/json" \
           --data-binary 'theJsonPayloadFromAboveOnASingleLine' \
           $host/fx/fxdefs

where `$host` is one of:

* `https://modelcatalogsvc-stg.apixio.com:8443`
* `https://modelcatalogsvc.apixio.com:8443`
* `http://localhost:8036`   # if MCS is locally-hosted

The (formatted) JSON response will be similar to:

    {
        "fxid": "FX_bb33300b-ff54-438d-b2c9-a048be0ebdd1"
    }

It's possible to retrieve the (JSON-formatted) `FxDef` via:

        $ curl --insecure $host/fx/fxdefs/{fxid}

where `{fxid}` is from the prior returned JSON.  An example returned FxDef in JSON is:

    {
        "name": "extract",
        "returns": {
            "sequenceInfo": {
                "ofType": {
                    "containerInfo": {
                        "name": "com.apixio.ensemble.ifc.Signal",
                        "isStruct": true
                    },
                    "tag": "CONTAINER"
                }
            },
            "tag": "SEQUENCE"
        },
        "parameters": [
            {
                "containerInfo": {
                    "name": "com.apixio.ensemble.ifc.PageWindow",
                    "isStruct": true
                },
                "tag": "CONTAINER"
            }
        ]
    }

------------------
## Setup for publishing

A Java-based command line tool has been created to replace part of `mctool`; it provides the
initial publishing of a ModelCombination (i.e., `f(x)` implementation) but not search,
lifecycle management, etc.  For those operations the current `mctool` must be used.

This Java-based MC publishing is intended to serve as the specification of how f(x) implementations
must be published; any Python-based tool (or modifications) must use this Java code for this
specification purpose.

This command line tool requires a `YAML` file to specify configuration that is not specific to a
given `f(x)` implementation and consists of the following keys and structure:

	cache:
	  directory:  "local file system directory"
	  options:    "csv list of options"         # taken from [VERBOSE, NO_MKDIR]

	s3:
	  accessKey:  "encrypted key"
	  secretKey:  "encrypted key"
	  accessKeyUnencrypted:  "plaintext key"
	  secretKeyUnencrypted:  "plaintext key"

	mcs:
	  serverUrl: "https://modelcatalogsvc.apixio.com:8443"
	  connectTimeoutMs: nn              # default is 10000 (10 seconds)
	  socketTimoueMs: nn                # default is  5000
	  connectionRequestTimeoutMs: nn    # default is 10000

	artifactory:
	  uriPrefix:  "https://repos.apixio.com"
	  username:  "artifactory username"
	  password:  "long artifactory password"


Note that timeouts can occur if large assets need to be uploaded over a slow connection such as the VPN.  To
avoid these timeouts, increase/set the values under the `mcs` key.

Only one set (encrypted or unencrypted) of access+secret keys needs to be specified in the `s3` section, and
the `artifactory` key/section is optional if no asset URI is being pulled from Artifactory.

The `cache` configuration is used to avoid refetching assets for any URI-based resource (e.g., S3, https)
if the same `f(x)` implementation is published more than once in a short time period.  Because this
caching mechanism assumes the source object is readonly it does *not* check for size/timestamp changes
and so will not pull down modified assets.  Because it this, it's recommended that the cache directory is
in something like `/tmp` so that (eventually) updated assets will be fetched (of course, the contents of
that cache directory can be removed and the publishing code will fetch the assets again).

------------------
## Publishing an `f(x)` implementation

Publishing an `f(x)` implementation uses the same back-end mechanisms as what is currently used when
publishing a Model Combination, although the `mctool` command has not been modified to handle
this task.

Key to the publishing process is the `mc.publish` file (renamed from `mc.yaml` to prevent
unintentional misuse of files with tools).  This file is a declaration of all the information
needed to publish an SDK-based Model Combination.  The file is a `YAML` file and the structure
of the keys and values are as follows:

    # This publishing config file mimics AssumedDate config.  Note that the details that change between
    # publishing the "same" f(x) implementation are likely:
    #  * URIs
    #  * modelVersion
    #  * version
    #  * snapshot (not sure what this does)

    # purely used for convenience/centralization in this file only:
    macros:
      generator_name:     AssumedDate
      artifactory_jars:   "https://repos.apixio.com/artifactory/apixio-release-snapshot-jars"
      artifactory_models: "https://repos.apixio.com/artifactory/models/models"
      ensemble_ver:       1.19.0-SNAPSHOT

    ################################################################

    # this gets stored as top/parent ModelCombination metadata:
    meta:
      name:        "{%core.version.name%}"
      functionId:  "FX_e0dbb624-4ddd-44f7-868c-f96a5b7bb023"          # this key or functionDef key must exist
      functionDef:                                                    # this key or functionId key must exist
        idl:         'list<apixio.Signal> extractIt(apixio.PageWindow)'
        creator:     smccarty@apixio.com
        name:        extractIt
        description: "Extracts things"
      entry:       "com.apixio.ensemble.impl.generators.page.AssumedDate"
      logicalId:   "{%core.version.name%}:{%core.version.version%}/{%core.version.modelVersion%}"

    # these get added as parts:
    assets:
      implementation.jar:
        uri:  "{%macros.artifactory_jars%}/apixio/apixio-ensemble-benchmarks/{%macros.ensemble_ver%}/apixio-ensemble-benchmarks-{%macros.ensemble_ver%}-20201124.204720-47.jar"
        mimeType: application/octet-stream

      model.zip:
        uri: "{%macros.artifactory_models%}/release_10_0_0_model/20201117_214847/release_10_0_0_model_20201117_214847.zip"

      config.yaml:
        uri:  "{%macros.artifactory_jars%}/apixio/assumeddate-config.yaml"

    dependencies:
      - aLogicalId

    # this gets stored as top/parent extra_1 JSON
    core:
      algorithm:
        type: DOCUMENT_SIGNAL
      version:
        modelVersion: 1.0.1-V23
        name: "{%macros.generator_name%}"
        version: 1.0.0

    # this gets stored as top/parent extra_2 JSON
    search:
      tags:
      - SIGNAL_GENERATOR
      - DOCUMENT_SIGNAL
      - "{%macros.generator_name%}"

A few items of note for the above `YAML` file spec:
* the preferred approach for the f(x) definition it to use the `functionDef` yaml key instead of the the
  `functionId` key
* *most* of the structure of `mc.publish` is the same as the structure of `mc.yaml`
* the existing `mc.yaml` key of `core.algorithm.javaClass` is replaced with `meta.entry`
* there's support for `{%...%}`-based macro expansion within string values; this allows for explict declaration
  of derived values as compared to the current implicit mechanism (some/all? of which was done in `mctool`).
* a macro reference to a non-scalar will copy the object referenced
* missing required keys/values will result in a publishing error

------------------
## Scripts for Publishing

The `scripts` top-level directory contains a few bash scripts to help with the publishing tasks described above.
All of these scripts use the maven artifact in `cmdline/target`

The most useful scripts are:

  * `scripts/publish-fxdef.sh`:  this script accepts the four required parameters needed for publishing an
    f(x) definition and performs the `curl` command described above.  As this command directly references
    the MCS server it supports the ability to override the default server URL (which is to staging MCS) by
    exporting the environment variable `MCS_SERVER`; the override value must be in the form of `https://{host}:{port}`
  * `scripts/publish.sh`:  this script accepts file references to the "config" and "publish" files and
    publishes the `f(x)` implementation to the MCS server specified in the referenced `pubconfig.yaml` file
    (under the `mcs.serverUrl` key)
