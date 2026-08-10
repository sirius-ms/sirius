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

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The staged token input of the search bar, modeled after GitLab's filtered search. When the current
 * container already holds a sibling the input starts at the {@link Stage#CONNECTOR} stage
 * ({@code AND}/{@code OR}); otherwise, and after the connector is chosen, the {@link Stage#FIELD}
 * stage lists all fields plus {@code NOT} and the parens. Choosing a field advances to the operator
 * stage (numeric fields only) and then the value stage(s) - ranges take two staged values, an empty
 * bound meaning open-ended. Backspace on empty input pops one stage; with nothing pending it asks
 * the owner to remove the last committed chip.
 * <p>
 * Pure model: the UI renders {@link #suggestions}, {@link #pendingFragments} and {@link #stagePrompt}
 * and applies the returned {@link Event}s to the query tree. Free text is not consumed here - typed
 * text that resolves to nothing stays the free-text search segment of the owner.
 */
public class TokenInputModel {

    /**
     * Field types whose clauses carry a comparison operator.
     */
    public static final EnumSet<SearchableFieldType> NUMERIC_TYPES = EnumSet.of(
            SearchableFieldType.INTEGER, SearchableFieldType.LONG, SearchableFieldType.DOUBLE,
            SearchableFieldType.FLOAT, SearchableFieldType.DATE, SearchableFieldType.TIME);

    /**
     * CONNECTOR and FIELD are the two "entry" stages a fresh token starts in: CONNECTOR when the
     * current container has a sibling to join (offers AND/OR), FIELD otherwise (offers fields, NOT,
     * parens). The remaining stages build one clause.
     */
    public enum Stage {CONNECTOR, FIELD, OPERATOR, VALUE, VALUE2}

    public enum SpecialToken {
        NOT("NOT", "Negate the next filter or group"),
        AND("AND", "Both conditions must match"),
        OR("OR", "Either condition may match"),
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
                return op.getLabel();
            }

            @Override
            public String description() {
                return null;
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
    }

    /**
     * A change the owner has to apply to the query tree.
     */
    public sealed interface Event {
        record ClauseCompleted(@NotNull QueryClause clause, @NotNull LogicOp logic) implements Event {
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
    private boolean hasSibling;
    private boolean groupOpen;

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

    public void updateContext(@NotNull List<SearchableField> fields, boolean hasSibling, boolean groupOpen) {
        this.fields = fields;
        this.hasSibling = hasSibling;
        this.groupOpen = groupOpen;
        // keep the entry stage in sync with the (possibly changed) sibling context, as long as
        // nothing has been staged yet for this token
        if (atEntryStage() && pendingLogic == null && !pendingNegated && pendingField == null)
            stage = hasSibling ? Stage.CONNECTOR : Stage.FIELD;
    }

    public Stage stage() {
        return stage;
    }

    /**
     * True at the two stages that start a fresh token (CONNECTOR/FIELD) - where typed text that
     * resolves to nothing is the owner's free-text search rather than a clause value.
     */
    public boolean atEntryStage() {
        return stage == Stage.CONNECTOR || stage == Stage.FIELD;
    }

    /**
     * The suggestions for the current stage, narrowed by the typed prefix (empty prefix = all).
     */
    public List<Suggestion> suggestions(@NotNull String typed) {
        String prefix = typed.trim().toLowerCase(Locale.ROOT);
        return switch (stage) {
            case CONNECTOR -> connectorSuggestions(prefix);
            case FIELD -> fieldStageSuggestions(prefix);
            case OPERATOR -> operatorSuggestions(prefix);
            case VALUE, VALUE2 -> valueSuggestions(prefix);
        };
    }

    private List<Suggestion> connectorSuggestions(String prefix) {
        List<Suggestion> suggestions = new ArrayList<>(2);
        if (matches(SpecialToken.AND, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.AND));
        if (matches(SpecialToken.OR, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.OR));
        return suggestions;
    }

    private List<Suggestion> fieldStageSuggestions(String prefix) {
        List<Suggestion> suggestions = new ArrayList<>(
                CompletionParser.fieldMatches(prefix, fields).stream()
                        .map(f -> (Suggestion) new Suggestion.FieldSuggestion(f)).toList());
        if (!pendingNegated && matches(SpecialToken.NOT, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.NOT));
        if (matches(SpecialToken.OPEN_GROUP, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.OPEN_GROUP));
        if (groupOpen && matches(SpecialToken.CLOSE_GROUP, prefix))
            suggestions.add(new Suggestion.TokenSuggestion(SpecialToken.CLOSE_GROUP));
        return suggestions;
    }

    private static boolean matches(SpecialToken token, String prefix) {
        return token.display.toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private List<Suggestion> operatorSuggestions(String prefix) {
        return java.util.Arrays.stream(NumberOp.values())
                .filter(op -> op.getLabel().toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(op -> (Suggestion) new Suggestion.OperatorSuggestion(op))
                .toList();
    }

    private List<Suggestion> valueSuggestions(String prefix) {
        if (pendingField == null)
            return List.of();
        return CompletionParser.valueSuggestions(pendingField).stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(value -> (Suggestion) new Suggestion.ValueSuggestion(value))
                .toList();
    }

    /**
     * Applies a chosen suggestion, advancing the stage.
     */
    public Optional<Event> choose(@NotNull Suggestion suggestion) {
        if (suggestion instanceof Suggestion.FieldSuggestion field) {
            pendingField = field.field();
            stage = NUMERIC_TYPES.contains(field.field().getFieldType()) ? Stage.OPERATOR : Stage.VALUE;
            return Optional.empty();
        }
        if (suggestion instanceof Suggestion.TokenSuggestion token) {
            return switch (token.token()) {
                case NOT -> {
                    pendingNegated = true;
                    yield Optional.empty();
                }
                case AND -> {
                    pendingLogic = LogicOp.AND;
                    stage = Stage.FIELD; // connector chosen -> pick the field/token to join
                    yield Optional.empty();
                }
                case OR -> {
                    pendingLogic = LogicOp.OR;
                    stage = Stage.FIELD;
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
        // value suggestion
        return acceptValue(((Suggestion.ValueSuggestion) suggestion).value());
    }

    /**
     * Enter with raw typed text: value stages take it as the value; the operator stage matches it
     * against the operators; at IDLE multi-token grammar input ({@code or not ion}) is applied in
     * one go, anything else is the owner's free-text segment (no state change, empty result).
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
            case CONNECTOR, FIELD -> {
                return applyGrammar(trimmed);
            }
        }
        return Optional.empty();
    }

    private Optional<Event> applyGrammar(String text) {
        Optional<Completion> parsed = CompletionParser.parse(text, fields, hasSibling, groupOpen);
        if (parsed.isEmpty())
            return Optional.empty(); // free text - stays in the input, owned by the overlay

        Completion completion = parsed.get();
        if (completion instanceof Completion.CloseGroup)
            return Optional.of(new Event.CloseGroup());

        if (completion instanceof Completion.OpenGroup group) {
            Event event = new Event.OpenGroup(group.groupNegated(),
                    group.logic() != null ? group.logic() : effectiveLogic());
            resetPending();
            if (group.clause() != null)
                applyClauseStart(group.clause());
            return Optional.of(event);
        }

        applyClauseStart((Completion.ClauseStart) completion);
        return Optional.empty();
    }

    private void applyClauseStart(Completion.ClauseStart clause) {
        if (clause.logic() != null)
            pendingLogic = clause.logic();
        if (clause.negated())
            pendingNegated = true;
        pendingField = clause.field();
        stage = NUMERIC_TYPES.contains(clause.field().getFieldType()) ? Stage.OPERATOR : Stage.VALUE;
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
                // pop the field-stage decorations in reverse; clearing the connector drops back to
                // the connector stage (via updateContext), then Backspace removes the last chip
                if (pendingNegated)
                    pendingNegated = false;
                else if (pendingLogic != null)
                    pendingLogic = null;
                else
                    return Optional.of(new Event.RemoveLastNode());
            }
            case CONNECTOR -> {
                return Optional.of(new Event.RemoveLastNode());
            }
        }
        return Optional.empty();
    }

    /**
     * The staged (not yet committed) token fragments rendered as chips before the input,
     * e.g. {@code [OR, NOT, ionMass, >=]}.
     */
    public List<String> pendingFragments() {
        List<String> fragments = new ArrayList<>(4);
        if (pendingLogic != null)
            fragments.add(pendingLogic.toString());
        if (pendingNegated)
            fragments.add("NOT");
        if (pendingField != null)
            fragments.add(pendingField.getName());
        if (pendingOp != null)
            fragments.add(pendingOp.getSymbol());
        return fragments;
    }

    /**
     * Placeholder/guidance text for the current stage.
     */
    public String stagePrompt() {
        return switch (stage) {
            case CONNECTOR -> "Combine with the previous filter (AND / OR)";
            case FIELD -> "Search, or filter by field name...";
            case OPERATOR -> "How to compare - [ ] includes the bounds, { } excludes them";
            case VALUE -> {
                if (pendingOp != null && pendingOp.isRange())
                    yield "Lower bound (Enter on empty = open)";
                if (pendingField != null && !CompletionParser.valueSuggestions(pendingField).isEmpty())
                    yield "Select or type a value";
                yield "Enter a value" + (pendingField != null && pendingField.getFieldType() == SearchableFieldType.TEXT
                        ? " (*, ?, ~ wildcards)" : "");
            }
            case VALUE2 -> "Upper bound (Enter on empty = open)";
        };
    }

    /**
     * The connector for the token being built: the explicitly chosen one, AND otherwise.
     */
    private LogicOp effectiveLogic() {
        return pendingLogic != null ? pendingLogic : LogicOp.AND;
    }

    public void reset() {
        resetPending();
    }

    private void resetPending() {
        // FIELD by default; updateContext bumps to CONNECTOR when the container has a sibling
        stage = hasSibling ? Stage.CONNECTOR : Stage.FIELD;
        pendingLogic = null;
        pendingNegated = false;
        pendingField = null;
        pendingOp = null;
        pendingValue1 = null;
    }
}
