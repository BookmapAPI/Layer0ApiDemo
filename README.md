# Bookmap Layer 0 API

## Overview
L0 API serves two main purposes:
* Replay your own file format with Bookmap.
* Connect Bookmap to your own datasource in real time (both for receiving data and trading)

Doing so requires basic Java knowlege, however file replay example that is available in this repository uses simple text format which you can convert data to using your favourite tools.

## Documentation

It's recommended to begin with this [getting started guide](doc/Layer0APIGettingstarted.pdf). **Please note, that running from IDE is described for 6.1. In 7.0 you should add to classpath Bookmap.jar+all .jar files in ./lib folder instead of BookMap.exe**

Also after you install Bookmap javadoc will be available in C:\Program Files\Bookmap\lib\bm-l1api-javadoc.jar (or C:\Program Files (x86)\Bookmap\lib\bm-l1api-javadoc.jar for x86 version).

## Changes

Bookmap 7.0.0 b32 - **[BREAKING CHANGE]** Use getters instead of direct access to certain fields of builders. Commit 63321ec1 of this repository is the latest fully compatible with 6.1.

Bookmap 6.1.0 b28 - API introduced.
