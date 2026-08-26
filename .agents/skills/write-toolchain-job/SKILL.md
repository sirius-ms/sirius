---
name: write-toolchain-job
description: Writes a SIRIUS tool-chain job — choosing between InstanceJob, DataSetJob, PreprocessingJob and PostprocessingJob, implementing the template-method contract, keeping CPU work in nested jobs, and getting recompute and invalidation right. Use when adding a computation step, when a job recomputes or skips when it should not, or when deciding where heavy work belongs.
---

# Write a tool-chain job

A job is the unit of computation, and it has **two entry points and one implementation**. The CLI builds a
`ToolChainWorkflow`; the REST service builds the same class, via `JobSubmission.asCommand()` →
`ComputeServiceImpl` → `BackgroundRuns.runCommand` → `WorkflowBuilder` → `new ToolChainWorkflow(...)`.
The job classes live in `:sirius_cli`, which `:sirius_rest_service` depends on. The **GUI is a client of
the REST service**: it assembles a `JobSubmission` and posts it through the SDK, and never runs a job
itself.

## Which base class

| Work | Base class | Your entry point |
|---|---|---|
| One compound at a time | `InstanceJob` | `computeAndAnnotateResult(Instance)` |
| All compounds together (networks, cross-sample re-ranking) | `DataSetJob` | `computeAndAnnotateResult(List<Instance>)` |
| Turning input files into instances | `PreprocessingJob<P extends Iterable<Instance>>` | `compute()` |
| Runs last — summaries, cleanup | `PostprocessingJob<P>` | `compute()`, plus `setInput(instances, config)` |

`InstanceJob` and `DataSetJob` are `ToolChainJobImpl` subclasses and participate in recompute and
invalidation. `PostprocessingJob` is a plain `BasicJJob` and does not.

The two ends of the chain:

* **`PreprocessingJob`** returns the `ProjectSpaceManager` — that object *is* the `Iterable<Instance>`
  the rest of the chain walks. It extends `BasicMasterJJob`, so it can `submitSubJob` like the tool-chain
  jobs; delegating to `rootOptions.makeDefaultPreprocessingJob()` reuses the standard import.
* **`PostprocessingJob`** gets `setInput(instances, config)` **before** `compute()` runs, so store both
  there and do the work in `compute()`. It receives the assembled `ParameterConfig` alongside the
  instances.

## The InstanceJob contract

The base class owns the control flow; you fill in three methods. The call order:

```
checkInput()                      → return true to skip this instance gently
isAlreadyComputed(inst)
    false                         → computeAndAnnotateResult(inst)
    true  + recompute requested   → invalidateResults(inst) → computeAndAnnotateResult(inst)
    true  + no recompute          → nothing runs at all
```

* `checkInput()` — throws if the input is null, and returns `true` (skip) when `needsMs2()` is true and
  the instance has no MS/MS. **`needsMs2()` defaults to `true`, so a new job requires MS/MS unless you
  say otherwise.** Override it to `false` if the computation works on MS1 alone; override `checkInput()`
  itself only for extra conditions.
* **MS1-only data passes two independent gates.** The first removes the compound before any job exists,
  so an instance that never reaches your job was filtered at import rather than by `checkInput()`:

  | Gate | Controlled by | Effect |
  |---|---|---|
  | Import | `allowMs1Only` | drops the compound entirely — no instance, no job |
  | `checkInput()` | `needsMs2()` | keeps the instance, skips *this one job* |

  A compound dropped at the import gate logs *"Skipping instance 'X' because it does not contain any non
  Empty MS/MS"*; that message identifies the import gate, not `checkInput()`.

  **The import default differs per entry point:** the CLI's `--allow-ms1-only` is `false` (hidden, and
  `@Deprecated(forRemoval = true)`), while `ProjectController`'s import endpoints default it to `true`.
  So on the REST/GUI path MS1-only compounds do reach the jobs, and `checkInput()` is what protects the
  ones needing MS/MS.

  Only two production jobs opt out: `SiriusSubToolJob` (formula annotation works on MS1) and
  `AddConfigsJob` (must run for every instance). Downstream tools such as fingerprint computation inherit
  `true` and skip MS1-only instances, which is the intended behaviour rather than a bug to fix.
* `isAlreadyComputed(inst)` — normally `return inst.hasXxxResult();`. This is the *only* thing standing
  between your job and recomputation on every run.
* `computeAndAnnotateResult(inst)` — do the work, then persist with `inst.saveXxxResult(...)`.

Progress, interruption checks and the recompute decision are already handled around your method. Call
`checkForInterruption()` inside long loops and `updateProgress()` for anything slow.

**`isRecompute()` is state on the compound, shared by every job in its chain.** It is declared on
`Instance`, so two compounds in one run can carry different values. `AddConfigsJob` initialises it from
the `RecomputeResults` config (default `False`), which is what both `--recompute` on the CLI and
`JobSubmission.recompute` over REST write. Because `AddConfigsJob` runs at the head of every chain, that
initialisation also clears the flag between computations — it is in-memory state on a long-lived
`Instance`, not a stored property. `InstanceJob` then sets it `true` immediately before calling
`computeAndAnnotateResult`, so that — in the base class's words — "following tools will recompute if
results exist": a preceding job produced fresh results, so a dependent job's stored results are stale and
its outcome may change.

The flag is also the gate for invalidation. `invalidateResults(inst)` runs **iff** results already exist
*and* the flag is set — both call sites, in `InstanceJob` and `DataSetJob`, are nested that way. The flag
and the invalidator are two halves of one mechanism.

**Do not branch on `isRecompute()` inside `computeAndAnnotateResult`** — it is always `true` there. On a
fresh project the job ran because `!hasResults`, and the flag was flipped on the way in.

## Keep CPU work in a nested job

The class comment says a scheduler job is for organising the workflow, "NOT for CPU intense task -> use a
nested CPU job instead". A scheduler job and its nested jobs run on **different executors**: the job body
on the job manager's pool (`pool-N-thread-M`), the nested job on a `ForkJoinPool` worker.

```java
List<IdentificationResult> results = submitSubJob(sirius.makeIdentificationJob(exp)).awaitResult();
```

Blocking on `awaitResult()` in a scheduler job is fine and expected — that is what the pool is for.
Forward progress from the nested job with `addJobProgressListener` if it is long-running.

## The DataSetJob contract

* `isInstanceValid(Instance)` is called **once per instance**;
* `computeAndAnnotateResult(List<Instance>)` is then called **once**, with the whole batch;
* an upstream failure does not abort the batch: the failed instance is **left out** of the list rather
  than passed in broken, and the job runs over the survivors with `hasFailedInstances()` true and the
  count in `getFailedJobs()`. That tolerance is the default (`ReqJobFailBehaviour.WARN`); the
  two-argument constructor changes it.

Persist per instance inside the loop; there is no batch-level save.

**Recompute is decided for the whole batch, not per instance.** `DataSetJob.compute()` uses
`anyMatch` — if *any* instance has results, results exist; if *any* instance wants recompute, the whole
batch recomputes. `InstanceJob` decides this per compound, so the same `isAlreadyComputed` implementation
behaves differently in the two base classes.

## Wiring it up so it runs

A job needs an options class to be reachable as a subtool. `AddConfigsJob` is the exception: it has no
options class and no command name, because `ToolChainWorkflow` prepends it to every chain directly.
Minimum for a job users invoke:

```java
public class MyOptions implements ToolChainOptions<MyJob, InstanceJob.Factory<MyJob>> {
    @Override public InstanceJob.Factory<MyJob> call() {
        return new InstanceJob.Factory<>(MyJob::new, getInvalidator());
    }
    @Override public Consumer<Instance> getInvalidator() { return Instance::deleteMyResult; }
    @Override public List<Class<? extends ToolChainOptions<?,?>>> getDependentSubCommands() { return List.of(); }
}
```

plus a `@CommandLine.Command(name = "...")` annotation and an entry in the `toolChainTools` map in
`WorkflowBuilder`'s constructor. `add-sirius-subtool` covers that options class in full — the interface
choice, registration, and how the dependency lists shape the command tree.

**The map entry is the step that fails quietly.** Without it the class still compiles and still lands on
the classpath, but the command is absent from the root spec and `parseArgs` rejects the name with
`UnmatchedArgumentException`. `getInvalidator()` is abstract, so it cannot be forgotten — but a *wrong*
one silently leaves stale results behind, because it is what runs on the recompute path.

## Complex jobs: shape first

For a job with real algorithmic content, write the skeleton first — base class, `isAlreadyComputed`, the
`save`/`has`/`delete` triple on `Instance`, the options class and the map entry — and confirm it runs and
is skipped correctly with a stub body. Then fill in the computation. The wiring and the algorithm fail in
completely different ways, and debugging them together is much harder than in sequence.

## Traps

* **`isAlreadyComputed` returning a constant** defeats the whole mechanism. `return false` recomputes on
  every run. `return true` means the job never runs *at all* unless recompute is explicitly requested —
  and then it runs on every run, invalidating first.
* **A new result type needs three methods on `Instance`** — `saveXxxResult`, `hasXxxResult` and
  `deleteXxxResult`. The job uses the first two; the options class's invalidator uses the third. They
  write through immediately: after `save`, `has` is `true`; after `delete`, `false`.
* **Cancellation is cooperative — `checkForInterruption()` or nothing.** `ToolChainJobImpl.cancel`
  deliberately avoids interrupting the thread, because interrupting closes database channels. After
  `cancel(true)` the worker's interrupt flag is still `false`, and the job stops only because
  `checkForInterruption()` throws. A loop that never calls it cannot be cancelled at all.
* **`PostprocessingJob` is not a `ToolChainJob`** — no invalidator and no recompute handling. Only one
  can run per workflow, and the type enforces it: `ToolChainWorkflow` holds a single nullable
  `PostprocessingJob<?>` field and submits it once at the end.
