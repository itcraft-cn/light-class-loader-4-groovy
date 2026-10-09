# light-class-loader-4-groovy

English | [中文](README.md)

An experimental repository that loads Groovy compilation output in a lighter-weight way on the JVM. It contains two independent sub-projects, each exploring a different technique: anonymous classes (Lambda style) on JDK 8, and hidden classes on JDK 9+.

## Sub-projects

| Directory | Mechanism | JDK | Groovy | Description |
| --- | --- | --- | --- | --- |
| [class-lambda-4-groovy4](class-lambda-4-groovy4/README_en.md) | `Unsafe#defineAnonymousClass` | 8 | 4.0.x | Defines compilation output as anonymous classes to ease Metaspace usage |
| [class-hidden-4-groovy-new](class-hidden-4-groovy-new/README_en.md) | `MethodHandles.Lookup#defineHiddenClass` | 17+ | 6.0.0 | Hidden classes stay out of the type lookup table and support hot-swapping same-named classes |

Design traits they share:

- **Source-level caching**: keyed on the MD5 of script text plus CodeSource, so identical sources reuse the same `Class` instance and Metaspace does not bloat
- **Compilation-callback injection**: bytecode is defined inside the `CompilationUnit` classgen callback instead of `ClassLoader#defineClass`
- **Shared-interface invocation**: script classes are up-cast to host interfaces for type-safe calls

## Quick Start

The sub-projects are independent; build each one in its own directory:

```bash
cd class-lambda-4-groovy4 && mvn -q test
cd ../class-hidden-4-groovy-new && mvn -q test
```

## Documentation

- Changelog: [CHANGELOG_en.md](CHANGELOG_en.md)

## License

[Apache License 2.0](LICENSE)
