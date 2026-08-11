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

import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The characterization test that gates collapsing the two query-building mechanisms into one: for a
 * matrix of filter-panel states, the query the model executes ({@link FeatureFilterModel#toLuceneQuery})
 * must be SEMANTICALLY EQUAL to the query compiled from {@link PanelQueryNodeFactory}'s nodes. Compared
 * by parsing both strings with a points-configured parser and rewriting against an empty index, so
 * {@code +/-} vs {@code AND/OR} syntax, number formatting and clause order do not matter - only meaning.
 */
public class PanelQueryNodeEquivalenceTest {

    private static final IndexSearcher EMPTY;

    static {
        try {
            EMPTY = new IndexSearcher(new MultiReader());
        } catch (java.io.IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static NumberFormat plainFormat() {
        DecimalFormat f = new DecimalFormat("0.################", DecimalFormatSymbols.getInstance(Locale.ROOT));
        f.setGroupingUsed(false);
        return f;
    }

    /** Points config for every numeric field a panel facet may emit (dynamic element/db keys added per test). */
    private static Map<String, PointsConfig> pointsConfig(String... intFields) {
        Map<String, PointsConfig> map = new HashMap<>();
        PointsConfig dbl = new PointsConfig(plainFormat(), Double.class);
        for (String f : List.of(PanelQueryNodeFactory.FIELD_MZ, PanelQueryNodeFactory.FIELD_RT_START,
                PanelQueryNodeFactory.FIELD_RT_APEX, PanelQueryNodeFactory.FIELD_RT_END,
                PanelQueryNodeFactory.FIELD_CONFIDENCE_APPROX, PanelQueryNodeFactory.FIELD_CONFIDENCE_EXACT,
                PanelQueryNodeFactory.FIELD_BLANK))
            map.put(f, dbl);
        PointsConfig integer = new PointsConfig(plainFormat(), Integer.class);
        for (String f : intFields)
            map.put(f, integer);
        return map;
    }

    private static Query parseRewrite(String query, Map<String, PointsConfig> points) throws Exception {
        StandardQueryParser parser = new StandardQueryParser(new KeywordAnalyzer());
        parser.setPointsConfigMap(points);
        return EMPTY.rewrite(parser.parse(query, "text"));
    }

    /** The panel facets compiled to a single lucene string (the core query, without inversion). */
    private static String compileCore(FeatureFilterModel model, ConfidenceDisplayMode mode) {
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(model, mode);
        List<LogicOp> ands = new ArrayList<>();
        for (int i = 1; i < nodes.size(); i++)
            ands.add(LogicOp.AND);
        return LuceneQueryCompiler.compile(new QueryContainer(nodes, ands), "");
    }

    private static void assertQueriesEquivalent(String reference, String candidate, String... intFields) {
        try {
            Map<String, PointsConfig> points = pointsConfig(intFields);
            assertEquals(parseRewrite(reference, points), parseRewrite(candidate, points),
                    "not semantically equal\n  model    : " + reference + "\n  compiled : " + candidate);
        } catch (Exception e) {
            fail("could not parse/compare\n  model    : " + reference + "\n  compiled : " + candidate + "\n  " + e, e);
        }
    }

    /**
     * Asserts the model's compiled query and the compiled panel nodes are semantically equal.
     * {@code intFields} lists the dynamic integer field names (elements/DBs) the state uses.
     */
    private static void assertEquivalent(FeatureFilterModel model, ConfidenceDisplayMode mode, String... intFields) {
        assertQueriesEquivalent(model.toLuceneQuery(mode).orElse(""), compileCore(model, mode), intFields);
    }

    private static FeatureFilterModel cleanSlate() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset();
        return model;
    }

    @Test
    public void testMz() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300);
        model.setCurrentMaxMz(400);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testMzOpenUpper() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300); // upper stays at absolute max
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testRt() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinRt(10);
        model.setCurrentMaxRt(120);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testConfidenceExact() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinConfidence(0.5);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testConfidenceApproximate() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinConfidence(0.5);
        assertEquivalent(model, ConfidenceDisplayMode.APPROXIMATE);
    }

    @Test
    public void testHasMs1AndMsMs() {
        FeatureFilterModel model = cleanSlate();
        model.setHasMs1(true);
        model.setHasMsMs(true);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testAdducts() {
        FeatureFilterModel model = cleanSlate();
        model.setAdducts(java.util.Set.of(
                de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType.fromString("[M+H]+"),
                de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType.fromString("[M+Na]+")));
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testLipidAny() {
        FeatureFilterModel model = cleanSlate();
        model.setLipidFilter(FeatureFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testLipidNo() {
        FeatureFilterModel model = cleanSlate();
        model.setLipidFilter(FeatureFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testElements() {
        FeatureFilterModel model = cleanSlate();
        model.setElementFilter(new de.unijena.bioinf.ms.gui.utils.filter.ElementFilter("CHNOPS"));
        String p = PanelQueryNodeFactory.PREFIX_ELEMENT;
        assertEquivalent(model, ConfidenceDisplayMode.EXACT,
                p + "C", p + "H", p + "N", p + "O", p + "P", p + "S");
    }

    @Test
    public void testDbHit() {
        FeatureFilterModel model = cleanSlate();
        model.setDbFilter(new de.unijena.bioinf.ms.gui.utils.filter.DbFilter(
                java.util.List.of(new io.sirius.ms.sdk.model.SearchableDatabase().databaseId("PubChem")), 5));
        assertEquivalent(model, ConfidenceDisplayMode.EXACT, PanelQueryNodeFactory.PREFIX_DB + "PubChem");
    }

    @Test
    public void testBlankFoldChange() {
        FeatureFilterModel model = cleanSlate();
        model.getSampleBlankFoldChange().setEnabled(true);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testOverallQualityDefault() {
        // fresh model WITHOUT resetting the feature quality filter -> enabled by default
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testCategorizedQuality() {
        FeatureFilterModel model = cleanSlate();
        // enable one categorized quality filter by narrowing its selection (isEnabled = strict subset)
        var categorized = model.getCategorizedQualityFilters().get(0);
        categorized.removeQuality(0);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }

    @Test
    public void testInvertedIsTheSameWrapperOnBothSides() {
        // Inversion is NOT part of the node representation - it is a wrapper the single-source
        // mechanism applies around the compiled core, exactly as the model does. Verify the wrapper
        // reproduces the model's inverted query.
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300);
        model.setAdducts(java.util.Set.of(de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType.fromString("[M+H]+")));
        model.setInverted(true);

        String reference = model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow();
        String candidate = "*:* AND NOT (" + compileCore(model, ConfidenceDisplayMode.EXACT) + ")";
        assertQueriesEquivalent(reference, candidate);
    }

    @Test
    public void testCombination() {
        FeatureFilterModel model = new FeatureFilterModel(); // keep default quality + msms active too
        model.setCurrentMinMz(300);
        model.setCurrentMinRt(10);
        model.setAdducts(java.util.Set.of(de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType.fromString("[M+H]+")));
        model.setLipidFilter(FeatureFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED);
        assertEquivalent(model, ConfidenceDisplayMode.EXACT);
    }
}
