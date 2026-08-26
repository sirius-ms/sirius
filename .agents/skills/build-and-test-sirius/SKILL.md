---
name: build-and-test-sirius
description: Builds, runs and tests SIRIUS — which of the five test tasks to use (test, integrationTest, accountIntegrationTest, allIntegrationTests, acceptanceTest), how to scope work to a single Gradle module, and how to start the REST service, CLI or GUI locally. Use when running tests, verifying a change, launching the app, or answering a question about real behaviour with a throwaway probe test.
---

# Build, run and test SIRIUS

~90 Gradle modules; a full build is expensive. **Always scope to the module you changed**, and
pick the right test task — the default `test` task deliberately does not run most of them.

## Test task taxonomy

The default `test` task **excludes** integration, API and acceptance tests by name, so a green
`./gradlew test` says nothing about them. All five tasks are defined in the
`allprojects` block of the root `build.gradle`:

| Task | Runs | Notes |
|---|---|---|
| `test` | everything **except** `*IntegrationTest`, `*ApiTest`, `*AcceptanceTest` | the fast per-commit gate |
| `integrationTest` | `*IntegrationTest` + `*ApiTest`, **excluding** tag `account-state` | boots a real server (SDK tests `dependsOn :sirius_rest_service:bootJar`) |
| `accountIntegrationTest` | **only** tag `account-state` | login/logout/subscription tests; mutate server-global state; `mustRunAfter integrationTest` |
| `allIntegrationTests` | both of the above, in order | use this rather than running the two by hand |
| `acceptanceTest` | `*AcceptanceTest` | full-system, `maxHeapSize = "16G"` |

All use `failOnNoMatchingTests false`, so a task that matches nothing **passes silently**. When
a run reports no tests, verify your class name actually matches the filter before believing it.

```bash
./gradlew :chemistry_base:test -q                    # one module, fast loop
./gradlew :io:test --tests "*AgilentCefExperimentParserTest" -q   # one test class
./gradlew :sirius-sdk:sirius-sdk.openapi:integrationTest -q
./gradlew allIntegrationTests -q                     # never run the two account tasks concurrently
```

Add `--info` (and drop `-q`) only when you need the failure detail.

## Probing with a throwaway test

A probe answers a question that needs a situation which does not otherwise occur — two configurations
side by side, a deliberately stale record, a thread holding a lock. When the situation already exists in
a running process, `debug-sirius` reads the value out of it without touching the source, which is the
better instrument for that case.

**Making a probe conclusive**

* **Run the conditions together, with a control.** One measurement is a number; the comparison is the
  answer. Keep an untouched neighbour in the same run — the handler you did not annotate, the field whose
  declared name still matches — so a difference can be attributed to the change rather than to the setup.
* **Print the precondition beside the result.** Setup often produces something other than you assume: an
  import that fills a neighbouring collection, a filter that matches nothing. A probe with no input emits
  the same shape of output as a real finding, so print the count of whatever the question rests on.
* **Warm up and alternate anything timed.** Run each condition several times before measuring, then
  interleave them and print every round rather than an average. Cold caches and one-sided ordering
  produce confident, wrong ratios, and a spread that overlaps between conditions means the harness is
  measuring itself rather than the code.

**Running one here**

* **A probe that prints nothing usually did not run the way you think.** `-q` suppresses its output
  entirely — use `-q -i`, or read `build/test-results/test/TEST-*.xml`, which captures stdout and stderr
  even when the console shows neither. An identical re-run prints nothing either, since Gradle skips the
  task as up-to-date; edit the probe or pass `--rerun-tasks`. Prefix the output with a marker you can grep
  for — build logs are long.
* **Assertions run here and not in shipped builds**, and the algorithm modules use `assert` liberally. So
  an `AssertionError` is not the symptom a user gets — production runs past that point — but the invariant
  it names was still violated, unchecked.

A surprising probe result is a hypothesis about the probe before it is a finding about the code (root
`AGENTS.md` §4). The `--tests` caveat above applies: a mistyped pattern matches nothing and passes silently.

## Building

```bash
./gradlew :<module>:build -q          # preferred: module-scoped
./gradlew build -q                    # whole build; slow
./gradlew :sirius_rest_service:bootJar -q
./gradlew :sirius_dist:sirius_gui_dist:buildJpDists   # installers + zip + checksums
```

`gradle.properties` sets `-Xmx16G` for the Gradle JVM; don't lower it.

**No Java toolchain is pinned** — no `sourceCompatibility`, `targetCompatibility` or `toolchain`
anywhere in the build — so the JDK is whichever the launcher uses, and the IDE and the shell can
differ. Worth checking when behaviour differs between them.

## Running locally

**REST service** — Spring Boot, entry point
`sirius_rest_service/src/main/java/de/unijena/bioinf/ms/middleware/SiriusMiddlewareApplication.java`:

```bash
./gradlew :sirius_rest_service:bootRun -q
./gradlew :sirius_rest_service:bootRun -q --args="rest -s -p 8888"   # fixed port
```

`application.properties` defaults: `server.port=0` (dynamic — read the actual port from the
log), `spring.profiles.active=local`, `spring.main.lazy-initialization=true`,
`sirius.middleware.project-space=NITRITE-NOSQL`. Swagger UI at `/swagger-ui.html`,
spec at `/v3/api-docs`.

Verify a REST change without the GUI — the service boots in seconds once the build is warm:

```bash
./gradlew :sirius_rest_service:bootRun -q --args="rest -s -p 8888" &
curl -s localhost:8888/actuator/health              # {"status":"UP"} once ready
curl -s localhost:8888/v3/api-docs                  # live OpenAPI spec
curl -X POST localhost:8888/actuator/shutdown       # clean stop
```

`/swagger-ui.html` 302-redirects to `/swagger-ui/index.html`, so `curl` it with `-L`. Only one
SIRIUS service may run at a time — a second start exits with "SIRIUS ALREADY RUNNING".

**CLI** — picocli; `:sirius_cli` is a library and the runnable artifact is assembled by
`:sirius_dist`. It applies the `application` plugin, so a `:sirius_cli:run` task exists — but no
`mainClass` is configured for it, so it fails when executed. Go through `:sirius_dist`.

**GUI** — hosted by the same middleware process; add `--gui` to the args above. Its JS views are
**pre-built assets** under `sirius_rest_service/src/main/resources/templates/`, not sources —
changing them means rebuilding in `../sirius-js-monorepo` and re-running `copyJSAppToFrontend`
(see `regenerate-sirius-sdk`).

## Config/properties coupling

Parameters annotated `@DefaultProperty` are turned into profile templates by an annotation processor,
and `processResources dependsOn checkDefaultProfile`, so this check runs on any
`:sirius_rest_service` build:

```bash
./gradlew :sirius_rest_service:checkDefaultProfile   # not -q: it reports success via lifecycle
```

A failure here (`<key> is NULL!`) is not a build problem — it means a parameter has no default yet.
See the `add-config-parameter` skill.

## Traps

* A green `test` does **not** cover integration/acceptance tests.
* `failOnNoMatchingTests false` means a typo'd filter is a silent pass.
* Never run `integrationTest` and `accountIntegrationTest` in parallel — only one test-started
  SIRIUS server may exist at a time; that is why `mustRunAfter` exists.
* Changed the REST API? The SDK is generated — see the `regenerate-sirius-sdk` skill.
* **"No matching tests found" usually means the wrong task, not a missing test.** The IDE does not run
  tests itself — it asks Gradle, which must choose one of the five tasks above. Choose
  `acceptanceTest` for an `*IonTest` and its `*AcceptanceTest` filter matches nothing, so the build
  fails with "No matching tests found in any candidate test task" and the test never runs. The message
  names the test, which reads as "your test does not exist". Name the task yourself:
  `./gradlew :<module>:test --tests "*IonTest.testIonParser"`.
* A green run proves nothing on its own here — read `build/test-results/<task>/TEST-*.xml` and check
  `tests="N"` is what you expected.

Need a runtime value rather than a pass/fail? See the `debug-sirius` skill.
