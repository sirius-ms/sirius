---
name: write-agent-docs
description: Writes and edits the agent-facing instruction files in this repo — the root and per-module AGENTS.md files and the SKILL.md playbooks under .agents/skills. Use when adding or changing agent guidance, authoring a new skill, or restructuring AGENTS.md sections, so the result stays portable across Claude Code and Gemini and the cross-references stay valid.
---

# Write agent docs in this repo

## The file surface

| File | Role |
|---|---|
| `AGENTS.md` (root) | always-on context, loaded every session — the expensive one |
| `<module>/AGENTS.md` | short module orientation (`chemistry_base`, `sirius_cli`, `sirius_gui`, `sirius_rest_service`) |
| `.agents/skills/<name>/SKILL.md` | on-demand playbooks; canonical location |
| `CLAUDE.md` | one line, `@AGENTS.md` — an import, not a copy; resolved at launch, so editing `AGENTS.md` mid-session leaves your loaded copy stale — re-read it |
| `.gemini/settings.json` | `context.fileName: ["AGENTS.md"]` |
| `.claude/skills` | **generated** pointer to `.agents/skills`; gitignored, never hand-edited |

Edit `AGENTS.md` once and both Claude Code and Gemini see it. Never duplicate content into
`CLAUDE.md`.

## Where does this information belong?

Ask in this order:

1. **Is it derivable from the repo?** (module structure, signatures, dependency edges, counts) →
   don't write it down; point at the code map or the IntelliJ index instead. Hand-written copies of
   generated facts go stale. Exact counts are the worst offenders: `AGENTS.md` once said "87 Gradle
   modules" and "~2,700 Java files" when the repo had 86 and 2,824. Write `~90 modules`, or point at
   `index.md`, which prints the live count.
2. **Is it needed in *every* session?** → root `AGENTS.md`. This is the costly slot; keep it to
   conventions and gotchas.
3. **Is it a procedure for a specific recurring task?** → a skill. Loaded only when its
   `description` matches, so depth is cheap here.
4. **Is it a personal preference of one developer?** → agent memory, not the repo. Memory is keyed to
   the absolute repo path on one machine, so it does not survive a checkout to a different folder and
   is never shared with colleagues. Anything the team should follow must be committed.

Where a topic recurs, put its headline fact in `AGENTS.md` and name the skill beside it: the always-on
sentence is what changes behaviour when the skill is not loaded, and the name says where the rest is.
Skills for rare tasks need no pointer.

**Don't restate the harness.** Claude Code's own system prompt and tool descriptions already cover
generic agent behaviour (when to delegate to a subagent, being concise, acting once you have enough
information). Repeating it in `AGENTS.md` costs always-on tokens and can conflict with the harness.
Write down what is specific to *this* repo.

That includes **personas**. Role prompting works where it defines an agent that does not exist yet —
a system prompt, or the prompt that spawns a subagent. The files here are neither: `AGENTS.md` and
`SKILL.md` bodies load into an agent the harness has already defined. Describe the project, not the
reader.

## Rationales and scope: verifiable, or none

State a reason for a rule only when it is **verifiable in the repo** or **came from the user**.
Otherwise leave the instruction bare. A rule with no reason is fine; a rule with an invented reason
is worse than bare — it is confidently wrong and it narrows the rule to the case it names.

Reasons help **conditional** rules and hurt **absolute** ones:

* Good — the reason tells you when the rule stops applying:
  "Append `-q`; this build prints plugin chatter that buries the result. Drop it when a failure needs
  detail."
* Bad — the reason invites a carve-out:
  "Never push to remote; CI publishes from `master`/`stable`." (Reads as: a feature branch is fine.)
  Prefer the bare "Never push to remote, even if asked."

The same applies to **scope qualifiers**. "In hot paths", "at the boundaries", "except in the GUI
layer" narrow a rule exactly as a reason does, and an invented one hands back the behaviour the rule
existed to prevent. Scope by something checkable — "wherever the object count grows with the data" —
or not at all.

## Skill files: what differs here

The SKILL.md format itself — frontmatter fields, naming rules, description style, body length,
reference depth — is knowledge the model already has. Don't restate it in this repo. Only the
following are local:

* `---` on **line 1**. Gemini silently skips a skill when anything precedes it — a title, a comment,
  even a blank line — so a malformed skill fails with no error and no missing-skill message.
* Keep frontmatter to `name` and `description`. Tool-specific keys are ignored by whichever tool did
  not define them, so a skill that depends on one behaves differently across the two.
* Verify every command, Gradle task path and runtime claim against the build before writing it down.
  A wrong command in a playbook is worse than no playbook, because it will be trusted. Runtime claims
  need the service run, not grepped (see `build-and-test-sirius`). **The verification is reported in the
  conversation; the file states the conclusion.** No `Measured:`, `Verified:` or `(checked)` in the text —
  keep an observable a reader can match against what they see, such as an exception type or a log line,
  and drop the account of how it was obtained.

New skills are picked up automatically; run `./gradlew installAgentSkills` if `.claude/skills` is
missing (see root `AGENTS.md` §8).

## Editing AGENTS.md sections

Section numbers are referenced as `§N` from other sections, so renumbering silently breaks them.
Prefer rewriting a section in place, or appending a new one, over reordering. If you do renumber,
**verify the target of every reference, not merely that it resolves** — a stale `§6` that happens to
land on a real section passes an existence check and still misleads:

```bash
grep -nE '^## ' AGENTS.md          # headings sequential and unique?

# every §N reference, with its target title AND the citing line
grep -noE '§[0-9]+' AGENTS.md | while IFS=: read ln ref; do n=${ref#§}; \
  t=$(grep -E "^## $n\. " AGENTS.md | head -1 | sed -E 's/^## [0-9]+\. //'); \
  printf 'line %-4s %-4s -> %-35s | %s\n' "$ln" "$ref" "${t:-*** BROKEN ***}" \
  "$(sed -n "${ln}p" AGENTS.md | cut -c1-58)"; done

grep -rnE '§[0-9]+' */AGENTS.md    # per-module files citing root sections
wc -lc AGENTS.md                   # always-on weight: characters, not lines
```

Read the printed target against the citing line and confirm it is the section actually meant.

Names in these docs go stale when the code is renamed or moved. The git hooks run this check
automatically on checkout and merge; run it by hand after editing:

```bash
.githooks/check-agent-docs   # silent when clean, ~0.2s, warn-only
```

It reports four things: the `.claude/skills` pointer missing, module paths in the `AGENTS.md` files
that `settings.gradle` no longer declares, repo-relative file paths in any of the docs that no longer
resolve, and CamelCase type names that appear nowhere in the Java sources. The last two accept a name
found anywhere — an import, a usage, a comment — so they catch a removal or a rename, not a member
that merely moved. Neither replaces reading the claim: a path that resolves can still be cited for
the wrong reason.

## Before finishing

* Cross-reference check above is clean.
* Always-on weight did not grow without cause (`wc -c AGENTS.md`).
* Every new rationale is verifiable or user-supplied.
* No `Measured:`/`(verified)` narration.
* Read the finished file end to end, not only the lines you touched — every sentence should state a
  settled fact about the repo, not answer a question from the conversation that produced it. An edit
  that removes a clause also leaves non-sequiturs behind.
* Skill frontmatter: `---` on line 1, `name` + `description` only.
* No content duplicated between `AGENTS.md` and a skill — one owns it, the other points.
