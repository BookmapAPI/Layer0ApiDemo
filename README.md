# Bookmap Layer 0 API

## General overview of L0 API

Bookmap API consists of 2 main parts:

- L0 API - allows you to feed raw data into Bookmap
- L1 API - used for building indicators/strategies and feeding processed data into Bookmap

This repository contains examples of L0 API usage. For L1 API, visit
the [DemoStrategies repository](https://github.com/BookmapAPI/DemoStrategies)

## Use cases

Layer0 API is a relatively simple way to:

1) Replay your own file format with Bookmap - referred to as **Replay** modules.
2) Connect Bookmap to your own data source in real-time (both for receiving data and
   trading) - referred to as **Live** modules

Doing that requires basic Java knowledge, however (1) is possible even without any Java
knowledge at all using any programming language if you don’t mind converting files before
playing instead of integrating format support into Bookmap platform.

## Code examples

**Layer0ApiDemo** project provides 4 demo classes with detailed comments that illustrate the most
typical scenarios:

1) [`DemoTextDataReplayProvider`](src/main/java/velox/api/layer0/replay/DemoTextDataReplayProvider.java) - loads simple text format into Bookmap. As an option,
   you can just generate a file in this format and feed it to Bookmap using this module. This
   enables using any language for conversion to Bookmap format.
2) [`DemoGeneratorReplayProvider`](src/main/java/velox/api/layer0/replay/DemoGeneratorReplayProvider.java) - mimics a replay provider, but instead of loading data
   this provider generates it. Example of setting order queue position and displaying simple
   indicators using legacy API.
3) [`DemoExternalRealtimeProvider`](src/main/java/velox/api/layer0/live/DemoExternalRealtimeProvider.java) - example of custom realtime data provider. This one
   only generates random data for illustration purposes, but you can use it to build your
   own connectivity.
4) [`DemoExternalRealtimeTradingProvider`](src/main/java/velox/api/layer0/live/DemoExternalRealtimeTradingProvider.java) - extends `DemoExternalRealtimeProvider`
   providing trading capability. Simulates limit orders over the generated data. Shows how
   to build your own provider for connecting to a platform with trading support.
   The project also contains some more complicated examples.

## Javadoc

Javadoc is available in:

- [Bookmap Maven repository](https://maven.bookmap.com/maven2/releases/com/bookmap/api/api-core/) -
  open a directory with a name that corresponds to your Bookmap version (available at _Help -> About_).
  Download the file `api-core-<your_version>-javadoc.jar`, where `<your_version>` is the version of
  your Bookmap.

- Use the javadoc bundled with your Bookmap - `bm-l1api-javadoc.jar` typically located
  in `C:\Program Files\Bookmap\lib`
  (location might differ depending on the path selected during installation).

The javadoc contains documentation for all levels of API, but for L0 API it’s mostly sufficient to
only look inside `velox.api.layer0` package.

## Developing your L0 module

The good starting points for your L0 modules are the classes:
- `ExternalLiveBaseProvider` - for `Live` modules
- `ExternalReaderBaseProvider` - for `Replay` modules

Extending those classes will provide some parts of the logic already implemented for you.
It’s also highly advised to read the javadoc for these classes, as it describes the L0 modules lifecycle.

## Loading modules

There are 2 ways to load modules.

### 1. Loading via configuration files

This approach is recommended for development, as it allows loading class files directly, without
building a jar file.
For Bookmap to load your module, it should be added to a configuration file inside
`C:\Program Files\Bookmap\lib\UserModules\L0`
There are 2 files, `external-live-modules.txt` and `external-reader-modules.txt`
The first one contains a list of live connectivity modules, and the second one contains a list of
replay modules.

Files can contain comment lines (start with #) and lines in the following format:
`<full-class-name> <path-without-spaces>`
The path can point to either folder or .jar file with classes, whatever is more convenient.
For example, the line in `external-live-modules.txt` can look like this:
`velox.api.layer0.live.DemoExternalRealtimeTradingProvider D:\Layer0ApiDemo\build\classes\java\main\`

Note that for this approach to work all the files should be included into the
folder/jar file that you specify as `<full-class-name>`, i.e. if your module uses
a file from `resources` folder of class path - make sure it is in the same folder.

### 2. Loading from a jar file

Modules annotated with `@Layer0LiveModule` or `@Layer0ReplayModule` can be
packed into the JAR file and placed in `C:\Bookmap\API\Layer0ApiModules` (or similar location if
the path was changed during the installation process). Bookmap will load it automatically on
startup, as
long as it’s permitted by license.

## Using the modules

After modules are configured, those are used similarly to Bookmap internal functionality.

### 1. Using `Replay` modules

Select a file the same way you would open a Bookmap feed file. Note, that since the extension of
your file
will likely be different from `.bmf`, you should first type “*” into the file name field and press
enter -
this will reset the file type filter and will allow you to load any type of file.

### 2. Using `Live` modules

Navigate to _Connections -> Configure_, and select your module from the _Platform_ dropdown.  Fill username,
password fields, connect the same way you would do with any other connection.
If you don’t need username or password just leave those empty - actual use of that data will be
defined by your code.
If you need to place some additional data like server address, you can place it into one of those
fields. E.g. user “u1” at server “127.0.0.1” could be set as u1@127.0.0.1 inside “username” field.

## Environment setup

Check out [IDE and tricks](https://github.com/BookmapAPI/DemoStrategies#ide-and-tricks)
for more details about running your project from IDE - the process and the main caveats are similar.

### Modifying the example project

The repository contains a gradle project which you can import into an IDE of your choice.
Make sure you have Bookmap installed (using the default path will make things a bit more simple),
then import the example `Layer0ApiDemo` project as a gradle project.
Use `gradle build` command for building the classes, or `gradle jar` to build the jar file (
available
in `build/libs/`)

### Creating your own project

For your own project, you can copy the build.gradle file to a new folder and create `src\main\java`
and `src\main\resources` folders near it.
If you don’t want to use gradle, you can just use API (either from `C:\Program
Files\Bookmap\lib\bm-l1api.jar` or downloaded from the repository) as a compile-time
dependency and build classes or jar file in any other way.


