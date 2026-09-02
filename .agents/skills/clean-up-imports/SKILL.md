---
name: clean-up-imports
description: Removes unused imports from the Java files a change touches, using the IntelliJ MCP inspections to find them, since the IDE exposes no optimize-imports action over MCP and its reformat tool rewrites whole files. Use before committing, when asked to optimize or clean up imports, or when a compiler or inspection warning reports an unused import.
---

# Clean up imports before committing

Root `AGENTS.md` §7 asks for unused imports to be removed from every file a change touches before it is
committed. This is how to do it without hand-reading every import block, and without the two traps that
look like shortcuts.

## What the IDE does and does not offer

* **There is no optimize-imports tool.** `execute_tool` with an unknown command prints the full list of
  MCP tools the IDE exposes; none of them optimizes imports.
* **`reformat_file` is not a substitute.** It applies the code style to the whole file — wrapping,
  spacing, blank lines — and leaves the import block as it is. On a file whose formatting has drifted
  from the IDE style that is a large, unrelated diff. Do not run it for this purpose; if you did, revert
  the file with `git checkout --` before going on.
* **`lint_files` is the instrument.** It runs the IDE inspections and reports every unused import as a
  `WARNING` with the description `Unused import statement`, the 1-based `line` and the exact `lineText`.

## The procedure

**1. Collect the files.** Committed and uncommitted changes, Java only:

```bash
base=$(git merge-base HEAD master)
{ git diff --name-only --diff-filter=AMR "$base" HEAD -- '*.java';
  git diff --name-only --diff-filter=AMR HEAD -- '*.java'; } | sort -u > /tmp/changed_java.txt
```

Use `master` for a feature branch; for reviewing a single working tree, the second line alone is enough.

**2. Lint in batches.** Pass project-relative paths, `min_severity: warning`, and a generous `timeout`
(240000 ms is fine for ~25 files; some files here carry hundreds of warnings). Batches of 25–30 files
stay within budget. Check every result for `more: true`, `timedOut: true` or a `notAnalyzedReason` — a
batch that was cut short reports nothing for the files it skipped, and silence reads like a clean file.

**3. Filter the result.** A batch over files with many warnings exceeds the tool output limit and is
saved to a file instead; its shape is `{items: [{filePath, problems: [{severity, description, lineText,
line, column}]}]}`, and files without problems are omitted. Pull out the import findings with `jq`:

```bash
jq -r '.items[] | .filePath as $f | .problems[]? | select(.description|test("import";"i"))
       | "\($f):\(.line) \(.description) | \(.lineText|gsub("^\\s+";""))"' "$saved_result"
```

The `test("import")` net also catches `Unused import statement` phrased differently by another
inspection, and the odd unrelated hit (a method named `import…` reported as never used) is easy to drop
by eye.

**4. Remove the lines.** Match the reported `lineText` exactly — `import <name>;` — and delete that line
only. Do not resort or regroup the rest of the block: the IDE has not asked for it, and a reordered
import block hides the one real change in the diff. Wildcard imports the IDE collapsed earlier
(`import de.unijena.bioinf.jjobs.*;`) are the code style and stay.

**5. Compile what you touched.** `./gradlew :<module>:compileTestJava -q` for each module, or one
downstream module such as `:sirius_cli:compileJava` plus the test sources of the modules whose tests
changed. A build reshuffles tracked `.class.map` resources (root `AGENTS.md` §7) — revert those before
committing.

## Judgement calls

* An unused import in a changed file may predate the branch. Remove it anyway; the file is in the diff
  and the cleanup costs one line.
* The removal belongs in the commit that touched the file, or in a separate import-cleanup commit when
  the cleanup spans files no single commit owns. Do not fold it into an unrelated commit.
