---
name: add-sirius-subtool
description: Adds a subtool (a `sirius <command>`) to the picocli command line — choosing between StandaloneTool and ToolChainOptions, registering it in WorkflowBuilder, and declaring how it chains with and invalidates other tools. Use when adding or renaming a CLI command, when a new subtool does not show up in the help, or when chained tools run in the wrong order or leave stale results behind.
---

# Add a SIRIUS subtool

A subtool is a picocli command class under `sirius_cli/src/main/java/de/unijena/bioinf/ms/frontend/subtools/`.
`WorkflowBuilder` registers it, and its parse handler dispatches on the interface the class implements —
so the interface decides how the command is registered and when it runs.

## Which interface

| Implement | When | You write |
|---|---|---|
| `StandaloneTool<W>` | the command does its own work and needs no compounds from other tools | `makeWorkflow(rootOptions, config)` |
| `ToolChainOptions<O,F>` | the command computes over the compounds of a project and can be chained | `call()`, `getInvalidator()`, `getDependentSubCommands()` |

A `StandaloneTool` **ends the chain**: the handler drills past any further subcommands to the deepest one
and returns only that workflow — `custom-db show` yields the workflow of `ShowDBOptions`, not of
`custom-db`. A `ToolChainOptions` is appended to a list of job factories that all run over the same
project.

`PreprocessingTool` (turns input into instances — `lcms-align`) and `PostprocessingTool` (runs last —
`summaries`) also exist and change rarely. A `PostprocessingTool` displaces the default
`ClosingProjectPostprocessor`, so its job has to close the project itself.

## A tool-chain subtool

Write the job first. `ToolChainOptions<O extends ToolChainJob<?>, F extends ToolChainJob.Factory<O>>` is
generic over the job type, so the options class cannot compile before the job exists — see
`write-toolchain-job` for the job contract.

```java
@CommandLine.Command(name = "my-tool", aliases = {"mt"}, description = "...",
        versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MyToolOptions implements ToolChainOptions<MyToolJob, InstanceJob.Factory<MyToolJob>> {
    public MyToolOptions(DefaultParameterConfigLoader configOptionLoader) { /* the map entry calls this */ }

    @Override public InstanceJob.Factory<MyToolJob> call() {
        return new InstanceJob.Factory<>(MyToolJob::new, getInvalidator());
    }
    @Override public Consumer<Instance> getInvalidator() { return Instance::deleteMyToolResult; }
    @Override public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of(NextToolOptions.class);
    }
}
```

Then add an entry to the `toolChainTools` map in `WorkflowBuilder`'s constructor. **The map entry is what
makes the command exist.** Without it the class still compiles and still lands on the classpath, but it is
absent from the root spec and `parseArgs` rejects the name with `UnmatchedArgumentException`. `passatutto`
sits in exactly that state: a complete `ToolChainOptions` annotated `@Command(name = "passatutto")` whose
map entry is commented out, unreachable from the CLI.

### The two dependency lists control different things

`getSubCommands()` unions `getDependentSubCommands()` with `getFollowupSubCommands()`, and that union
builds the command tree — it is what may follow your tool on the command line. Only the **dependent** list
drives invalidation, and `configureInvalidator` takes its **transitive closure**: registering your tool
attaches the invalidator of every tool reachable downstream, so a recompute of yours clears all of them.

* A tool that consumes your results → `getDependentSubCommands()`.
* A tool that should merely be reachable after yours → `getFollowupSubCommands()`.

Filing a consumer as followup keeps it reachable, but `configureInvalidator` walks the dependent list
only, so that tool's invalidator is never attached and its results survive a recompute of yours. Nothing
reports it.

Execution order is command-line order. Data flow is decided separately by `assignEarliestInputProvider`,
which scans backwards over the parsed tools without stopping at the first hit — so the **earliest** tool
that declares yours as dependent becomes your input provider.

## A standalone subtool

```java
@CommandLine.Command(name = "my-cmd", description = "...")
public class MyCmdOptions implements StandaloneTool<Workflow> {
    @Override public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> { /* the entire command */ };
    }
}
```

Registration takes **two edits** in `WorkflowBuilder`: a field, and an entry in the `standaloneTools()`
stream. That method enumerates its fields explicitly, so a field on its own leaves the command
unreachable.

A tool belonging to another front-end goes through the `List<StandaloneTool<?>>` constructor argument
instead of an edit to `WorkflowBuilder`. `SiriusMiddlewareApplication` passes `MiddlewareAppOptions` that
way, which is how `sirius service` (aliases `rest`, `REST`) exists without the CLI knowing about it.

## Confirm it is reachable

Registration failures are silent, so read the root spec rather than the source:

```java
ApplicationCore.VERSION();                                   // static init first
WorkflowBuilder wb = new WorkflowBuilder(
        new CLIRootOptions(new DefaultParameterConfigLoader(), new NitriteProjectSpaceManagerFactory()));
wb.initRootSpec();
wb.getRootSpec().subcommands().containsKey("my-tool");       // false when registration is missing
```

A throwaway JUnit test run with `./gradlew :sirius_cli:test --tests …` is enough. Dumping `subcommands()`
recursively also shows the nesting, which is the fastest way to see whether your dependency declarations
produced the tree you intended.

## Traps

* **One options object and one `CommandSpec` serve every parent.** A chain tool's spec is registered under
  the root, under `config` and under each preprocessing tool, each registration in its own `CommandLine`
  wrapper, and the spec back-references only the wrapper registered last — the root's. An injected `@Spec`
  therefore reads the correct parse result for `sirius formulas` and **null** for `sirius config formulas`,
  the same tool reached through another entry point; once anything has been parsed through the root it
  returns that earlier parse instead of null. Always take the parse result from the
  `validate(parseResult)` parameter, which is the one that really parsed your tool.
* **`call()` runs while the command line is parsed, not when the workflow runs.** `WorkflowBuilder.execute`
  invokes it during `makeWorkflow`, so anything it throws aborts the run before any computation starts.
  Keep it cheap and free of side effects.
* **Each alias is its own key in the command tree.** `aliases = {"trees", "formula", "sirius"}` puts four
  entries in the root spec for one tool, and a reachability check succeeds for any of them.
* **`descriptionKey` names a config key, not a resource bundle** — `add-config-parameter` covers the
  option surface, including the `Default: null` that a mistyped key renders silently.
* **The REST service builds the same tree with different arguments** — `BackgroundRuns` passes
  `spaceManagerFactory = null` and `closeProject = false`. A `PreprocessingTool` reached on that path
  throws `IllegalStateException("Preprocessing tool requires a ProjectSpaceManagerFactory!")`, and no
  `ClosingProjectPostprocessor` is appended there. Both are invisible when testing through the CLI.
