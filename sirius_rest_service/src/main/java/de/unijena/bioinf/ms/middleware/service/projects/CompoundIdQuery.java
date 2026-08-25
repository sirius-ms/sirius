/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.service.projects;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Selection of compounds by their id, expressed as a search query.
 * <p>
 * Compounds are not indexed yet, so queries cannot be answered by the search service. Until they are, queries that
 * refer to nothing but the compound id are answered from the document store, which covers the common case of
 * quantifying a known set of compounds. Everything else is rejected instead of being approximated, so that a query
 * never silently selects the wrong compounds.
 * <p>
 * The supported queries are interpreted with the semantics of the search index: since a compound has exactly one id,
 * {@code compoundId:1 AND compoundId:2} selects nothing, just as it would if compounds were indexed.
 * <p>
 * Deprecated: will be removed if compound index exists.
 */
@Deprecated(forRemoval = true)
record CompoundIdQuery(@NotNull Set<Long> ids, boolean negated) {

    static final String COMPOUND_ID_FIELD = "compoundId";

    private static final CompoundIdQuery EVERYTHING = new CompoundIdQuery(Set.of(), true);

    /**
     * @param searchQuery query in lucene syntax that must not refer to any field but the compound id
     * @return the selected compounds
     * @throws ResponseStatusException if the query refers to anything but compound ids
     */
    static CompoundIdQuery parse(@NotNull String searchQuery) {
        try {
            //a keyword analyzer keeps the ids as they are written instead of tokenizing them
            return of(new StandardQueryParser(new KeywordAnalyzer()).parse(searchQuery, COMPOUND_ID_FIELD));
        } catch (QueryNodeException e) {
            throw unsupported("The query could not be parsed: " + e.getMessage());
        }
    }

    private static CompoundIdQuery of(Query query) {
        return switch (query) {
            case MatchAllDocsQuery ignored -> EVERYTHING;
            case TermQuery term -> ofTerm(term);
            case BooleanQuery bool -> ofBoolean(bool);
            default -> throw unsupported("Only compound ids can be selected, but the query contains a "
                    + query.getClass().getSimpleName() + ".");
        };
    }

    private static CompoundIdQuery ofTerm(TermQuery term) {
        if (!COMPOUND_ID_FIELD.equals(term.getTerm().field()))
            throw unsupported("Only the field '" + COMPOUND_ID_FIELD + "' can be searched, but the query contains '"
                    + term.getTerm().field() + "'.");
        try {
            return new CompoundIdQuery(Set.of(Long.parseLong(term.getTerm().text())), false);
        } catch (NumberFormatException e) {
            throw unsupported("'" + term.getTerm().text() + "' is not a compound id.");
        }
    }

    /**
     * Combines the clauses like the search index would: required clauses are intersected, optional clauses are
     * united and only used if there is no required one, and prohibited clauses are subtracted.
     */
    private static CompoundIdQuery ofBoolean(BooleanQuery query) {
        CompoundIdQuery required = null;
        CompoundIdQuery optional = null;
        CompoundIdQuery prohibited = null;

        for (BooleanClause clause : query.clauses()) {
            CompoundIdQuery selection = of(clause.query());
            switch (clause.occur()) {
                case MUST, FILTER -> required = required == null ? selection : required.and(selection);
                case SHOULD -> optional = optional == null ? selection : optional.or(selection);
                case MUST_NOT -> prohibited = prohibited == null ? selection : prohibited.or(selection);
            }
        }

        CompoundIdQuery selected = required != null ? required : (optional != null ? optional : EVERYTHING);
        return prohibited == null ? selected : selected.and(prohibited.not());
    }

    private CompoundIdQuery not() {
        return new CompoundIdQuery(ids, !negated);
    }

    private CompoundIdQuery or(CompoundIdQuery other) {
        if (!negated && !other.negated)
            return new CompoundIdQuery(union(ids, other.ids), false);
        if (negated && other.negated)
            return new CompoundIdQuery(intersection(ids, other.ids), true);
        //everything but the ids that are excluded by the negated side and not selected by the other one
        CompoundIdQuery negatedSide = negated ? this : other;
        CompoundIdQuery positiveSide = negated ? other : this;
        return new CompoundIdQuery(difference(negatedSide.ids, positiveSide.ids), true);
    }

    private CompoundIdQuery and(CompoundIdQuery other) {
        if (!negated && !other.negated)
            return new CompoundIdQuery(intersection(ids, other.ids), false);
        if (negated && other.negated)
            return new CompoundIdQuery(union(ids, other.ids), true);
        CompoundIdQuery negatedSide = negated ? this : other;
        CompoundIdQuery positiveSide = negated ? other : this;
        return new CompoundIdQuery(difference(positiveSide.ids, negatedSide.ids), false);
    }

    /**
     * @return true if no compound can match this selection, so that the store does not have to be queried at all
     */
    boolean selectsNothing() {
        return !negated && ids.isEmpty();
    }

    /**
     * @return true if this selection does not restrict anything
     */
    boolean selectsEverything() {
        return negated && ids.isEmpty();
    }

    private static Set<Long> union(Set<Long> a, Set<Long> b) {
        Set<Long> result = new LinkedHashSet<>(a);
        result.addAll(b);
        return result;
    }

    private static Set<Long> intersection(Set<Long> a, Set<Long> b) {
        Set<Long> result = new LinkedHashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private static Set<Long> difference(Set<Long> a, Set<Long> b) {
        Set<Long> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    private static ResponseStatusException unsupported(String detail) {
        return new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Searching compounds is not yet supported! "
                + "Only selecting compounds by their id (e.g. '" + COMPOUND_ID_FIELD + ":1 OR " + COMPOUND_ID_FIELD
                + ":2') is possible. " + detail);
    }
}
