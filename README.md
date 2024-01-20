# PDAL Java bindings

[![CI](https://github.com/PDAL/java/workflows/CI/badge.svg)](https://github.com/PDAL/java/actions) [![Join the chat at https://gitter.im/PDAL/PDAL](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/PDAL/PDAL?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Maven Central](https://img.shields.io/maven-central/v/io.pdal/pdal_2.13)](https://search.maven.org/search?q=g:io.pdal) [![Snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.pdal/pdal_2.13)](https://oss.sonatype.org/content/repositories/snapshots/io/pdal/)

Java bindings to use PDAL on JVM (supports PDAL >= 2.0).
Mac users can experience some issues with bindings that were build against a different PDAL version,
so try to use a consistent PDAL version.

It is released independently from PDAL itself as of PDAL 1.7.

See [https://pdal.io/java.html](https://pdal.io/java.html) for more info.

## Using PDAL JNI with SBT

```scala
// pdal is published to maven central, but you can use the following repos in addition
resolvers ++= 
  Resolver.sonatypeOssRepos("releases") ++ 
  Resolver.sonatypeOssRepos("snapshots") // for snaphots

// `<latest version>` refers to the version indicated by the badge above
libraryDependencies ++= Seq(
  "io.pdal" %% "pdal" % "<latest version>", // core library
  "io.pdal" %  "pdal-native" % "<latest version>" // jni bindings
)
```

If you would like to use your own bindings, it is necessary to set `java.library.path`:

```scala
// Mac OS X example with manual JNI installation
// cp -f native/target/resource_managed/main/native/x86_64-darwin/libpdaljni.2.1.dylib /usr/local/lib/libpdaljni.2.1.dylib
// place built binary into /usr/local/lib, and pass java.library.path to your JVM
javaOptions += "-Djava.library.path=/usr/local/lib"
```

You can use `pdal-native` dep in case you don't have installed JNI bindings and to avoid steps described above.
Dependency contains bindings for `x86_64-darwin` and `x86_64-linux`, other versions are not supported yet.

### Mac-OS ARM tutorial

If you need to build your own `pdal-native` for `arm64-darwin`, the following is a temporary workaround:

- If you are using `pdal` version `<= v2.4.x`, use `./sbt`. Otherwise, you need to install `sbt`: `brew install sbt`
- Pay attention to your `pdal` installation: at the time of writing this, `conda` is the only working method, unless you build your own `pdal`. To install `pdal` with `conda`:
```
brew install miniconda
conda create --name pdal_env python=3.9 # to create an environment separate from your system's
conda activate pdal_env
conda install -c conda-forge pdal
pdal --version # to check the installation
conda env export --from-history > pdal_env.yml # if you need to export the env for collaborative use
conda deactivate # to exit the conda env
```
- Clone this repository, then run:
`./sbt native/publishLocal` or `sbt native/publishLocal`
- Run the following to check that everything is okay: `./sbt core/test` or `sbt core/test`
- Go to `../pdal-java/native/target/`, here you will find the built `pdal-native.jar`. If you want to use it in a Java project, for example, you can go to `./m2/repository/io/pdal/pdal-native/<your-version>/` and replace the one taken from Maven with the one you have just built.





## PDAL-Scala (Scala 2.x)

Scala API allows to build pipeline expressions instead of writing a raw JSON.

```scala
// `<latest version>` refers to the version indicated by the badge above
libraryDependencies ++= Seq(
  "io.pdal" %% "pdal-scala" % "<latest version>", // scala core library
  "io.pdal" %  "pdal-native" % "<latest version>" // jni bindings
)
```

Scala API covers PDAL 2.0.x, to use any custom DSL that is not covered by the 
current Scala API you can use `RawExpr` type to build `Pipeline Expression`.

## Code examples

#### Demo project with examples

JNI bindings basic usage examples can be found [here](./examples).

### PDAL Core (Scala 2.x / 3.x)

```scala
import io.pdal._

// pipeline definition
val json =
  """
     |{
     |  "pipeline" : [
     |    {
     |      "filename" : "/path/to/las",
     |      "type" : "readers.las"
     |    },
     |    {
     |      "type" : "filters.crop"
     |    },
     |    {
     |      "filename" : "/path/to/new/las",
     |      "type" : "writers.las"
     |    }
     |  ]
     |}
  """.stripMargin

val pipeline = Pipeline(json)
pipeline.validate() // check if our JSON and options were good
pipeline.setLogLevel(LogLevel.Debug5) // make it really noisy
pipeline.execute() // execute the pipeline
val metadata = pipeline.getMetadata() // retrieve metadata
val pvs      = pipeline.getPointViews() // iterator over PointViews
val pv       = pvs.next() // let's take the first PointView

// load all points into JVM memory
// PointCloud provides operations on PDAL points that
// are loaded in this case into JVM memory as a single Array[Byte]
val pointCloud = pv.getPointCloud()
val x = pointCloud.getDouble(0, DimType.X) // get a point with PointId = 0 and only a single dimensions

// in some cases it is not neccesary to load everything into JVM memory
// so it is possible to get only required points directly from the PointView
val y = pv.getDouble(0, DimType.Y)

// it is also possible to get access to the triangular mesh generated via PDAL
val mesh = pv.getTriangularMesh()
// the output is an Array of Triangles
// Each Triangle contains PointIds from the PDAL point table
val triangles = mesh.asArray

pv.close()
pvs.close()
pipeline.close()
```

### PDAL Core (Java)

```java
import io.pdal.*;

// pipeline definition
String json =
  """
     |{
     |  "pipeline" : [
     |    {
     |      "filename" : "/path/to/las",
     |      "type" : "readers.las"
     |    },
     |    {
     |      "type" : "filters.crop"
     |    },
     |    {
     |      "filename" : "/path/to/new/las",
     |      "type" : "writers.las"
     |    }
     |  ]
     |}
  """;

var pipeline = new Pipeline(json, LogLevel.Error());  

pipeline.initialize(); // initialize the pipeline
pipeline.execute(); // execute the pipeline

var metadata = pipeline.getMetadata(); // retrieve metadata
var pvs      = pipeline.getPointViews(); // iterator over PointViews
var pv       = pvs.next(); // let's take the first PointView

// load all points into JVM memory
// PointCloud provides operations on PDAL points that
// are loaded in this case into JVM memory as a single Array[Byte]
var pointCloud = pv.getPointCloud();
var x = pointCloud.getDouble(0, DimType.X()); // get a point with PointId = 0 and only a single dimensions

// in some cases it is not neccesary to load everything into JVM memory
// so it is possible to get only required points directly from the PointView
var y = pv.getDouble(0, DimType.Y());

// it is also possible to get access to the triangular mesh generated via PDAL
var mesh = pv.getTriangularMesh();
// the output is an Array of Triangles
// Each Triangle contains PointIds from the PDAL point table
var triangles = mesh.asArray();

pv.close();
pvs.close();
pipeline.close();
```

### PDAL Scala

```scala
import io.pdal._
import io.pdal.pipeline._

// To construct the expected json
val expected =
  """
     |{
     |  "pipeline" : [
     |    {
     |      "filename" : "/path/to/las",
     |      "type" : "readers.las"
     |    },
     |    {
     |      "type" : "filters.crop"
     |    },
     |    {
     |      "filename" : "/path/to/new/las",
     |      "type" : "writers.las"
     |    }
     |  ]
     |}
  """.stripMargin
  
// The same, but using scala DSL
val pc = ReadLas("/path/to/las") ~ FilterCrop() ~ WriteLas("/path/to/new/las")

// The same, but using RawExpr, to support not implemented PDAL Pipeline API features
// RawExpr accepts a circe.Json type, which can be a json object of any desired complexity
val pcWithRawExpr = ReadLas("/path/to/las") ~ RawExpr(Map("type" -> "filters.crop").asJson) ~ WriteLas("/path/to/new/las") 

// Create Pipelines from the constructed expressions
val pipelinePc = pc.toPipeline
val pipelinePc = pcWithRawExpr.toPipline
```

## How to compile

Development purposes (including binaries) compilation:
  1. Install PDAL (using brew / package managers (unix) / build from sources / etc)     
  2. Build native libs `./sbt native/nativeCompile` (optionally, binaries would be built during tests run)
  3. Run `./sbt core/test` to run PDAL tests

Only Java development purposes compilation:
  1. Provide `$LD_LIBRARY_PATH` or `$DYLD_LIBRARY_PATH`
  2. If you don't want to provide global variable you can pass `-Djava.library.path=<path>` into sbt:
    `./sbt -Djava.library.path=<path>`
  3. Set `PDAL_DEPEND_ON_NATIVE=false` (to disable `native` project build)
  4. Run `PDAL_DEPEND_ON_NATIVE=false ./sbt`

Finally the possible command to launch and build PDAL JNI bindings could be:

```bash
# Including binaries build
./sbt
```

```bash
# Java side development without binaries build
PDAL_DEPEND_ON_NATIVE=false ./sbt -Djava.library.path=<path>
```

### Possible issues and solutions

1. In case of not installed as global PDAL change [this](./java/native/src/CMakeLists.txt#L25) line to:

  ```cmake
  set(CMAKE_CXX_FLAGS "$ENV{PDAL_LD_FLAGS} $ENV{PDAL_CXX_FLAGS} -std=c++11")
  ```
  In this case sbt launch would be the following:

  ```bash
  PDAL_LD_FLAGS=`pdal-config --libs` PDAL_CXX_FLAGS=`pdal-config --includes` ./sbt
  ```

2. Sometimes can happen a bad dynamic linking issue (somehow spoiled environment),
   the quick workaround would be to replace [this](./java/native/src/CMakeLists.txt#L25) line to:

  ```cmake
  set(CMAKE_CXX_FLAGS "-L<path to dynamic libs> -std=c++11")
  ```

## How to release

All the instructions related to the local / maven release process are documented in the [HOWTORELEASE.txt](./HOWTORELEASE.txt) file.

For the local publish it is possible to use the following commands:

* `scripts/publish-local.sh` - to publish Scala artifacts
* `scripts/publish-local-native.sh` - to compile and publish artifact with native binaries

For the additional information checkout the [HOWTORELEASE.txt](./HOWTORELEASE.txt) file and the [scripts](./scripts) directory.
