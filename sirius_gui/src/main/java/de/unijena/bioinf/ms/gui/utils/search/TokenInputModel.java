/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;
import de.unijena.bioinf.ms.gui.utils.query.*;

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The staged token input of the search bar, modeled after GitLab's filtered search. The {@link
 * Stage#FIELD} stage lists all fields plus {@code NOT} and the parens; when there is something to
 * join to ({@link Join}) every one of those rows carries the connector that would join it
 * ({@link Suggestion.Joined}), so the connector and what it joins are a single pick rather than a
 * stage of their own. Which connectors are offered follows the {@link QueryMode}. Choosing a field
 * advances to the operator stage (numeric fields only) and then the value stage(s) - ranges take two
 * staged values, an empty bound meaning open-ended. Backspace on empty input pops one stage; with
 * nothing pending it asks the owner to remove the last committed chip.
 * <p>
 * An untouched token additionally offers the owner's {@link RunAction} as the top suggestion, so the
 * default pick of an empty input is "run the search" rather than the first field; typing anything
 * drops that row again.
 * <p>
 * Pure model: the UI renders {@link #suggestions}, {@link #pendingFragments} and {@link #stagePrompt}
 * and applies the returned {@link Event}s to the query tree. Free text is not consumed here - typed
 * text that resolves to nothing stays the free-text search segment of the owner (offered joined by a
 * connector like everything else). What the current mode does not allow is rejected instead of
 * silently reinterpreted - see {@link #inputProblem}.
 */
public class TokenInputModel {

    /**
     * Field types whose clauses carry a comparison operator.
     */
    public static final EnumSet<SearchableFieldType> NUMERIC_TYPES = EnumSet.of(
            SearchableFieldType.INTEGER, SearchableFieldType.LONG, SearchableFieldType.DOUBLE,
            SearchableFieldType.FLOAT, SearchableFieldType.DATE, SearchableFieldType.TIME);

    /**
     * What a new term would be joined to - which decides whether a connector is asked for at all, and
     * which ones may be chosen.
     * <p>
     * {@link #PANEL_TERMS} is the filter-panel block sitting to the left of an otherwise empty user
     * query. The connector chosen there is the joint between the two blocks of the executed query
     * ({@code (facets) AND (query)}) rather than one inside the user's query - a different thing to
     * pick, which is why such a row shows the opening brace of the block it starts
     * ({@link Suggestion.Joined#blockJoin()}).
     */
    public enum Join {NOTHING, USER_TERMS, PANEL_TERMS}

    /**
     * FIELD is the "entry" stage a fresh token starts in (fields, NOT, parens - each joined by a
     * connector where one applies). The remaining stages build one clause.
     */
    public enum Stage {FIELD, OPERATOR, VALUE, VALUE2}

    public enum SpecialToken {
        NOT("NOT", "Negate the next filter or group"),
        OPEN_GROUP("(", "Group filters, e.g. a AND (b OR c)"),
        CLOSE_GROUP(")", "Close the open group");

        final String display;
        final String description;

        SpecialToken(String display, String description) {
            this.display = display;
            this.description = description;
        }
    }

    /**
     * One row of the suggestion dropdown.
     */
    public sealed interface Suggestion {
        String display();

        @Nullable
        String description();

        record FieldSuggestion(@NotNull SearchableField field) implements Suggestion {
            @Override
            public String display() {
                return field.getName();
            }

            @Override
            public String description() {
                return field.getDescription();
            }
        }

        record TokenSuggestion(@NotNull SpecialToken token) implements Suggestion {
            @Override
            public String display() {
                return token.display;
            }

            @Override
            public String description() {
                return token.description;
            }
        }

        record OperatorSuggestion(@NotNull NumberOp op) implements Suggestion {
            @Override
            public String display() {
                return op.getSymbol(); // the same terse form the chips use (e.g. >=, [ TO ])
            }

            @Override
            public String description() {
                return op.getDescription(); // human-readable, e.g. "greater than or equal"
            }
        }

        record ValueSuggestion(@NotNull String value) implements Suggestion {
            @Override
            public String display() {
                return value;
            }

            @Override
            public String description() {
                return null;
            }
        }

        /**
         * Turn the typed text into a keyless full-text clause (default field matching).
         */
        record FreeTextSuggestion(@NotNull String text) implements Suggestion {
            @Override
            public String display() {
                return "\"" + text + "\"";
            }

            @Override
            public String description() {
                return "full-text search";
            }
        }

        /**
         * Parse the typed text as a full lucene query and turn it into chips (for users who type a
         * query directly rather than building it clause by clause).
         */
        record ParseQuerySuggestion(@NotNull String text) implements Suggestion {
            @Override
            public String display() {
                return text;
            }

            @Override
            public String description() {
                return "parse as query";
            }
        }

        /**
         * A suggestion together with the connector that joins it to the terms already in the container:
         * what used to be two picks (the connector, then the field) is one row. Wrapping is what makes
         * the connector visible in the list - the query it builds is the same either way.
         *
         * @param blockJoin whether that connector is the joint between the filter-panel block and the
         *                  user's query rather than one inside the query (see {@link Join#PANEL_TERMS});
         *                  such a row opens the user's block, which is what its brace shows
         */
        record Joined(@NotNull LogicOp logic, @NotNull Suggestion target, boolean blockJoin) implements Suggestion {
            @Override
            public String display() {
                return logic + (blockJoin ? " (" : "") + " " + target.display();
            }

            @Override
            public String description() {
                return target.description();
            }
        }

        /**
         * Run the owner's action (search / apply) instead of adding anything to the query. Offered on
         * top of an untouched input so it is the default pick, and gone as soon as anything is typed -
         * the owner intercepts it, the model itself has nothing to do with it (see {@link #choose}).
         */
        record RunActionSuggestion(@NotNull String display, @NotNull String description) implements Suggestion {
        }
    }

    /**
     * The owner's "this is finished, run it" action, surfaced as the top suggestion of an untouched
     * input: {@code label}/{@code description} render that row, {@code emptyPrompt} replaces the stage
     * prompt while the row is offered. Null (unset) hides the row - a host without such a key.
     */
    public record RunAction(@NotNull String label, @NotNull String description, @NotNull String emptyPrompt) {
    }

    /**
     * One staged token fragment: {@code text} as the model has it, and the fully-qualified
     * {@code fieldName} it stands for - set only on the field fragment, so the owner knows which
     * fragment may be shortened for display (the connector, {@code NOT} and the operator are display
     * text already).
     */
    public record Fragment(@NotNull String text, @Nullable String fieldName) {
    }

    /**
     * A change the owner has to apply to the query tree.
     */
    public sealed interface Event {
        record ClauseCompleted(@NotNull QueryClause clause, @NotNull LogicOp logic) implements Event {
        }

        /**
         * A whole parsed query to splice into the tree: its items joined to the existing container by
         * {@code logic}, and to each other by the container's own {@code logics}.
         */
        record QueryParsed(@NotNull QueryContainer container, @NotNull LogicOp logic) implements Event {
        }

        record OpenGroup(boolean negated, @NotNull LogicOp logic) implements Event {
        }

        record CloseGroup() implements Event {
        }

        /**
         * Backspace with nothing pending: remove the last committed chip.
         */
        record RemoveLastNode() implements Event {
        }
    }

    // --- context (owned by the overlay, changes as the query tree changes) ---
    private List<SearchableField> fields = List.of();
    private Join join = Join.NOTHING;
    private boolean groupOpen;
    private QueryMode mode = QueryMode.SIMPLE;
    @Nullable
    private RunAction runAction;

    // --- staged token state ---
    private Stage stage = Stage.FIELD;
    @Nullable
    private LogicOp pendingLogic;
    private boolean pendingNegated;
    @Nullable
    private SearchableField pendingField;
    @Nullable
    private NumberOp pendingOp;
    @Nullable
    private String pendingValue1;

    public void updateContext(@NotNull List<SearchableField> fields, @NotNull Join join, boolean groupOpen) {
        this.fields = fields;
        this.join = join;
        this.groupOpen = groupOpen;
    }

    /** Whether there is anything for a new term to be joined to. */
    private boolean joins() {
        return join != Join.NOTHING;
    }

    /**
     * Sets how much of the grammar to offer; {@link QueryMode#SIMPLE} by default.
     */
    public void setMode(@NotNull QueryMode mode) {
        this.mode = mode;
    }

    /**
     * The connectors a new term may be joined by here, best first: in SIMPLE mode the single one the
     * query shape implies (AND between top-level terms, OR between the alternatives inside a group), in
     * ADVANCED mode both, AND first so it stays the default pick.
     */
    public List<LogicOp> allowedConnectors() {
        if (mode == QueryMode.ADVANCED)
            return List.of(LogicOp.AND, LogicOp.OR);
        return List.of(groupOpen ? LogicOp.OR : LogicOp.AND);
    }

    /**
     * Whether a group may be opened here: always at the top level, inside another group only in
     * ADVANCED mode (SIMPLE keeps the query one level deep, which is what makes its shape readable).
     */
    public boolean nestingAllowed() {
        return mode == QueryMode.ADVANCED || !groupOpen;
    }

    /**
     * Sets (or clears) the owner's run action - the top row of an untouched input.
     */
    public void setRunAction(@Nullable RunAction runAction) {
        this.runAction = runAction;
    }

    public Stage stage() {
        return stage;
    }

    /**
     * Whether typed text may become the owner's free-text search term at this stage: the entry (FIELD)
     * stage. The value stages take text too, but as a clause value, not as free text.
     */
    public boolean acceptsFreeText() {
        return stage == Stage.FIELD;
    }

    /**
     * Why the typed text cannot be applied here, for the owner to show on the input; empty when it is
     * fine. What is rejected is a connector or a nesting the current mode does not offer: reinterpreting
     * a typed OR as the AND the mode allows would change the query's meaning behind the user's back, so
     * it is refused and named instead (the list keeps showing what IS possible).
     */
    public Optional<String> inputProblem(@NotNull String typed) {
        String trimmed = typed.trim();
        if (!acceptsFreeText() || trimmed.isEmpty())
            return Optional.empty();
        TypedConnector connector = typedConnector(trimmed);
        if (connector.logic() != null && joins() && pendingLogic == null
                && !allowedConnectors().contains(connector.logic()))
            return Optional.of(refusedConnector(connector.logic()));
        if (!nestingAllowed() && connector.rest().startsWith("("))
            return Optional.of("Groups cannot be nested in simple mode"
                    + " - close this group first, or switch to advanced mode");
        return Optional.empty();
    }

    private String refusedConnector(LogicOp logic) {
        if (join == Join.PANEL_TERMS)
            return logic + " cannot join the filter-panel filters and your query in simple mode -"
                    + " they are combined with AND, so the panel filters always narrow the search;"
                    + " switch to advanced mode to make your query an alternative to them instead";
        return logic + " cannot join "
                + (groupOpen ? "the alternatives inside a group (they are combined with OR)"
                : "the top-level terms (they are combined with AND)")
                + " in simple mode - switch to advanced mode to mix connectors freely";
    }

    /** A typed connector and the text after it: {@code or ion} is {@code (OR, "ion")}. */
    private record TypedConnector(@Nullable LogicOp logic, @NotNull String rest) {
    }

    /**
     * A leading connector, whether it is finished ({@code or ion}) or the whole input so far
     * ({@code or}) - it narrows the list to that connector's rows. Whitespace (or the end of the input)
     * has to follow, so {@code orma} stays a field prefix, as in the typed grammar
     * ({@link CompletionParser}).
     */
    private static final Pattern TYPED_CONNECTOR = Pattern.compile("^(and|or)(?:\\s+|$)", Pattern.CASE_INSENSITIVE);

    private static TypedConnector typedConnector(String trimmed) {
        Matcher matcher = TYPED_CONNECTOR.matcher(trimmed);
        if (!matcher.find())
            return new TypedConnector(null, trimmed);
        return new TypedConnector(LogicOp.valueOf(matcher.group(1).toUpperCase(Locale.ROOT)),
                trimmed.substring(matcher.end()));
    }

    /**
     * The suggestions for the current stage, narrowed by the typed prefix (empty prefix = all).
     */
    public List<Suggestion> suggestions(@NotNull String typed) {
        String trimmed = typed.trim();
        String prefix = trimmed.toLowerCase(Locale.ROOT);
        List<Suggestion> suggestions = switch (stage) {
            case FIELD -> entrySuggestions(trimmed);
            case OPERATOR -> operatorSuggestions(prefix);
            case VALUE, VALUE2 -> valueSuggestions(prefix);
        };
        // an untouched input offers the owner's action FIRST, so it is the default pick and Enter runs
        // the search straight away; typing anything drops it and the best match becomes the default again
        if (offersRunAction(trimmed)) {
            List<Suggestion> withRunAction = new ArrayList<>(suggestions.size() + 1);
            withRunAction.add(new Suggestion.RunActionSuggestion(runAction.label(), runAction.description()));
            withRunAction.addAll(suggestions);
            suggestions = withRunAction;
        }
        return suggestions;
    }

    /**
     * The entry stage: every field and applicable token, then the free-text rows, each joined by the
     * connector that would attach it to what is already there (see {@link #connectorsOffered}) - so the
     * connector is visible in the row and chosen with it, never asked for separately.
     * <p>
     * A leading connector in the typed text narrows the rows to that one ({@code or ion} offers only the
     * OR row of {@code ionMass}); a connector the mode does not allow is refused instead, which
     * {@link #inputProblem} explains while the allowed rows stay on offer.
     */
    private List<Suggestion> entrySuggestions(String trimmed) {
        TypedConnector typed = typedConnector(trimmed);
        String rest = typed.rest();
        List<Suggestion> candidates = fieldStageSuggestions(rest.toLowerCase(Locale.ROOT));
        // typed text can always become a keyless full-text clause; offered last so a matching field
        // stays the default pick. A paren the mode will not open is the exception: nobody searches for
        // a literal "(", so offering it as a term would be a nonsense row where inputProblem already
        // says what the problem is
        if (!rest.isEmpty() && !(rest.startsWith("(") && !nestingAllowed())) {
            candidates.add(new Suggestion.FreeTextSuggestion(rest));
            // ... and, when it reads as more than plain words (a field, operator, group or boolean),
            // it can be parsed as a whole query and turned into chips
            if (parsesToStructuredQuery(rest))
                candidates.add(new Suggestion.ParseQuerySuggestion(rest));
        }
        return join(candidates, connectorsOffered(typed.logic()));
    }

    /**
     * The connectors to offer for each row: none when there is nothing to join to (the first term of a
     * container, with no panel filters either) or one is already staged, the typed one when it is
     * allowed, the allowed set otherwise.
     */
    private List<LogicOp> connectorsOffered(@Nullable LogicOp typedLogic) {
        if (!joins() || pendingLogic != null)
            return List.of();
        List<LogicOp> allowed = allowedConnectors();
        return typedLogic != null && allowed.contains(typedLogic) ? List.of(typedLogic) : allowed;
    }

    /**
     * One row per connector and candidate, the variants of a candidate adjacent: the best match's first
     * connector (AND, or the one the simple shape implies) stays the default pick, and its OR variant is
     * one step away rather than a whole list further down.
     */
    private List<Suggestion> join(List<Suggestion> candidates, List<LogicOp> connectors) {
        if (connectors.isEmpty())
            return candidates;
        boolean blockJoin = join == Join.PANEL_TERMS;
        List<Suggestion> joined = new ArrayList<>(candidates.size() * connectors.size());
        for (Suggestion candidate : candidates) {
            // closing a group joins nothing - it ends the term before it
            if (candidate instanceof Suggestion.TokenSuggestion token && token.token() == SpecialToken.CLOSE_GROUP) {
                joined.add(candidate);
                continue;
            }
            for (LogicOp logic : connectors)
                joined.add(new Suggestion.Joined(logic, candidate, blockJoin));
        }
        return joined;
    }

    /**
     * The run-action row shows on a fresh token only: nothing typed and nothing staged (a pending
     * connector/NOT/field means the user is mid-token, where running the search would drop it).
     */
    private boolean offersRunAction(String trimmed) {
        return runAction != null && trimmed.isEmpty() && acceptsFreeText() && pendingFragments().isEmpty();
    }

    /** True when {@code text} parses to a query richer than a single free-text clause (which the
     *  free-text suggestion already covers), so offering "parse as query" is not redundant. */
    private boolean parsesToStructuredQuery(String text) {
        return QueryStringParser.parse(text, fields)
                .filter(c -> !c.isEmpty() && !isSingleFreeText(c))
                .isPresent();
    }

    private static boolean isSingleFreeText(QueryContainer container) {
        return container.items().size() == 1
                && container.items().get(0) instanceof QueryClause clause && clause.isFreeText();
    }

    private List<Suggestion> fieldStageSuggestions(String prefix) {
        List<Suggestion> suggestions = new ArrayList<>(
                CompletionParser.fieldMatches(prefix, fields).stream()
                        .map(f -> (Suggestion) new Suggestion.FieldSuggestion(f)).toList());
        if (!pendingNegated && matches(SpecialToken.NOT, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.NOT));
        if (nestingAllowed() && matches(SpecialToken.OPEN_GROUP, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.OPEN_GROUP));
        if (groupOpen && matches(SpecialToken.CLOSE_GROUP, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.CLOSE_GROUP));
        return suggestions;
    }

    private static boolean matches(SpecialToken token, String prefix) {
        return token.display.toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private List<Suggestion> operatorSuggestions(String prefix) {
        // match the terse symbol (">", "[") or the human phrase ("greater", "between") so typing
        // either narrows the list
        return java.util.Arrays.stream(NumberOp.values())
                .filter(op -> op.getSymbol().toLowerCase(Locale.ROOT).startsWith(prefix)
                        || op.getDescription().toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(op -> (Suggestion) new Suggestion.OperatorSuggestion(op))
                .toList();
    }

    private List<Suggestion> valueSuggestions(String prefix) {
        if (pendingField == null)
            return List.of();
        return CompletionParser.valueMatches(prefix, CompletionParser.valueSuggestions(pendingField)).stream()
                .map(value -> (Suggestion) new Suggestion.ValueSuggestion(value))
                .toList();
    }

    /**
     * Applies a chosen suggestion, advancing the stage.
     */
    public Optional<Event> choose(@NotNull Suggestion suggestion) {
        // not ours: the owner runs its action and leaves the (empty) token untouched
        if (suggestion instanceof Suggestion.RunActionSuggestion)
            return Optional.empty();
        if (suggestion instanceof Suggestion.Joined joined) {
            pendingLogic = joined.logic();
            return choose(joined.target());
        }
        if (suggestion instanceof Suggestion.FieldSuggestion field) {
            return stageField(field.field());
        }
        if (suggestion instanceof Suggestion.TokenSuggestion token) {
            return switch (token.token()) {
                case NOT -> {
                    pendingNegated = true;
                    yield Optional.empty();
                }
                case OPEN_GROUP -> {
                    Event event = new Event.OpenGroup(pendingNegated, effectiveLogic());
                    resetPending();
                    yield Optional.of(event);
                }
                case CLOSE_GROUP -> Optional.of(new Event.CloseGroup());
            };
        }
        if (suggestion instanceof Suggestion.OperatorSuggestion op) {
            pendingOp = op.op();
            stage = Stage.VALUE;
            return Optional.empty();
        }
        if (suggestion instanceof Suggestion.FreeTextSuggestion freeText) {
            Event event = new Event.ClauseCompleted(QueryClause.freeText(freeText.text(), pendingNegated), effectiveLogic());
            resetPending();
            return Optional.of(event);
        }
        if (suggestion instanceof Suggestion.ParseQuerySuggestion parse) {
            Optional<QueryContainer> parsed = QueryStringParser.parse(parse.text(), fields);
            // it was only offered when it parses, but stay defensive and fall back to free text
            Event event = parsed.filter(c -> !c.isEmpty())
                    .<Event>map(c -> new Event.QueryParsed(c, effectiveLogic()))
                    .orElseGet(() -> new Event.ClauseCompleted(QueryClause.freeText(parse.text(), pendingNegated), effectiveLogic()));
            resetPending();
            return Optional.of(event);
        }
        // value suggestion
        return acceptValue(((Suggestion.ValueSuggestion) suggestion).value());
    }

    /**
     * The accept key (Enter, or Tab) with raw typed text: value stages take it as the value; the
     * operator stage matches it against the operators; at IDLE multi-token grammar input
     * ({@code or not ion}) is applied in one go, anything else is the owner's free-text segment (no
     * state change, empty result).
     */
    public Optional<Event> submitTyped(@NotNull String text) {
        String trimmed = text.trim();
        switch (stage) {
            case VALUE, VALUE2 -> {
                return acceptValue(trimmed);
            }
            case OPERATOR -> {
                List<Suggestion> matching = operatorSuggestions(trimmed.toLowerCase(Locale.ROOT));
                if (matching.size() == 1)
                    return choose(matching.get(0));
                return Optional.empty();
            }
            case FIELD -> {
                // a connector or nesting the mode does not offer is refused, not reinterpreted
                return inputProblem(trimmed).isEmpty() ? applyGrammar(trimmed) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Whether {@link #submitTyped} would actually consume {@code typed} - i.e. whether the accept key
     * has anything to add at this stage. False lets the owner's Enter fall through to running the
     * search. Plain free text at an entry stage is NOT accepted here (the free-text suggestion row
     * covers it), so it stays false there.
     */
    public boolean canAccept(@NotNull String typed) {
        String trimmed = typed.trim();
        return switch (stage) {
            // a range bound may be empty (open end); a single value may not
            case VALUE -> (pendingOp != null && pendingOp.isRange()) || !trimmed.isEmpty();
            case VALUE2 -> true;
            case OPERATOR -> operatorSuggestions(trimmed.toLowerCase(Locale.ROOT)).size() == 1;
            case FIELD -> inputProblem(trimmed).isEmpty()
                    && CompletionParser.parse(trimmed, fields, joins(), groupOpen).isPresent();
        };
    }

    private Optional<Event> applyGrammar(String text) {
        Optional<Completion> parsed = CompletionParser.parse(text, fields, joins(), groupOpen);
        if (parsed.isEmpty())
            return Optional.empty(); // free text - stays in the input, owned by the overlay

        Completion completion = parsed.get();
        if (completion instanceof Completion.CloseGroup)
            return Optional.of(new Event.CloseGroup());

        if (completion instanceof Completion.OpenGroup group) {
            Event event = new Event.OpenGroup(group.groupNegated(),
                    group.logic() != null ? group.logic() : effectiveLogic());
            resetPending();
            if (group.clause() != null) {
                // only one event can be returned, and the group is it - so a clause typed with the paren is
                // staged as a draft even when naming its field would otherwise have been enough
                stageClauseStart(group.clause());
                stageFieldWithoutCompleting(group.clause().field());
            }
            return Optional.of(event);
        }

        return applyClauseStart((Completion.ClauseStart) completion);
    }

    private void stageFieldWithoutCompleting(SearchableField field) {
        pendingField = field;
        stage = NUMERIC_TYPES.contains(field.getFieldType()) ? Stage.OPERATOR : Stage.VALUE;
    }

    /**
     * Stages the field of a clause and, for a field that can only hold one value, finishes the clause with it
     * - see {@link CompletionParser#isSingleValued}. The pending negation and connector are applied either way.
     */
    private Optional<Event> applyClauseStart(Completion.ClauseStart clause) {
        stageClauseStart(clause);
        return stageField(clause.field());
    }

    /** The clause context without the field: what a group carries when its first clause is typed with it. */
    private void stageClauseStart(Completion.ClauseStart clause) {
        if (clause.logic() != null)
            pendingLogic = clause.logic();
        if (clause.negated())
            pendingNegated = true;
    }

    /**
     * @return the finished clause when naming the field was enough, empty when a value (or an operator and a
     * value) is still to come
     */
    private Optional<Event> stageField(SearchableField field) {
        pendingField = field;
        if (CompletionParser.isSingleValued(field)) {
            stage = Stage.VALUE;
            return acceptValue(field.getPossibleValues().get(0));
        }
        stage = NUMERIC_TYPES.contains(field.getFieldType()) ? Stage.OPERATOR : Stage.VALUE;
        return Optional.empty();
    }

    private Optional<Event> acceptValue(String value) {
        if (stage == Stage.VALUE && pendingOp != null && pendingOp.isRange()) {
            pendingValue1 = value; // empty = open lower bound
            stage = Stage.VALUE2;
            return Optional.empty();
        }
        if (stage == Stage.VALUE2)
            return Optional.of(complete(pendingValue1 == null ? "" : pendingValue1, value));

        // single-value stage: an empty value cannot complete a clause
        if (value.isEmpty())
            return Optional.empty();
        return Optional.of(complete(value, null));
    }

    private Event complete(String value1, @Nullable String value2) {
        QueryClause clause = pendingOp != null
                ? QueryClause.numeric(pendingField.getName(), pendingOp, value1, value2, pendingNegated)
                : QueryClause.text(pendingField.getName(), value1, pendingNegated);
        Event event = new Event.ClauseCompleted(clause, effectiveLogic());
        resetPending();
        return event;
    }

    /**
     * Backspace on empty input: pops one stage; with nothing pending asks the owner to remove the
     * last committed chip.
     */
    public Optional<Event> backspaceOnEmpty() {
        switch (stage) {
            case VALUE2 -> {
                pendingValue1 = null;
                stage = Stage.VALUE;
            }
            case VALUE -> {
                if (pendingOp != null) {
                    pendingOp = null;
                    stage = Stage.OPERATOR;
                } else {
                    pendingField = null;
                    stage = Stage.FIELD;
                }
            }
            case OPERATOR -> {
                pendingField = null;
                stage = Stage.FIELD;
            }
            case FIELD -> {
                // pop the staged decorations in reverse: NOT, then the connector, and only with
                // nothing left does Backspace reach the last committed chip
                if (pendingNegated)
                    pendingNegated = false;
                else if (pendingLogic != null)
                    pendingLogic = null;
                else
                    return Optional.of(new Event.RemoveLastNode());
            }
        }
        return Optional.empty();
    }

    /**
     * The staged (not yet committed) token fragments rendered as chips before the input,
     * e.g. {@code [OR, NOT, ionMass, >=]}. Only the field fragment names a field - the owner shortens
     * that one per its display mode and keeps the fully-qualified name for the chip's tooltip.
     */
    public List<Fragment> pendingFragments() {
        List<Fragment> fragments = new ArrayList<>(4);
        if (pendingLogic != null)
            fragments.add(new Fragment(pendingLogic.toString(), null));
        if (pendingNegated)
            fragments.add(new Fragment("NOT", null));
        if (pendingField != null)
            fragments.add(new Fragment(pendingField.getName(), pendingField.getName()));
        if (pendingOp != null)
            fragments.add(new Fragment(pendingOp.getSymbol(), null));
        return fragments;
    }

    /**
     * Placeholder/guidance text for the current stage.
     */
    public String stagePrompt() {
        // Enter is the key that accepts/adds the current token (Tab still does the same); every prompt
        // ends in ", then Enter" so users learn the accept key. Range bounds open with an empty Enter.
        // On an untouched token Enter runs the owner's action instead - the prompt says so.
        if (offersRunAction(""))
            return runAction.emptyPrompt();
        return switch (stage) {
            case FIELD -> "Search, or pick a field, then Enter";
            case OPERATOR -> "Choose how to compare, then Enter ([ ] includes the bounds, { } excludes them)";
            case VALUE -> {
                if (pendingOp != null && pendingOp.isRange())
                    yield "Lower bound, then Enter (empty = open end)";
                if (pendingField != null && !CompletionParser.valueSuggestions(pendingField).isEmpty())
                    yield "Select or type a value, then Enter";
                yield "Type a value, then Enter" + (pendingField != null && pendingField.getFieldType() == SearchableFieldType.TEXT
                        ? " (*, ?, ~ wildcards)" : "");
            }
            case VALUE2 -> "Upper bound, then Enter (empty = open end)";
        };
    }

    /**
     * Whether the token currently being built is a "terminal" clause that running the query should
     * accept as a chip first: a value has been specified (single-valued value stage, or the
     * upper bound of a range) or a default-field full-text term is typed at an entry stage. An
     * incomplete token (only a field, operator, connector or the lower bound of a range chosen) is
     * not terminal - running the query discards it.
     */
    public boolean isTerminal(@NotNull String typed) {
        String trimmed = typed.trim();
        return switch (stage) {
            // text the mode rejects must not be baked into a clause by running the query either
            case FIELD -> !trimmed.isEmpty() && inputProblem(trimmed).isEmpty();
            case OPERATOR -> false;
            case VALUE -> pendingOp != null && pendingOp.isRange() ? false : !trimmed.isEmpty();
            case VALUE2 -> true;
        };
    }

    /**
     * Completes the current entry-stage token into a keyless full-text clause from the typed text
     * (carrying any pending negation/connector). Use for the terminal free-text search when the query
     * is run.
     */
    public Event completeFreeText(@NotNull String text) {
        Event event = new Event.ClauseCompleted(QueryClause.freeText(text.trim(), pendingNegated), effectiveLogic());
        resetPending();
        return event;
    }

    /**
     * The connector for the token being built: the chosen one, else the one this position implies -
     * AND, or OR between the alternatives of a group in simple mode (see {@link #allowedConnectors}).
     */
    private LogicOp effectiveLogic() {
        return pendingLogic != null ? pendingLogic : allowedConnectors().get(0);
    }

    public void reset() {
        resetPending();
    }

    private void resetPending() {
        stage = Stage.FIELD;
        pendingLogic = null;
        pendingNegated = false;
        pendingField = null;
        pendingOp = null;
        pendingValue1 = null;
    }
}
