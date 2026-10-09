# 更新日志

本仓库的所有重要变更均记录于此。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，提交遵循[约定式提交](https://www.conventionalcommits.org/zh-hans/v1.0.0/)。

[English](CHANGELOG_en.md) | 中文

## [1.0-SNAPSHOT] - 2026-10-09

### 新增

- **feat**: 导入两个 Groovy 类加载器子项目的源码与 Maven 构建（`5961af4`）
  - `class-lambda-4-groovy4`：基于 `Unsafe#defineAnonymousClass` 的 Groovy 4 类加载器
  - `class-hidden-4-groovy-new`：基于 `MethodHandles.Lookup#defineHiddenClass` 的 Groovy 6.x 类加载器
- **chore**: 初始化仓库配置与 AI 指南文件（`73c3b32`）
- **docs**: 建立根目录双语文档布局：README / CHANGELOG 中英互链
- **docs**: 引入 [Apache License 2.0](LICENSE)，各 README 许可声明同步更新（`d9d6932`）

### 说明

- 子目录内各自的 README / CHANGELOG / MANUAL 六文件布局保持原样
- 子项目 CHANGELOG 中的提交哈希均指向上方导入提交（源仓库历史不再可读）

## 相关文档

- [README.md](README.md)
