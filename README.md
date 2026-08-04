# Nginx Companion

IntelliJ-family plugin. Syntax highlighting and directive completion for
nginx config files.

## Why it exists

Born from real evidence in JetBrains Marketplace reviews, not
assumptions: the leading nginx plugin's paid tier (254K+ downloads) has
recent complaints of locking up the IDE "even on non-nginx related
edits," and of the free tier repeatedly prompting for analytics/tracking
consent on every startup regardless of how many times the user declines.
The historical free alternative has been abandoned since 2019.

## Why built this way

- **Content-based detection, not extension-based.** `.conf` is used by
  Apache, Redis, systemd, and dozens of other tools. The plugin only
  treats a file as nginx config when its content actually looks like
  nginx syntax (brace blocks + semicolon-terminated statements +
  nginx-specific directive names, and explicitly not Apache's
  `<VirtualHost>`-style syntax) — never by claiming every `.conf` file on
  disk. That is the direct fix for "hijacks files it shouldn't."
- **A hand-rolled lexer, not a grammar library.** nginx's syntax is small
  and stable: comments, quoted strings, `$variables`, braces, semicolons,
  and bare words. A manual scanner is simpler and lighter than pulling in
  a parser-generator dependency for a grammar this size.
- **Completion from the real directive catalog**, not a hand-picked
  shortlist — parsed from nginx.org's own documentation index (940
  directives across 94 modules), so suggestions are directives that
  actually exist.
- **No network access, no analytics, no license prompts.** Free, and
  built specifically not to repeat the incumbent's "won't stop asking"
  complaint.

## Usage

Open any file that looks like an nginx config (`nginx.conf`, files under
`sites-available/`/`conf.d/`, etc.) and directive names get highlighted
and auto-completed automatically — no configuration needed.

## Enterprise / Team Licensing

Need enterprise features, custom directive catalogs, or team licensing?
Contact us at **kennyj.diazm@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
