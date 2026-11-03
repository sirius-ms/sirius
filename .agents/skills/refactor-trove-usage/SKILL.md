---
name: refactor-trove-usage
description: Frames and executes the move off the legacy trove4j collections — sizing a candidate refactor's effort, blast radius and risk, putting the choice to the user, and mapping trove types and idioms onto fastutil. Use when a task makes you modify code that imports gnu.trove, when deciding between keeping a change on trove4j and refactoring it, or when a trove type sits in a signature you need to change.
---

# Refactor a trove4j usage

`trove4j` is legacy and never belongs in new code; `fastutil` or a native array replaces it. It is
still used by a large amount of existing code, so a task will drop you inside trove code long before
that migration is finished.

**The decision is the user's.** Do not quietly refactor a trove usage you happened to touch, and do
not quietly extend one either. Size the refactor, put the choice, and let the answer set the scope.

## Size it before you ask

Three numbers, cheap to get, and they decide the recommendation.

**Effort — how much code moves.** The imports and call sites in the file you are touching:

```bash
git grep -c "gnu\.trove" -- <file>                      # imports and uses in this file
git grep -lw "<TroveType>" -- '*.java' | wc -l          # files sharing the type
```

**Blast radius — who else sees the type.** This is the number that matters, and it splits sharply. A
usage that stays inside its file is a refactor that stops at the file boundary; one in a public or
protected signature or field moves every caller with it. Which kind you have is one grep:

```bash
# does this file's trove usage escape into its own API?
git grep -nE '^\s*(public|protected).*\bT[A-Z][A-Za-z0-9]*(List|Map|Set|Iterator|Procedure)\b' -- <file>
```

If it does escape, get the call sites from the IntelliJ index rather than grep (`AGENTS.md` §5) — it
resolves overloads and inheritance, which a text search across the tree will not. `trove4j` is
declared `implementation`, not `api`, so the type cannot reach a consumer through the compile
classpath; the callers to worry about are inside the same module, plus any module that declares trove
itself — `git grep -n "libs.trove4j" -- '*.gradle'` lists those, and
`build/ai-overview/module-dependencies.md` gives the edges between them.

**Risk — what is not protected.** Two specific exposures, both worth checking by hand:

```bash
git ls-files '*/src/test/java/*' | grep -i "<ClassName>"   # is the behaviour covered at all?
git grep -nE "@Json|Serializable|@Id" -- <file>            # is a trove field persisted?
```

Where the file has no test, the refactoring rule in `AGENTS.md` §3 governs: write the tests first,
and ask before starting the refactor.

A persisted trove field is the harder case: changing its type changes the stored format. It is not a
blocker, because `FastUtilJson` in `:chemistry_base` already supplies Jackson serializers for the
fastutil int/long/double/float collections and `Int2IntMap`, and `MetadataUtils` registers them for
the document storage. Only those types have a paved path; anything else needs its own serializer.

## Put the choice

Two options, as a poll rather than prose, each carrying its effort, blast radius and risk, and a
recommendation of one:

1. **Keep this change on `trove4j`.** The task stays its own size; the file remains legacy.
2. **Refactor it to `fastutil` or a native array.** Name what moves with it — call sites, tests to
   write first, a serializer if a persisted field changes.

Recommend **refactor now** when the usage is file-internal, no persisted field changes, and the
behaviour is covered or cheap to cover. Recommend **stay on trove4j** when the type is in a
public or protected signature, or a persisted field changes, or the code is uncovered and the
collection is incidental to the task. Say which of the three numbers drove it.

## Mapping trove to fastutil

| trove | fastutil |
|---|---|
| `TIntArrayList`, `TDoubleArrayList`, `TShortArrayList`, … | `ints.IntArrayList`, `doubles.DoubleArrayList`, `shorts.ShortArrayList`, … |
| `TIntHashSet`, `TLongHashSet`, `TCharHashSet` | `ints.IntOpenHashSet`, `longs.LongOpenHashSet`, `chars.CharOpenHashSet` |
| `TObjectIntHashMap<K>`, `TObjectDoubleHashMap<K>` | `objects.Object2IntOpenHashMap<K>`, `objects.Object2DoubleOpenHashMap<K>` |
| `TIntObjectHashMap<V>`, `TIntIntHashMap`, `TLongLongHashMap` | `ints.Int2ObjectOpenHashMap<V>`, `ints.Int2IntOpenHashMap`, `longs.Long2LongOpenHashMap` |
| `TCustomHashMap` / `TCustomHashSet` + `gnu.trove.strategy.HashingStrategy` | `objects.Object2ObjectOpenCustomHashMap` / `objects.ObjectOpenCustomHashSet` + `Hash.Strategy` |

The fastutil column is written as `<subpackage>.<Type>` under `it.unimi.dsi.fastutil`, which is how
that library splits per primitive. The type rename is the easy half; these four idiom differences
are where a mechanical port breaks:

* **The primitive accessors are renamed, and the boxing ones still compile.** `get(i)` → `getInt(i)`,
  `toArray()` → `toIntArray()`. fastutil's lists also inherit `Integer get(int)` and `Object[]
  toArray()` from `List<Integer>`, so a missed rename compiles and silently boxes on a hot path
  instead of failing the build. Grep the finished file for a bare `.get(` on a primitive collection.
* **Missing-key values are configured differently.** trove takes a no-entry value in the constructor
  and reports it via `getNoEntryValue()`; fastutil sets it with `defaultReturnValue(v)`. Both default
  to `0`, so an omitted call is invisible until a map legitimately stores `0`.
* **Counters are named differently.** `adjustOrPutValue(k, delta, initial)` and `increment(k)` become
  `addTo(k, delta)`, which returns the *previous* value.
* **There are no procedure interfaces.** `forEachEntry(TObjectIntProcedure)` becomes an entry-set
  loop; `Object2IntMaps.fastForEach` / `fastIterator` keep the per-step `Entry` allocation away.
  Note also that trove's `HashingStrategy` extends `Serializable` and `Hash.Strategy` does not, and
  its hash method is `computeHashCode(T)` against fastutil's `hashCode(T)`.

For an unchecked `getQuick`/`setQuick` loop, take fastutil's backing array with `elements()` — or
drop the collection for a plain `double[]`/`int[]`, which is the better answer whenever the size is
known up front.

## Finishing

Run the module's tests, not the whole build: `./gradlew :<module>:test -q`.

When a module loses its last `gnu.trove` import, drop `implementation libs.trove4j` from its
`build.gradle` too. Nothing pulls trove in transitively — the root build excludes the old `trove4j`
module from every configuration and forces `net.sf.trove4j:core` — so the declaration is the only
thing keeping it on the classpath. Confirm with:

```bash
./gradlew :<module>:dependencyInsight --dependency trove --configuration compileClasspath
```
