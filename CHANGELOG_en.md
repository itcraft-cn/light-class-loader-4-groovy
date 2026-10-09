# Changelog

All notable changes to this repository are documented here. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), commits follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

[English](CHANGELOG_en.md) | 中文

## [1.0-SNAPSHOT] - 2026-10-09

### Added

- **feat**: Imported source code and Maven builds for the two Groovy class-loader sub-projects (`5961af4`)
  - `class-lambda-4-groovy4`: a Groovy 4 class loader based on `Unsafe#defineAnonymousClass`
  - `class-hidden-4-groovy-new`: a Groovy 6.x class loader based on `MethodHandles.Lookup#defineHiddenClass`
- **chore**: Initialized repository configuration and AI guide files (`73c3b32`)
- **docs**: Established the bilingual root documentation layout: README / CHANGELOG cross-linked in both languages
- **docs**: Introduced [Apache License 2.0](LICENSE); license statements in all READMEs updated accordingly (`d9d6932`)

### Notes

- The six-file README / CHANGELOG / MANUAL layout inside each sub-directory is kept as-is
- Commit hashes in the sub-project CHANGELOGs all point to the import commit above (the source repositories' history is no longer readable)

## Related documents

- [README_en.md](README_en.md)
