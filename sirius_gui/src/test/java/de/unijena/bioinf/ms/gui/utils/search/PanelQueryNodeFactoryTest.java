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
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.filter.DbFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link PanelQueryNodeFactory}: active filter-panel facets render as query nodes with the real
 * lucene field names and the right structural shape (scalar range, boolean term, OR/AND groups,
 * negated clause). Also cross-checks that every field name used occurs in the model's compiled query,
 * so the hand-written mapping cannot silently drift from {@code FeatureFilterModel.toLuceneQueryBuilder}.
 */
public class PanelQueryNodeFactoryTest {

    /** Neutralizes the default-active filters (MS/MS present, feature quality) for a clean slate. */
    private static FeatureFilterModel cleanSlate() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset();
        return model;
    }

    private static List<QueryNode> nodes(FeatureFilterModel model) {
        return PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.EXACT);
    }

    private static QueryClause onlyClause(List<QueryNode> nodes) {
        assertEquals(1, nodes.size());
        assertInstanceOf(QueryClause.class, nodes.get(0));
        return (QueryClause) nodes.get(0);
    }

    private static QueryGroup onlyGroup(List<QueryNode> nodes) {
        assertEquals(1, nodes.size());
        assertInstanceOf(QueryGroup.class, nodes.get(0));
        return (QueryGroup) nodes.get(0);
    }

    @Test
    public void testMzRendersAsRealFieldRange() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300); // interior lower; the upper end stays unbounded

        QueryClause mz = onlyClause(nodes(model));
        assertEquals("ionMass", mz.field());
        assertEquals(NumberOp.RANGE_INCLUSIVE, mz.op());
        assertEquals("300", mz.value1());
        assertEquals("", mz.value2(), "an unbounded upper end is no bound at all, and renders as *");
        assertFalse(mz.negated());
    }

    @Test
    public void testRtRendersAsOrGroupOverTheThreeRtFields() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinRt(10);

        QueryGroup rt = onlyGroup(nodes(model));
        assertEquals(3, rt.items().size());
        assertEquals(List.of(LogicOp.OR, LogicOp.OR), rt.logics());
        List<String> fields = rt.items().stream().map(n -> ((QueryClause) n).field()).toList();
        assertEquals(List.of("rtStartSeconds", "rtApexSeconds", "rtEndSeconds"), fields);
    }

    @Test
    public void testConfidenceFieldFollowsTheDisplayMode() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinConfidence(0.5);

        assertEquals("topAnnotations.confidenceExactMatch",
                ((QueryClause) PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.EXACT).get(0)).field());
        assertEquals("topAnnotations.confidenceApproxMatch",
                ((QueryClause) PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.APPROXIMATE).get(0)).field());
    }

    @Test
    public void testHasMsMsRendersAsBooleanTerm() {
        FeatureFilterModel model = cleanSlate();
        model.setHasMsMs(true);

        QueryClause msms = onlyClause(nodes(model));
        assertEquals("hasMsMs", msms.field());
        assertNull(msms.op(), "a boolean term has no numeric operator");
        assertEquals("true", msms.value1());
    }

    @Test
    public void testAdductsRenderAsOrGroupOfRealFieldClauses() {
        FeatureFilterModel model = cleanSlate();
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString("[M+Na]+")));

        QueryGroup group = onlyGroup(nodes(model));
        assertEquals(2, group.items().size());
        assertEquals(List.of(LogicOp.OR), group.logics(), "the selected adducts are alternatives (OR)");
        group.items().forEach(item -> assertEquals("detectedAdducts", ((QueryClause) item).field()));
    }

    @Test
    public void testOverallQualityRendersAsQualityFieldGroup() {
        // fresh model WITHOUT resetting the feature quality filter -> it is enabled by default
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);

        List<QueryNode> nodes = nodes(model);
        assertEquals(1, nodes.size());
        List<String> fields = new ArrayList<>();
        collectFields(nodes.get(0), fields);
        assertFalse(fields.isEmpty());
        fields.forEach(f -> assertEquals("quality", f, "overall quality clauses use the 'quality' field"));
    }

    @Test
    public void testElementsRenderAsAndGroupOfPerElementRanges() {
        FeatureFilterModel model = cleanSlate();
        model.setElementFilter(new ElementFilter("CHNOPS"));

        QueryGroup group = onlyGroup(nodes(model));
        assertEquals(List.of(LogicOp.AND), group.logics().stream().distinct().toList(),
                "element constraints must all hold (AND)");
        group.items().forEach(item -> {
            QueryClause c = (QueryClause) item;
            assertTrue(c.field().startsWith("topAnnotations.formulaAnnotation.molecularFormula."), c.field());
            assertEquals(NumberOp.RANGE_INCLUSIVE, c.op());
        });
    }

    @Test
    public void testDbHitRendersWithCandidateCountRange() {
        FeatureFilterModel model = cleanSlate();
        model.setDbFilter(new DbFilter(List.of(new SearchableDatabase().databaseId("PubChem")), 5));

        // a single DB collapses the OR group to a plain clause
        QueryClause db = onlyClause(nodes(model));
        assertEquals("topAnnotations.matchedDatabases.PubChem", db.field());
        assertEquals(NumberOp.RANGE_INCLUSIVE, db.op());
        assertEquals("1", db.value1());
        assertEquals("5", db.value2());
    }

    @Test
    public void testBlankFoldChangeRendersAsOpenEndedRange() {
        FeatureFilterModel model = cleanSlate();
        model.getSampleBlankFoldChange().setEnabled(true); // default currentMinFoldChange = 2.0

        QueryClause blank = onlyClause(nodes(model));
        assertEquals(FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME, blank.field());
        assertEquals("2", blank.value1());
        assertEquals("", blank.value2(), "fold change is a lower-bounded, open-ended range");
    }

    @Test
    public void testNoLipidIsNegatedClauseAndAnyLipidIsPlain() {
        FeatureFilterModel noLipid = cleanSlate();
        noLipid.setLipidClassDetected(false);
        assertTrue(onlyClause(nodes(noLipid)).negated(), "'no lipid class' must be a NOT clause");

        FeatureFilterModel anyLipid = cleanSlate();
        anyLipid.setLipidClassDetected(true);
        assertFalse(onlyClause(nodes(anyLipid)).negated(), "'a lipid class' must be a plain clause");
    }

    @Test
    public void testInactiveModelYieldsNoNodes() {
        assertTrue(nodes(cleanSlate()).isEmpty());
    }

    // --- pfas tag filter ---

    @Test
    public void testPfasTaggedIsAnOpenRangeOnTheTagField() {
        FeatureFilterModel model = cleanSlate();
        model.setPfasDetected(true);

        QueryClause pfas = onlyClause(nodes(model));
        assertEquals(FeatureFilterModel.FIELD_PFAS, pfas.field());
        assertEquals(NumberOp.RANGE_INCLUSIVE, pfas.op());
        assertFalse(pfas.negated());
        assertEquals("tags.pfas:[* TO *]", model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow(),
                "a bare wildcard would be rejected by the parser, an open range is not");
    }

    @Test
    public void testNoPfasIsTheNegatedRangeAndNeedsTheAnchor() {
        FeatureFilterModel model = cleanSlate();
        model.setPfasDetected(false);

        assertTrue(onlyClause(nodes(model)).negated(), "'no pfas' must be a NOT clause");
        assertEquals("*:* AND NOT tags.pfas:[* TO *]",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    @Test
    public void testUnsetPfasFilterYieldsNoFacet() {
        FeatureFilterModel model = cleanSlate();
        model.setPfasDetected(null);
        assertTrue(nodes(model).isEmpty());
    }

    // --- the user query runs in its executable form ---

    @Test
    public void testTheUserQueryIsAnchoredWhereLuceneNeedsIt() throws Exception {
        FeatureFilterModel model = cleanSlate();
        model.setExecutableQueryForm(QueryStringParser::executableForm);
        searchText(model, "NOT computedTools.zodiac:true");
        assertEquals("*:* AND NOT computedTools.zodiac:true",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow(),
                "a query that only excludes matches nothing without the anchor");

        // with facets, the anchor has to stay INSIDE the user block: (facets) AND (NOT x) matches nothing
        model.setPfasDetected(true);
        assertEquals("(tags.pfas:[* TO *]) AND (*:* AND NOT computedTools.zodiac:true)",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    @Test
    public void testWithoutTheStrategyTheQueryRunsAsWritten() throws Exception {
        // the rule lives in the search package and is handed in (GuiProjectManager does it); a model that was
        // never given it must not silently rewrite anything
        FeatureFilterModel model = cleanSlate();
        searchText(model, "NOT computedTools.zodiac:true");
        assertEquals("NOT computedTools.zodiac:true",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    // --- adducts: the unknown ion types are part of the offer ---

    @Test
    public void testTheUnknownAdductOfEveryDetectedPolarityIsOfferedAndPreselected() {
        FeatureFilterModel model = cleanSlate();
        model.updateAdducts(List.of(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString("[M+Na]+")));

        // a feature whose adduct could not be determined is indexed as [M+?]+, so leaving it out of the
        // list would drop those features from every default-filtered view
        assertTrue(model.getPossibleAdducts().contains(PrecursorIonType.unknownPositive()));
        assertTrue(model.getSelectedAdducts().contains(PrecursorIonType.unknownPositive()),
                "and it has to be selected by default, or the default filter still drops them");
        // the polarity follows the project - nothing negative was detected here
        assertFalse(model.getPossibleAdducts().contains(PrecursorIonType.unknownNegative()));
    }

    @Test
    public void testWithNothingDetectedBothUnknownAdductsAreOffered() {
        FeatureFilterModel model = cleanSlate();
        model.updateAdducts(List.of());
        // nothing to infer the polarity from (a project that was never preprocessed for adducts), and the
        // one that does not occur matches nothing anyway
        assertTrue(model.getSelectedAdducts().containsAll(
                Set.of(PrecursorIonType.unknownPositive(), PrecursorIonType.unknownNegative())));
    }

    @Test
    public void testTheUnknownAdductReachesTheCompiledQuery() {
        FeatureFilterModel model = cleanSlate();
        model.updateAdducts(List.of(PrecursorIonType.fromString("[M+H]+")));
        String compiled = model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow();
        assertTrue(compiled.contains(FeatureFilterModel.FIELD_ADDUCTS), compiled);
        // the value the index holds for such a feature, escaped the way a bare term has to be
        assertTrue(compiled.contains("?"), "the unknown adduct is missing from the query: " + compiled);
    }

    // --- the joint between the panel facets and the user's own query ---

    private static void searchText(FeatureFilterModel model, String text) throws Exception {
        model.getSearchTextDoc().insertString(0, text, null);
    }

    @Test
    public void testTheFacetsNarrowTheUserQueryByDefault() throws Exception {
        FeatureFilterModel model = cleanSlate();
        model.setPfasDetected(true);
        searchText(model, "name:caffeine");
        assertEquals("(tags.pfas:[* TO *]) AND (name:caffeine)",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    @Test
    public void testTheUserQueryCanBeAnAlternativeToTheFacets() throws Exception {
        FeatureFilterModel model = cleanSlate();
        model.setPfasDetected(true);
        searchText(model, "name:caffeine");
        model.setQueryJoin(LogicOp.OR);
        // the panel facets stop narrowing: a feature matching the query alone passes too
        assertEquals("(tags.pfas:[* TO *]) OR (name:caffeine)",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());

        // inversion still wraps the whole thing, joint included
        model.setInverted(true);
        assertEquals("*:* AND NOT ((tags.pfas:[* TO *]) OR (name:caffeine))",
                model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    @Test
    public void testTheJointIsMootWithoutAUserQuery() {
        FeatureFilterModel model = cleanSlate();
        model.setPfasDetected(true);
        model.setQueryJoin(LogicOp.OR);
        assertEquals("tags.pfas:[* TO *]", model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow(),
                "an OR with nothing on the other side is not an alternative");
    }

    @Test
    public void testResetRestoresTheDefaultJoint() {
        FeatureFilterModel model = cleanSlate();
        model.setQueryJoin(LogicOp.OR);
        model.resetFilter();
        assertEquals(LogicOp.AND, model.getQueryJoin());
    }

    @Test
    public void testUsedFieldNamesOccurInTheModelsCompiledQuery() {
        // activate a broad set of facets together (feature quality stays enabled by default)
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.setCurrentMinMz(300);
        model.setCurrentMinRt(10);
        model.setCurrentMinConfidence(0.5);
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+")));
        model.setElementFilter(new ElementFilter("CHNOPS"));
        model.setDbFilter(new DbFilter(List.of(new SearchableDatabase().databaseId("PubChem")), 5));
        model.getSampleBlankFoldChange().setEnabled(true);
        model.setLipidClassDetected(false);

        String compiled = model.toLuceneQuery(ConfidenceDisplayMode.EXACT)
                .orElseThrow(() -> new AssertionError("model with active filters must compile to a query"));

        List<String> usedFields = new ArrayList<>();
        PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.EXACT).forEach(node -> collectFields(node, usedFields));
        assertFalse(usedFields.isEmpty());
        usedFields.forEach(field -> assertTrue(compiled.contains(field),
                "field '" + field + "' used by a chip is not in the model's compiled query: " + compiled));
    }

    private static void collectFields(QueryNode node, List<String> out) {
        if (node instanceof QueryClause clause) {
            if (!clause.field().isEmpty())
                out.add(clause.field());
        } else if (node instanceof QueryGroup group) {
            group.items().forEach(item -> collectFields(item, out));
        }
    }
}
