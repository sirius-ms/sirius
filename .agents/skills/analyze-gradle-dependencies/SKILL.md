---
name: analyze-gradle-dependencies
description: Resolves, inspects and troubleshoots external library dependencies in the SIRIUS Gradle build, and adds new ones via the version catalog. Use when a dependency version is unclear or conflicting, when debugging NoClassDefFoundError / NoSuchMethodError / ClassNotFoundException, or when adding or upgrading a third-party library.
---

# Analyze Gradle dependencies

**External libraries only.** Inter-module (project→project) edges are a different question and are
answered faster by the generated code map — see the `navigate-codebase` skill.

Groovy DSL, versions centralised in `gradle/libs.versions.toml`.

**The declared version is not the resolved version.** Transitive dependencies and conflict
resolution mean the jar actually on the classpath is frequently not the one written in a
`build.gradle`. Reading build files therefore answers "what did we ask for", never "what did we
get" — so resolve it with Gradle rather than inferring it from the source.

## Inspect the resolved graph

```bash
./gradlew :<module>:dependencies -q                          # one module (prefer this)
./gradlew :<module>:dependencies --configuration runtimeClasspath -q
./gradlew dependencies -q                                    # root project only, rarely what you want
```

Useful configurations: `compileClasspath`, `runtimeClasspath`, `testCompileClasspath`,
`testRuntimeClasspath`. A version shown as `2.1 -> 2.5` means conflict resolution upgraded it.

## Investigate one library or a conflict

```bash
./gradlew :<module>:dependencyInsight --dependency <name> --configuration runtimeClasspath
```

This is the tool for "why is this version here / who pulls it in". Use it before changing or forcing
any version — the answer is usually a transitive path you did not expect.

For a runtime failure (`NoClassDefFoundError`, `NoSuchMethodError`, `ClassNotFoundException`), check
`runtimeClasspath` rather than `compileClasspath`: the two can resolve differently, and these errors
are almost always a runtime-only divergence or a duplicate class from two coordinates.

## Adding or upgrading a dependency

Declare it in the version catalog, not inline in a module:

1. Add the version under `[versions]` and the coordinates under `[libraries]` in
   `gradle/libs.versions.toml` (`[plugins]` for plugins).
2. Reference it as `libs.<alias>` in the module's `build.gradle`.
3. Pick the right configuration — `api` re-exports the dependency to consumers of the module,
   `implementation` keeps it internal. Prefer `implementation` unless the type genuinely appears in
   the module's public API.
4. Re-resolve and confirm the version you expected is the one you got:
   `./gradlew :<module>:dependencyInsight --dependency <name> --configuration runtimeClasspath`.

Existing overrides live in the root `build.gradle` (e.g. a `resolutionStrategy.force` for
`trove4j`); check for one before concluding a version is unexplained.

