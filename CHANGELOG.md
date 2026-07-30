<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Nginx Companion Changelog

## [Unreleased]

## [0.1.2]

### Fixed

- Syntax highlighting never rendered in the editor: the bundled
  TextMate plugin's own `FileType` also implements
  `FileTypeIdentifiableByVirtualFile` (the platform's highest-priority
  file-type resolution tier) and, being bundled, registered before this
  plugin — winning the resolution race every time for `.conf` files.
  `NginxFileType` now also implements
  `FileTypeIdentifiableByVirtualFile` with `order="first"` in its
  `plugin.xml` registration, so it wins the race deterministically. Real
  syntax coloring now renders for the first time.

## [0.1.1]

### Added

- Gap Hunter Labs brand icon (`pluginIcon.svg` / `pluginIcon_dark.svg`).

## [0.1.0]

### Added

- Syntax highlighting for nginx config: comments, strings, `$variables`,
  braces, and known directives (colored as keywords), backed by a
  hand-rolled lexer — no grammar library needed for a syntax this small.
- Directive-name completion from the real, official nginx directive
  catalog (940 directives, 94 modules, sourced from nginx.org's own
  documentation index).
- Content-based file detection: the plugin only treats a `.conf` file (or
  `nginx.conf`, `mime.types`, etc.) as nginx config when its content
  actually looks like one. It never claims every `.conf` file on disk.

[Unreleased]: https://github.com/GapHunterLabs/nginx-companion/compare/0.1.2...HEAD
[0.1.2]: https://github.com/GapHunterLabs/nginx-companion/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/GapHunterLabs/nginx-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/nginx-companion/commits/0.1.0
