# AGENTS.md — `:sirius_gui`

The SIRIUS desktop GUI: Java Swing (FlatLaf look-and-feel, MigLayout, GlazedLists, SwingX, JSVG).
Presents projects, spectra, and identification results. Some views are web views: this module supplies
the JXBrowser Swing container, and the pages it displays are served by `:sirius_rest_service`.

- **Code map**: `build/ai-overview/modules/sirius_gui.md` (regenerate with `./gradlew aiOverview`).
- **Test**: `./gradlew :sirius_gui:test`. Packaged for distribution via `:sirius_dist`.
- Swing UI code — keep long-running work off the EDT and follow the existing panel/action patterns.
- The web-view assets are built in a separate repo and copied in pre-built; changing one is not an edit
  in this repo. See the `regenerate-sirius-sdk` skill and root [AGENTS.md](../AGENTS.md).
