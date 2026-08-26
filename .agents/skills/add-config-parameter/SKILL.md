---
name: add-config-parameter
description: Adds or changes a SIRIUS algorithm parameter driven by @DefaultProperty, ParameterConfig and the generated .auto.config defaults. Use when introducing a new configurable parameter, renaming or removing one, changing a default value, or when a build fails with "<key> is NULL!" from checkDefaultProfile.
---

# Add a config parameter

Algorithm parameters are declared with `@DefaultProperty` and read at runtime through
`ParameterConfig` / `PropertyManager`. An annotation processor turns the annotated members into
`.auto.config` property files; the app loads the concatenated result from its classpath at startup.

Two properties of this pipeline are easy to get wrong:

* **The annotation carries no default value.** `@DefaultProperty` has only `propertyKey` and
  `propertyParent`. The value lives in the generated `.auto.config` file, and the processor
  *preserves whatever is already there* — so a new key arrives **empty** and you must fill it in.
* **The processor writes into `src/main/resources/`, not `build/`.** The generated files are
  **tracked in git**. Building can therefore modify tracked files, and those modifications are part of
  your change.

## The pipeline

```
@DefaultProperty on a field/class          (propertyKey, propertyParent — no value)
      │  javac + :default_properties:processor  (annotationProcessor, per module)
      ▼
<module>/src/main/resources/de.unijena.bioinf.ms.defaults/
      <module>.auto.config    ← TRACKED; keys + javadoc generated, values hand-maintained
      <module>.class.map      ← TRACKED
      │  :sirius_rest_service:collectProfiles → makeProfileTemplates
      ▼
concat.auto.config + concat.class.map
      │  checkDefaultProfile  — fails on any empty value
      ▼
packaged into :sirius_rest_service resources
      │  PropertyManager (DEFAULT_PREBUILT_CONFIG) at startup
      ▼
ParameterConfig — runtime lookup
```

For a class-level `@DefaultProperty` the key is the **simple class name**, and `.class.map` maps it to
the fully qualified name — `ProbeWiringSetting=de.unijena.bioinf.ChemistryBase.ms.ProbeWiringSetting`.

What you set here is only the **bottom layer** of the runtime stack: the project space, the input data
file and the command line can each override it. For where a value actually came from, see
`trace-config-parameter`.

## Steps

1. **Annotate**, in a module that wires the processor. Not all do, and grepping that module's
   `build.gradle` is **not** a reliable check: `fragmentation_tree` wires it inside an `allprojects {}`
   block, so `fragmentation_tree_construction` inherits it while its own build file never mentions it.
   Ask Gradle instead:

   ```bash
   ./gradlew :<module>:dependencies --configuration annotationProcessor -q | grep default_properties:processor
   ```

   A hit means wired, including by inheritance. No hit means not wired — and that is a real answer
   rather than an empty report, because every module lists Lombok in that configuration.
2. **Write the javadoc.** It becomes the parameter's help text, all the way through:
   `getDocComment` → the comment in `.auto.config` → the description in `sirius config --help`.
   Skip it and the parameter still works and still appears in help, with **no explanation at all** —
   nothing warns you. `NumberOfCandidates` has no javadoc and its help entry is a bare
   `--NumberOfCandidates=10`; `NumberOfCandidatesPerIonization` has one and reads as a sentence.
3. **Build the module** so the processor regenerates the `.auto.config`, then **fill in the value** by
   hand — the new key will be present but empty.
4. **Validate**, and commit the regenerated resources with the code:

   ```bash
   ./gradlew :sirius_rest_service:checkDefaultProfile     # no -q: see the note below
   git status --short -- '*.auto.config' '*.class.map'    # expect your module's files modified
   git diff -- '*.class.map'                              # key change or reordering? (AGENTS.md §7)
   ```

   **Run this one without `-q`.** It reports success as `DONE: No NULL values found!` via
   `logger.lifecycle`, which `-q` suppresses entirely — so a pass and a task that never ran look
   identical. Failure does print under `-q`, but you want the positive confirmation here.

## Reaching the command line

**You get a CLI option for free.** `DefaultParameterConfigLoader` walks every config key and builds one
picocli option per key under the `config` subtool, so a newly declared parameter is immediately
settable as `sirius config --YourKey=value` with no CLI code at all. Nested keys work the same way
(`--MS1MassDeviation.allowedMassDeviation`). The value shown after `=` in help is the *current default*,
and the setter writes into the `CLI` layer of the config stack.

**Subtool options are the surface users actually use.** Only the important parameters get one, usually
under a shorter name, and they are **not always aliases**. A setter may transform the value or set
several keys at once. `SiriusOptions` contains all of these shapes:

| Option | Writes | Shape |
|---|---|---|
| `--candidates 5` | `NumberOfCandidates = 5` | 1:1 |
| `--ppm-max 7` | `MS1MassDeviation.allowedMassDeviation = 7ppm` | value transformed — the config key carries a unit, the option does not |
| `--no-recalibration` | `ForbidRecalibration = FORBIDDEN` | boolean → enum, and **nothing is written when false** |
| `--elements-considered SBrClBSe` | `FormulaSettings.detectable` **and** `.fallback` | one option, two keys |
| `--database BIO` | `FormulaSearchDB=BIO`, `FormulaSearchSettings.performDeNovoBelowMz=0`, `…performBottomUpAboveMz=Infinity` | one option, three keys — it encodes a policy |

`--database` is the case to watch: it deliberately *disables* de novo and bottom-up search, so it is
narrower than setting `FormulaSearchDB` through `config`. Its own description says so and points users
at the config keys. **If your option is narrower than the keys it sets, document that in the
description** — otherwise the two surfaces disagree and only one of them says why.

The mechanics either way:

```java
@Option(names = {"-c", "--candidates"}, descriptionKey = "NumberOfCandidates", ...)
public void setNumberOfCandidates(DefaultParameter value) throws Exception {
    defaultConfigOptions.changeOption("NumberOfCandidates", value);
}
```

`changeOption` puts the value in the `CLI` layer. That makes it *eligible* to be persisted, but does not
persist anything by itself: when a computation runs, `AddConfigsJob` assembles the layer stack and writes
it back **per compound** — the store is keyed `upsertConfig(alignedFeatureId, ConfigType, …)` — and that
is what makes the run reproducible. Setting an option and not computing stores nothing.

`DefaultParameter` carries the raw string and defers typing, so a transformed value like `value + "ppm"`
is still validated — `changeConfig` instantiates the target type and throws if it fails.

**`descriptionKey` is not picocli's resource bundle here — it names a config key.** `WorkflowBuilder`
reads `opt.descriptionKey()` and appends `"Default: " + config.getConfigValue(...)` to the option's help,
so the human text comes from `description=` and the *live default* comes from `descriptionKey=`. Two
consequences: the two strings can drift apart (`--ppm-max` says "Maximum allowed mass deviation in ppm"
while the config comment explains the `X ppm (Y Da)` format), and a **renamed or mistyped key silently
renders `Default: null`** — `getConfigValue` returns null for an unknown key rather than failing, so
nothing breaks at compile or start-up. Grep the help output after renaming a key.

Watch the format mismatch this creates: `--ppm-max 7` is accepted unit-less and stored as `7ppm`, while
the help for that same option reads `Default: 10.0 ppm`.

**An option that only sets a field stays outside the config system.** Options like `--input` are not
among the generated ones and never enter the config stack, so they are not stored and not reproducible
from a project. That is correct for *data selection and presentation*, and wrong for *algorithm tuning*:
if the parameter changes what a result means, it belongs in the config system.

The GUI and the REST service reach parameters by **building CLI command strings**
(`ParameterBinding.asParameterList`, `JobSubmission.asCommand`), so this option surface is shared by
all three. Per the maintainers this coupling is not the intended end state — the direction of travel is
for the CLI to query the REST API rather than the reverse — so do not build new dependencies on it.

## Traps

* **A missing `annotationProcessor` wiring fails silently.** No `.auto.config` entry is generated, so
  `checkDefaultProfile` has nothing to complain about and the parameter is simply absent at runtime.
  Not every module that wires the processor has generated config: several hold no annotated member at
  all (`io`, `utils`, `mass_decomposition` and `sirius_project_space` among them), and a parent may wire
  it for a child that holds the annotations. Use the Gradle query in step 1 rather than grepping a build
  file.
* **`<key> is NULL!` means an empty value, not a missing key.** `checkDefaultProfile` loads the
  concatenated config and throws on any empty value. Fill in the default; do not delete the key.
* **It fires on ordinary builds, not just when you ask for it.** `processResources dependsOn
  checkDefaultProfile`, and `checkDefaultProfile` is in `bootJar`'s task graph
  (`collectProfiles → makeProfileTemplates → checkDefaultProfile → processResources → … → bootJar`). So
  an empty default breaks *packaging the REST service*, with an error about a parameter you may not have
  been working on.
* **Do not hand-edit the keys or comments** in `.auto.config`; they are regenerated from the
  annotation and the javadoc. The **values** are the hand-maintained part.
* Removing a parameter leaves its key in the tracked `.auto.config` until the file is regenerated —
  rebuild the module and check the diff rather than assuming the removal propagated.
