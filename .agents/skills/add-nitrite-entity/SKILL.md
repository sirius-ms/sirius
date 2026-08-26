---
name: add-nitrite-entity
description: Stores and queries entities in the Nitrite document storage behind SIRIUS project spaces and custom structure/spectral databases. Use when adding or changing a stored entity, collection, index, optional field or serializer, when a project-space read or write is slow, when choosing between in/eq filters or insertAll/upsertAll, or when a project fails to open with "is not registered" or "has no primary key".
---

# Store and query entities in the Nitrite document storage

Project spaces and custom structure/spectral databases are Nitrite 4 documents on an MVStore backend,
behind the `Database` abstraction in `:document-storage` (`NitriteDatabase`). The project-space schema is
declared in `:ms_persistence_oss:ms_persistence_document_storage_oss`, the custom databases in
`chemical_db_oss/chemdb_nitrite_oss`. The pre-Nitrite project-space directory format is obsolete — do not
extend it. Nitrite itself is a patched fork here, so where upstream behaviour and ours disagree, the
patch notes in `gradle/libs.versions.toml` are the reference.

## Add an entity

1. **Entity class.** A field annotated `jakarta.persistence.Id`, plus Lombok
   `@NoArgsConstructor @Getter @Setter` (`@SuperBuilder` where the existing ones use it). Jackson
   instantiates through the no-arg constructor.
2. **Register it** in `SiriusProjectDocumentDatabase.buildMetadata()` (project space) or the relevant
   `initMetadata` (custom databases), with its indices:
   `.addRepository(MyEntity.class, Index.nonUnique("alignedFeatureId"))`. Pass the primary-key field name
   explicitly — `.addRepository(MyEntity.class, "formulaId", …)` — when it is not the annotated `@Id`.
3. **Name the parent key `alignedFeatureId`** if the result hangs off a feature. Cascade delete collects
   its classes by reflecting for a field of exactly that name (`SiriusProjectDatabaseImpl.getRelatedToAF`),
   so any other name leaves rows behind when the feature is deleted.
4. **Declare large fields optional**: `.setOptionalFields(MyEntity.class, "fingerprint")`. They are then
   omitted from every read that does not name them.
5. **Add a serializer** for bulk numeric data — `.addSerialization(…)`, as `FingerprintCandidate` does.
6. **Searchability is a separate mechanism.** `@IndexField` feeds the Lucene search index, not any
   Nitrite index, and it belongs on the API model that gets indexed — an entity carries it only where an
   API model embeds that entity. The `add-searchable-field` skill covers it.
7. **Migrate existing projects** when they need the new field backfilled: raise
   `ProjectSchemaMigrator.CURRENT_SCHEMA_VERSION` and add the conversion step. `migrateIfNeeded` runs on
   every open and returns in O(1) once the stored version is current. `SIRIUS_PROJECT_SCHEMA_VERSION` is a
   different constant — it sets Nitrite's own store schema version, not the data migration.

Steps 1 and 2 fail loudly: an entity with no `@Id` throws `IllegalStateException: … has no primary key. The
primary key must be annotated with jakarta.persistence.Id!` while the metadata is built, and inserting an
unregistered class throws `IOException: class … is not registered.` Steps 3–6 are silent — the code runs and
the data looks right.

## Indices

The primary key gets a unique index automatically. Nitrite's planner picks an index by matching the
filter's field string against the index's field strings for exact equality:

* **A compound index is ordered.** `Index.nonUnique("alignedFeatureId", "structureRank")` serves a filter
  on `alignedFeatureId`, or on both, but a filter on `structureRank` alone scans. Order the fields by how
  you query them. Changing the order of an existing index rebuilds it on the next open.
* **Nested paths are indexable.** `Index.nonUnique("alignedFeatureId", "searchResult.rank")` is a live
  project-space index, and a dotted path also indexes a field inside a `List<…>`. What scans is a filter on
  a path with *no* index — which is why `Filter.where("candidate.inchikey")` in `ChemicalNoSQLDatabase` is
  a full scan: the declared indices there are `formula` and `mass`.
* **A misspelled path still creates an index.** Nitrite does not check the path against the class, so the
  index exists, is listed, and never matches anything.
* **A dotted path into a `List` matches only through an index.** On a scan, `document.get("children.name")`
  yields the whole list of element values, which no scalar `eq` matches, so the filter returns nothing: a
  wrong answer rather than a slow one, and one that comes back if the index is ever dropped. `elemMatch` finds
  list elements with or without an index (always as a scan) and is the only form that requires several
  conditions to hold for the *same* element. So use `elemMatch` for list membership, and a dotted path only
  where it is indexed. A dotted path into a single nested document — `candidate.inchikey` — matches either
  way; only lists have this trap.

## Reading: `in` or per-key `eq`, decided by the index

`in` and `notIn` select an index and then walk *every* entry in it, hash-testing each against the value set
(`InFilter.applyOnIndex`), where `EqualsFilter.applyOnIndex` is a single `indexMap.get`. An `in` therefore
costs more the larger the collection gets, while a per-key `eq` stays flat: for a five-key lookup the two
are ~20× apart at 20k documents and ~100× at 80k.

| To fetch | Call |
|---|---|
| one row by primary key | `getByPrimaryKey` |
| many rows by primary key | `findAllByPrimaryKeyStr` |
| many rows by an indexed non-id field | `findAllByIndexedFieldStr` |
| many rows to delete by primary key | `removeAllByPrimaryKey` |

Each batch helper fans the key list into one indexed lookup per distinct key.

**Whether `in` or per-key `eq` wins depends entirely on whether the path is indexed, so establish that
first.** A filter on an unindexed field — or on a non-leading component of a compound index — scans the
collection and deserializes every document, and `in` and `eq` cost the same per document. There a single
`in` is the right call: it scans once where k lookups scan k times. Once the path is indexed the ordering
reverses, because `eq` becomes a point lookup while `in` still walks the whole index. Reach for
`findAllByIndexedFieldStr` and friends on indexed paths only; on an unindexed path they multiply the scans.

To see which of these a query actually does, read Nitrite's own plan:
`Cursor.getFindPlan()` names the chosen index, and `getIndexDescriptor() == null` together with a non-null
`getCollectionScanFilter()` is a full scan. A test reaches the `Nitrite` handle through
`NitriteDatabase`'s private `db` field.

## Write in batches, and keep documents small

* **`insertAll` is one batch; `upsertAll` is not.** `upsertAll` runs one update per object, and every
  object must already carry a primary key: an unkeyed one fails the whole call with
  `IOException: id can not be null`. `insertAll` fills missing keys from the registered supplier and then
  inserts in a single batch, so for new records it is both the faster and the only working option.
* **Request optional fields explicitly.** `fingerprint` and `spectrum` are declared optional precisely so
  ordinary reads skip them; pass the name in `withOptionalFields`, or call `injectOptionalFields`
  afterwards, only where the value is used. Where the omitted fields dominate the document — which is the
  case in which you would declare them optional at all — skipping them makes the read roughly two to three
  times cheaper. The declared string must match the stored field name — a field
  renamed without updating its `setOptionalFields` entry is simply loaded on every read again, with no
  error and no log line.
* **Store bulk numeric data as a `byte[]` written by a custom serializer.** Declaring the field
  `double[]` does not carry through: the Jackson mapper stores it as an `ArrayList` of boxed `Double`,
  and MVStore's default data type then serializes that. A `byte[]` field is stored as a `byte[]`.
  Oversized documents also cost on every read — importing a spectrum above 1000 peaks warns that it
  `contains an unexpectedly high number of peaks`.
* **Batch a group of writes into one `write(…)`** so they commit together, as the fold-change delete does.
  The batch helpers above are not atomic on their own. Reads nest inside `write` — the write lock is
  reentrant — but a write inside `read` deadlocks, since the read lock does not upgrade.
* **MVStore is append-only.** Updates and deletes leave dead chunks, and the file grows rather than shrinks
  when you delete: dropping 90% of a 32 MB store leaves 33 MB, which `compact()` then rewrites to 3 MB. Make
  it the last thing you do, because it closes the store and does not reopen it — every later call fails with
  `IOException: Nitrite database is closed!` (`NoSQLProjectSpaceManager` exposes it as its own operation for
  that reason). Compaction goes through H2's multi-threaded path, which races in h2-mvstore 2.4.240
  (H2 issue #4286): with assertions on — Gradle enables them in test JVMs — it aborts above roughly ten
  thousand documents with an `AssertionError` out of `MVStoreTool`, leaving the file byte-for-byte
  untouched. Running with `-Dh2.compactThreads=1` avoids it.

`:document-storage` tests cover this layer directly; `./gradlew :document-storage:test -q` is a fast loop.
