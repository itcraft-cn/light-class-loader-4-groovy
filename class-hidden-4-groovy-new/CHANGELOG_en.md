# Changelog

[English](CHANGELOG_en.md) | [中文](CHANGELOG.md)

This project follows [Semantic Versioning](https://semver.org/) and [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## 1.0-SNAPSHOT

### 2026-10-09

- **feat**: add `GroovyHiddenClassLoader`, a Hidden Class based loader for Groovy 6.x (`5961af4`)
  - defines classes via `MethodHandles.Lookup#defineHiddenClass` with `NESTMATE` + `STRONG` options
  - source-level caching keyed on the MD5 of script text plus CodeSource, avoiding repeated Metaspace usage
  - injects `TimestampAdder` to add the `__timeStamp` field, matching default Groovy behaviour
  - adds `pom.xml` (JDK 17 / Groovy 6.0.0 / JUnit 5 / JMH)
- **test**: add `GroovyHiddenClassLoaderTest` covering hidden class compilation, instantiation and interface invocation (`5961af4`)

## Related documents

- [README_en.md](README_en.md)
- [MANUAL_en.md](MANUAL_en.md)
