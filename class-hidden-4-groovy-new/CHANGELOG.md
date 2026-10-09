# 更新日志

[中文](CHANGELOG.md) | [English](CHANGELOG_en.md)

本项目遵循[语义化版本](https://semver.org/lang/zh-CN/)，提交信息遵循[约定式提交](https://www.conventionalcommits.org/zh-hans/v1.0.0/)。

## 1.0-SNAPSHOT

### 2026-10-09

- **feat**: 新增 `GroovyHiddenClassLoader`，面向 Groovy 6.x 的 Hidden Class 类加载器实现（`5961af4`）
  - 基于 `MethodHandles.Lookup#defineHiddenClass` 定义类，`NESTMATE` + `STRONG` 选项
  - 按脚本文本 + CodeSource 的 MD5 做源码级缓存，避免元空间重复占用
  - 注入 `TimestampAdder` 补充 `__timeStamp` 字段，保持与 Groovy 默认行为一致
  - 引入 `pom.xml`（JDK 17 / Groovy 6.0.0 / JUnit 5 / JMH）
- **test**: 新增 `GroovyHiddenClassLoaderTest`，验证隐藏类编译、实例化与接口调用（`5961af4`）

## 相关文档

- [README.md](README.md)
- [MANUAL.md](MANUAL.md)
