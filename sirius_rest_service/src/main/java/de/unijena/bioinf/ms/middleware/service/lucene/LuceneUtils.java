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

import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.NumberDateFormat;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LuceneUtils {

    private static Filter createTagRangeFilter(ValueType valueType, byte[] lower, byte[] upper) {
        Filter.FilterLiteral filter = Filter.where(valueType.getValueFieldName());
        return switch (valueType){
            case INTEGER, TIME -> filter.betweenBothInclusive(IntPoint.decodeDimension(lower, 0), IntPoint.decodeDimension(upper, 0));
            case REAL -> filter.betweenBothInclusive(DoublePoint.decodeDimension(lower, 0), DoublePoint.decodeDimension(upper, 0));
            case DATE -> filter.betweenBothInclusive(LongPoint.decodeDimension(lower, 0), LongPoint.decodeDimension(upper, 0));

            default -> throw new IllegalArgumentException("Unsupported ValueType: " + valueType + ". ValueType is not numeric!");
        };
    }

    private static Filter joinClauses(List<BooleanClause> clauses, @NotNull Map<String, TagDefinition> tagDefinitionMap) throws IOException {
        List<Filter> or = new ArrayList<>();
        List<Filter> and = new ArrayList<>();
        for (BooleanClause clause : clauses) {
            if (clause.occur().equals(BooleanClause.Occur.SHOULD)) {
                if (!and.isEmpty()) {
                    or.add(Filter.and(and.toArray(Filter[]::new)));
                    and.clear();
                }
                or.add(convertQueryWithTagFieldMapping(clause.query(), tagDefinitionMap));
            } else if (clause.occur().equals(BooleanClause.Occur.MUST) || clause.occur().equals(BooleanClause.Occur.FILTER)) {
                and.add(convertQueryWithTagFieldMapping(clause.query(), tagDefinitionMap));
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

    //todo support queries with non tag fields.
    private static Filter convertQueryWithTagFieldMapping(@NotNull Query query, @NotNull Map<String, TagDefinition> tagDefinitionMap) throws IOException {
        //finalize query by boolean
        if (query instanceof BooleanQuery booleanQuery)
            return joinClauses(booleanQuery.clauses(), tagDefinitionMap);

        TagDefinition tagDef;
        Filter valueFilter;
        switch (query) {
            case TermQuery termQuery -> {
                tagDef = getTagDefinition(termQuery.getTerm().field(), tagDefinitionMap);
                String value = termQuery.getTerm().text();
                valueFilter = Filter.where(tagDef.getValueType().getValueFieldName()).eq(tagDef.getValueType() == ValueType.BOOLEAN ? Boolean.parseBoolean(value) : value);
            }
            case RegexpQuery regexpQuery -> {
                tagDef = getTagDefinition(regexpQuery.getRegexp().field(), tagDefinitionMap);
                valueFilter = Filter.where(tagDef.getValueType().getValueFieldName()).regex(regexpQuery.getRegexp().text());
            }
            case PhraseQuery phraseQuery -> {
                tagDef = getTagDefinition(phraseQuery.getField(), tagDefinitionMap);
                valueFilter = Filter.where(tagDef.getValueType().getValueFieldName())
                        .eq(Arrays.stream(phraseQuery.getTerms()).map(Term::text).collect(Collectors.joining(" ")));
            }
            case PointRangeQuery pointRangeQuery -> {
                tagDef = getTagDefinition(pointRangeQuery.getField(), tagDefinitionMap);
                valueFilter = createTagRangeFilter(tagDef.getValueType(), pointRangeQuery.getLowerPoint(), pointRangeQuery.getUpperPoint());
            }
            default -> throw new IllegalArgumentException("Query type not supported.");
        }

        //Add tag name filter if field corresponds to tag.
        valueFilter = Filter.and(Filter.where("tagName").eq(tagDef.getTagName()), valueFilter); //

        return valueFilter;
    }

    /**
     * @param queryFieldName
     * @param tagDefinitionMap
     * @return
     */
    @NotNull
    private static TagDefinition getTagDefinition(String queryFieldName, Map<String, TagDefinition> tagDefinitionMap) {
        // check if field refers to a tag.
        if (queryFieldName.startsWith(TAG_FIELD_PREFIX)) {
            String tagName = queryFieldName.substring(TAG_FIELD_PREFIX.length());
            TagDefinition tagDef = tagDefinitionMap.get(tagName);
            if (tagDef == null)
                throw new IllegalArgumentException("No such tag: " + tagName);
            return tagDef;

        }
        throw new IllegalArgumentException("Currently only tags are searchable!");
    }

    public static StandardQueryParser makeDefaultQueryParser(@NotNull Stream<TagDefinition> tagDefinitions) {
        StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer());
        parser.setPointsConfigMap(new HashMap<>());
        tagDefinitions.forEach(tagDef -> updatePointValue(parser.getPointsConfigMap(), tagDef));;
        return parser;
    }

    public static Filter translateTagFilter(String filter, @NotNull Stream<TagDefinition> tagDefinitions) throws QueryNodeException, IOException {
        return translateTagFilter(filter, makeDefaultQueryParser(tagDefinitions), tagDefinitions);
    }

    public static Filter translateTagFilter(String filter, StandardQueryParser parser, @NotNull Stream<TagDefinition> tagDefinitions) throws QueryNodeException, IOException {
        return translateTagFilter(filter, parser, tagDefinitions.collect(Collectors.toMap(TagDefinition::getTagName, Function.identity())));
    }

    public static Filter translateTagFilter(String filter, StandardQueryParser parser, @NotNull Map<String, TagDefinition> tagDefinitionMap) throws QueryNodeException, IOException {
        Query query = parser.parse(filter, "text");
        return convertQueryWithTagFieldMapping(query, tagDefinitionMap);
    }

    public static final String TAG_FIELD_PREFIX = "tags.";

   public static void updatePointValue(@NotNull final Map<String, PointsConfig> pointsConfigMap, @NotNull final TagDefinition tagDefinition) {
        String indexFieldName = TAG_FIELD_PREFIX + tagDefinition.getTagName();

        PointsConfig pointsConfig = pointsConfigFromTagDefinition(tagDefinition);
        if (pointsConfig != null)
            pointsConfigMap.put(indexFieldName, pointsConfig);
        else
            pointsConfigMap.remove(indexFieldName);

    }


    private static PointsConfig pointsConfigFromTagDefinition(TagDefinition tagDefinition) {
        return switch (tagDefinition.getValueDefinition().getValueType()) {
            case INTEGER -> new PointsConfig(DecimalFormat.getInstance(), Integer.class);

            case REAL -> new PointsConfig(DecimalFormat.getInstance(), Double.class);

            case DATE -> new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class);

            case TIME -> new PointsConfig(new NumberDateFormat(new SimpleDateFormat("HH:mm:ss")), Integer.class);

            default -> null;
        };
    }
}
