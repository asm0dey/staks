# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Simplified the implementation of out-of-order element processing by removing the handler-based approach
- Improved memory usage by adding support for limiting the cache size

### Removed
- Removed the handler-based architecture for processing XML events
- Removed support for complex out-of-order element processing scenarios (e.g., within list contexts)
- Removed explicit JVM 11+ compatibility badge from README

## [1.1.0] - 2023-04-07

[Initial release with handler-based architecture]

## [1.0.0] - 2023-03-15

[Initial public release]

[Unreleased]: https://github.com/asm0dey/staks/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/asm0dey/staks/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/asm0dey/staks/releases/tag/v1.0.0