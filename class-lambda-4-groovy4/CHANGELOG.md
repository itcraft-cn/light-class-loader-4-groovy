# 更新日志

本项目的所有重要变更均记录于此。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

[English](CHANGELOG_en.md) | 中文

## [1.0-SNAPSHOT] - 2026-10-09

### 新增

- **feat**: 新增面向 Groovy4 的 `GroovyLambdaClassLoader`，基于 `Unsafe#defineAnonymousClass` 定义编译产物（5961af4）
  - 按源码 MD5 的类缓存，避免相同脚本重复编译占用元空间
  - `TimestampAdder`：`recompileGroovySource` 开启时注入 `__timeStamp` 字段
  - 工具类 `UnsafeUtil`、`ForbiddenClassInitializer`（`AccessController.doPrivileged` 封装）
- **test**: 新增 `GroovyLambdaClassLoaderTest`，验证动态生成类并调用接口方法（5961af4）

### 变更

- **chore**: 调整 `pom.xml`，支持在 mvn/mvnd 下运行测试（5961af4）
  - surefire 3.2.5，`forkCount=4`、`reuseForks=false`、`-Xms/-Xmx 1024M`
  - `build-helper-maven-plugin` 追加 `src/test/groovy` 测试源目录

