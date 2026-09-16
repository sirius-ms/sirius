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

package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lets a numeric range bound be written as a percentage: {@code intensityApex:[10% TO *]} (or
 * {@code intensityApex>=10%}) is matched against the field's relative form, the indexed field of the same name
 * with the suffix {@value #RELATIVE_SUFFIX}, which holds the value in percent of its reference - for the
 * intensities, the most intense feature of the same run. So the clause becomes
 * {@code intensityApexRelative:[10 TO *]}, and what the percentage refers to is decided where the relative
 * field is written, not here.
 * <p>
 * Runs before the points processors, which would otherwise parse {@code 10%} leniently as {@code 10} and
 * silently filter on the wrong number. Fields without a {@link PointsConfig} are left alone: for a text field
 * a percent sign is just a character. A numeric field without a relative form refuses the bound, and so does
 * a range that mixes a percentage with an absolute bound - both would otherwise match something other than
 * what was written.
 */
public class RelativeBoundQueryNodeProcessor extends QueryNodeProcessorImpl {

    /** The suffix that names the relative form of a numeric field. */
    public static final String RELATIVE_SUFFIX = "Relative";

    private final Map<String, PointsConfig> pointsConfigs;

    /** @param pointsConfigs the numeric fields as the query parser knows them */
    public RelativeBoundQueryNodeProcessor(@NotNull Map<String, PointsConfig> pointsConfigs) {
        this.pointsConfigs = pointsConfigs;
    }

    @Override
    protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {
        if (!(node instanceof TermRangeQueryNode range))
            return node;
        String field = String.valueOf(range.getField());
        if (pointsConfigs.get(field) == null)
            return node;
        boolean lowerRelative = isRelative(range.getLowerBound());
        boolean upperRelative = isRelative(range.getUpperBound());
        if (!lowerRelative && !upperRelative)
            return node;

        String relativeField = field + RELATIVE_SUFFIX;
        if (pointsConfigs.get(relativeField) == null)
            throw new IllegalArgumentException("A percentage bound is not supported for '" + field + "': the field has no relative form.");
        if ((lowerRelative && isAbsolute(range.getUpperBound())) || (upperRelative && isAbsolute(range.getLowerBound())))
            throw new IllegalArgumentException("A range on '" + field + "' must not mix a percentage with an absolute bound.");

        stripPercent(range.getLowerBound());
        stripPercent(range.getUpperBound());
        range.setField(relativeField);
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }

    private static boolean isRelative(FieldQueryNode bound) {
        return bound != null && bound.getTextAsString().endsWith("%");
    }

    /** A bound that is neither a percentage nor open ({@code *} or empty). */
    private static boolean isAbsolute(FieldQueryNode bound) {
        if (bound == null || isRelative(bound))
            return false;
        String text = bound.getTextAsString();
        return !text.isEmpty() && !text.equals("*");
    }

    /** Turns a {@code <percent>%} bound into the plain number the relative field is compared with. */
    private static void stripPercent(FieldQueryNode bound) {
        if (!isRelative(bound))
            return;
        String text = bound.getTextAsString();
        String number = text.substring(0, text.length() - 1).trim();
        ParsePosition position = new ParsePosition(0);
        Number percent = NumberFormat.getInstance(Locale.ROOT).parse(number, position);
        if (percent == null || position.getIndex() != number.length())
            throw new IllegalArgumentException("'" + text + "' is not a percentage: expected a number followed by '%'");
        bound.setText(number);
    }
}
