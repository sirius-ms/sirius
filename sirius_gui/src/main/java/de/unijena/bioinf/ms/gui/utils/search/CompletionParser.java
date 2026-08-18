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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
     * All fields matching the prefix, deterministically ordered: full-name prefix matches first, then
     * dot-segment (leaf name) matches, then matches on a word INSIDE a segment, alphabetical within each.
     * <p>
     * The word tier is what lets the user search the way they think about a field: {@code mass} finds
     * {@code ionMass}, {@code adducts} finds {@code detectedAdducts}. It also decides typed input, so
     * {@code mass} + Tab starts a clause on {@code ionMass} - the suggestion list and Tab always agree.
     */
    public static List<SearchableField> fieldMatches(@NotNull String prefix, @NotNull List<SearchableField> fields) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        // rank once per field (matching tokenizes the name), then sort the survivors
        record Ranked(SearchableField field, int rank) {
        }
        return fields.stream()
                .map(field -> new Ranked(field, matchRank(field, lowerPrefix)))
                .filter(ranked -> ranked.rank() < NO_MATCH)
                .sorted(Comparator.comparingInt(Ranked::rank)
                        .thenComparing(ranked -> ranked.field().getName()))
                .map(Ranked::field)
                .toList();
    }

    public static Optional<SearchableField> bestFieldMatch(@NotNull String prefix, @NotNull List<SearchableField> fields) {
        return fieldMatches(prefix, fields).stream().findFirst();
    }

    /** Rank of a non-matching field; anything below it is offered, lower is a better match. */
    private static final int NO_MATCH = 3;

    private static int matchRank(SearchableField field, String lowerPrefix) {
        String name = field.getName().toLowerCase(Locale.ROOT);
        if (name.startsWith(lowerPrefix))
            return 0;
        for (String segment : name.split("\\."))
            if (segment.startsWith(lowerPrefix))
                return 1;
        for (String word : words(field.getName()))
            if (word.startsWith(lowerPrefix))
                return 2;
        return NO_MATCH;
    }

    /**
     * The lower-cased words a field name is made of, so a prefix can be matched against each of them:
     * {@code rtApexSeconds} is {@code rt|apex|seconds}, {@code qualities.PEAK_QUALITY} is
     * {@code qualities|peak|quality}, {@code topAnnotations.GNPSLibraryHit} is
     * {@code top|annotations|gnps|library|hit}.
     * <p>
     * Boundaries are the separators that occur in field names ({@code . _ -}), a lower-to-upper case
     * change and the end of an upper-case run that starts a new word (the {@code L} in
     * {@code GNPSLibrary}). Digits deliberately do NOT start a word, so {@code hasMs1} stays
     * {@code has|ms1} and both {@code ms} and {@code ms1} find it. This mirrors how the index tokenizes
     * the VALUES it stores (SiriusStandardAnalyzer splits on case changes just the same).
     */
    static List<String> words(@NotNull String fieldName) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder(fieldName.length());
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (c == '.' || c == '_' || c == '-' || Character.isWhitespace(c)) {
                flush(word, words);
                continue;
            }
            if (Character.isUpperCase(c) && startsNewWord(fieldName, i))
                flush(word, words);
            word.append(c);
        }
        flush(word, words);
        return words;
    }

    /** Whether the upper-case character at {@code i} starts a word rather than continuing an acronym. */
    private static boolean startsNewWord(String name, int i) {
        if (i == 0)
            return false;
        char previous = name.charAt(i - 1);
        if (Character.isLowerCase(previous) || Character.isDigit(previous))
            return true; // ionMass -> ion|Mass
        // inside an upper-case run only the last one starts a word: GNPSLibrary -> gnps|library
        return Character.isUpperCase(previous)
                && i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
    }

    private static void flush(StringBuilder word, List<String> words) {
        if (!word.isEmpty())
            words.add(word.toString().toLowerCase(Locale.ROOT));
        word.setLength(0);
    }

    /**
     * The values the draft editor can offer for a field: whatever closed vocabulary the field reports,
     * true/false for BOOLEAN fields, nothing otherwise.
     * <p>
     * Having a vocabulary is a property of the field, not of its type: a text field holds one when its values
     * come from a fixed domain (a compound class ontology, a tag restricted by its definition), and those are
     * exactly the fields a user cannot type from memory. The vocabulary stays an offer - the field can still be
     * queried with anything, e.g. a wildcard.
     * <p>
     * NOT_APPLICABLE is never offered. It does not mean bad quality but that there was nothing to judge, so it
     * is not a choice a user makes: features in that state always pass a quality filter, which the query builder
     * ensures by adding the term itself. Typing it by hand still works.
     */
    public static List<String> valueSuggestions(@NotNull SearchableField field) {
        if (field.getPossibleValues() != null)
            return field.getPossibleValues().stream()
                    .filter(value -> !DataQuality.NOT_APPLICABLE.toString().equals(value))
                    .toList();
        if (field.getFieldType() == SearchableFieldType.BOOLEAN)
            return List.of("true", "false");
        return List.of();
    }

    /**
     * The offered values matching the typed prefix, best match first.
     * <p>
     * Values are not typed the way they are written. A vocabulary can be a whole ontology, where matching only
     * the start of a value would hide "Carboxylic acids and derivatives" from someone typing "acids" - so every
     * word of a value starts a match, words being separated by the punctuation the value happens to use. And a
     * value can be punctuation itself: an adduct is indexed as "[M + H]+" but typed as "[M+H]+" or "M+H", which
     * no prefix of a word covers - so the value with its whitespace removed is matched anywhere as well.
     * <p>
     * Ranked from the most literal match to the loosest, and within a rank the given order is kept: it carries
     * meaning, e.g. the declaration order of an ordered enum or the curated order of a tag definition.
     */
    public static List<String> valueMatches(@NotNull String prefix, @NotNull List<String> values) {
        String lowerPrefix = prefix.toLowerCase();
        return values.stream()
                .filter(value -> valueMatchRank(value, lowerPrefix) < NO_MATCH)
                .sorted(Comparator.comparingInt((String value) -> valueMatchRank(value, lowerPrefix)))
                .toList();
    }

    private static final int NO_MATCH = 3;

    /** Words of a value: what is left between the punctuation it is written with. */
    private static final Pattern NON_WORD = Pattern.compile("\\W+");

    private static int valueMatchRank(String value, String lowerPrefix) {
        String lower = value.toLowerCase();
        if (lower.startsWith(lowerPrefix))
            return 0;
        for (String word : NON_WORD.split(lower))
            if (!word.isEmpty() && word.startsWith(lowerPrefix))
                return 1;
        if (withoutWhitespace(lower).contains(withoutWhitespace(lowerPrefix)))
            return 2;
        return NO_MATCH;
    }

    private static String withoutWhitespace(String text) {
        return text.replaceAll("\\s+", "");
    }
}
