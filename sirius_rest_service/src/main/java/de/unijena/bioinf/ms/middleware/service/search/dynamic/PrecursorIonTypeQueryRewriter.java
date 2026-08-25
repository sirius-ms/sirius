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

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.QueryRewriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.regex.Pattern;

/**
 * Normalizes an adduct query to the notation adducts are indexed in.
 * <p>
 * Adducts are stored as {@link PrecursorIonType#toString()}, which spaces its operators ({@code [M + H]+}),
 * and the field is keyword indexed - so it matches the exact term and nothing else. Users write adducts the way
 * they are used to ({@code [M+H]+}, {@code M+H}, {@code M+H+}) and would find nothing. Parsing the query into a
 * {@link PrecursorIonType} and printing it back accepts every notation the periodic table understands, which is
 * far more than a list of spelling variants could cover.
 * <p>
 * Anything that is not an adduct is left untouched, so the field stays searchable with wildcards and free text.
 * A phrase is rewritten to a term like everything else: the values are single keyword terms, a phrase over them
 * would match nothing anyway.
 * <p>
 * Parsed without storing: an ion mode the periodic table does not know yet is used for this query and not
 * registered, so a mistyped adduct in a search box cannot widen what SIRIUS treats as a common ion mode.
 */
public class PrecursorIonTypeQueryRewriter implements QueryRewriter {

    /**
     * What an adduct looks like before it is parsed: the neutral molecule {@code M} (optionally preceded by a
     * multimer count and an opening bracket) followed by the start of a modification, a charge or the closing
     * bracket - {@code [M+H]+}, {@code M-H}, {@code 2M+Na}, {@code [M]+}.
     * <p>
     * The parser this guards is lenient: it reads unrecognized text as a molecular formula, and "Xx" happens to
     * be a real element symbol - so "Xxx" would come back as the adduct [M + Xx]+. Anything not shaped like an
     * adduct therefore never reaches the parser.
     */
    private static final Pattern ADDUCT_SHAPE = Pattern.compile("^\\[?\\s*\\d*\\s*M\\s*[\\]+\\-]");

    @Override
    public Query rewrite(String field, String text, boolean isPhrase) {
        if (text == null || text.isBlank())
            return null;

        String queryText = text.trim();
        if (!ADDUCT_SHAPE.matcher(queryText).find())
            return null; // not even shaped like an adduct - do not hand it to the parser at all

        final PrecursorIonType ionType;
        try {
            ionType = PeriodicTable.getInstance().ionByName(queryText, true); // a query must not change the table
        } catch (Exception e) {
            return null; // not an adduct - leave the query as the user wrote it
        }
        if (ionType == null)
            return null;

        // The parser is lenient: text it recognizes nothing in yields no ionization, no adduct and no in-source
        // fragment, which it reports as the intrinsically charged [M]+. Rewriting to that would silently turn a
        // search for something else into a search for all intrinsically charged features, so accept it only
        // when the user really asked for it (i.e. wrote a known spelling of it).
        if (ionType.isIntrinsicalCharged() && !PeriodicTable.getInstance().hasIon(queryText))
            return null;

        String canonical = ionType.toString();
        if (canonical.equals(text))
            return null; // already the indexed notation, no rewrite needed

        return new TermQuery(new Term(field, canonical));
    }
}
