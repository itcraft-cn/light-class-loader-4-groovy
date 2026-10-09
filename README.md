# light-class-loader-4-groovy

[English](README_en.md) | 中文

以更轻量的方式在 JVM 中动态加载 Groovy 编译产物的实验性仓库，包含两个相互独立的子项目，分别探索 JDK 8 的匿名类（Lambda 风格）与 JDK 9+ 的隐藏类两条技术路线。

## 子项目

| 目录 | 机制 | JDK | Groovy | 说明 |
| --- | --- | --- | --- | --- |
| [class-lambda-4-groovy4](class-lambda-4-groovy4/README.md) | `Unsafe#defineAnonymousClass` | 8 | 4.0.x | 以匿名类方式定义编译产物，缓解 Metaspace 占用 |
| [class-hidden-4-groovy-new](class-hidden-4-groovy-new/README.md) | `MethodHandles.Lookup#defineHiddenClass` | 17+ | 6.0.0 | 隐藏类不进入类型查找表，支持同名类热替换 |

两者共同的设计：

- **源码级缓存**：以脚本文本 + CodeSource 的 MD5 为键，相同源码复用同一 `Class` 实例，防止元空间膨胀
- **编译回调注入**：在 `CompilationUnit` 的 classgen 回调中定义字节码，而非走 `ClassLoader#defineClass`
- **共享接口调用**：脚本类向上转型为宿主接口，保证类型安全

## 快速开始

各子项目相互独立，进入目录后分别构建：

```bash
cd class-lambda-4-groovy4 && mvn -q test
cd ../class-hidden-4-groovy-new && mvn -q test
```

## 文档

- 更新日志：[CHANGELOG.md](CHANGELOG.md)

## 许可证

待定（以仓库实际声明为准）。
