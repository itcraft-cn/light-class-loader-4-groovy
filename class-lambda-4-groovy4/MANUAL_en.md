# User Manual (MANUAL)

English | [中文](MANUAL.md)

Back to [README_en](README_en.md) | See also: [CHANGELOG_en](CHANGELOG_en.md)

## 1. Overview

`GroovyLambdaClassLoader` is a subclass of `groovy.lang.GroovyClassLoader`. Its core difference: instead of going through `ClassLoader#defineClass`, the compiled bytecode is defined directly inside the `CompilationUnit.ClassgenCallback` via `sun.misc.Unsafe#defineAnonymousClass` (with `AnonymousGroovyClassHolder` as the host class).

**Purpose & value**:

- **Leverages the lambda lifecycle**: artifacts are defined as anonymous classes (same mechanism as lambda metafactory output); their lifetime follows references rather than residing permanently, unloading is more natural, and combined with the source cache it significantly reduces Metaspace pressure
- **Hot-swapping same-named code**: anonymous classes are not subject to the usual "a class name may not be defined twice" restriction — recompiling an identically named script yields a new `Class` implementation, which naturally supports hot-swapping of same-named code (rules/strategies that change frequently)

Use cases: frequently and repeatedly generating Groovy classes inside the JVM (script hot-reload, rule engines) where you want to reduce regular class-loading overhead and Metaspace pressure.

## 2. Environment & Dependencies

| Item | Version |
| --- | --- |
| JDK | 8 (`source/target=8`, depends on `sun.misc.Unsafe`) |
| Apache Groovy | 4.0.4 |
| slf4j-api | 1.7.35 |
| log4j2 (runtime) | 2.17.1 |
| JUnit Jupiter (test) | 5.9.0 |
| JMH (test) | 1.35 |

> Note: `Unsafe#defineAnonymousClass` was removed/restricted in JDK 9+; this project targets JDK 8.

## 3. API Reference

### 3.1 Constructor

```java
public GroovyLambdaClassLoader(ClassLoader parent, CompilerConfiguration config,
                               boolean useConfigurationClasspath)
```

- `parent`: parent class loader
- `config`: compiler configuration; `null` falls back to `CompilerConfiguration.DEFAULT`
- `useConfigurationClasspath`: whether to add the configured classpath to the search path

### 3.2 parseClass(String text, String fileName)

Overrides `GroovyClassLoader`, flow:

1. Build a `GroovyCodeSource` in a privileged context and `setCachable(false)` (caching is managed by this class itself)
2. Generate the cache key: `MD5("scriptText:" + scriptText + "/codeSource:" + cs)`; when script text is empty it degrades to `MD5("name:" + name)` to trigger validation and skip caching
3. Return on `sourceCache` hit; otherwise run `doParseClass` to compile

### 3.3 doParseClass(GroovyCodeSource)

1. Create a `CompilationUnit`; if `recompileGroovySource=true`, register `TimestampAdder` before the CLASS_GENERATION phase
2. Register `AnotherClassCollector` as the classgen callback
3. Compile up to `Phases.CLASS_GENERATION` (or `Phases.OUTPUT` when `targetDirectory` is set)
4. In the callback, for each `ClassNode`'s bytecode (optionally processed by `BytecodePostprocessor`), call `UNSAFE.defineAnonymousClass(AnonymousGroovyClassHolder.class, bytes, null)`
5. Perform `definePackage` and `setClassCacheEntry` for each class, then return the main class

### 3.4 Helper Classes

| Class | Purpose |
| --- | --- |
| `UnsafeUtil` | Reflectively obtains the `Unsafe.theUnsafe` singleton |
| `ForbiddenClassInitializer` | Wraps `AccessController.doPrivileged`; logs a warning and returns `null` on exceptions |
| `AnonymousGroovyClassHolder` | Host (loader/definition site) class for anonymous class definition |
| `TimestampAdder` | Injects `__timeStamp` and timestamp-named fields into non-interface, non-inner classes |
| `AnotherClassCollector` | Collects compilation output and defines classes via Unsafe |

## 4. Usage Example

```java
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;

CompilerConfiguration config = new CompilerConfiguration();
config.setSourceEncoding("UTF-8");

GroovyClassLoader loader = new GroovyLambdaClassLoader(
        Thread.currentThread().getContextClassLoader(), config, false);

Class<?> clazz = loader.parseClass(
        "class demo" + System.nanoTime() + " implements com.example.DemoInterface {\n"
      + "  def boolean demo(String name, int age){ name.length() > 5 && age > 12 }\n"
      + "}\n");

DemoInterface demo = (DemoInterface) clazz.newInstance();
boolean ret = demo.demo("abcdefgh", 31);
```

Key points:

- Each generated class name must be unique (e.g. a `System.nanoTime()` suffix) to avoid clashing with already-defined classes
- Generated classes can be cast to a shared interface for type-safe calls between scripts and the host

## 5. Build & Test

```bash
mvn -q compile          # compile
mvn -q test             # run tests (surefire 3.2.5, forkCount=4, reuseForks=false)
mvn -q package          # package target/class-lambda-4-groovy4-1.0-SNAPSHOT.jar
```

Test source directories: `src/test/java` and `src/test/groovy` (added by `build-helper-maven-plugin`).

## 6. Caveats & Limitations

1. **Scripts must be complete Groovy classes**: the source must be a full class definition (e.g. `class demoX implements ... { ... }`). A bare method fragment such as `def judge(name,age){name.length() > 5 && age > 12}` **cannot be compiled** and fails with a "class cannot be initialized"-style error — the compilation output must contain a definable `ClassNode`, and an orphan method does not form a valid main class
2. **JDK version**: relies on `sun.misc.Unsafe#defineAnonymousClass`, unavailable on JDK 9+; run on JDK 8
3. **SecurityManager**: obtaining Unsafe via `setAccessible(true)` requires appropriate permissions; restricted environments may yield `null` with a warning logged
4. **Caching**: identical source + codeSource returns the same `Class` instance; randomized class names (e.g. nanoTime) make the source differ, so each is cached separately
5. **Metaspace**: anonymous classes still occupy Metaspace; caching only prevents redefinition of the same source
6. **Exception handling**: `ForbiddenClassInitializer` catches all exceptions, logs a warning, and returns `null` — watch the logs

## 7. Related Documents

- [README_en.md](README_en.md)
- [CHANGELOG_en.md](CHANGELOG_en.md)
- [MANUAL.md](MANUAL.md)
