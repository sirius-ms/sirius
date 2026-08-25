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
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live validation of the free-text query segment. Two checks, both advisory (the search still runs
 * either way, matching the lenient server-side behavior): lucene syntax (unbalanced quotes/parens
 * degrade to a literal text search) and unknown field names (the server silently matches nothing
 * for those, which is the most confusing failure mode for users).
 */
public final class QueryValidator {

    private QueryValidator() {
    }

    private static final String FAKE_FIELD = "__FAKE_FIELD__";

    /**
     * Fielded clauses like {@code ionMass:} at the start, after whitespace or after an opening
     * paren; an escaped colon ({@code 12\:00}) is a value character, not a field separator.
     */
    private static final Pattern FIELD_USE = Pattern.compile("(?:^|[\\s(])([A-Za-z][\\w.]*):");

    private static final Analyzer ANALYZER = buildAnalyzer();

    @SneakyThrows
    private static Analyzer buildAnalyzer() {
        // values must not be lowercased, same as the parser the filter model uses
        return CustomAnalyzer.builder().withTokenizer("standard").build();
    }

    /**
     * A human-readable problem description, or empty if the query looks fine.
     */
    public static Optional<String> validate(@NotNull String query, @NotNull List<SearchableField> knownFields) {
        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return Optional.empty();

        try {
            // parser instances are cheap and not thread-safe - create per call
            new QueryParser(FAKE_FIELD, ANALYZER).parse(trimmed);
        } catch (ParseException e) {
            return Optional.of("Invalid query syntax - the text will be searched literally. "
                    + firstLine(e.getMessage()));
        }

        Matcher matcher = FIELD_USE.matcher(trimmed);
        while (matcher.find()) {
            String field = matcher.group(1);
            // tag definitions can lag behind the cached field list - never flag tags.*
            if (field.toLowerCase().startsWith("tags."))
                continue;
            boolean known = knownFields.stream().anyMatch(f -> f.getName().equalsIgnoreCase(field));
            if (!known) {
                List<String> suggestions = knownFields.stream()
                        .map(SearchableField::getName)
                        .filter(name -> similar(name, field))
                        .sorted().limit(3).toList();
                String didYouMean = suggestions.isEmpty() ? ""
                        : " Did you mean: " + String.join(", ", suggestions) + "?";
                return Optional.of("Unknown field '" + field + "' - it will match nothing." + didYouMean);
            }
        }
        return Optional.empty();
    }

    /**
     * Whether a known field name is a plausible correction for what was typed: one is a prefix of
     * the other (either direction, so 'ionmasses' still suggests 'ionMass'), on the full name or
     * any dot segment.
     */
    private static boolean similar(String name, String typed) {
        String lowerName = name.toLowerCase();
        String lowerTyped = typed.toLowerCase();
        if (lowerName.startsWith(lowerTyped) || lowerTyped.startsWith(lowerName))
            return true;
        for (String segment : lowerName.split("\\."))
            if (segment.startsWith(lowerTyped) || lowerTyped.startsWith(segment))
                return true;
        return false;
    }

    private static String firstLine(String message) {
        if (message == null)
            return "";
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
