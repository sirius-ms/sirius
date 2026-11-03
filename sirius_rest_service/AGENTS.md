# AGENTS.md — `:sirius_rest_service`

The SIRIUS middleware/REST service (Spring Boot). Exposes project-space and computation operations
over HTTP; the OpenAPI spec generated here drives the `:sirius-sdk` client.

- **Code map**: `build/ai-overview/modules/sirius_rest_service.md` (regenerate with `./gradlew aiOverview`).
- **Test**: `./gradlew :sirius_rest_service:test`.
- `src/main/resources/templates/sirius_java_integrated/` is pre-built JS copied from
  `../sirius-js-monorepo` and tracked in git, but it is not source — `copyJSAppToFrontend` deletes the
  directory before copying, so an edit here is lost on the next sync. Make the change in the monorepo;
  see the `regenerate-sirius-sdk` skill.
- Spring Boot service (controllers/services/config). **API-surface changes** — adding or renaming
  REST endpoints, or changing request/response models (DTOs) — flow into the OpenAPI spec and the
  generated `:sirius-sdk`, so they require regenerating the SDK and can break clients (the GUI and
  external API users). Coordinate such changes. See root [AGENTS.md](../AGENTS.md).
