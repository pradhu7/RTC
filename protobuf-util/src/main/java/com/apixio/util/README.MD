# ProtoBuf utilities


To help the programming of your producers and consumers of messages we have created a 
light weight library to do some of the repetitive work and initialization routines for 
your messages


### ProtoBuf Class
in the package com.apixio.util we have added a class called ProtoBuf. This class provides the following




### Initializing the ProtoBuf Class

The protobuf class needs to be initialized by anyone sending messages. 
Receiving of messages does not require this initialization. The MessageHeader in our messages
initialization requires a SenderId which is a composit of the service name and the service version.
We get these values from the Maven package information that is pushed in during the build.

You as a user will need to call the senderId initialization method in the protobuf class 

    public static void setSenderID(String name, String version)


This initialization code should be called in the boot up of you application. 
We will provide 2 examples for DropWizard services in both Java and Scala

#### Scala DW Init
 
```
import com.apixio.util.Protobuf

class CerebroApplication extends Application[ApxConfiguration] with ApixioLoggable {
  ...
  override def run(cfg: ApxConfiguration, env: Environment) {
    ...
    val p = this.getClass.getPackage
    Protobuf.setSenderID(p.getSpecificationTitle, p.getSpecificationVersion)
    ...
  }  
  
```

#### Java DW Init

```
import com.apixio.util.Protobuf

public class EccApplication extends MicroserviceApplication<EccConfig> {
    ...
    @Override
    public void run(EccConfig configuration, Environment environment) throws Exception
        ...
        Package p = this.getClass().getPackage();
        Protobuf.setSenderID(p.getSpecificationTitle(), p.getSpecificationVersion());
        ...
    }
    
```

#### Maven Init


In the POM file for the assembly you are building, in this case the DW service, 
you need to make sure that the Maven `maven-jar-plugin` has the following item set in the manifest

    addDefaultSpecificationEntries

as shown below. This asks maven to add the packaging information into the manifest

```
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>2.3.2</version>
            <configuration>
              <archive>
                <manifest>
                  <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                  <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                  <addClasspath>true</addClasspath>
                  <mainClass>com.apixio.tools.relabel.Relabel</mainClass>
                </manifest>
                <manifestEntries>
                  <Class-Path>bcprov-jdk15on-1.47.jar</Class-Path>
                </manifestEntries>
              </archive>
            </configuration>
          </plugin> 
```

#### SBT package assembly

To verify - It seems like the SBT assembly plugin creates the correct manifest entries automatically.
What we have not yet determined is where the Name and Versions are set in the SBT file or whether the 
assembly plugin is just correctly figuring it out on its own

We did find the following 

in version.sbt 

    version := VersionSuffixSupport.normalizeVersion("0.0.1")

in build.sbt

    lazy val root = (project in file("."))
      .settings(commonSettings)
      .settings(
         name := "cerebro",
    