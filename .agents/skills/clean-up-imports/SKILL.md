---
name: clean-up-imports
description: Removes unused imports from the Java files a change touches, using the IntelliJ MCP inspections to find them. Use before committing, when asked to optimize or clean up imports, or when a compiler or inspection warning reports an unused import.
---

# Clean up imports before committing

Root `AGENTS.md` §7 asks for unused imports to be removed from every file a change touches before it is
committed. This is how to do it without hand-reading every import block.

The IDE exposes no optimize-imports action over MCP, and **`reformat_file` is not a substitute**: it
applies the code style to the whole file — wrapping, spacing, blank lines — and leaves the import block as
it is. On a file whose formatting has drifted from the IDE style that is a large, unrelated diff. Do not
run it for this purpose; if you did, revert the file with `git checkout --` before going on.

## The procedure

**1. Collect the files.** Committed and uncommitted changes, Java only:

```bash
base=$(git merge-base HEAD master)
{ git diff --name-only --diff-filter=AMR "$base" HEAD -- '*.java';
  git diff --name-only --diff-filter=AMR HEAD -- '*.java'; } | sort -u
```

Use `master` for a feature branch; for reviewing a single working tree, the second line alone is enough.

**2. Lint in batches.** `lint_files` runs the IDE inspections and reports every unused import as a
`WARNING` with the description `Unused import statement`, the 1-based `line` and the exact `lineText`.
Pass project-relative paths, `min_severity: warning`, and a generous `timeout` (240000 ms is fine for ~25
files; some files here carry hundreds of warnings). A batch that size finishes inside that timeout, but
its result will usually exceed the tool's separate output limit and be saved to a file, whose path the
tool's message names — that file is what step 3 reads. Check every result for `more: true`,
`timedOut: true` or a `notAnalyzedReason`: a batch that was cut short reports nothing for the files it
skipped, and silence reads like a clean file.

**3. Filter the result.** The saved result has the shape `{items: [{filePath, problems: [{severity,
description, lineText, line, column}]}]}`, and files without problems are omitted. Pull out the import
findings with `jq`:

```bash
jq -r '.items[] | .filePath as $f | .problems[]? | select(.description|test("import";"i"))
       | "\($f):\(.line) \(.description) | \(.lineText|gsub("^\\s+";""))"' "$saved_result"
```

The net is deliberately loose, so read the hits instead of piping them straight into a delete: an
unrelated one (a method named `import…` reported as never used) is easy to drop by eye.

**4. Remove the lines.** Match the reported `lineText` exactly — `import <name>;` — and delete that line
only. Do not resort or regroup the rest of the block: the IDE has not asked for it, and a reordered
import block hides the one real change in the diff. Wildcard imports (`import de.unijena.bioinf.jjobs.*;`)
never reach this step — the inspection does not report them, not even an entirely unused one — so they stay.

**5. Compile what you touched.** `./gradlew :<module>:compileTestJava -q` for each module, or one
downstream module such as `:sirius_cli:compileJava` plus the test sources of the modules whose tests
changed. A build reshuffles tracked `.class.map` resources (root `AGENTS.md` §7) — revert those before
committing.

## Judgement calls

* An unused import in a changed file may predate the branch. Remove it anyway; the file is in the diff
  and the cleanup costs one line.
* The removal belongs in the commit that touched the file, or in a separate import-cleanup commit when
  the cleanup spans files no single commit owns. Do not fold it into an unrelated commit.
