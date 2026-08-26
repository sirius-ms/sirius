---
name: trace-config-parameter
description: Explains where a SIRIUS parameter value actually came from at runtime — the layered ParameterConfig stack, its precedence order, and how to inspect it. Use when a parameter has an unexpected value, when a CLI override appears to be ignored, when a value differs between runs or between compounds, or when reasoning about which of defaults, project space, input file or CLI wins.
---

# Trace where a parameter value came from

`ParameterConfig` is not a map — it is a stack of **named layers** over a Commons Configuration
`CombinedConfiguration` built with an **`OverrideCombiner`**. A lookup returns the value from the
highest layer that has the key. Debugging a parameter means finding which layer won.

**The newest layer wins.** Both `addNewConfig` and `newIndependentInstance` insert the new layer at
the *front*, and with `OverrideCombiner` the front layer takes precedence — so each layer stacked over
the defaults overrides the one below it.

## Inspecting the stack

`getConfigNames()` returns the layer names **highest precedence first**. That list is the answer:

```
[PROBE_L2, PROBE_L1, RUNTIME_DEFAULT_CONFIGS, de.unijena.bioinf.ms.defaults/chemistry_base.auto.config]
```

| Question | Call |
|---|---|
| What is the precedence order right now? | `getConfigNames()` — first entry wins |
| What is the effective value? | `getConfigValue(key)` |
| Which layer am I writing to? | `getLocalConfigName()` — the only writable one |
| What did *this* layer set? | `toMap(layerName)` |
| What did the top layer change? | `getModifiedConfigs()` / `getModifiedConfigKeys()` |

`changeConfig(key, value)` only ever writes to the local (top) layer, and validates the value by
instantiating it — an unparseable value throws rather than being stored silently.

## Where the layers come from

Names are `ConfigType`: `PERSISTENT_PROPERTIES, GLOBAL, CUSTOM, INPUT_FILE, CLI, BATCH_COMPUTE,
PROJECT, RUNTIME` (plus ad-hoc names such as `MS_FILE:<compound>`).

| Layer | Created by | Source |
|---|---|---|
| `GLOBAL` + `RUNTIME_DEFAULT_CONFIGS` | `PropertyManager` static init | `concat.auto.config` on the classpath — see `add-config-parameter` |
| `CUSTOM` | `ApplicationCore` | a custom profile file, if present |
| `PROJECT` | `ProjectSpaceConfigSerializer` | the config stored **in the project space** |
| `CLI` / `BATCH_COMPUTE` | `DefaultParameterConfigLoader` | `--Key=value` on the command line / a REST compute request |
| `MS_FILE:<compound>` → `INPUT_FILE` | `JenaMsParser`, `MgfParser` | `>option` lines **inside the data file** |
| `RUNTIME` | `AddConfigsJob` | scratch layer for one computation |

**Data files carry parameters.** `JenaMsParser` creates a per-compound layer and `changeConfig`s into
it for `>elements`, `>ppm-max`, `>ppm-max-ms2`, `>noise`, `>compound-timeout`, `>tree-timeout`,
`>no-recalibration`, and for *any* `>key` that `containsConfigKey` recognises. So a `.ms` file can set
almost any parameter, and it is stored on the experiment as `InputFileConfig`.

## The assembly order, per compound

`AddConfigsJob` rebuilds the stack for each instance, pushing in this order — later is higher:

1. the compute layer (`CLI` or `BATCH_COMPUTE`);
2. the stored `PROJECT` config becomes the base, with the compute layer pushed over it, and stale
   `RUNTIME*` layers removed;
3. **the input-file config, reloaded and pushed on top** — its own comment says this is deliberate,
   "to ensure that it is on top position after CLI config";
4. a fresh `RUNTIME` layer, highest of all;

then the whole stack is written back with `updateProjectConfig`, so a re-run starts from it.

## Traps

* **The input file can outrank the command line.** Step 3 pushes the `.ms` config above the CLI layer.
  If a `--Key=value` override looks ignored, check the input file for a `>key` line before suspecting
  the CLI parsing.
* **The project space remembers.** A stored `PROJECT` config from an earlier run participates in every
  later computation on that project, so "it behaves differently in my project than in a fresh one" is
  expected, not a bug.
* **Layers are per compound.** The `MS_FILE:<name>` layer is created per compound, so two compounds in
  one run can legitimately resolve the same key differently.
* **`newIndependentInstance` produces a detached copy** — changing a child does not affect the parent,
  so a `changeConfig` on a derived config does not propagate back.
* **Only the top layer is writable.** `localConfig()` throws on an unmodifiable config; that is what
  "please use newIndependentInstance(name)" means.
* To see values at runtime rather than reasoning about the stack, put a logpoint on
  `ParameterConfig.getConfigValue` — see `debug-sirius`.
