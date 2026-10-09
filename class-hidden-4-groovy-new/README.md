# class-hidden-4-groovy-new

[English](README_en.md) | 中文

基于 JDK Hidden Class（`MethodHandles.Lookup#defineHiddenClass`）实现的 Groovy 类加载器，面向 Groovy 6.x 及以上版本，用于在 JVM 中编译并加载更轻量的动态类，且支持同名代码的热替换。

## 背景

Groovy 传统 `GroovyClassLoader` 通过自身 `defineClass` 将编译产物写入 Metaspace，类一旦定义便无法卸载与重定义，重复编译同名不同实现的脚本会持续占用元空间，也无法完成"同名类"的逻辑更新。

本项目改用 JDK 9+ 的 Hidden Class 机制：编译产物不进入 ClassLoader 的命名类型空间，仅通过 `MethodHandles.Lookup` 暴露，从而获得更轻量的类实例与天然的"同名多版本"能力。

## 特性

- **轻量类实例**：使用 `defineHiddenClass` 定义，不注册到类加载器的类型查找表，避免元空间被同名脚本反复占用。
- **同名热替换**：每次编译生成新的 hidden class 实例，同名类可并存，天然支持逻辑热更新。
- **源码级缓存**：按脚本文本 + CodeSource 的 MD5 作为缓存键，同一份源码复用同一 Class 实例，防止元空间膨胀。
- **Groovy 6.x 适配**：适配 `org.apache.groovy:groovy:6.0.0` 的 `CompilationUnit` 回调与 `groovyjarjarasm.asm` 字节码栈。

## 环境要求

| 项 | 版本 |
| --- | --- |
| JDK | 17+（依赖 Hidden Class API） |
| Groovy | 6.0.0（`org.apache.groovy:groovy`） |
| 构建工具 | Maven |

## 快速开始

```bash
mvn -q test
```

最小用例：

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

> 注意：脚本必须是完整的类定义，且 `package` 必须与锚点类一致。详见 [MANUAL.md](MANUAL.md)。

## 文档

- 使用手册：[MANUAL.md](MANUAL.md)
- 更新日志：[CHANGELOG.md](CHANGELOG.md)
- English version: [README_en.md](README_en.md)

## 项目结构

```
src/main/java/cn/itcraft/ch4gn/groovy/GroovyHiddenClassLoader.java  核心加载器
src/test/java/cn/itcraft/ch4gn/groovy/GroovyHiddenClassLoaderTest.java  用例验证
```

## 许可

内部项目，未经许可请勿外发。
