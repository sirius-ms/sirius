---
name: add-rest-endpoint
description: Adds or changes a REST endpoint in the SIRIUS middleware — controller anatomy, DTO and optional-field conventions, reaching project data, error responses, and how an endpoint enters or stays out of the published OpenAPI spec. Use when adding, renaming, deprecating or hiding an endpoint, when introducing a request/response model, when marking something experimental or internal, or when a change to a controller or DTO needs to reach the generated SDK.
---

# Add a REST endpoint

Endpoints live in `sirius_rest_service/.../middleware/controller/`, their models in
`middleware/model/`, and everything they touch goes through `middleware/service/`. A controller never
reaches storage itself: it asks `ProjectsProvider` for a `Project`, and the `Project` implementation owns
the database. Keep that order — a controller that queries storage directly bypasses the locking and
index maintenance the service layer performs.

**Javadoc on an endpoint is API documentation, not a comment.** The `therapi` annotation processor bakes
it into the OpenAPI spec, so it reaches the Swagger UI, the generated SDK and external users. Write it
for them.

## Anatomy

A controller carries `@RestController`, a `@RequestMapping` prefix and a `@Tag` that names its group in
the spec. Project-scoped resources nest under `/api/projects/{projectId}/…`, plural and without a version
segment: there is no URI versioning here. What a consumer sees is decided by the marking mechanism below,
and the names they compile against are pinned by the guard test at the end.

Per endpoint:

* **`@Operation(operationId = …)` is optional.** Left off, the Java method name becomes the
  operationId — and the operationId becomes the method name in every generated client. So the Java
  method name is what callers compile against: camelCase verb + noun, paged variants ending in
  `Page`. Generators re-case it per language (camelCase for Java, snake_case for Python), so the
  words are yours to choose and the casing is not.
* **Set `produces`** (and `consumes` for uploads) on the mapping.
* **Ids are `String` path variables**, even where the entity stores a `long`.
* **Optional response parts use the `OptField` pattern:** a nested enum on the DTO, requested as
  `@RequestParam(defaultValue = "none") EnumSet<T.OptField> optFields`, and passed through
  `AnnotationUtils.removeNone(…)` before the service sees it. `none` is a sentinel meaning "nothing
  extra", not a value the service should receive.
* **Paged endpoints take `@ParameterObject Pageable`** and return `Page<T>`; the `Pages` helper turns a
  storage query into a `Page` whose total comes from a real count of the collection or the filter. New list
  endpoints are paged — the unpaged collection variants are deprecated.
* **Shared endpoint families come from mixins.** Interfaces in `controller/mixins/` —
  `TaggableController`, `StatisticsController`, `ComputeServiceController`, `ProjectProvidingController` —
  carry `default` methods with their own mappings, so a controller inherits a whole tag or statistics
  surface by implementing one. Add to the mixin when the family should grow, not to one controller.

## Errors

A `@RestControllerAdvice` handler turns exceptions into RFC 9457 `ProblemDetail` responses, so an endpoint
signals failure by throwing — `ResponseStatusException(HttpStatus.NOT_FOUND, …)` and friends — rather than
by returning an error body. `ProjectsProvider.getProjectOrThrow` already produces the 404 for an unknown
project id.

Document the failures with the repeatable `@ApiError(status = …, value = …)` annotation, and `@NoApiError`
to suppress a generic entry that cannot occur. **Do not add `@ApiResponse` for this.** Declaring even one,
even for an unrelated status, makes springdoc drop the success response it derived from the return type —
and the documented error entries still appear, so the only symptom is a missing `200` and, with it, the
schema of whatever the method returns.

## Reaching the data

`getProjectOrThrow(projectId)` then a method on `Project`. Two things a mutating endpoint must respect,
because neither is automatic:

* **Deletes cascade explicitly.** The `cascadeDelete…` methods collect the dependent rows; a new
  result type only participates if it follows the persistence conventions (see `add-nitrite-entity`).
* **The search index is maintained by hand.** A write that changes indexed data must also make the
  matching `SearchService` call, and those calls serialise on `searchIndexLock` — a delete blocks there
  until it can take that monitor. `add-searchable-field` lists which call belongs to which change. There
  is no `@Transactional` anywhere in this layer.

Work that takes longer than a request goes to `ComputeService`, which returns a `Job` immediately and
reports progress through the job endpoints. Controllers stay thin: no computation, no threads.

## Models

DTOs live under `middleware/model/<domain>/`, use Lombok accessors and a builder, and carry
`@JsonInclude(NON_NULL)` so absent optional parts vanish from the JSON rather than appearing as nulls.
`@Schema` supplies what javadoc cannot, including a stable name for a nested enum. These are the same
objects `add-searchable-field` calls the *API models* — the Lucene index is built over them, so a field
added here is also where an `@IndexField` would go.

**Domain types do reach the API surface.** Several models expose `chemistry_base` and persistence types
directly — molecular formulas, spectra, `ComputedSubtools`. Treat that as a decision with consequences
rather than a rule to follow or break: whatever you expose becomes public schema, is generated into the
SDK, and cannot be changed without breaking clients.

What breaks a generated client: renaming a field or an operationId, changing a type or nullability,
removing a response field, and **reordering an enum**.

## Whether your endpoint is published

Three mechanisms, in the order they act:

1. **Markers in the documentation.** `[EXPERIMENTAL]`, `[INTERNAL]` or `[DEPRECATED]` anywhere in the
   summary or description is what classifies an endpoint; the operationId of an experimental one also ends
   in `Experimental`. The match is made **per path, not per operation** — one marked operation puts the
   whole path on the list, and excluding a path removes every operation on it, so an experimental `DELETE`
   would take the stable `GET` beside it with it.
2. **A generated exclusion list.** `./gradlew :sirius_rest_service:updateUnstableExclusions` parses the
   spec for those markers and rewrites the path lists in `MiddlewareAppOptions`. The lists are generated
   output — mark the endpoint and re-run the task rather than editing them by hand.
3. **The run mode.** `--api-mode` maps those lists onto `springdoc.pathsToExclude`: `STABLE` hides
   internal, experimental and advanced paths, `STABLE_ADVANCED` hides internal and experimental,
   `ADVANCED` hides nothing, and the default `BASIC` hides only the advanced ones.

So an endpoint with no marker is public by default, and it reaches the SDK. `[DEPRECATED]` is the
exception to the pattern: it does not hide anything in any mode. A deprecated operation stays in the spec
carrying `deprecated: true`, which is what keeps the generated method in the SDK — annotated rather than
deleted — for clients still calling it. Mark a deprecation both ways: `[DEPRECATED]` in the text, and
`@Deprecated` on the Java method or parameter. In this codebase the two always travel together.

Two specs are committed next to the service — `openapi-spec-enums-as-ref.json` and
`openapi-spec-enums-as-string.json`. They are generated, and then read back: the SDK generator and the
exclusion task above both consume them, which is why the order matters. Regenerating them and the SDK is
its own procedure, and the `regenerate-sirius-sdk` skill owns it.

## Verifying

`ApiBackwardCompatibilityTest` guards the published surface: it pins the operationIds of endpoints
external users depend on, checks that paged endpoints are not marked experimental, and checks that the two
committed specs agree. `RequestPrimitiveContractTest` sits beside it. Run the API guard plus your own
controller test:

```bash
./gradlew :sirius_rest_service:test --tests "*middleware.api.*" -q
```

A failure there is not a broken test — it means the change alters a contract someone else compiles
against.

Note what it cannot tell you: it reads the **committed** specs, so it compares the published surface with
itself. Nothing in the build compares the spec to the code, which is why a new endpoint that was never
regenerated into the spec passes every check and still reaches nobody.
