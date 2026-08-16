# TODO later: move API error documentation to typed exceptions

Notes on a known limitation of the current error-documentation setup, and the migration that would remove it.
Nothing here is urgent; the current solution works and is release-ready.

## Where we are

Error responses in the OpenAPI document come from two places:

- **Generic rules** in `ErrorResponseDocumentation`: every operation can return 500, anything with a path
  variable can return 404, anything taking input can return 400.
- **Per-endpoint wording** on the handler method, as `@ApiError(status = …, value = "…")`, with `@NoApiError(…)`
  to opt out of a generic rule that does not apply.

Both are applied through an `OperationCustomizer` (plus a small `OpenApiCustomizer` for the actuator endpoints,
which have no `HandlerMethod`), *after* springdoc has derived the success response from the return type.

## Why not swagger's `@ApiResponse`

Declaring any `@ApiResponse` on a handler method makes springdoc discard the responses it derived from the return
type - verified empirically, and it happens even for an annotation carrying nothing but a description. The success
response then has to be restated by hand, and for generic return types it cannot be restated faithfully:

- naming the raw type (`PagedModel`) erases the element type, and
- referencing the generated schema by name (`PagedModelDatabaseStructure`) stops springdoc emitting it, leaving a
  dangling `$ref` and dropping the model from `components`.

So `@ApiError` is a project annotation on purpose. It is read by us, never by springdoc.

## Why not javadoc `@throws`, which would be the natural place

This was the preferred option and it does not work today, for two independent reasons:

1. **springdoc ignores `@throws` on handler methods.** Its javadoc provider has `getMethodJavadocThrows` and
   `GenericResponseService` references it, but tested both with and without the exception declared in the method's
   `throws` clause, the text appears nowhere in the generated operation. It appears to serve `@ExceptionHandler`
   methods in `@ControllerAdvice`, not handlers.
2. **therapi keys `@throws` entries by exception type and keeps only the last one.** Verified in isolation:

   ```
   sameType   throwsEntries=1     # three @throws ResponseStatusException declared, only the last survived
   twoTypes   throwsEntries=2     # two different exception types, both survived
   ```

   We raise every API error as `ResponseStatusException` with the status as a constructor argument, so an endpoint
   that genuinely answers both 400 and 409 - `createDatabase` does - could only ever document one of them, and
   silently.

Reason 1 we could work around, since we could read therapi ourselves from the `OperationCustomizer`. Reason 2 is
the blocker.

## The migration that fixes it

Introduce typed exceptions carrying their status, e.g.

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoSuchDatabaseException extends RuntimeException { … }
```

and replace `throw new ResponseStatusException(HttpStatus.NOT_FOUND, …)` in the service layer with them. Then:

- one `@throws` per exception type per method is enough, so therapi's collapsing stops mattering;
- the documentation lives in the javadoc next to `@param` and `@return`, where it belongs;
- the IDE generates the `@throws` stub, so the author is reminded to write it;
- springdoc can additionally infer some responses from `@ResponseStatus` on its own.

`@ApiError` / `@NoApiError` would then be retired in favour of javadoc, and `ErrorResponseDocumentation` would
read therapi instead of the annotations. **The endpoints themselves would not have to change** - only the
customizer and the javadoc.

Scope: roughly a dozen `throw new ResponseStatusException(...)` sites in `ChemDbServiceImpl` alone, plus the rest
of the service layer. Worth doing as its own change, not alongside a release.

## Smaller follow-ups

- `ErrorResponseDocumentation` documents that any endpoint with parameters can answer 400. That is true (Spring
  returns 400 for a type mismatch on a query parameter) but coarse. Once error handling is typed, it could be
  narrowed to endpoints that actually validate.
- Nothing verifies that a documented status is one the endpoint really returns. Only a test could, and no such
  test exists. If the error contract ever becomes load-bearing for clients, consider asserting the documented
  statuses against the ones the integration tests actually observe.
