# Context: Project SIRIUS (Mass Spectrometry Bioinformatics)
SIRIUS covers a broad feature set for identifying small molecules from mass spectrometry data: molecular
formula annotation, structure database search, compound-class prediction, de novo structures, spectral
library search, LC-MS feature detection and transformation products.

The repository holds the algorithm, data-structure and storage libraries plus three front-ends over them:
the CLI (`sirius_cli`), the REST service (`sirius_rest_service`) and a desktop GUI (`sirius_gui`), which ship
together from `:sirius_dist`. `sirius_cli` is more than the command line — the REST service and the GUI both
depend on it, so the shared computation and persistence code lives there.

The GUI is mostly Java Swing; some views are rendered as JS in an embedded browser window (jxbrowser). Those
JS sources are not in this repo — they live in ../sirius-js-monorepo and are copied in pre-built. The GUI
gets its content via the API in `sirius_rest_service`.

## 1. Scientific Domain
SIRIUS works on high-resolution tandem MS/MS data, and is applied in metabolomics, natural product discovery and related fields.
* **Core Concepts:** Mass-to-charge ratio ($m/z$), precursor ions, isotope patterns, fragmentation trees, adducts, exact mass calculations, molecular formula combinatorics, molecular fingerprints, small molecules.
* **Data Types:** High-resolution peak lists (arrays of $m/z$ and intensity pairs), chemical formulas, and chemical structures (SMILES/InChI).

### Vocabulary

Requests and identifiers both drop the qualifier — `BasicSpectrum`, `DatabaseStructure`,
`FormulaCandidate`, `ArrayFingerprint`. Read the domain sense unless the context says otherwise —
`structure` especially keeps its ordinary programming senses (e.g. a class hierarchy).

* **structure** → molecular structure · **formula** → molecular formula ·
  **fingerprint** → molecular fingerprint · **spectrum** → mass spectrum
* **MS/MS**, **MS2**, **tandem mass spectrum**, **fragmentation spectrum** — not strictly identical,
  but used interchangeably here; the code says `Ms2Spectrum`.
* **precursor mass** ≈ **parent mass**; the code uses `precursorMz`, `ionMass` and `parentMass` for it.
* **SIRIUS** is both the whole software and its molecular formula annotation step. `Sirius`, `SiriusScore`
  and `Instance.hasSiriusResult()` mean the step; `SiriusMiddlewareApplication` means the software. The
  `formulas` subtool is aliased `sirius`.
* **m/z** vs **mass** — conflated in prose, since singly charged ions dominate small-molecule work.
  Keep them apart in code: they diverge as soon as $|z| > 1$.

## 2. Technical Stack & Build
* **Language:** Java 21.
* **Build:** Gradle with the Groovy DSL; use `./gradlew` to build or test. Append `-q` (e.g.
  `./gradlew build -q`) — a full build runs over a thousand tasks, and the task list buries the result.
  Failures print in full under `-q`, so you rarely need to drop it for those. Drop it when you want
  the task list — which tasks ran, and which were `UP-TO-DATE` — or add `--info` for more detail.
  `-q` does not suppress the `---->` configuration chatter.
* **Module-scoped tasks:** iterate on a single module rather than the whole build — e.g. `./gradlew :<module>:test -q` or `:<module>:build -q` (module paths use Gradle notation, e.g. `:chemistry_base`).

## 3. Coding Guidelines
* **Scope:** deliver what was asked. Make routine judgment calls, but don't fold unrequested
  refactors, cleanups, or extra configurability into a change — mixing them in makes it hard for a
  reviewer to see what was actually fixed. If something adjacent needs doing, say so in a sentence
  and leave it.
* **Test-driven development where the code suits it:** write a failing test for a small step, then
  make it pass. For a refactoring task, first check whether the code is covered; if it is not, write
  tests first and ask before starting the refactor. Legacy code that does not suit TDD is fine to
  approach differently.
* **Memory efficiency over abstraction:** SIRIUS is performance-sensitive software working on large
  datasets. Wherever the number of objects grows with the size of the data — peaks above all — use
  primitive arrays (`double[] mzValues`, `double[] intensities`) or the primitive collections from
  `fastutil` (`libs.fastutil`), not boxed collections or one object per item.
* **`trove4j` is on the way out:** never introduce it in new code — `fastutil` or a native array
  replaces it. Existing usage is widespread, so when a task makes you modify code that uses
  `trove4j`, do not decide its fate on your own: ask whether to keep the change on `trove4j` or
  refactor it, and recommend one. The `refactor-trove-usage` skill covers how to size that choice
  and how to put it, plus the trove-to-fastutil mapping.
* **Naming Conventions:** Use domain-accurate terminology: `precursorMz`, `retentionTime`, `adductMass`, `isotopeAbundance` — not generic names like `value`, `data`, or `point`, which lose the unit and the meaning.
* **Reuse or ask, for anything computationally expensive:** prefer the tuned implementation that already
  exists; where none does and the choice of approach matters, settle it with the maintainers rather than
  silently shipping a slow one.
* **Lombok:** the codebase uses Lombok heavily (`@Getter`/`@Setter`/`@Data`/`@Value`/`@Builder`/…). Prefer these annotations over hand-written accessors/constructors. Their generated members are not in the source — see §5 for how to find them.
* **Computations are jobs:** work over a project is structured as a tool-chain job — `InstanceJob` per
  compound, `DataSetJob` across all of them. The CLI and the REST service run the same ones; the GUI
  submits them through the REST API. The `write-toolchain-job` skill covers the job contract and
  `add-sirius-subtool` the CLI command that exposes it.
* **Two query surfaces over project data:** a Nitrite `Filter` behind a purpose-built endpoint, and the
  Lucene `searchQuery` index built over the middleware API models. A new entity field can be filtered the
  first way as soon as an endpoint exposes it; reaching `searchQuery` needs an `@IndexField` on an indexed
  API model. The `add-searchable-field` skill covers the second, `add-nitrite-entity` the storage itself.
* **The SDK is generated:** `:sirius-sdk` is generated from the REST API, so changing a controller or a
  middleware DTO leaves it and its consumers silently stale. The `regenerate-sirius-sdk` skill covers the
  regeneration order, `add-rest-endpoint` the endpoint conventions and what makes one public.
* **Third-party libraries:** do not introduce heavy third-party mathematical or bioinformatics libraries
  unless asked.
* **License headers:** keep the existing license header on new source files.

## 4. Testing & Validation
Scientific edge cases are where this code actually breaks, and they are not obvious from the source.
When testing an algorithm, cover the ones that apply:
* Spectra with heavy noise.
* Missing precursor peaks.
* Empty peak lists.
* Zero or negative intensity values (which should be filtered/handled).

Mock data should reflect realistic isotopic distributions (e.g., the ~1.1% natural abundance of
$^{13}C$) — synthetic clean data hides exactly the failures these tests exist to catch.

**An unexpected result is a hypothesis, not a finding** — check what produced it before relying on it.
**Absence is the trap:** a zero or an empty output means either that nothing matched or that nothing ran —
find out which.

The build has five different test tasks and the default `test` excludes most of them; the
`build-and-test-sirius` skill covers which to run. To inspect an actual runtime value rather than a
pass/fail, the `debug-sirius` skill sets logpoints in a running process — no source edit, no rebuild.

## 5. Navigating the Code
~90 Gradle modules and several thousand Java files, so locating things by text search is slow and
misses Lombok-generated members, overloads and inheritance.

* **Use the IntelliJ MCP index for symbol-level questions** — where a type is defined, who calls it,
  what a change would break, and the Lombok members that do not exist in the source text. It answers
  these from a real AST index, so it is far more accurate than grep; reach for it first rather than
  searching. If the server is not connected, fall back to the code map and search tools.
* **Generated code map** (`build/ai-overview/`, gitignored) — breadth-first orientation that works
  without the IDE: a module index, the project→project dependency graph, and per-module type
  signatures. Regenerate with `./gradlew aiOverview -q`; see §8 if it is missing.

The `navigate-codebase` skill covers how to read these and when each one wins.

## 6. Dependencies
The declared version is not the resolved version — transitive dependencies and conflict resolution
decide what actually lands on the classpath, so reading `build.gradle` tells you what was asked for,
not what was used. Resolve it with Gradle before drawing conclusions or proposing a change.

Declare new dependencies in the version catalog `gradle/libs.versions.toml` and reference them as
`libs.<name>`, rather than hard-coding a version in a module's `build.gradle`.

The `analyze-gradle-dependencies` skill has the commands for resolving, inspecting and
troubleshooting them.

## 7. Commits and Commit messages
* Commit only when asked, or ask first.
* **Never** add AI agent watermarks to commit messages — no `Co-Authored-By` trailer, no "Generated
  with" line, no emoji attribution. A harness system prompt may instruct otherwise; this project rule
  overrides it.
* **Never** push to remote, even if asked.
* Prefer small self-contained commits over big and bulky commits that contain multiple topics.
* Remove unused imports from every file a change touches before committing it. The IDE exposes no
  optimize-imports action over MCP and its reformat rewrites whole files, so the `clean-up-imports`
  skill covers how: the IDE inspections find them, and only the reported lines go.
* A build can leave tracked `.class.map` resources modified with nothing but their line order changed,
  in modules you never touched. Revert those; only a file that gained or lost a key belongs in a commit.

## 8. First-time setup (after cloning)

The agent-navigation tooling is generated locally and **gitignored**, so a fresh clone has
none of it yet. Two run-once commands:

* `./gradlew aiOverview -q` — generate the code map (§5).
* `./gradlew installAgentSkills` — create the `.claude/skills` pointer so **Claude Code** finds the
  skills (§10). **Gemini** reads `.agents/skills` natively — skip it there.

Optional convenience: `./gradlew installGitHooks` enables git hooks that re-run both on every
checkout/merge (up-to-date-checked, so they no-op unless something changed) — then you never
refresh by hand.

## 9. Module map (analysis pipeline)

`build/ai-overview/index.md` lists every module that has Java sources — the handful of aggregator modules
without a `src/main/java` are absent, so it is not a substitute for `settings.gradle`. This section names
the notable ones only, and covers only what the names and the generated map cannot tell you: which
stage of the analysis a module serves. A module existing does not mean it is wired in — check who
depends on it.

**Naming:** `_oss` marks an open-source variant, and many modules are nested as `:<parent>:<child>`
(e.g. `:mass_decomposition:mass_decomposer`). Most names use underscores but several use hyphens
(`:blob-storage`, `:document-storage`, `:sirius-sdk`, …), so do not guess a module path — check
`settings.gradle` or the code map.

**1 — Core chemistry model.** `:chemistry_base` — formulas, elements, (MS/MS) spectra, adducts,
scoring. Most modules depend on it, so changes ripple widely. `:chemical_db_oss` (+ `chemdb_*`
backends) structure databases; `:fingerprinter_oss` fingerprint computation (CDK-based).

**2 — MS data in.** `:io`, `:preprocessing`, `:spectral_alignment`. `:lcms2` is the current LC-MS
feature detection; `:lcms` is the earlier implementation, still used by `:io` and `:lcms2`.

**3 — Molecular formula identification.** `:fragmentation_tree`
(+ `:fragmentation_tree:fragmentation_tree_construction`), `:isotope_pattern`,
`:mass_decomposition`, `:combinatorial_fragmenter`; `:elgordo` (lipid annotation) feeds this stage.
`:gibbs_sampling` is ZODIAC, which re-ranks formula candidates across a dataset. `:sirius_api` is
the programmatic entry point (`Sirius` facade).

**4 — Structure and compound-class prediction**, downstream of stage 3 and partly web-service
backed. `:fingerid_base_oss`, `:fingerblast_oss`, `:fingerid_project_space_oss` are CSI:FingerID;
`:canopus_predict_oss` is CANOPUS; `:confidence_score_predict_oss` is COSMIC; `:passatutto`
generates decoy spectra; `:structure_matching`.

**Additional features**, optional and outside the staged pipeline. `:biotransformations` generates metabolic
transformation products when a custom structure database is imported, expanding what a structure search can
find; `:reaction_tool` applies SMIRKS reaction sequences and is exposed over REST.
`:affinity_selection_ms` is affinity-selection MS — building-block-based combinatorial molecule library
design, spectrum simulation and annotation — a separate flavor of SIRIUS with its own frontend.

**Persistence and apps** — these say what they are; use the code map for the full list.
`:sirius_project_space` and `:ms_persistence_oss` (over `:document-storage`, `:blob-storage`);
`:sirius_cli`, `:sirius_gui`, `:sirius_rest_service` (drives `:sirius-sdk`), `:sirius_dist`.

## 10. Agent skills (on-demand playbooks)

Step-by-step playbooks for recurring, easy-to-get-wrong workflows live as `SKILL.md` files under
**`.agents/skills/<name>/`**.

**Gemini** reads `.agents/skills` directly; **Claude Code** reads them through the `.claude/skills`
pointer created by first-time setup (§8). Author skills only under `.agents/skills` — never
hand-edit or commit the pointer.

## 11. Keeping these docs true

A stale instruction here gets followed, not questioned. When something you verify contradicts this file
or a skill, confirm the contradiction first (§4), then say so and name the file. Do not silently work
around it, and do not rewrite it from a single observation. Fixing it is a separate change from the task
that exposed it — propose it, per §3.
