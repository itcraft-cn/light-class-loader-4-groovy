# 使用手册

[中文](MANUAL.md) | [English](MANUAL_en.md)

本文档描述 `GroovyHiddenClassLoader` 的工作原理、使用方式与约束条件。前置概念见 [README.md](README.md)，版本变更见 [CHANGELOG.md](CHANGELOG.md)。

## 1. 工作原理

```mermaid
flowchart TD
    A[Groovy 源码文本] --> B[genSourceCacheKey<br/>MD5(scriptText + codeSource)]
    B --> C{sourceCache 命中?}
    C -- 是 --> D[返回缓存 Class 实例]
    C -- 否 --> E[CompilationUnit 编译<br/>至 CLASS_GENERATION]
    E --> F[AnotherClassCollector<br/>classgen 回调]
    F --> G[MethodHandles.Lookup<br/>defineHiddenClass<br/>NESTMATE + STRONG]
    G --> H[写入 sourceCache<br/>与类缓存 setClassCacheEntry]
    H --> D
```

关键点：

1. `parseClass(String, String)` 重写了父类实现，先算 MD5 缓存键，未命中才真正编译。
2. 编译产物不再走 `ClassLoader#defineClass`，而是在 `AnotherClassCollector.createClass` 中调用 `lookup.defineHiddenClass`。
3. `NESTMATE` 使隐藏类与锚点类互为 nestmate，可访问锚点类的包私有成员；`STRONG` 保证其不被提前回收。

## 2. 使用方式

```java
CompilerConfiguration config = new CompilerConfiguration();
config.setSourceEncoding("UTF-8");

GroovyClassLoader loader =
        new GroovyHiddenClassLoader(Thread.currentThread().getContextClassLoader(), config, false);

Class<?> clazz = loader.parseClass(scriptText, "demo.groovy");
Object instance = clazz.getDeclaredConstructor().newInstance();
```

推荐的脚本形态（完整类定义，见 `GroovyHiddenClassLoaderTest`）：

```groovy
package cn.itcraft.ch4gn.groovy
class demo1 implements cn.itcraft.ch4gn.groovy.GroovyHiddenClassLoaderTest$DemoInterface {
    def boolean demo(String name, int age){ name.length() > 5 && age > 12 }
}
```

## 3. 局限性

### 3.1 必须是完整的 Groovy 类

传入的源码必须是一段完整的 Groovy（完整的类定义）。单纯一个方法体是无法编译的，例如：

```groovy
def judge(name, age) { name.length() > 5 && age > 12 }
```

这种写法会编译失败，运行期报 **类无法初始化** 的错误。原因是隐藏类必须有一个明确的类名与类节点才能被 `defineHiddenClass` 定义，裸方法片段不产生可用的主类。

### 3.2 package 必须与锚点一致

由于使用了 Hidden Class 特性，隐藏类需要一个锚点（anchor）所在的类加载器与查找上下文，因此脚本的 `package` 必须与锚点保持一致：

```
cn.itcraft.ch4gn.groovy
```

其中 `ch4gn` 是 `class-hidden-4-groovy-new` 的缩写（ch4gn = class-hidden 4 groovy new）。包名不一致时无法完成定义，会抛出类初始化相关的异常。

## 4. 作用与定位

### 4.1 作用

- 利用 **hidden 模式** 创建更轻量的 class：隐藏类不进入类加载器的类型查找表，不产生可被常规反射枚举到的命名类型，元空间占用更小。
- 实现 **同名代码的热替换**：每次编译都生成一个新的隐藏类实例，同名类可多版本并存，从而在不重启的前提下替换同名类中的逻辑。

### 4.2 生命周期的取舍

在生命周期上，本方案 **不如 JDK8 下的 lambda class**：JDK 8 的 `LambdaMetafactory` 生成的类由 `ClassLoader` 持有，生命周期与类加载器绑定、随类加载器整体回收；而隐藏类的存续依赖 `MethodHandles.Lookup` 与调用方的强引用，`STRONG` 选项下虽不会被提前回收，但缺少 JDK 8 lambda 那样清晰的"随宿主类加载器批量卸载"的语义。因此在需要长期复用、批量回收的场景中，其生命周期管理弱于 JDK 8 的 lambda class。

## 5. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 编译期报"类无法初始化" | 源码不是完整类，例如只有 `def judge(...)` | 补全为完整的 `class X { ... }` 定义 |
| 定义失败/初始化异常 | `package` 与锚点 `cn.itcraft.ch4gn.groovy` 不一致 | 将脚本 package 改为锚点包 |
| 同一源码反复占用元空间 | 绕过了 `parseClass` 缓存 | 统一走 `parseClass`，复用 MD5 缓存键 |

## 6. 相关文档

- [README.md](README.md)
- [CHANGELOG.md](CHANGELOG.md)
- [MANUAL_en.md](MANUAL_en.md)
