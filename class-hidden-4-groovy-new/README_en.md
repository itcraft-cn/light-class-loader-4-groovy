# class-hidden-4-groovy-new

English | [中文](README.md)

A Groovy class loader built on the JDK Hidden Class mechanism (`MethodHandles.Lookup#defineHiddenClass`), targeting Groovy 6.x and later. It compiles and loads lighter-weight dynamic classes in the JVM, with native support for hot-swapping code under the same class name.

## Background

The classic `GroovyClassLoader` defines compiled bytecode through its own `defineClass`. Once defined, a class can neither be unloaded nor redefined, so repeatedly compiling scripts with the same name but different implementations keeps consuming Metaspace, and logic updates under a same-named class are impossible.

This project instead uses the JDK 9+ Hidden Class feature: the compiled bytes never enter the loader's named type space and are only reachable through a `MethodHandles.Lookup`, which yields lighter class instances and a natural "multiple versions of one name" capability.

## Features

- **Lighter class instances**: defined via `defineHiddenClass`, never registered in the loader's type lookup tables, so Metaspace is not exhausted by same-named scripts.
- **Hot swap under the same name**: every compilation produces a new hidden class instance; versions coexist, enabling logic hot updates by design.
- **Source-level caching**: the MD5 of script text plus CodeSource is used as the cache key, so one source yields one Class instance and Metaspace growth is contained.
- **Groovy 6.x support**: adapted to the `CompilationUnit` callbacks and the `groovyjarjarasm.asm` bytecode stack of `org.apache.groovy:groovy:6.0.0`.

## Requirements

| Item | Version |
| --- | --- |
| JDK | 17+ (Hidden Class API) |
| Groovy | 6.0.0 (`org.apache.groovy:groovy`) |
| Build | Maven |

## Quick start

```bash
mvn -q test
```

Minimal example:

```java
CompilerConfiguration config = new CompilerConfiguration();
config.setSourceEncoding("UTF-8");
GroovyClassLoader loader =
        new GroovyHiddenClassLoader(Thread.currentThread().getContextClassLoader(), config, false);

Class<?> clazz = loader.parseClass(
        "package cn.itcraft.ch4gn.groovy\n"
      + "class demo1 implements cn.itcraft.ch4gn.groovy.GroovyHiddenClassLoaderTest$DemoInterface {\n"
      + "def boolean demo(String name, int age){name.length() > 5 && age > 12}\n"
      + "}");
```

> The script must be a complete class definition, and its `package` must match the anchor package. See [MANUAL_en.md](MANUAL_en.md).

## Documentation

- User manual: [MANUAL_en.md](MANUAL_en.md)
- Changelog: [CHANGELOG_en.md](CHANGELOG_en.md)
- 中文版本: [README.md](README.md)

## Project layout

```
src/main/java/cn/itcraft/ch4gn/groovy/GroovyHiddenClassLoader.java  core loader
src/test/java/cn/itcraft/ch4gn/groovy/GroovyHiddenClassLoaderTest.java  verification test
```

## License

[Apache License 2.0](../LICENSE)
