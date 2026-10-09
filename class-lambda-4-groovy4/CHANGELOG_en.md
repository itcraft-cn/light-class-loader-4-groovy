# Changelog

All notable changes to this project are documented here. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

English | [中文](CHANGELOG.md)

## [1.0-SNAPSHOT] - 2026-10-09

### Added

- **feat**: Added `GroovyLambdaClassLoader` for Groovy4, defining compilation output via `Unsafe#defineAnonymousClass` (5961af4)
  - Source-code MD5 based class cache; identical scripts are not recompiled and do not repeatedly occupy Metaspace
  - `TimestampAdder`: injects `__timeStamp` fields when `recompileGroovySource` is enabled
  - Utility classes `UnsafeUtil` and `ForbiddenClassInitializer` (wrapped with `AccessController.doPrivileged`)
- **test**: Added `GroovyLambdaClassLoaderTest` verifying dynamic class generation and interface method invocation (5961af4)

### Changed

- **chore**: Adjusted `pom.xml` to support running tests under mvn/mvnd (5961af4)
  - surefire 3.2.5 with `forkCount=4`, `reuseForks=false`, `-Xms/-Xmx 1024M`
  - `build-helper-maven-plugin` adds `src/test/groovy` as an extra test source directory

