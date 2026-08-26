---
name: add-searchable-field
description: Makes project data searchable through the Lucene index that backs the search bars and the searchQuery parameters of the REST API, and explains how that index is built, persisted and rebuilt. Use when adding or changing an @IndexField, when a search query returns nothing for a field that should match, when a searchable-fields list is missing an entry, when choosing between a Lucene search field and a Nitrite index, or when reasoning about index staleness, rebuilds and search-versus-database disagreement.
---

# Make project data searchable

Project data can be filtered over REST in two independent ways, and only the second is what this skill
calls *searchable*:

* a **purpose-built endpoint or parameter** backed by a Nitrite `Filter` over the persistence entities —
  how lookups by id, by parent and by tag group work;
* the **`searchQuery` parameter**, an Apache Lucene query against an index built from the **middleware
  API models**.

So a field added to a persistence entity becomes queryable as soon as an endpoint asks for it, and it
never appears in `searchQuery` or in a searchable-fields list until it reaches an indexed API model.
Choosing between the two is its own question — see the last section.

A class is searchable only when **both** halves are in place: its fields are annotated, and something
indexes it. The annotations are only the schema — documents arrive via the build path in
`NoSQLProjectImpl.createSearchIndex` and the incremental calls beside it. A class can carry a full set
of annotations and still index nothing, so `createSearchIndex` is the list to check, and it is the
shorter of the two.

The index is **derived state**. Nitrite holds the truth and the index is rebuilt from it by paging API
models back out of the database, so it is always safe to throw away.

## Where the index lives

There are two copies:

| | |
|---|---|
| the **persisted blob** — `PersistentSearchIndex` in the project | one `byte[]` per indexed class, keyed by simple class name; travels with the project |
| the **working copy** — a Lucene `Directory` | what Lucene actually reads and writes |

`io.sirius-ms.project.index.inMemory` decides what the working copy is, and it **defaults to in memory**
— a started service logs `Running in in-memory search index mode.` and creates no index directory at
all. Setting it false puts the working copy on disk, one directory per project under `indexingHome`,
which comes from `io.sirius-ms.project.index.homeDir` or a per-session temp directory
(`sirius-search-indexes`) when that is unset. Either way **the persisted blob is authoritative**: on open
the working copy is cleared before the blob is deserialized into it, so files left from a previous session
are never reused. On close the blob is written back — but only if the index is non-empty *and* complete;
otherwise the record is deleted so the next open rebuilds.

`write.lock` is deliberately excluded from the persisted blob. Do not "fix" that.

## Add a searchable field

1. **Annotate the field on the API model**, not the entity:
   `@IndexField(fullTextSearch = true, sortable = true)`. Exactly one field per class carries
   `documentId = true`; that is the value updates and deletes are matched on.
2. **If the field hangs off an optional sub-object, add its `OptField` to the set the rebuild loads**
   (`AlignedFeature.INDEXED_OPT_FIELDS`, and the equivalent argument in each build call). The rebuild
   fetches only those, so a field outside the set indexes as null however correctly it is annotated.
3. **Keep the incremental paths in step.** Index maintenance is *not* automatic on a Nitrite write:
   `NoSQLProjectImpl` calls `addDocuments`, `updateDocuments`, `removeDocumentsById` and
   `updateDocumentsFields` explicitly, under `searchIndexLock`. Only tag definitions ride Nitrite
   listeners. A new field that some write path does not populate goes stale there.
4. **Complex types need a mapper.** One Java field to several Lucene fields, or any custom encoding, is
   a `FieldMapper` referenced by `@IndexFieldWithMapper` — as `TagMapper`, `FormulaElementMapper`
   (element counts as range-queryable points), `FoldChangeMapper` and `CompoundClassesMapper` do.
5. **Rebuild existing projects.** Nothing versions the index *schema*, so a project that already carries
   a persisted index keeps it, silently missing the new field, until something forces a rebuild.

What the attributes do: `stored` keeps the value retrievable (and is required for partial updates),
`fullTextSearch` chooses a tokenized `TextField` over a keyword `StringField`, `sortable` adds doc
values, `defaultSearchField` includes the field in queries that name no field, `name` overrides the
Lucene field name, and `queryRewriter` rewrites incoming terms for that field — as
`InchiKey2DQueryRewriter` does by truncating a full InChIKey to its 2D prefix.

Most mistakes here are **silent**: a field that is not annotated, is annotated on the entity instead of
the API model, or sits outside the loaded `OptField` set simply never matches. Two fail loudly, the
first time anything touches that class's index rather than at startup: no `documentId` field gives
`IllegalArgumentException: No document ID field defined!`, and a second one
`IllegalStateException: Document ID field already set.` One fails only once the data arrives: `sortable`
on a collection maps and indexes happily until a document carries two values, and then throws
`IllegalArgumentException: DocValuesField "…" appears more than once in this document`, so single-valued
test data will not catch it. A partial update touching a field that is not `stored` throws
`UnsupportedOperationException`.

## Rebuild and staleness

A rebuild happens when the index is empty on open, when `createSearchIndex(force)` is called, or when the
persisted blob is judged stale. Staleness is a tolerance, not an equality: the blob records the database
commit it was written against (`storageCommitId`), and it is discarded when the database's commit has
moved backwards relative to that or ahead by more than five — which catches a project modified by a build
that did not maintain the index, and forgives the handful of commits a normal session adds after the blob
was written.

While a rebuild runs the index is marked incomplete, and any failed write marks it incomplete too, so an
interrupted build is neither trusted nor persisted. Compaction rewrites the project's commit ids, so the
persisted blobs are reanchored to the new commit afterwards; without that every index would look stale on
the next open.

## Reads

Writes are near-real-time: they are visible once a searcher refreshes, not when they are made, and
nothing commits per write — durability comes at close or when the blob is serialized. The read path
refreshes best-effort, so it can no-op while a refresh is in flight; marking a build complete forces a
blocking refresh instead, which is what guarantees a finished rebuild is visible before the next query.

Choose the read by what you need back: `search` reconstructs whole API models from stored fields,
`searchIds` returns only ids to hydrate from Nitrite, and `searchFields` reads named stored fields
without rebuilding the object. Totals are exact — Lucene stops counting at a threshold and reports a
lower bound, so the code re-counts with a dedicated count when that happens. Deep paging still walks
every hit up to the offset; there is no `searchAfter` yet.

## Lucene index or Nitrite index?

Reach for the search index when the query needs free text, tokenization or ranking; ranges and boolean
combinations across many fields; or field names not known at compile time, such as tags. Use a Nitrite
index and a `Filter` for everything else — lookups by id or by an exact value, cascading deletes, and
anything that must be transactional with the write that caused it. Both can sort and page, so that is
not what decides it; what decides it is whether the database can express the query at all.
The two also compose, in both directions: a search can return ids that are then hydrated from the
database, and a saved tag group is a Nitrite row holding a Lucene query — found by `Filter`, executed by
the index.

Where the same field lives in both, they can disagree. Search reflects the index, which lags a write
until refresh, may be mid-rebuild, and is absent entirely for a class whose build path is not wired.
A direct `Filter` query always reflects the data.

## Verifying

The search tests live in `sirius_rest_service/src/test/java/.../service/search/` and cover the mapper
round trip, index completeness, concurrency, full-text behaviour, tag semantics, exact page totals and
searchable-field descriptions:

```bash
./gradlew :sirius_rest_service:test --tests "*service.search*" -q
```
