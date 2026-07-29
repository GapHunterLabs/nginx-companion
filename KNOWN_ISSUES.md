# Known Issues

## Syntax highlighting didn't render in the editor (found and fixed, 2026-07-28, after 5 rounds of investigation)

**Symptom:** Opening a file that resolves to `NginxFileType` (either by
exact filename like `nginx.conf`, or by content detection like a file
named `site.conf` with nginx-shaped content) shows no syntax coloring at
all — not even braces, semicolons, comments, or strings. Only the
generic gray "unrecognized word" underline appears under `WORD` tokens.
Confirmed on:

- `./gradlew runIde` sandbox, IntelliJ IDEA `2025.2.6.2` (the Gradle
  build's `intellijIdea()` target)
- A real, separately-installed IntelliJ IDEA `2026.2` (plugin installed
  from disk via `Install Plugin from Disk...`)

Same result in both — this rules out "sandbox-only artifact" and
"IDE version mismatch" as the sole cause.

### What's confirmed working (via temporary debug logging, since removed)

- `NginxFileTypeOverrider.detect()` **is** called for both file-open
  paths (initial project scan and on-demand when a tab opens), and
  **does** return `NginxFileType` correctly. Confirmed via
  `thisLogger().warn(...)` inside `detect()` across multiple sandbox
  runs — never returns null for either `nginx.conf` or a
  content-detected `site.conf`.
- `NginxConfigDetector.isNginxConfig()` unit tests all pass — the
  content heuristic itself is correct in isolation.
- The `FileType` shown in `Settings > Editor > File Types` for these
  files is genuinely ours — its description
  (`NginxFileType.getDescription()` = `"Nginx configuration file"`) is
  what appears in that list, which is easy to mistake for a conflicting
  third-party plugin's FileType (its `getName()` is the different
  string `"Nginx Config"`; Settings shows the description, not the
  name). **No other nginx-related plugin is installed** — confirmed via
  `Settings > Plugins` search.

### What's confirmed NOT working / never invoked

- `NginxSyntaxHighlighterFactory.getSyntaxHighlighter()` — instrumented
  with the same logging pattern; **zero invocations** logged across
  every sandbox run, including after reopening the file tab (Ctrl+F4 +
  reopen) once the project was already indexed.
- `NginxParserDefinition.createLexer()` — also instrumented, also zero
  invocations.
- `NginxFile` (the `PsiFileBase` subclass) — its `init` block was
  instrumented, also zero invocations. This is the strongest signal:
  **the PSI file for this VirtualFile is never actually built as an
  `NginxFile`**, despite `FileTypeManager` reporting the correct
  `FileType` for the file.

### What was tried and did NOT fix it

1. Deleting `.intellijPlatform/sandbox/` entirely between runs (rules
   out stale per-project file-type override state — a real but separate
   issue also found and worth keeping in mind, see below).
2. Testing with a differently-named file (`site.conf`, forcing the
   content-detection path instead of the `EXACT_FILENAMES` fast path) —
   identical failure, rules out anything specific to the exact-filename
   branch.
3. Bumping `NginxFileTypeOverrider.getVersion()` from `1` to `2` to
   force-invalidate the platform's file-type detection cache (the
   documented mechanism for this — see
   `../INTELLIJ_PLATFORM_KNOWLEDGE.md`) — no change in behavior, but
   kept as a real improvement regardless (was hardcoded at `1`
   forever, meaning any future detection-logic change would silently
   never invalidate old cached results without this bump existing as
   the template to increment again).
4. Registering an explicit `editorHighlighterProvider` extension point
   (`NginxEditorHighlighterProvider`, building a `LexerEditorHighlighter`
   directly instead of relying on the platform's automatic
   `Language` → `SyntaxHighlighterFactory` resolution) — compiled
   correctly, installed correctly (confirmed the class is present in the
   installed jar via `unzip -l`), no change in behavior. **Kept in the
   codebase** since it's the documented, more-explicit alternative path
   and doesn't hurt — but its lack of effect means the failure is
   upstream of highlighter resolution entirely (consistent with the
   `NginxFile` non-instantiation finding above).

### Separate, smaller issue also found along the way (worth fixing regardless)

`./gradlew runIde` reuses `.intellijPlatform/sandbox/` between runs.
Closing the sandbox IDE window does **not** clear this — a per-project
"Ignore extension" click (or similar file-type override) made in one
session persists into the next `runIde` launch, showing the Marketplace
"Plugins supporting *.conf files found" banner again on a file that
should already resolve correctly. Not the highlighting bug's root
cause (confirmed by testing on a genuinely fresh sandbox directory,
which showed the *same* highlighting failure with no banner at all),
but a real rough edge for anyone manually testing this plugin
repeatedly. Delete the sandbox directory for a truly clean state,
don't just close the window.

### Leading hypothesis (untested)

Since the PSI file itself never gets built as `NginxFile`, the fault is
likely somewhere between "platform resolves `VirtualFile` → `FileType`"
and "platform builds a `PsiFile` for that `VirtualFile` using the
`FileType`'s `Language`" — i.e. in `FileViewProviderFactory` resolution,
or a `FileViewProvider` subtlety specific to how `LanguageFileType` +
a from-scratch `ParserDefinition` (no Grammar-Kit, hand-rolled
`PsiParser` per `NginxParserDefinition`) interact with whatever the
platform's default `FileViewProviderFactory` expects. Worth comparing
against a minimal, known-working hand-rolled-lexer example plugin from
JetBrains' own SDK code samples to spot what's structurally different.

### Round 2 findings (2026-07-28, later session)

Two more things were tried and ruled out:

5. `ParserDefinition.createElement()` was throwing
   `UnsupportedOperationException` unconditionally (leftover from the
   "flat parser, no composite nodes" assumption — wrong assumption: the
   platform can call `createElement` regardless of grammar shape).
   **Fixed for real** — now returns `ASTWrapperPsiElement(node)`, the
   platform's own generic wrapper for exactly this case, matching the
   reference sample plugin's pattern (`SimpleNamedElementImpl extends
   ASTWrapperPsiElement`). This is a legitimate fix and stays in the
   codebase, but **did not resolve the highlighting bug** — reinstalled
   and retested, identical failure.
6. Compared line-by-line against JetBrains' own reference plugin
   (`JetBrains/intellij-sdk-code-samples/simple_language_plugin`), which
   has the exact same shape (custom `Language`, hand-rolled lexer, no
   Grammar-Kit for the base language). Structurally near-identical
   except one attribute: the reference plugin's `<fileType>` declares
   `extensions="simple"` (a static extension claim); ours declares none
   (intentional — the whole point of this plugin is not claiming `.conf`
   statically, unlike the incumbent it was built to fix). **Not tested
   as a fix** — the user declined to experiment with adding a static
   extension claim even temporarily, since it contradicts the plugin's
   core design goal. This remains the single most concrete structural
   difference found against a known-working reference, and is the
   strongest lead for whoever picks this up next.

**New instrumentation result, more precise than round 1:** with
`getFileNodeType()` also logged this round, it turned out to **be
called** (twice, same millisecond timestamp — suggesting two concurrent
index-related consumers) during the initial project scan/indexing phase
(`FileBasedIndexInfrastructureExtensionStartup` /
`UnindexedFilesScanner` in the log, before the editor tab even opens).
`createLexer`/`createParser`/`createFile`/`createElement` still never
fire. So the cutoff point is now narrower: somewhere between "platform
resolves `getFileNodeType()` during file-based indexing" and "platform
decides to build a lexer/parser/PSI for that node type."

Researched whether `IFileElementType` (non-stub) file element types can
silently fail inside `StubUpdatingIndex` — found general documentation
about `IStubFileElementType` being the class to extend for indexed
languages, but this plugin doesn't need stub indexing (no
go-to-symbol/find-usages yet) and the reference plugin also just uses
plain `IFileElementType` successfully, so this alone doesn't explain it
either. No further leads found via web research this round.

### Round 3 (2026-07-28, same day, later): `extensions="conf"` tested and ruled out

Tested for real, temporarily, purely as diagnosis (reverted immediately
after): added `extensions="conf"` to the `<fileType>` declaration,
matching the reference plugin's static-extension-claim pattern exactly.
**Result: identical failure.** Still no syntax highlighting, same gray
"unrecognized word" underline. This conclusively rules out "missing
static extension claim" as the cause — the reference plugin comparison
was a red herring, or the two plugins differ in some other way not yet
identified. Confirmed via log that the experimental build loaded
correctly (`Loaded custom plugins: Nginx Companion (0.1.1)`) before
concluding the negative result, so this isn't a "the build didn't
actually apply" false negative.

This narrows the problem further: it is **not** about static vs.
dynamic file type association at all. Whatever prevents
`createLexer`/`createFile` from firing after `getFileNodeType()` is
called happens regardless of how the `FileType` gets claimed.

### Round 4 (2026-07-28, separate session): persistent-cache hypothesis raised and conclusively ruled out

A new hypothesis surfaced while reading the platform's own
`FileTypeDetectionService.java` source directly (not just documentation):
`FileTypeDetector.getVersion()` bumps are documented (via reading the
class's cache-invalidation code) to **not** independently invalidate the
per-file detection cache — only a change to the *set of registered
detectors* triggers `onDetectorListChange()` → `clearCaches()`. This
contradicted Round 1's assumption (item 3 above) that the `getVersion()`
bump alone should have been a sufficient test, since a persistent VFS
`FileAttribute` (`AUTO_DETECTION_CACHE_ATTRIBUTE`) could in principle
outlive both a `getVersion()` bump and a deleted
`.intellijPlatform/sandbox/` directory if it's keyed by file content hash
or VirtualFile ID rather than by project-local sandbox state.

**Tested for real, under the cleanest possible conditions:**
1. Deleted `.intellijPlatform/sandbox/` entirely (403 MB, full reindex
   from scratch on relaunch — not just closing the window).
2. Created a genuinely new file (`site-checkout-cache-probe.conf`,
   content never seen by any prior sandbox, this exact filename never
   used in any previous test round) — rules out any VirtualFile-ID- or
   content-hash-keyed cache entry that could exist from earlier rounds.
3. Confirmed via `Settings > Plugins` (UI, not just log grep) that
   "Nginx Companion 0.1.1, Gap Hunter Labs" was genuinely installed and
   enabled in this exact sandbox session — the `idea.log` line
   `"Loaded custom plugins"` this project expected to grep for **did not
   appear at all** in this run, which is itself a new finding: that log
   line is not a reliable universal signal of plugin load status across
   all IDE versions/configurations, contradicting the "Lesson" already
   written in the K2-mode section of `AUTOMATION_PLAYBOOK.md` — UI
   confirmation (Settings > Plugins) is the more reliable check when the
   log line doesn't appear.

**Result: identical failure.** No syntax coloring at all, same generic
gray "unrecognized word" underline under the config's directive words.
This conclusively rules out any persistent-cache explanation — the
bug reproduces from the cleanest possible cold-start state, with a
file that has no possible cached detection history anywhere in the
system. The cutoff point identified in Round 2 (between
`getFileNodeType()` firing during indexing and `createLexer`/`createFile`
never firing) remains the most precise characterization of where the
failure actually happens — this round narrows the *cause* space further
(not caching) without identifying the actual cause.

**`NginxLexer` manually re-reviewed against this round's fresh context:**
extends `LexerBase` correctly, implements every abstract member, has no
external dependencies, includes a zero-length-token safety net
(`BAD_CHARACTER` fallback) to avoid infinite-looping the platform's
lexer-consistency checks. No structural issue found — consistent with
`createLexer` never being called at all (a broken lexer *would* fail
differently, e.g. by throwing once invoked, not by never being invoked
in the first place).

### Round 5 (2026-07-28, same session): root cause found — a bundled TextMate file type wins the resolution race, fixed with `FileTypeIdentifiableByVirtualFile` + `order="first"`

Round 4 left the investigation with a precise cutoff point (between
`getFileNodeType()` firing and `createLexer`/`createFile` never firing)
but no cause. This round finally found it by trying something no prior
round had: **an independent diagnostic check from outside the plugin's
own classes entirely.**

**The check that found it:** a temporary `FileEditorManagerListener`
(`FileTypeDiagnosticListener`, removed after use — not part of the
shipped plugin) that fires the instant a file tab opens and logs, in one
line, everything the platform *actually* resolved at that exact moment:
`FileTypeManager.getFileTypeByFileName()`, `FileTypeManager
.getFileTypeByFile()`, and `PsiManager.findFile()`'s resulting
`psiFile.fileType` / `psiFile.javaClass` / `psiFile.language`. Every
prior round only instrumented this plugin's own classes
(`NginxFileTypeOverrider`, `NginxParserDefinition`, `NginxFile`) — never
asked the platform directly "what did you actually build for this file,
right now."

**The result that broke the case open:**
```
DIAGNOSTIC file=nginx.conf getFileTypeByFileName=UNKNOWN
getFileTypeByFile=textmate psiFile.fileType=textmate
psiFile.javaClass=org.jetbrains.plugins.textmate.psi.TextMateFile
psiFile.language=textmate document!=null=true
```

The editor was building a **`TextMateFile`**, not an `NginxFile` — for
a `.conf` file that both `Settings > File Types` and our own
content-detection logging (correctly, but as it turns out irrelevantly)
reported as `Nginx Config`. Two independent, both-correct resolution
paths were disagreeing, and the editor's real PSI-building pipeline was
listening to the wrong one.

**Root cause, confirmed against platform source
(`FileTypeManagerImpl.java`, `TextMateFileType.java`):** the platform
resolves a file's type through tiers with different priority.
`FileTypeIdentifiableByVirtualFile` implementations are asked first (an
array called `specialFileTypes`, iterated in **registration order**,
first match wins — no tie-breaking by specificity or relevance); then
static extension mappings; then `FileTypeDetector` (`NginxFileTypeOverrider`,
what this plugin relied on exclusively) **last**. The bundled TextMate
plugin's `TextMateFileType` *also* implements
`FileTypeIdentifiableByVirtualFile` — its own `isMyFileType()` checks
`FileTypeManager.getFileTypeByFileName()` first and self-activates
whenever that returns `Unknown`/`PlainText`/TextMate itself, which is
exactly what `.conf` resolves to given this plugin deliberately doesn't
claim `.conf` as a static extension (see the "MOVE... not a gap"-shaped
design note elsewhere in this doc — same philosophy: opt-in by content,
never a blanket extension claim). Because TextMate is bundled into the
platform, it registers before any third-party plugin, so it was winning
the `specialFileTypes` race every single time, regardless of anything
in this plugin's own code being correct.

**First fix attempt, confirmed insufficient on its own:** making
`NginxFileType` also implement `FileTypeIdentifiableByVirtualFile`
(delegating to the existing `NginxConfigDetector.isNginxConfig()`).
Re-tested with the same diagnostic listener: **identical failure**, still
`textmate`. This put both file types in the same priority tier, but
registration-order still favored TextMate.

**Actual fix — both pieces required together:**
1. `NginxFileType` implements `FileTypeIdentifiableByVirtualFile`
   (`isMyFileType()` reads a bounded prefix of the file and delegates to
   the same detector `NginxFileTypeOverrider` already uses — cheap on
   purpose, since the platform calls this frequently).
2. `<fileType ... order="first"/>` in `plugin.xml` — the ordering
   attribute the extension point mechanism itself supports, forcing this
   plugin's entry to the front of `specialFileTypes` regardless of when
   TextMate registered.

**Verified with the same diagnostic listener, this time positive:**
```
DIAGNOSTIC file=nginx.conf getFileTypeByFileName=UNKNOWN
getFileTypeByFile=Nginx Config psiFile.fileType=Nginx Config
psiFile.javaClass=dev.gaphunter.nginxcompanion.lang.NginxFile
psiFile.language=Nginx document!=null=true
```
And confirmed visually in the editor: real syntax coloring appeared for
the first time across all 5 rounds — strings/values in orange, braces in
yellow, directive structure fully colored. The diagnostic listener was
then removed (it was explicitly temporary, never meant to ship).

**Lesson, the one that actually cracked this:** when platform-internal
classes are suspected of interfering with a plugin's own registration,
instrumenting only the plugin's own classes can leave a real bug
invisible indefinitely — all 4 prior rounds' instrumentation confirmed
this plugin's own code was being *reached* correctly (`getFileNodeType()`
fired, detection logged the right answer), which is exactly the kind of
evidence that misleadingly rules out "something's wrong in our
registration" while never revealing "something else registered in the
same priority tier and wins first." A diagnostic that queries the
platform's actual, current resolution state from *outside* the
suspected code path — not just logging inside it — is what surfaced the
conflict. Worth reaching for this pattern much earlier next time a
"platform apparently isn't calling my code" investigation stalls past
one round.
