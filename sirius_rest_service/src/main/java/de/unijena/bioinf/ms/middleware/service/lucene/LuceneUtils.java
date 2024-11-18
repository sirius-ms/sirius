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

package de.unijena.bioinf.ms.middleware.service.lucene;

import de.unijena.bioinf.storage.db.nosql.Filter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.NumberDateFormat;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LuceneUtils {

    private static Filter createRangeFilter(String field, byte[] lower, byte[] upper) {
        if (field.equals("real")) {
            return Filter
                    .where("real")
                    .betweenBothInclusive(DoublePoint.decodeDimension(lower, 0), DoublePoint.decodeDimension(upper, 0));
        } else if (field.equals("integer")) {
            long low = LongPoint.decodeDimension(lower, 0);
            long up = LongPoint.decodeDimension(upper, 0);
            return Filter
                    .where("int32")
                    .betweenBothInclusive(
                            low < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) low,
                            up > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) up
                    );
        } else {
            return Filter
                    .where("int64")
                    .betweenBothInclusive(LongPoint.decodeDimension(lower, 0), LongPoint.decodeDimension(upper, 0));
        }
    }

    private static Filter joinClauses(List<BooleanClause> clauses) throws IOException {
        List<Filter> or = new ArrayList<>();
        List<Filter> and = new ArrayList<>();
        for (BooleanClause clause : clauses) {
            if (clause.occur().equals(BooleanClause.Occur.SHOULD)) {
                if (!and.isEmpty()) {
                    or.add(Filter.and(and.toArray(Filter[]::new)));
                    and.clear();
                }
                or.add(convertQuery(clause.query()));
            } else if (clause.occur().equals(BooleanClause.Occur.MUST) || clause.occur().equals(BooleanClause.Occur.FILTER)) {
                and.add(convertQuery(clause.query()));
            } else {
                throw new IllegalArgumentException();
            }
        }
        if (!and.isEmpty()) {
            or.add(Filter.and(and.toArray(Filter[]::new)));
        }

        if (or.isEmpty())
            throw new IllegalArgumentException();
        if (or.size() == 1)
            return or.getFirst();
        return Filter.or(or.toArray(Filter[]::new));
    }

    private static Filter convertQuery(Query query) throws IOException {
        if (query instanceof TermQuery termQuery) {
            return Filter.where(termQuery.getTerm().field()).eq(termQuery.getTerm().text());
        } else if (query instanceof RegexpQuery regexpQuery) {
            return Filter.where(regexpQuery.getRegexp().field()).regex(regexpQuery.getRegexp().text());
        } else if (query instanceof PhraseQuery phraseQuery) {
            return Filter.where(phraseQuery.getField()).eq(Arrays.stream(phraseQuery.getTerms()).map(Term::text).collect(Collectors.joining(" ")));
        } else if (query instanceof PointRangeQuery pointRangeQuery) {
            return createRangeFilter(pointRangeQuery.getField(), pointRangeQuery.getLowerPoint(), pointRangeQuery.getUpperPoint());
        } else if (query instanceof BooleanQuery booleanQuery) {
            return joinClauses(booleanQuery.clauses());
        }
        throw new IllegalArgumentException("Query type not supported.");
    }

    public static Filter translateTagFilter(String filter) throws QueryNodeException, IOException {
        StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer());
        parser.setPointsConfigMap(Map.of(
                "integer", new PointsConfig(new DecimalFormat(), Long.class),
                "real", new PointsConfig(new DecimalFormat(), Double.class),
                "date", new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class),
                "time", new PointsConfig(new NumberDateFormat(new SimpleDateFormat("HH:mm:ss")), Long.class)
        ));
        Query query = parser.parse(filter, "text");
        return convertQuery(query);
    }

}
