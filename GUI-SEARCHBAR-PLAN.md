# Plan: Autocompleting Lucene Search Bar for the Feature List (Swing GUI)

Decided with Markus 2026-08-10. Not committed; transfer to issue tracker when refined.

## Decisions taken

| Question | Decision |
|---|---|
| Relation to filter dialog (2b) | **Chips + text segment**: dialog/model state renders as chips (from the model, never parsed from lucene); the user's own query is a separate segment, ANDed in — exactly today's `MUST`-clause semantics. No lucene→model parser. |
| Editor style | **Full chip builder** (like JS `LuceneChemicalSearchBar`), hosted in an **overlay that expands rightward over the result tab pane** when the bar is focused; collapsed compact state lives in the narrow left rail. |
| Autocompletion | **Grammar-driven Tab-completion** like JS: `[and\|or] [not] <field-prefix>` ⇥ starts a draft clause, `(`/`)` opens/closes groups; plus enum-value completion inside the draft editor. |
| Layout | Collapsed single row in the rail; expanded state is a flyout/overlay (no permanent vertical cost). |

## Facts the design builds on (verified in code)

- Filtering is **server-side**: `FeatureFilterModel.toLuceneQuery()` → `GuiProjectManager.reloadFeatures()` → `getAlignedFeaturesPage(..., filterQuery, ...)`; `fireUpdateCompleted()` triggers reload.
- The free-text search is already one lucene `MUST` clause: parsed via `QueryParser` with `FAKE_FIELD` default (unfielded terms → server default search fields); parse failure degrades to a literal term. Fielded queries already work, they are just undiscoverable.
- Header search field and dialog fulltext field share one `PlainDocument` (`filterModel.getSearchTextDoc()`).
- Model→query compilation is one-way; the model emits shapes that are not round-trippable (RT SHOULD-triple, quality `NOT_APPLICABLE` alternatives, inversion wrapper, confidence-mode field switch).
- The JS bar is a chip *builder*: chips are structured state compiled to lucene; free text passes through; it never parses lucene into chips. `LuceneChemicalSearchBar.tsx` is the behavioral spec (no jest spec exists).
- SDK provides `FeaturesApi.getAlignedFeaturesSearchableFields(projectId)` → name, fieldType, fullTextSearch, sortable, defaultSearchField, possibleValues (enums), description — including per-project `tags.*` fields.
- `sirius_gui` already depends on `lucene-queryparser` + `lucene-analysis-common`; no autocomplete infra exists yet (SwingX available).

## Architecture

New package `de.unijena.bioinf.ms.gui.utils.search` (moves with nothing; pure-UI).

### 1. `SearchableFieldsProvider`
Fetches and caches the searchable fields per project via the SDK (background job, non-blocking).
Refresh on overlay open when cache is older than ~30 s (tags are dynamic; the call is cheap).
API: `List<SearchableField> get()`, `Optional<SearchableField> byName(String)`, `List<SearchableField> byPrefix(String)`.

### 2. Query AST + compiler (pure java, TDD)
Port of the TSX tree model, semantics 1:1:
- `QueryClause` (field, numeric op or none, value(s), negated), `QueryGroup` (children, logic ops, negated).
- Compiler = `renderNodeToLucene`/`generateLuceneQuery`: string values quoted, numeric ops → ranges
  (`>` → `{v TO *}`, `<=` → `[* TO v]`, …), empty groups compile to nothing, free text ANDed as
  `(chips) AND (freetext)`.
- Tree ops = `removeNodeById` (groups deleted whole; lone-child groups auto-unpack with negation XOR),
  `resolvePath`/`getContainer`/`updateContainer` (cursor path into open group).

### 3. Completion engine (pure java, TDD)
Port of `parseCompletion`/`parseClauseTail`: `[and|or] [not] <field-prefix>`, `[and|or] [not] (`,
`)` (only when a group is open). Field prefix matching against SearchableField names (and
`tags.<name>`). Additionally: enum `possibleValues` and `true/false` completion for the draft
editor's value control, driven by the field's `fieldType`.

### 4. `LuceneSearchBar` component
Two states:
- **Collapsed** (in `FilterableCompoundListPanel` header, replaces the plain `searchField`):
  search icon + elided summary of the combined query ("3 filters · ionMass:[300 TO 400] …");
  focus/click expands. The `openFilterPanelButton` stays next to it.
- **Expanded overlay**: an undecorated, non-modal `JDialog` owned by the `MainFrame` (popup-style
  window), anchored at the collapsed bar and extending right over the result tab pane
  (width ≈ min(800 px, available)). Chosen over `JPopupMenu`/`PopupFactory` (built to be
  non-focusable and to auto-dismiss — wrong for an editor with text fields and combos) and over a
  `JLayeredPane` overlay (manual bounds/z-order/global click-outside handling). The owned dialog
  gives full keyboard focus; combo dropdowns are owned child popups and do not count as focus
  loss; dismissal is `windowLostFocus` + Esc; it repositions via a `ComponentListener` on the
  frame (or simply hides when the frame moves). Exact windowing is an implementation detail
  behind the component boundary — fall back to a layered-pane panel only if a platform/LaF issue
  shows up. Contents (mirroring the JS bar):
  - **model chips** (from `FeatureFilterModel` getters; visually distinct fill): one per active
    structured filter — label like `m/z 300–400`, tooltip shows the lucene it compiles to;
    ✕ resets that model part (calls the model setter + `fireUpdateCompleted`), click opens
    `FeatureFilterOptionsDialog`.
  - **user clause chips** (from the AST): deletable, `NOT`/logic tags, groups as nested chips with
    editable-while-open semantics (open group ring, connector select) as in the TSX.
  - **draft clause editor**: connector combo (2nd+ clause), NOT toggle, field tag, operator combo
    (numeric fields), value control — enum fields get a combo fed by `possibleValues`, booleans
    true/false, text a plain field.
  - **free-text field** with the ⇥ completion hint chip; Tab applies the completion.
  - **"Add filter" dropdown** listing fields from the provider (descriptions as tooltips), plus
    open/close group entries.
  - search button (commit), clear-all, copy-compiled-query.
  - Dismissal: Esc or commit collapses; focus loss to a non-owned window closes it
    (`WindowFocusListener` — combo dropdowns are owned and therefore harmless).

### 5. Model integration (the 2b answer, concretely)
- The user segment (AST + free text) compiles to one string written into the **existing shared
  `searchTextDoc`**; commit = `fireUpdateCompleted()`. The `FeatureFilterModel` needs **no change**:
  it already parses that document into the query's MUST clause, the dialog's fulltext field shows
  the compiled user query automatically, and reset paths (`resetFilter`) keep working.
- **External-edit rule**: the AST lives in the bar. If `searchTextDoc` changes underneath (user
  typed in the dialog's fulltext field), the bar degrades gracefully: document content becomes the
  free-text segment, user chips are cleared. One-way, no parsing, no surprises.
- Model chips are pure renderings of model state; they update via the existing
  `filterUpdateCompleted`/property events.

## Phases (each shippable)

| Phase | Content | Effort |
|---|---|---|
| 0 | `SearchableFieldsProvider` + SDK wiring, cache/refresh | ~0.5 d |
| 1 | AST + compiler + completion grammar as pure classes, JUnit tests ported from TSX semantics | ~2 d |
| 2 | Overlay shell, free text + ⇥ hint, Add-filter menu, draft editor, user chips, commit→`searchTextDoc`, collapsed summary | ~3–4 d |
| 3 | Model chips (render/reset/open-dialog), visual distinction, external-edit rule | ~1.5 d |
| 4 | Polish: enum/boolean value combos, live parse validation + unknown-field warning, copy button, keyboard traversal, `Colors`/theme, software-tour entry | ~1 d |

Total ≈ 8–9 dev days. Phases 0–2 already deliver the autocompleted query builder (2a); phase 3
delivers the dialog-state rendering (2b).

## Risks / notes

- **Overlay focus handling** is the main Swing risk; the owned-JDialog approach reduces it to
  standard `windowLostFocus` semantics (owned combo popups don't trigger it). Still test on
  Linux+macOS+Windows LaFs; a layered-pane panel is the documented fallback.
- Value completion for `tags.*` values and structure names would need value-listing endpoints —
  out of scope (fields only, values only for enums/booleans).
- Groups + negation are in scope because the grammar and chip semantics come from the TSX port;
  if phase 2 runs long, groups can be deferred without touching the architecture.
- The collapsed summary and the dialog can be open at the same time — both edit the same model;
  events already keep them consistent.
- the shortcut/quick-filter buttons can be removed if the default filter state is visualized with the query chips. 
  however this is optional. but if keeping them makes things complicated its fine to drop them  

## Implementation status (2026-08-10, branch feature/gui-lucene-searchbar)

All phases implemented; 64 new unit tests (78 total in sirius_gui) green. Quick-filter toggles kept
(they only write model state, which the chips render - no conflict).

Smoke test with the tomato project (79798 features):
- Collapsed bar renders in the left rail; overlay opens anchored at it (900px wide over the result
  view) and shows the default filter state as chips: "has MS/MS", "adducts (9)",
  "quality: DECENT, GOOD" - the toggles' state visualized, as hoped.
- searchable-fields endpoint on the real project: 40 fields incl. the project's dynamic tags
  (tags.pfas, tags.runType, tags.sampleType) and enum values for the quality fields.
- Compiled query shapes verified end-to-end via the running app's REST API:
  ionMass:[300 TO 400] -> 17130 of 79798; (ionMass:[300 TO 400] AND quality:GOOD) AND
  (hasMsMs:true) -> 514. Unknown fields silently match 0 - confirming the value of the
  unknown-field warning in the bar.
- NOT smoke-tested interactively: clicking/typing inside the overlay. Synthetic input (XTEST) is
  not delivered to the app windows on this Wayland session (pointer motion/hover works, button
  presses and key events are dropped), so draft editor, Tab completion and commit could only be
  verified by unit tests. -> needs one manual click-through.
- Server rejects leading wildcards (tags.X:* -> LEADING_WILDCARD_NOT_ALLOWED); the value-less tag
  syntax documented for the searchQuery parameters applies to NONE-type tags only. Check the
  filter-syntax docs of the paged endpoints on occasion.

---

# Rework: GitLab-style inline filtered search (planned 2026-08-11)

Markus' click-through findings + refined specs. Blueprint: the GitLab work-item/issue filter bar
(tokens inline in ONE field; a staged suggestion dropdown lists ALL candidates for the current
builder stage - field, then operator, then value - narrowing as you type; keyboard-selectable).

## Findings -> resolutions

| # | Finding | Resolution |
|---|---|---|
| 1 | Overlay often stays open when clicking elsewhere in the GUI | In-app clicks don't reliably transfer X window focus, so windowLostFocus alone is insufficient. Add a global AWTEventListener on MOUSE_PRESSED while the overlay is visible: press outside the overlay hierarchy (and its owned popups) closes it. windowLostFocus stays for clicks into other applications. |
| 2 | Autocomplete offers only ONE candidate (the ⇥ hint) | Replace with a SuggestionPopup: keyboard-navigable list of all stage-appropriate candidates, prefix-narrowed while typing (GitLab behavior; list shows immediately on focus). |
| 3 | Committed query not rendered in the original field | Structural fix via #4/#7: the collapsed view renders the committed chips itself. |
| 4 | Original field looks disabled, nobody would click it | Collapsed view becomes an active-looking chip field (normal text-field styling, hover cursor); focusing or typing opens the overlay. |
| 5/7 | Inline preferred: overlay = a LONGER search field on top of the original one, covering the result tabs; after finishing, chips render (partially) in the non-overlay field | Adopted. One perceived control that grows on focus and shrinks back on close. |
| 6 | Use GitLab filter as blueprint | Adopted throughout. |

## Decisions (asked & answered)

- Ranges: two staged values (from -> to), Enter on empty bound = open end (*).
- "Add Filter" button: dropped - the dropdown IS the entry point (all fields listed on focus).
- Groups/negation: kept as typed/suggested tokens ('(', ')', 'not', 'and', 'or' appear in the
  suggestion list at the right stages); open group renders as highlighted paren chip, inline.

## Architecture changes

### R1. TokenInputModel (new; pure java, TDD) - the staged state machine
States: IDLE -> FIELD chosen -> OPERATOR (numeric fields only) -> VALUE (-> VALUE2 for ranges) ->
clause committed to the AST. At IDLE, special tokens are offered alongside fields: `not`,
`and`/`or` (only with a sibling), `(`, `)` (only with an open group). Free text stays possible:
typed text that matches nothing is the free-text search segment (Enter commits it as such).
Backspace on empty input pops one stage (GitLab behavior); at IDLE it removes the last chip.
API sketch: `suggestions(typed)`, `choose(suggestion)`, `acceptTyped(text)`, `completed()`,
`stagePrompt()`. Reuses CompletionParser matching, NumberOp, valueSuggestions.

### R2. SuggestionPopup (new)
Non-focusable window with a list under the inline input; the text field keeps focus, Up/Down/
Enter/Tab/Esc are forwarded, mouse click selects. Rows: display + dimmed description (field
descriptions from the API). Visible whenever the overlay input is focused.

### R3. Inline editor row (overlay content rewrite)
ONE wrapping line: [model chips][user chips incl. paren chips][staged partial token as chip
fragments (field chip, then op chip)][borderless inline text input]. Trailing: Clear ✕ and
Search 🔍. Removed: draft editor panel, ⇥ hint button, Add-Filter menu, bottom row. Commit =
Enter while IDLE with empty input, or the Search button.

### R4. Dismissal fix
As per finding #1. Esc keeps closing without commit; state is kept for the next open.

### R5. Collapsed chip field (LuceneSearchBar rewrite)
Text-field-look panel rendering the committed model+user chips inline, elided with a "+n" overflow
hint; active styling. Click/focus/first keystroke opens the overlay EXACTLY on top of the field
(same origin/height, width extended over the result tabs); the triggering keystroke is forwarded
into the overlay input.

### Unchanged
Query AST + compiler + tree ops, QueryValidator, SearchableFieldsProvider, ModelChipFactory,
ChipComponent, WrapLayout, commit path (compile -> shared searchTextDoc -> fireUpdateCompleted),
external-edit degradation, quick-filter toggles (removal still optional per earlier note).

## Phases

| Phase | Content | Effort |
|---|---|---|
| R1 | TokenInputModel + tests (stage transitions, narrowing, range from->to, token grammar, backspace-pops-stage) | ~1.5 d |
| R2 | SuggestionPopup + keyboard routing | ~1 d |
| R3 | Inline editor row; remove draft panel/hint/menu | ~1.5 d |
| R4 | Dismissal via AWTEventListener | ~0.5 d |
| R5 | Collapsed chip field + overlay-on-top positioning | ~1 d |
| R6 | Manual click-through with tomato project + fixes | ~0.5 d |

Total ~6 d.

## Overlay: modal dialog + Cancel + outside-click (2026-08-11)

Made the overlay document-modal, added the missing Cancel button (next to Search), and outside-click
cancel via an AWTEventListener registered while shown (ignores presses inside the dialog or its owned
combo popups). Robust exits: Esc, Cancel, Search - all guaranteed. Outside-click is best-effort:
clicks on the native JxBrowser windows produce no AWT event and cannot be caught, but modality makes
the rest of the UI inert so it does not matter. Modal setVisible(true) blocks, so openAt() now takes
the type-ahead char and queues focus/dropdown before showing; the model-chip "open filter dialog"
action closes first then opens via invokeLater to avoid nested modals. Verified: clean start, no
exceptions, no auto-open (synthetic input still blocked by XWayland this session, so the modal
interaction itself needs a manual click-through).

## Overlay: back to a (single) heavyweight dialog (2026-08-11)

The layered-pane panel is lightweight and was hidden behind the native JxBrowser windows of the
result views (heavyweight peers always paint over lightweight Swing). Reverted to an undecorated
modeless JDialog so the overlay floats above the browser - but kept the robustness gains: it is now
a SINGLE window with the suggestion list embedded in it (no separate JWindow popup, which was the
real source of the earlier multi-window fragility), dismissal stays Esc/Enter-only (no outside-click
/ focus / global-mouse listeners), and it still opens only on an explicit gesture (no open on
startup). Repositions with the frame via an owner ComponentListener.

## Overlay robustness rework (2026-08-11)

The separate-top-level-window overlay (undecorated JDialog + JWindow suggestion popup) was fragile
on XWayland: it opened on startup focus, closed nondeterministically on outside clicks, and
sometimes painted without the rest of the panel. Replaced with an in-frame approach:
- SearchBarOverlay is now a JPanel hosted in the main frame's JLayeredPane (POPUP_LAYER); the
  suggestion list is embedded in it (no JWindow). No extra top-level windows -> no focus/compositing
  fragility.
- Dismissal is Esc (cancel) or Enter/Search (run + close) only; outside-click dismissal removed
  (with it the AWTEventListener, windowLostFocus and hide-on-move listeners).
- Opens only on an explicit gesture (click or first typed char), never on focusGained -> no
  open-on-startup.
- Trailing controls are compact icons now (clipboard copy, FlatClearIcon clear, db-lens search).
- SuggestionPopup class + its display test removed.
Verified: does NOT open on startup (screenshot). Opening/keyboard/Esc-Enter still need a manual
click-through - XWayland dropped synthetic input during this session so it could not be driven.

## Rework status (2026-08-11)

R1-R5 implemented (commits f595cc381, 4eca6710d, d90d6e4b9, 7e1b1f964); 99 tests green
(TokenInputModel 17, SuggestionPopup 4 display-bound). Visual smoke with the tomato project:
- Collapsed bar renders the committed/default filter chips in an active-looking field, clipped.
- Overlay opens EXACTLY on top of the bar (900px wide, one inline line: model chips | input |
  copy/Clear/Search) and the suggestion dropdown appears immediately on focus, listing all fields
  alphabetically with dimmed API descriptions.
- Still not verifiable by automation (Wayland drops synthetic clicks/keys once the overlay is up):
  dropdown keyboard flow, staged token building, outside-click dismissal (R4), commit rendering
  chips back into the collapsed bar. -> R6 = manual click-through pending.

---

# Implementation state (2026-08-11, post-cleanup, branch feature/gui-lucene-searchbar)

Cleanup after manual click-throughs (committed): overlay is a single undecorated **non-modal**
heavyweight `JDialog`; model-chip removals are **staged** (applied on Search, reverted on Cancel);
`close()` **disposes** the native peer (fixes an (X)Wayland ghost-surface that reopened the filter
dialog on later clicks); dismissal is a **single focus-loss path** (`windowLostFocus`) - the global
AWTEventListener was removed as redundant; position-sync guarded by `isVisible()`. Bug fix:
`FeatureFilterModel.isActive()` now includes the blank fold-change facet (a blank-only filter used to
compile to an empty query). These are the baseline the v2 architecture below builds on.

---

# Architecture v2: unified query renderer/editor over a common filter model (planned 2026-08-11)

Extends the design from "a search-bar overlay" to **one reusable query renderer/editor** hosted in
several places, over a single common model. Supersedes R3's "one wrapping line holds everything".

## Goal
Visualize AND edit the FULL filter configuration - both what is configured with
`FeatureFilterOptionsDialog` widgets and the user's own query-builder clauses - as chips over one
common model. Panel-derived chips stay bound to their dialog widgets: editing a chip value syncs the
widget, and changing a widget updates the chip.

## Common-model decision (the crux)
- Common model = a **GUI-focused semantic layer (`FilterTerm`) + the existing small `QueryNode` AST**.
  NOT the Lucene flexible query model.
- Lucene stays at the edges: the classic builder compiles for **execution** (authoritative); the
  **flexible parser** is an OPTIONAL engine for string->chips hydration only (deferred, see P4).
- Why not the flexible `QueryNode` as the model: it lacks **value domains/constraints** (m/z bounds,
  enum value sets, the adduct/DB lists), **widget binding** (which subtree is the m/z spinner), and
  **provenance** (panel vs user) - the exact semantics this feature is about. Its vocabulary
  (fuzzy/slop/regexp/boost) is also far larger than a chip grammar. Good parser, wrong model.

## Layering
```
Panel widgets ┐                                   ┌ collapsed bar
User query    ┤                                   ┤ overlay editor
              ▼                                    ▼ dialog-embedded renderer
   FilterTerm registry (SEMANTIC model = SSOT) ──render──► QueryNode AST ──► Swing chips
              │                                   ◄──edit──
              └─ compile ─► Lucene query string (execution, authoritative)
   (optional) Lucene string ─ flexible parser ─► QueryNode (user clauses; unsupported -> free text)
```

## `FilterTerm` (semantic SSOT; an adapter over `FeatureFilterModel`, NOT a rewrite)
```
interface FilterTerm {
  String id();                          // "mz", "quality.<cat>", user-clause id
  Provenance provenance();              // PANEL | USER  (color + edit affordances)
  boolean isActive();
  QueryNode toQueryNode();              // -> render/AST layer
  List<EditableValue> editableValues(); // scalar/range values editable INLINE (carry domain: min/max/step/enum)
  void openExternalEditor(Host host);   // set/complex facets defer here (adducts/DBs/quality/elements)
  void reset();
  void addChangeListener(Runnable l);
}
```
- Panel terms wrap `FeatureFilterModel` getters/setters, carry the value domain, and know their
  owning dialog **tab + focus target** (for chip->tab navigation).
- User terms wrap a `QueryClause`/`QueryGroup` in the builder root.
- `FilterTermRegistry` assembles active panel terms + user terms into the unified stream the renderer
  consumes; the renderer no longer cares about the source.
- **Guard test**: each panel term's `toQueryNode()` compiles to the same lucene fragment
  `toLuceneQueryBuilder` emits - kills node/executed-query drift until/unless P5.

## Bidirectional binding
Dialog widgets and chips are both views over the same working model; every edit goes through a term
setter that fires a change -> re-render (+ re-filter on commit). Sharing the SSOT means no
widget<->chip wiring; guard against re-entrant fire while syncing.

## Hosts (one host-agnostic component)
1. **Collapsed bar** (left rail): clipped one-row summary; an in-field icon opens the overlay; a
   second small in-field icon opens `FeatureFilterOptionsDialog` (**replaces the `...` button**).
2. **Overlay** (expands over the result view): quick editor from the main window; **always closed
   when the dialog opens** (never two live editors on one model).
3. **Embedded in `FeatureFilterOptionsDialog`** (bottom, always visible, dialog made **resizable**):
   live-updates as tab widgets change; editable; clicking a set-chip **selects its tab + focuses the
   picker**; autocompletion available when editing in the renderer.

## Renderer/editor layout (supersedes R3)
- **Typing zone**: ALWAYS one line, non-scrollable; autocomplete dropdown below. Holds the inline
  text input + the in-progress token fragments (field-chip -> op-chip). When a group is open, a small
  **open-group context indicator** sits here so the user sees where a committed clause lands.
- **Chips zone**: **developer-configurable number of visible rows**; committed panel + user chips
  (groups as nested paren chips) wrap across rows; a **vertical scrollbar** appears on overflow. In
  the resizable dialog the zone may grow with the window (configured rows = minimum).
- Rationale: stable caret (no jumping as chips wrap), bounded predictable height for embedding, one
  component fits every host via row count. Consistent with Kibana KQL / GitHub search bars.

## Transactionality (Q1 = transactional)
- **Dialog**: widgets + embedded renderer are live views of the dialog's WORKING COPY; Apply commits,
  Discard reverts. "Live auto-update" and "transactional" coexist (live within the copy, commit/revert
  at the dialog boundary).
- **Overlay**: separate transactional editor over the real model (working copy + baseline, revert on
  Cancel) - as already built.

## Decisions (survey 2026-08-11)
- Q1 commit model = **Transactional**.
- Q2 inline editing = **scalars/ranges inline; set/complex facets open their dialog tab**.
- Q3 dialog relationship = **complement** (dialog + overlay both stay; add the embedded live renderer;
  dialog resizable; in-field icon replaces `...`; chip->tab navigation with autocomplete).
- Q4 combine logic = **panel facets AND user-group** (as today).
- Still-open, defaulted: panel chips **stay bound** (no detach); Lucene-string parser **deferred**;
  invert rendered as a **badge** first.

## Phases (additive; each shippable)
| Phase | Content | Effort | Risk |
|---|---|---|---|
| P0 | Read-only unified rendering: facets -> `QueryNode`, MODEL color, reset/open-dialog bindings | 3-5 d | low-med |
| P1 | `FilterTerm` + registry adapter; route user + panel through one renderer; typing/chips-zone layout split | ~1 wk | med |
| P2 | Embed renderer in the dialog; make dialog widgets live views of a working copy; chip->tab nav; make dialog resizable | ~1 wk | med (widget live-binding) |
| P3 | Inline scalar/range editing (overlay + dialog) w/ domain validation; set facets -> tab/dialog | ~3-4 d | low-med |
| P4 (opt) | Lucene-string -> chips via flexible parser (round-trip/hydration) | 1-2 wk | high |
| P5 (opt) | Refactor `FeatureFilterModel` to be composed of `FilterTerm`s (true SSOT) - touches search + delete | large | med-high |

Core vision (P0-P3) ~ 3-4 weeks.

## Risks
- Dialog widgets becoming controlled views of a working model = the biggest new work; needs
  re-entrancy guards on the sync.
- The chip renderer becoming multi-mode (user-editable / panel-editable / read-only) - keep a clean
  renderer abstraction + tests so user rendering does not regress.
- Node vs executed-query drift -> guard test (or P5).
- confidence field name depends on the display mode; invert is a wrapper; the delete action is NOT a
  term (excluded).
- Layout: the dialog is currently `setResizable(false)` and fairly full - needs a layout pass + a
  scrollable chips zone.

## Interface contracts — the generic seam (drafted 2026-08-11, for review before P1)

Goal: the renderer/editor/autocomplete ENGINE is pojo-agnostic and reusable for any future
searchable pojo (runs, compounds, ...). It only ever talks to two abstractions - `FilterTerm` and
`FilterTermProvider` - and to `SearchableField` (already per-pojo from the endpoint). Nothing
pojo-specific (`FeatureFilterModel`, `FeatureFilterOptionsDialog`) may be referenced by the engine;
those live behind an AlignedFeature *provider*. A pojo with no panel just supplies zero panel terms
and the engine still renders/edits user clauses.

### C1. `FilterTerm` — one editable unit of the query, provenance-tagged
```
interface FilterTerm {
    String id();                         // stable identity within a session
    Provenance provenance();             // PANEL (from a dialog widget) | USER (typed clause)

    boolean isActive();                  // contributes to the query right now (working state)
    QueryNode toQueryNode();             // render/compile form, built from the WORKING value

    // --- editing (transactional; see C3) ---
    List<EditableValue> editableValues();// inline-editable scalars/ranges w/ domain (min/max/step/enum);
                                         // empty => not inline-editable (use openExternalEditor)
    void setWorkingValue(EditableValue slot, String raw);  // stage an edit (fires change; no backing write)
    void openExternalEditor(Host host);  // set/complex facets defer here (adduct/DB/element pickers)
    void clear();                        // stage "remove this term" (revertible until commit)

    void addChangeListener(Runnable l);  // fires on any working-state change
}

enum Provenance { PANEL, USER }
record EditableValue(String key, ValueKind kind, String workingRaw, /* domain */ Object domain) {}
```
- `toQueryNode()` is derived from the term's WORKING value, so the renderer always shows the staged
  (not yet applied) state - exactly what transactional editing needs.
- `EditableValue.domain` carries the validation domain (numeric min/max/step, or enum values) so the
  inline editor can validate without knowing the pojo. This is the piece the Lucene flexible model
  could not provide and the reason we keep our own model.

### C2. `FilterTermProvider` + `Host` — the per-pojo plug and the UI callbacks
```
interface FilterTermProvider {
    List<FilterTerm> panelTerms();       // PANEL terms (may be empty for pojos without a filter panel)
    FilterTerm freeTextTerm(...);        // build USER clause/free-text terms from the query builder
    void commit(String compiledQuery);   // execution sink: run the query for THIS pojo (re-filter)
    List<SearchableField> searchableFields(); // autocomplete source (already per-pojo)
}

interface Host {                          // how a term asks its host to open a full editor
    void openPanelEditorFor(FilterTerm term);   // overlay -> open the dialog + select the term's tab;
                                                //  dialog  -> just select the term's tab + focus picker
}
```
- The AlignedFeature provider wraps `FeatureFilterModel` (panel terms) + `GuiProjectManager.reloadFeatures`
  (commit). A future Runs/Compounds provider wraps its own model or supplies only user terms.
- The engine takes a `FilterTermProvider` and a `Host`; it never imports `FeatureFilterModel`.

### C3. Working-copy / commit protocol (transactional, Q1) - reused everywhere
Chosen over a whole-model snapshot because `FeatureFilterModel` is not cleanly clonable and per-term
is naturally generic:
- Each term holds an **applied value** (last committed) and a **working value** (current edits).
  `toQueryNode()`/`isActive()` read the working value. `setWorkingValue`/`clear` mutate only the
  working value and fire the change listener.
- The **host** (overlay or dialog) owns the transaction boundary:
  - any term change -> engine recompiles the combined query and re-renders the chips; the list is
    NOT re-filtered yet (transactional).
  - **Commit** (Search / dialog Apply): engine compiles panel-terms (working) + user query,
    `provider.commit(compiled)` runs it; each term's applied := working.
  - **Revert** (Cancel / Esc / dialog Discard): each term's working := applied; re-render.
- Live panel<->chip sync inside the dialog is just: the dialog widget and the chip are two editors of
  the SAME term's working value; either `setWorkingValue` fires the shared change -> both refresh.
  No widget<->widget wiring; guard against re-entrant fire while applying.

### What this makes reusable vs per-pojo
- Reusable (engine): `QueryNode` + compiler, renderer (interactive/read-only modes), token editor,
  autocomplete, the `FilterTerm`/`FilterTermProvider`/`Host` contracts, the commit/revert protocol.
- Per-pojo (provider): the concrete `FilterTerm` set + their widget/model binding, and the execution
  sink. AlignedFeature is the first provider; others drop in without touching the engine.

