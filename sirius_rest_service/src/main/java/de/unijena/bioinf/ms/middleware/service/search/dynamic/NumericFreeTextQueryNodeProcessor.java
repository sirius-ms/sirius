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
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Makes a bare number in a query find the features it means: a term without a field that reads as a number is
 * searched, in addition to the default text fields, as a window on every NUMERIC default search field
 * ({@code @IndexField(defaultSearchField = true)} on a number - the precursor m/z of a feature). Typing a mass is
 * the most common search in mass spectrometry, and a mass is never typed exactly.
 * <p>
 * <b>The window.</b> For a number typed with {@code d} decimals the half-width is the larger of
 * <ul>
 *   <li>half a unit of the last decimal, {@code 10^-d / 2} - so {@code 301} means 300.5 to 301.5, {@code 301.1}
 *       means ±0.05, {@code 301.14} means ±0.005: fewer decimals, wider net, which is what a rounded mass says;</li>
 *   <li>{@code ppm × value / 1e6} - a floor for a mass pasted with many decimals, whose half-unit would be far
 *       below what any instrument resolves ({@code 301.1421} alone would mean ±0.00005, 0.2 ppm).</li>
 * </ul>
 * Bounds are inclusive.
 * <p>
 * <b>What it touches.</b> Only a plain, non-quoted, keyless term ({@code 301.14}); {@code name:301} or
 * {@code "301.14"} are left alone, and so are ranges, wildcards and fuzzy terms. The term keeps its default-field
 * search - a feature <i>named</i> "301.14" still matches - the window is OR-ed to it. It has to run
 * <em>before</em> the multi-field expansion, which is the last moment a keyless term is recognisable as one.
 */
public final class NumericFreeTextQueryNodeProcessor extends QueryNodeProcessorImpl {

    /** The ppm floor of the window's half-width, roughly the MS1 mass accuracy SIRIUS assumes by default. */
    public static final double DEFAULT_PPM = 10d;

    /** A plain decimal number; a sign would have been parsed as a boolean modifier, an exponent is not a mass. */
    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");

    private final Supplier<List<String>> fields;
    private final Map<String, PointsConfig> pointsConfigs;
    private final double ppm;

    /**
     * @param fields        the numeric default search fields a bare number also searches, read at parse time
     * @param pointsConfigs the parser's points configs; the window's bounds are written in the field's number format
     *                      so the range processor that follows reads them back exactly
     * @param ppm           the ppm floor of the half-width, see the class doc
     */
    public NumericFreeTextQueryNodeProcessor(@NotNull Supplier<List<String>> fields, @NotNull Map<String, PointsConfig> pointsConfigs, double ppm) {
        this.fields = fields;
        this.pointsConfigs = pointsConfigs;
        this.ppm = ppm;
    }

    /** The half-width of the window around {@code value} as typed in {@code text}: see the class doc. */
    public static double halfWidth(@NotNull String text, double value, double ppm) {
        int dot = text.indexOf('.');
        int decimals = dot < 0 ? 0 : text.length() - dot - 1;
        double halfUnit = Math.pow(10, -decimals) / 2;
        return Math.max(halfUnit, ppm * value / 1e6);
    }

    @Override
    protected QueryNode preProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        // exactly a plain term: the quoted, fuzzy, wildcard, regexp and range kinds are subclasses or other nodes
        if (node.getClass() != FieldQueryNode.class)
            return node;
        FieldQueryNode term = (FieldQueryNode) node;
        if (term.getField() != null) // fielded: the user said where to look
            return node;
        String text = term.getTextAsString();
        if (!NUMBER.matcher(text).matches())
            return node;
        List<String> numericFields = fields.get();
        if (numericFields.isEmpty())
            return node;

        double value = Double.parseDouble(text);
        double halfWidth = halfWidth(text, value, ppm);
        List<QueryNode> alternatives = new ArrayList<>(numericFields.size() + 1);
        for (String field : numericFields) {
            PointsConfig config = pointsConfigs.get(field);
            if (config == null)
                continue;
            String lower = config.getNumberFormat().format(value - halfWidth);
            String upper = config.getNumberFormat().format(value + halfWidth);
            alternatives.add(new TermRangeQueryNode(
                    new FieldQueryNode(field, lower, term.getBegin(), term.getEnd()),
                    new FieldQueryNode(field, upper, term.getBegin(), term.getEnd()),
                    true, true));
        }
        if (alternatives.isEmpty())
            return node;
        alternatives.add(term); // the default-field search the term always had
        return new GroupQueryNode(new OrQueryNode(alternatives));
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }
}
