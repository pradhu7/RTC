# **Mono Repo**

## Motivation
The mono repo was created with the following goals:

* help reduce incompatible versions of transitive dependencies of (third party) maven artifacts
* get rid of pom.xml inconsistencies across the various projects
* centralize the version number of artifacts (minimally for convenience)

Much effort was put into pom.xml file fixes and improvements; additionally module-level pom.xml files were modified to contain only the per-module information (e.g., dependencies).  Proper maintenance of pom.xml files includes:

* Use of macros (maven <property>) for any "shared" version number (e.g., jackson version number across the suite of related jackson artifacts)
* use of <dependencyManagement> to force the version number of transitive dependencies
* proper use of `<pluginManagement>` and `<plugin>`
* clear separation and documentation of the various "types" of declared `<dependency>`:
	* for transitive dependencies
	* for `<requireUpperBound>`
	* for `<dependencyConvergence>`

Please read the comments and consider the organization of these elements, and note that most of the pom.xml complexity is in the parent pom and not in the module poms.

## Artifact Versions

Because the code in mono repo is widely used and because we know some parts of the overall system are using old artifacts (e.g., hadoop), there are some limitations on using recent versions of some artifacts.  Because the unit tests in this repo don't test for these version problems, it's wise to be conservative when bumping up version numbers.  For example, it's likely that using recent versions of jackson components (i.e., something newer than 2.9.9) could be problematic.  springframework is also one that is possibly rightfully limited to an older version.

## v2 Security

The artifacts in mono repository use v2 security, which requires some local setup in order to run (both as a service, and for unit tests).

The main pom.xml file configures maven-surefire-plugin for all modules so that running with v2 security support is straightforward.  The key to this is creating a file

	$HOME/.apixio/apx-run.properties

that contains the v2-required Java system properties.  The minimal set of properties is:

* APX\_VAULT\_TOKEN
* bcprovpath
* bcfipspath

and an example file is:

	APX_VAULT_TOKEN=s.L7maYbGYEbeBFIletzEwXYLu
	bcfipspath=/Users/smccarty/bc-jars/bc-fips-1.0.1.jar
	bcprovpath=/Users/smccarty/bc-jars/bcprov-jdk15on-1.47.jar

With the above file, you can do `mvn test` and have correct v2 security initialization.

Note:  the plan is to have a CLI that will manage the contents of `apx-run.properties` but until then manual management is required (and not too burdensome). 

## Adding to mono repo
The first rule of mono repo is to not have it reference any Apixio artifacts that aren't part of mono repo.  It would be too easy to create a repository-level circular dependency otherwise.  That means that when another repo is merged into it, that repo MUST NOT reference any Apixio artifacts outside mono repo.

There are migration tools, guides, etc., in the `migration` directory.

## schemas

### module to manage anything that looks and/or acts like a schema

### Things that are ok
* Protocol buffer proto files
* SQL ddl 
* Documentations of Cassandra layouts 

### building for python 

There is a setuptools distribution for the generated python code at ```schemas/target/python/setup.py```. It requires the maven code generation step to complete first.

**Important Note** : There are *two* setup.py files. Make sure you use the one in the target/python directory or it won't work!

\> source <my venv activate>
\> cd schemas/target/python
\> pip uninstall apxprotobufs
\> python setup.py build install

if you are looking to run tests, then you need to run
\> python setup.py develop test

### Building for development:

- mvn package
- (in schemas/target/python/) python setup.py develop test

### Building a source distribution package:

- mvn package
- (in schemas/target/python/) python setup.py sdist

For examples of how to use the packages see ```src/python/tests```

Python generated code will be in the package ```apxprotobufs```.

