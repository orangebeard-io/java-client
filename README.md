<h1 align="center">
  <a href="https://github.com/orangebeard-io/java-client">
    <img src="https://raw.githubusercontent.com/orangebeard-io/java-client/master/.github/logo.svg" alt="Orangebeard.io Java Client" height="200">
  </a>
  <br>Orangebeard.io Java Client<br>
</h1>

<h4 align="center">Orangebeard Java client for Java based Orangebeard Listeners.</h4>

<p align="center">
  <a href="https://repo.maven.apache.org/maven2/io/orangebeard/java-client/">
    <img src="https://img.shields.io/maven-central/v/io.orangebeard/java-client?style=flat-square"
      alt="MVN Version" />
  </a>
  <a href="https://github.com/orangebeard-io/java-client/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/orangebeard-io/java-client/master.yml?branch=master&style=flat-square"
      alt="Build Status" />
  </a>
  <a href="https://github.com/orangebeard-io/java-client/blob/master/LICENSE.txt">
    <img src="https://img.shields.io/github/license/orangebeard-io/java-client?style=flat-square"
      alt="License" />
  </a>
</p>

<div align="center">
  <h4>
    <a href="https://orangebeard.io">Orangebeard</a> |
    <a href="#installation">Installation</a>
  </h4>
</div>

## Installation

### Install the mvn package

Add the dependency to your pom:
```xml
<dependency>
    <groupId>io.orangebeard</groupId>
    <artifactId>java-client</artifactId>
    <version>version</version>
</dependency>
```

### CLI
In some cases you may want to control a test- or alert run's lifecycle externally. (i.e. when running in a CI pipeline with parallel stages
or when a test tool doesn't let you easily capture start or finish events). The client provides a CLI to start and finish a run.  
Starting a run prints the generated run uuid on stdout, so it can be used captured to use in your scripts.
  
Note that when a test run UUID is obtained this way, it needs to be passed to the listener's client instance either by:
- Injecting it in the json file (not recommended)
- Setting it as system property orangebeard.testRunUUID: `-Dorangebeard.testRunUUID=$testRunUuid`
- Setting it as environment variable orangebeard_testRunUUID: `$env:orangebeard_testRunUUID = $testRunUuid` (Powershell) or `export orangebeard_testRunUUID=$testRunUuid` (bash) 

#### CLI usage
```shell
usage: Orangebeard CommandLine Utility
 -a,--attributes <arg>        Test run attributes
 -as,--alertrunstatus <arg>   Test Alert run status. "INTERRUPTED" or
                              "COMPLETED". Defaults to "COMPLETED"
 -at,--alerttool <arg>        The alert tool name (ZAP or BURP)
 -d,--description <arg>       The test run description
 -e,--endpoint <arg>          Your Orangebeard endpoint
 -id,--testRunUuid <arg>      The UUID of the test run to finish
 -k,--kind <arg>              The run kind. "test" or "security". Defaults
                              to "test"
 -p,--project <arg>           Orangebeard Project Name
 -s,--testset <arg>           The testset name
 -t,--accessToken <arg>       Your Orangebeard Access Token
 -x,--cmd <arg>               Command to execute (start/finish)

```
Start a test run: (assume configuration from orangebeard.json or orangebeard.properties)
```shell
#!/bin/bash
testrunUuid=$(java -cp java-client-x.y.z-jar-with-dependencies.jar io.orangebeard.client.Cli -x start -k test)  
```
Finish a test run:
```shell
#!/bin/bash
java -cp java-client-x.y.z-jar-with-dependencies.jar io.orangebeard.client.Cli -x finish -id $testRunUuid
```

Start an alert run: (assume configuration from orangebeard.json or orangebeard.properties)
```shell
#!/bin/bash
alertRunUuid=$(java -cp java-client-x.y.z-jar-with-dependencies.jar io.orangebeard.client.Cli -x start -k security)  
```
Finish an alert run:
```shell
#!/bin/bash
java -cp java-client-x.y.z-jar-with-dependencies.jar io.orangebeard.client.Cli -x finish -id $alertRunUuid -k security -as COMPLETED
```
