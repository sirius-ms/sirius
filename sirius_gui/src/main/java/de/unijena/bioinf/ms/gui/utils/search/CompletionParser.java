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

import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The completion grammar of the search bar (what the accept key makes of typed text): {@code [and|or] [not] <field-prefix>} for a clause,
 * {@code [and|or] [not] (} to open a group, and {@code )} to close one - so {@code or not (ma}
 * opens a negated OR-group and starts a clause inside it. The two {@code not} positions are told
 * apart purely by which side of the paren they fall on: before it negates the group, after it the
 * clause. Ported from parseCompletion/parseClauseTail of LuceneChemicalSearchBar.tsx.
 * <p>
 * Field matching is case-insensitive against the full field name, extended by dot-segment matching
 * so the leaf name of a nested field completes too ({@code inchi} finds
 * {@code topAnnotations.structureAnnotation.inchiKey}). Ambiguity resolves deterministically:
 * full-name matches before segment matches, alphabetical within each.
 */
public final class CompletionParser {

    private CompletionParser() {
    }

    private static final Pattern OPEN_GROUP = Pattern.compile("^(?:(and|or)\\s*)?(not\\s*)?\\(\\s*");
    private static final Pattern CONNECTOR = Pattern.compile("^(and|or)\\s+");
    private static final Pattern NOT_PREFIX = Pattern.compile("^not\\s+");

    /**
     * @param hasSibling whether the container the cursor is in already holds an expression the new
     *                   one would join - a connector only means something then
     * @param groupOpen  whether there is anything for {@code )} to close
     */
    public static Optional<Completion> parse(@NotNull String text, @NotNull List<SearchableField> fields,
                                             boolean hasSibling, boolean groupOpen) {
        String trimmed = text.trim().toLowerCase();
        if (trimmed.isEmpty())
            return Optional.empty();

        // only offer the closing paren when a group is actually open
        if (trimmed.equals(")"))
            return groupOpen ? Optional.of(new Completion.CloseGroup()) : Optional.empty();

        // open-group form: "not" here precedes the paren, so it binds to the group
        Matcher openMatch = OPEN_GROUP.matcher(trimmed);
        if (openMatch.find()) {
            String tail = trimmed.substring(openMatch.end());
            Completion.ClauseStart clause = tail.isEmpty() ? null : parseClauseTail(tail, fields, null);
            if (!tail.isEmpty() && clause == null)
                return Optional.empty(); // "(xyz" names no field - leave the whole thing as free text
            return Optional.of(new Completion.OpenGroup(openMatch.group(2) != null,
                    connector(openMatch.group(1), hasSibling), clause));
        }

        // plain clause form; whitespace after the connector is required so "orma" is not OR + "ma"
        Matcher logicMatch = CONNECTOR.matcher(trimmed);
        LogicOp logic = null;
        String rest = trimmed;
        if (logicMatch.find()) {
            logic = connector(logicMatch.group(1), hasSibling);
            rest = trimmed.substring(logicMatch.end());
        }
        return Optional.ofNullable(parseClauseTail(rest, fields, logic));
    }

    /**
     * {@code [not] <field-prefix>} - the tail shared by the plain clause form and one typed
     * directly inside a paren. The whitespace after {@code not} is required, so {@code notion}
     * stays free text rather than negating a match on {@code ion}.
     */
    @Nullable
    private static Completion.ClauseStart parseClauseTail(@NotNull String text, @NotNull List<SearchableField> fields,
                                                          @Nullable LogicOp logic) {
        Matcher notMatch = NOT_PREFIX.matcher(text);
        boolean negated = notMatch.find();
        String prefix = negated ? text.substring(notMatch.end()) : text;
        if (prefix.isEmpty())
            return null;
        return bestFieldMatch(prefix, fields)
                .map(field -> new Completion.ClauseStart(field, negated, logic))
                .orElse(null);
    }

    @Nullable
    private static LogicOp connector(@Nullable String match, boolean hasSibling) {
        // a leading and/or is always consumed so the rest still resolves, but it only means
        // something once there is a sibling to join
        if (match == null || !hasSibling)
            return null;
        return LogicOp.valueOf(match.toUpperCase());
    }

    /**
     * All fields matching the prefix, deterministically ordered: full-name prefix matches first,
     * then dot-segment (leaf name) matches, alphabetical within each.
     */
    public static List<SearchableField> fieldMatches(@NotNull String prefix, @NotNull List<SearchableField> fields) {
        String lowerPrefix = prefix.toLowerCase();
        return fields.stream()
                .filter(field -> matchRank(field, lowerPrefix) < 2)
                .sorted(Comparator.comparingInt((SearchableField field) -> matchRank(field, lowerPrefix))
                        .thenComparing(SearchableField::getName))
                .toList();
    }

    public static Optional<SearchableField> bestFieldMatch(@NotNull String prefix, @NotNull List<SearchableField> fields) {
        return fieldMatches(prefix, fields).stream().findFirst();
    }

    private static int matchRank(SearchableField field, String lowerPrefix) {
        String name = field.getName().toLowerCase();
        if (name.startsWith(lowerPrefix))
            return 0;
        for (String segment : name.split("\\."))
            if (segment.startsWith(lowerPrefix))
                return 1;
        return 2;
    }

    /**
     * The values the draft editor can offer for a field: enum constants for ENUM fields,
     * true/false for BOOLEAN fields, nothing otherwise.
     * <p>
     * NOT_APPLICABLE is never offered. It does not mean bad quality but that there was nothing to judge, so it
     * is not a choice a user makes: features in that state always pass a quality filter, which the query builder
     * ensures by adding the term itself. Typing it by hand still works.
     */
    public static List<String> valueSuggestions(@NotNull SearchableField field) {
        if (field.getFieldType() == SearchableFieldType.ENUM && field.getPossibleValues() != null)
            return field.getPossibleValues().stream()
                    .filter(value -> !DataQuality.NOT_APPLICABLE.toString().equals(value))
                    .toList();
        if (field.getFieldType() == SearchableFieldType.BOOLEAN)
            return List.of("true", "false");
        return List.of();
    }
}
