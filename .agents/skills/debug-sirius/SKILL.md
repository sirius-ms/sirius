---
name: debug-sirius
description: Captures runtime values from a running SIRIUS process using the IntelliJ MCP debugger — logpoints, debug sessions, draining output and breakpoint cleanup. Use when the question is what the code actually did at runtime (a value, a branch taken, a count) rather than what it says, or when the alternative would be adding a println and rebuilding.
---

# Debug SIRIUS at runtime

Source answers "what should happen". For "what did happen" — the actual `precursorMz`, how many
formula candidates survived pruning, which adduct branch ran — use a **logpoint**: an expression
evaluated at a line and logged *without suspending*, needing no source edit and no rebuild. The
alternative in this repo is editing a file, recompiling the module, restarting, and remembering to
revert.

Requires the IntelliJ MCP server. If it is not connected, this skill does not apply.

## Starting a session needs approval

Starting a debug session runs a run configuration, which the IDE gates on user confirmation. Two ways
through, and the first is sufficient:

* **Have the user confirm.** Tell them you are about to start it, then call — the dialog appears at
  once and a prompt click lands well inside the timeout. No settings change needed.
* **Brave mode** — `Settings | Tools | MCP Server` → "Run shell commands or run configurations without
  confirmation (brave mode)" — makes it unattended. This is a real permission escalation: any
  connected client may then run commands and run configurations unprompted. Do not enable it, and do
  not assume it is on; it is the user's decision.

If nobody clicks, the call **times out** — here a timeout means "waiting for a human", not broken
tooling. Setting breakpoints is never gated, so a breakpoint call succeeding says nothing about
whether a session will start.

## The loop

1. **Set a non-suspending logpoint** — `xdebug_set_breakpoint` with `filePath`, `line`,
   `logExpression`, `suspendPolicy: NONE`. Check the returned `lineText` matches the line you meant.
2. **Start a long-running target** — `xdebug_start_debugger_session` with `configurationName`.
   Call `get_run_configurations` first: run configurations are **developer-local**, not checked in,
   and many point at `.sirius` project files on one machine. Discover the names, never assume one.
   Pick a REST service config — those stay up, which is what makes step 3 possible.
3. **Drain while the session is alive** — `xdebug_control_session` with `action: DRAIN_EVENTS`.
   Values arrive in `tracepointOutputsTail`; problems in `breakpointErrorsTail`.
4. **Stop and clean up** — `action: STOP`, then `xdebug_remove_breakpoint` with `owner: agent`.

**Example.** A logpoint in `SiriusMiddlewareApplication.main`, on the line just after `headless` is
computed so that both values are in scope:

    "args=" + java.util.Arrays.toString(args) + " | headless=" + headless

Against a `REST -p 8080 -s` service configuration it yields
`args=[REST, -p, 8080, -s] | headless=false`.

## Traps

* **A short-lived target loses the output.** Hit counts and tracepoint buffers live *with the
  session*. A configuration that exits in seconds (e.g. `formula --help`) is gone before you can
  drain, and `xdebug_list_breakpoints` then reports `hitCount: 0` even if the line did execute.
  Pick a target that stays up.
* **Logpoint output never reaches the program's stdout log** — only `DRAIN_EVENTS`. An empty run log
  is not evidence the probe failed.
* **Starting from `filePath` + `line` on a test is routed through Gradle**, which picks among the five
  test tasks and can choose the wrong one — a `*Test` class sent to `acceptanceTest` matches nothing,
  so the test never runs and the probe never fires. Prefer a run configuration; see
  `build-and-test-sirius` for the task taxonomy.
* **`logExpression` must be side-effect-free.** No mutation, I/O, or iterator/stream advancement —
  this code is stream-heavy, so that is easy to violate by accident.
* **Cold start is slow.** The IDE delegates building to Gradle; pre-build the target module
  (`./gradlew :<module>:classes -q`) rather than letting the session start absorb a full build.
* **`breakpointErrorsTail` reports stale breakpoints** as "Breakpoint does not belong to any class" —
  a breakpoint whose line no longer holds code. It will never fire. Read this list; it also surfaces
  invalid `condition` expressions. It covers **every** breakpoint in the session, including the
  developer's own, so it is a cheap way to spot theirs going stale — report those, do not remove them.
* **Only one SIRIUS service may run at a time.** Stop anything already running first, including a
  `bootRun` started from a shell.

## Cleaning up

`xdebug_remove_breakpoint` defaults to `owner: agent`, which removes only breakpoints set through
MCP. **Never pass `owner: user`** — those are the developer's own, and there may be dozens.
Verify with `xdebug_list_breakpoints` and check the total returns to what it was.
