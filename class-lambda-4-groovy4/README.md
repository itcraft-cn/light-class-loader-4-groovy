# class-lambda-4-groovy4

> 基于 `Unsafe.defineAnonymousClass` 的 Groovy4 类加载器：以“匿名类（类 Lambda）”方式定义 Groovy 编译产物，避免占用 Metaspace 的常规类加载开销。

[English](README_en.md) | 中文

## 简介

`class-lambda-4-groovy4` 提供 `GroovyLambdaClassLoader`，它继承自 `groovy.lang.GroovyClassLoader`，在编译回调中使用 `sun.misc.Unsafe#defineAnonymousClass` 定义生成的字节码，而非传统的 `ClassLoader#defineClass`。同时内置按源码 MD5 的类缓存，避免相同脚本重复编译、反复占用元空间。

## 特性

- **Unsafe 匿名类定义**：通过 `Unsafe#defineAnonymousClass` 加载 Groovy 编译产物
- **源码级缓存**：以 `scriptText + codeSource` 的 MD5 作为缓存 key，相同源码复用同一 Class 实例
- **时间戳字段注入**：`recompileGroovySource` 开启时注入 `__timeStamp` 字段，保证重复编译产物可区分
- **特权代码执行**：借助 `AccessController.doPrivileged` 封装受限初始化逻辑

## 环境要求

- JDK 8（`maven.compiler.source/target = 8`，依赖 `sun.misc.Unsafe`）
- Maven 3.x（或 mvnd）
- Apache Groovy 4.0.x

## 快速开始

```java
CompilerConfiguration config = new CompilerConfiguration();
config.setSourceEncoding("UTF-8");

GroovyClassLoader loader = new GroovyLambdaClassLoader(
        Thread.currentThread().getContextClassLoader(), config, false);

Class<?> clazz = loader.parseClass(
        "class demo1 implements com.example.DemoInterface {\n" +
        "  def boolean demo(String name, int age){ name.length() > 5 && age > 12 }\n" +
        "}\n");

DemoInterface demo = (DemoInterface) clazz.newInstance();
demo.demo("abcdefgh", 31);
```

## 构建与测试

```bash
mvn -q compile
mvn -q test
mvn -q package
```

## 文档

- 使用手册：[MANUAL.md](MANUAL.md)
- 更新日志：[CHANGELOG.md](CHANGELOG.md)

## 许可证

[Apache License 2.0](../LICENSE)
