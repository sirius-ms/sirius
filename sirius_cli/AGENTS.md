# AGENTS.md — `:sirius_cli`

The SIRIUS command-line interface (picocli-based): subtools/workflows that orchestrate the core
algorithms (formula ID, CSI:FingerID, CANOPUS, …) over a project space.

- **Code map**: `build/ai-overview/modules/sirius_cli.md` (regenerate with `./gradlew aiOverview`).
- **Test**: `./gradlew :sirius_cli:test`. The runnable distribution is assembled via `:sirius_dist`.
- CLI commands are wired with picocli annotations; follow the existing subtool/command pattern when
  adding one. See root [AGENTS.md](../AGENTS.md).
