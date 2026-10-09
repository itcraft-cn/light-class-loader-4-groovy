# 使用手册 (MANUAL)

[English](MANUAL_en.md) | 中文

返回 [README](README.md) | 相关：[CHANGELOG](CHANGELOG.md)

## 1. 概述

`GroovyLambdaClassLoader` 是 `groovy.lang.GroovyClassLoader` 的子类，核心差异在于：编译产生的字节码不再走 `ClassLoader#defineClass`，而是在 `CompilationUnit.ClassgenCallback` 回调中通过 `sun.misc.Unsafe#defineAnonymousClass`（以 `AnonymousGroovyClassHolder` 为宿主类）直接定义。

**作用与价值**：

- **利用 Lambda 的生命周期**：产物以匿名类方式定义（与 lambda metafactory 产物同机制），其生命周期跟随引用而非永久驻留，类卸载更自然，配合源码缓存可显著减轻元空间压力
- **同名代码热替换**：匿名类不受“类名不可重复定义”的常规限制，同名脚本反复编译即可得到新 Class 实现，天然支持同名代码的热替换（规则/策略频繁变更的场景）

适用场景：需要在 JVM 内大量、反复动态生成 Groovy 类（如脚本热加载、规则引擎），希望减少常规类加载路径开销与元空间压力的场合。

## 2. 环境与依赖

| 项 | 版本 |
| --- | --- |
| JDK | 8（`source/target=8`，依赖 `sun.misc.Unsafe`） |
| Apache Groovy | 4.0.4 |
| slf4j-api | 1.7.35 |
| log4j2（runtime） | 2.17.1 |
| JUnit Jupiter（test） | 5.9.0 |
| JMH（test） | 1.35 |

> 注意：`Unsafe#defineAnonymousClass` 在 JDK 9+ 被移除/受限，本项目面向 JDK 8。

## 3. API 说明

### 3.1 构造器

```java
public GroovyLambdaClassLoader(ClassLoader parent, CompilerConfiguration config,
                               boolean useConfigurationClasspath)
```

- `parent`：父类加载器
- `config`：编译配置；传 `null` 时回退到 `CompilerConfiguration.DEFAULT`
- `useConfigurationClasspath`：是否把配置中的 classpath 加入搜索路径

### 3.2 parseClass(String text, String fileName)

重写自 `GroovyClassLoader`，流程：

1. 在特权上下文中构造 `GroovyCodeSource`，并 `setCachable(false)`（缓存由本类自行管理）
2. 生成缓存 key：`MD5("scriptText:" + scriptText + "/codeSource:" + cs)`；脚本文本为空时退化为 `MD5("name:" + name)` 以触发校验且不缓存
3. 命中 `sourceCache` 直接返回；未命中执行 `doParseClass` 编译

### 3.3 doParseClass(GroovyCodeSource)

1. 创建 `CompilationUnit`；`recompileGroovySource=true` 时在 CLASS_GENERATION 阶段前注册 `TimestampAdder`
2. 注册 `AnotherClassCollector` 作为 classgen 回调
3. 编译到 `Phases.CLASS_GENERATION`（配置了 `targetDirectory` 则到 `Phases.OUTPUT`）
4. 回调中对每个 `ClassNode` 的字节码（可选经过 `BytecodePostprocessor`）调用 `UNSAFE.defineAnonymousClass(AnonymousGroovyClassHolder.class, bytes, null)`
5. 依次 `definePackage`、`setClassCacheEntry`，返回主类（main class）

### 3.4 辅助类

| 类 | 作用 |
| --- | --- |
| `UnsafeUtil` | 反射获取 `Unsafe.theUnsafe` 单例 |
| `ForbiddenClassInitializer` | `AccessController.doPrivileged` 封装，异常仅告警并返回 `null` |
| `AnonymousGroovyClassHolder` | 匿名类定义的宿主（loader/定义位置）类 |
| `TimestampAdder` | 为非接口、非内部类注入 `__timeStamp` 及时间戳命名字段 |
| `AnotherClassCollector` | 收集编译产物并用 Unsafe 定义类 |

## 4. 使用示例

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

要点：

- 每次生成的类名需唯一（可用 `System.nanoTime()` 后缀），避免与已定义类冲突
- 生成的类可向上转型为共享接口，实现脚本与宿主之间的类型安全调用

## 5. 构建与测试

```bash
mvn -q compile          # 编译
mvn -q test             # 运行测试（surefire 3.2.5，forkCount=4，reuseForks=false）
mvn -q package          # 打包 target/class-lambda-4-groovy4-1.0-SNAPSHOT.jar
```

测试源目录：`src/test/java` 与 `src/test/groovy`（由 `build-helper-maven-plugin` 追加）。

## 6. 注意事项与限制

1. **脚本必须是完整的 Groovy 类**：源码必须是完整的类定义（如 `class demoX implements ... { ... }`）。单纯一个方法、类似 `def judge(name,age){name.length() > 5 && age > 12}` 的片段**不能编译**，会报“类无法初始化”类错误——因为编译产物需包含可定义的 `ClassNode`，孤立方法不构成合法的主类
2. **JDK 版本**：依赖 `sun.misc.Unsafe#defineAnonymousClass`，JDK 9+ 不可用；请在 JDK 8 上运行
3. **SecurityManager**：`setAccessible(true)` 获取 Unsafe 需要相应权限；受限环境可能返回 `null` 并记录告警
4. **缓存**：相同源码 + codeSource 返回同一 Class 实例；类名随机化（如 nanoTime）会使源码不同，从而各自缓存
5. **元空间**：匿名类仍占用元空间，缓存仅防止同一源码的重复定义
6. **异常处理**：`ForbiddenClassInitializer` 捕获所有异常仅打 warn 日志并返回 `null`，调用方需留意日志

## 7. 相关文档

- [README.md](README.md)
- [CHANGELOG.md](CHANGELOG.md)
- [MANUAL_en.md](MANUAL_en.md)
