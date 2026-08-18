/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.elgordo.LipidClass;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LipidAnnotationMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils;
import de.unijena.bioinf.projectspace.QueryRewriter;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lets a lipid class be searched by its abbreviation.
 * <p>
 * A lipid class is indexed under its long name - {@code Diacylglycerophosphocholine} - and nobody types that;
 * they type {@code PC}. The abbreviations are informal shorthand rather than an official vocabulary, so they
 * are made to match here instead of being offered as values: what a client is offered stays what the index
 * holds.
 * <p>
 * The rewritten query is built with the field's own analyzer, so it matches the indexed value however that
 * analyzer happens to break it up - a single term for {@code Ceramide}, a phrase for {@code Hexose Ceramide}.
 * Reproducing those rules here by hand would be a second place to get them wrong.
 * <p>
 * What this cannot fix: the index holds the words of a name, so {@code Cer} matches Ceramide and Hexose
 * Ceramide alike, and MGDG and DGDG share one long name and cannot be told apart by any query.
 */
public class LipidClassQueryRewriter implements QueryRewriter {

    /**
     * Abbreviation to long name, keyed by what reaches a rewriter: the query parser has already analyzed the
     * text, so it arrives lowercased, and an abbreviation split on its case change ({@code HexCer}) arrives as
     * its parts joined by a space. Stripping whitespace makes both forms land on the same key.
     */
    private static final Map<String, String> LONG_NAME_BY_ABBREVIATION = Arrays.stream(LipidClass.values())
            .collect(Collectors.toMap(lipidClass -> normalize(lipidClass.abbr()), LipidClass::longName,
                    (first, second) -> first));

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    @Override
    public Query rewrite(String field, String text, boolean isPhrase) {
        if (text == null || text.isBlank())
            return null;
        if (!field.endsWith(LipidAnnotationMapper.LIPID_CLASS_NAME))
            return null; // registered for everything the lipid mapper writes; only the class name is a class name

        String longName = LONG_NAME_BY_ABBREVIATION.get(normalize(text));
        if (longName == null)
            return null; // not an abbreviation - a word of a long name, or something else entirely

        // the same builder the query parser uses: a term for a one-word name, a phrase for a longer one
        return new QueryBuilder(LuceneMappingUtils.SIRIUS_TEXT_ANALYZER).createPhraseQuery(field, longName);
    }
}
